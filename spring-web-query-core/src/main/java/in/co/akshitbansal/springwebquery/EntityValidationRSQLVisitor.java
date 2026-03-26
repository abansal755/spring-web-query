package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.util.FieldResolvingUtil;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RSQL AST visitor that validates RSQL queries against a given entity class.
 * <p>
 * This visitor traverses the Abstract Syntax Tree (AST) produced by the RSQL parser
 * and ensures that:
 * <ul>
 *     <li>All fields referenced in the query exist on the entity class</li>
 *     <li>Only fields annotated with {@link RSQLFilterable} are filterable</li>
 *     <li>Only allowed RSQL operators (as defined in the {@link RSQLFilterable} annotation) are used</li>
 * </ul>
 * <p>
 * If any violation is detected, a {@link QueryException} is thrown describing the
 * invalid field or operator.
 * </p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * Node root = new RSQLParser().parse("status==ACTIVE;age>30");
 * EntityValidationRSQLVisitor visitor =
 *         new EntityValidationRSQLVisitor(User.class, new FieldMapping[0], annotationUtil, true, false, 1);
 * root.accept(visitor, NodeMetadata.of(0));
 * }</pre>
 *
 * <p>This visitor is typically used in combination with
 * {@link io.github.perplexhub.rsql.RSQLJPASupport} to ensure that only valid queries are converted into
 * Spring Data JPA {@link org.springframework.data.jpa.domain.Specification}s.</p>
 *
 * @see RSQLFilterable
 * @see RSQLDefaultOperator
 * @see cz.jirutka.rsql.parser.ast.Node
 */
public class EntityValidationRSQLVisitor extends ValidationRSQLVisitor {

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
     * Creates a new entity validation visitor with the specified configuration.
     *
     * @param entityClass    the entity class to validate against
     * @param fieldMappings  array of field mappings (aliases) to consider
     * @param annotationUtil helper for annotation resolution and operator checks
     * @param andNodeAllowed whether logical AND operator is allowed
     * @param orNodeAllowed whether logical OR operator is allowed
     * @param maxDepth maximum allowed depth for the RSQL AST
     */
    public EntityValidationRSQLVisitor(
            Class<?> entityClass,
            FieldMapping[] fieldMappings,
            Map<Class<?>, RSQLCustomOperator<?>> customOperators,
            boolean andNodeAllowed,
            boolean orNodeAllowed,
            int maxDepth
    ) {
        super(customOperators, andNodeAllowed, orNodeAllowed, maxDepth);
        this.entityClass = entityClass;
        // Map from name to FieldMapping
        this.fieldMappings = Collections.unmodifiableMap(Arrays
                .stream(fieldMappings)
                .collect(Collectors.toMap(
                        FieldMapping::name,
                        mapping -> mapping,
                        // Should not happen due to validation in AnnotationUtil
                        (existing, duplicate) -> existing,
                        HashMap::new
                )));
        // Map from original field name to FieldMapping
        this.originalFieldMappings = Collections.unmodifiableMap(Arrays
                .stream(fieldMappings)
                .collect(Collectors.toMap(
                        FieldMapping::field,
                        mapping -> mapping,
                        // Should not happen due to validation in AnnotationUtil
                        (existing, duplicate) -> existing,
                        HashMap::new
                )));
    }

    /**
     * Validates a comparison node against the entity class.
     *
     * @param node the comparison node to validate
     * @throws QueryValidationException if the field is not allowed or operator is invalid
     * @throws QueryConfigurationException if the field mapping is misconfigured
     */
    @Override
    protected void validateComparisonNode(ComparisonNode node) {
        // Extract the field name and operator from the RSQL node
        String reqFieldName = node.getSelector();
        ComparisonOperator operator = node.getOperator();

        // Resolve the field on the entity class using the requested field name and field mappings
        FieldResolvingUtil.resolveEntityPath(
                entityClass,
                reqFieldName,
                fieldMappings,
                originalFieldMappings,
                terminalField -> filterableFieldValidator.validate(new FilterableFieldValidator.Field(terminalField, operator, reqFieldName))
        );
    }
}
