package io.github.abansal755.spring_query;

import cz.jirutka.rsql.parser.ast.*;
import io.github.abansal755.spring_query.annotation.FieldMapping;
import io.github.abansal755.spring_query.annotation.RsqlFilterable;
import io.github.abansal755.spring_query.enums.RsqlOperator;
import io.github.abansal755.spring_query.exception.QueryException;
import io.github.abansal755.spring_query.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
 * new ValidationRSQLVisitor(User.class).visit(root);
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
    private final Map<String, FieldMapping> fieldMappings;
    private final Map<String, FieldMapping> originalFieldMappings;

    public ValidationRSQLVisitor(Class<?> entityClass, FieldMapping[] fieldMappings) {
        this.entityClass = entityClass;
        // Map from name to FieldMapping
        this.fieldMappings = Arrays
                .stream(fieldMappings)
                .collect(Collectors.toMap(FieldMapping::name, mapping -> mapping));
        // Map from original field name to FieldMapping
        this.originalFieldMappings = Arrays
                .stream(fieldMappings)
                .collect(Collectors.toMap(FieldMapping::field, mapping -> mapping));
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
     * @throws QueryException if the field does not exist, is not filterable,
     *                       or the operator is not allowed
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
     * @throws QueryException if the field is not allowed or operator is invalid
     */
    private void validate(ComparisonNode node) {
        // Extract the field name and operator from the RSQL node
        String fieldName = node.getSelector();
        ComparisonOperator operator = node.getOperator();

        // Find if there exists a field mapping with original field name and throw error if use is not allowed
        FieldMapping orginalFieldMapping = originalFieldMappings.get(fieldName);
        if(orginalFieldMapping != null && !orginalFieldMapping.allowOriginalFieldName())
            throw new QueryException(MessageFormat.format(
                    "Unknown field ''{0}''", fieldName
            ));

        // Find original field name if field mapping exists to correctly find the field
        FieldMapping fieldMapping = fieldMappings.get(fieldName);
        if(fieldMapping != null) fieldName = fieldMapping.field();

        // Resolve the Field object from the entity class using reflection
        Field field = ReflectionUtil.resolveField(entityClass, fieldName);
        // Retrieve the RsqlFilterable annotation on the field (if present)
        RsqlFilterable filterable = field.getAnnotation(RsqlFilterable.class);
        // Throw exception if the field is not annotated as filterable
        if(filterable == null) throw new QueryException(MessageFormat.format(
                "Filtering not allowed on field ''{0}''", fieldName
        ));

        // Collect the set of allowed operators for this field from the annotation
        Set<ComparisonOperator> allowedOperators = Arrays
                .stream(filterable.operators()) // Get operators defined in annotation
                .map(RsqlOperator::getOperator) // Map to ComparisonOperator
                .collect(Collectors.toSet()); // Collect into a Set for fast lookup
        // Throw exception if the provided operator is not in the allowed set
        if(!allowedOperators.contains(operator)) throw new QueryException(MessageFormat.format(
                "Operator ''{0}'' not allowed on field ''{1}''", operator, fieldName
        ));
    }
}
