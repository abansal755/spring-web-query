package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.*;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RSQL AST visitor that validates RSQL queries against a given entity class.
 * <p>
 * This visitor traverses the Abstract Syntax Tree (AST) produced by the RSQL parser
 * and ensures that:
 * <ul>
 *     <li>All fields referenced in the query exist on the entity class</li>
 *     <li>Only fields annotated with {@link RsqlFilterable} are filterable</li>
 *     <li>Only allowed RSQL operators (as defined in the {@link RsqlFilterable} annotation) are used</li>
 * </ul>
 * <p>
 * If any violation is detected, a {@link QueryException} is thrown describing the
 * invalid field or operator.
 * </p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * Node root = new RSQLParser().parse("status==ACTIVE;age>30");
 * new ValidationRSQLVisitor(User.class, new FieldMapping[0], Set.of()).visit(root);
 * }</pre>
 *
 * <p>This visitor is typically used in combination with
 * {@link io.github.perplexhub.rsql.RSQLJPASupport} to ensure that only valid queries are converted into
 * Spring Data JPA {@link org.springframework.data.jpa.domain.Specification}s.</p>
 *
 * @see RsqlFilterable
 * @see RsqlOperator
 * @see cz.jirutka.rsql.parser.ast.Node
 */
public class ValidationRSQLVisitor implements RSQLVisitor<Void, Void> {

    /**
     * The entity class against which RSQL queries are validated.
     */
    private final Class<?> entityClass;

    /**
     * Map from alias field names to their corresponding {@link FieldMapping}.
     */
    private final Map<String, FieldMapping> fieldMappings;

    /**
     * Map from original entity field names to their corresponding {@link FieldMapping}.
     */
    private final Map<String, FieldMapping> originalFieldMappings;

    /**
     * Map from custom operator class types to their actual implementations.
     */
    private final Map<Class<?>, RsqlCustomOperator<?>> customOperators;

    /**
     * Creates a new ValidationRSQLVisitor with the specified configuration.
     *
     * @param entityClass    the entity class to validate against
     * @param fieldMappings  array of field mappings (aliases) to consider
     * @param customOperators set of custom operators to allow in queries
     */
    public ValidationRSQLVisitor(Class<?> entityClass, FieldMapping[] fieldMappings, Set<? extends RsqlCustomOperator<?>> customOperators) {
        this.entityClass = entityClass;
        // Map from name to FieldMapping
        this.fieldMappings = Arrays
                .stream(fieldMappings)
                .collect(Collectors.toMap(FieldMapping::name, mapping -> mapping));
        // Map from original field name to FieldMapping
        this.originalFieldMappings = Arrays
                .stream(fieldMappings)
                .collect(Collectors.toMap(FieldMapping::field, mapping -> mapping));
        // Map from custom operator class to instance
        this.customOperators = customOperators
                .stream()
                .collect(Collectors.toMap(RsqlCustomOperator::getClass, operator -> operator));
    }

    /**
     * Visits an {@link AndNode} in the RSQL AST and recursively validates all child nodes.
     *
     * @param andNode the AND node
     * @param unused  unused parameter
     * @return null
     */
    @Override
    public Void visit(AndNode andNode, Void unused) {
        andNode.forEach(node -> node.accept(this));
        return null;
    }

    /**
     * Visits an {@link OrNode} in the RSQL AST and recursively validates all child nodes.
     *
     * @param orNode the OR node
     * @param unused unused parameter
     * @return null
     */
    @Override
    public Void visit(OrNode orNode, Void unused) {
        orNode.forEach(node -> node.accept(this));
        return null;
    }

    /**
     * Visits a {@link ComparisonNode} in the RSQL AST and validates the field
     * and operator against the entity class.
     *
     * @param comparisonNode the comparison node
     * @param unused         unused parameter
     * @return null
     * @throws QueryValidationException if the field does not exist, is not filterable,
     *                       or the operator is not allowed
     * @throws QueryConfigurationException if a custom operator or field mapping is misconfigured
     */
    @Override
    public Void visit(ComparisonNode comparisonNode, Void unused) {
        validate(comparisonNode);
        return null;
    }

    /**
     * Validates a comparison node against the entity class.
     *
     * @param node the comparison node to validate
     * @throws QueryValidationException if the field is not allowed or operator is invalid
     * @throws QueryConfigurationException if the field mapping is misconfigured
     */
    private void validate(ComparisonNode node) {
        // Extract the field name and operator from the RSQL node
        String reqFieldName = node.getSelector();
        String fieldName = reqFieldName; // Actual entity path to validate against, may be rewritten if field mapping exists
        ComparisonOperator operator = node.getOperator();

        // Find if there exists a field mapping with original field name and throw error if use is not allowed
        FieldMapping originalFieldMapping = originalFieldMappings.get(reqFieldName);
        if(originalFieldMapping != null && !originalFieldMapping.allowOriginalFieldName())
            throw new QueryValidationException(MessageFormat.format(
                    "Unknown field ''{0}''", reqFieldName
            ));

        // Find original field name if field mapping exists to correctly find the field
        FieldMapping fieldMapping = fieldMappings.get(reqFieldName);
        if(fieldMapping != null) fieldName = fieldMapping.field();

        // Resolve the Field object from the entity class using reflection
        Field field;
        try {
            field = ReflectionUtil.resolveField(entityClass, fieldName);
        }
        catch (Exception ex) {
            throw new QueryValidationException(MessageFormat.format(
                    "Unknown field ''{0}''", reqFieldName
            ), ex);
        }

        // Retrieve the RsqlFilterable annotation on the field (if present)
        RsqlFilterable filterable = field.getAnnotation(RsqlFilterable.class);
        // Throw exception if the field is not annotated as filterable
        if(filterable == null) throw new QueryValidationException(MessageFormat.format(
                "Filtering not allowed on field ''{0}''", reqFieldName
        ));

        // Combine default and custom operators into a set for quick lookup
        Set<ComparisonOperator> allowedOperators = getAllowedOperators(filterable);

        // Throw exception if the provided operator is not in the allowed set
        if(!allowedOperators.contains(operator)) throw new QueryValidationException(MessageFormat.format(
                "Operator ''{0}'' not allowed on field ''{1}''", operator, reqFieldName
        ));
    }

    /**
     * Computes the full set of operators allowed for a field by combining
     * built-in operators and registered custom operators referenced by
     * {@link RsqlFilterable}.
     *
     * @param filterable field-level filterability metadata
     * @return allowed comparison operators for the field
     * @throws QueryConfigurationException if a referenced custom operator is not registered
     */
    private Set<ComparisonOperator> getAllowedOperators(@NonNull RsqlFilterable filterable) {
        // Collect the set of allowed operators for this field from the annotation
        // Stream of default operators defined in the annotation
        Stream<ComparisonOperator> defaultOperators = Arrays
                .stream(filterable.operators())
                .map(RsqlOperator::getOperator);
        // Stream of custom operators defined in the annotation
        // Note: The annotation references classes, which are looked up in the customOperators map
        Stream<ComparisonOperator> customOperators = Arrays
                .stream(filterable.customOperators())
                .map(this::getCustomOperator)
                .map(RsqlCustomOperator::getComparisonOperator);
        return Stream
                .concat(defaultOperators, customOperators)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves the custom operator instance for the given operator class.
     *
     * @param clazz the custom operator class to look up
     * @return the registered custom operator instance
     * @throws QueryConfigurationException if the custom operator class is not registered
     */
    private RsqlCustomOperator<?> getCustomOperator(@NonNull Class<?> clazz) {
        RsqlCustomOperator<?> operator = customOperators.get(clazz);
        if(operator == null) throw new QueryConfigurationException(MessageFormat.format(
                "Custom operator ''{0}'' referenced in @RsqlFilterable is not registered", clazz.getSimpleName()
        ));
        return operator;
    }
}
