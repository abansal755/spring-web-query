package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.resolver.EntityAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.FieldResolver;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RSQL AST visitor that validates selectors directly against an entity model.
 *
 * <p>This visitor resolves request selectors to entity fields, applies any
 * configured {@link FieldMapping} aliases, and ensures that the resolved
 * terminal field is filterable for the requested operator.</p>
 *
 * <p>It is typically used by entity-aware specification resolvers before
 * converting a parsed RSQL tree into a Spring Data JPA
 * {@link org.springframework.data.jpa.domain.Specification}.</p>
 *
 * @see RSQLFilterable
 * @see RSQLDefaultOperator
 * @see cz.jirutka.rsql.parser.ast.Node
 */
public class EntityValidationRSQLVisitor extends ValidationRSQLVisitor {

    /**
     * Map from alias field names to their corresponding {@link FieldMapping}.
     */
    private final Map<String, FieldMapping> fieldMappings;

    /**
     * Map from original entity field names to their corresponding {@link FieldMapping}.
     */
    private final Map<String, FieldMapping> originalFieldMappings;

    private final FieldResolver fieldResolver;

    /**
     * Creates a new entity validation visitor with the specified configuration.
     *
     * @param entityClass    the entity class to validate against
     * @param fieldMappings  array of field mappings (aliases) to consider
     * @param customOperators registered custom operators keyed by implementation class
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
        // Map from name to FieldMapping
        this.fieldMappings = Collections.unmodifiableMap(Arrays
                .stream(fieldMappings)
                .collect(Collectors.toMap(
                        FieldMapping::name,
                        mapping -> mapping,
                        // Should not happen because mappings are validated before visitor construction
                        (existing, duplicate) -> existing,
                        HashMap::new
                )));
        // Map from original field name to FieldMapping
        this.originalFieldMappings = Collections.unmodifiableMap(Arrays
                .stream(fieldMappings)
                .collect(Collectors.toMap(
                        FieldMapping::field,
                        mapping -> mapping,
                        // Should not happen because mappings are validated before visitor construction
                        (existing, duplicate) -> existing,
                        HashMap::new
                )));
        this.fieldResolver = new EntityAwareFieldResolver(entityClass, this.fieldMappings, this.originalFieldMappings);
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
        fieldResolver.resolvePathAndValidateTerminalField(
                reqFieldName,
                terminalField -> filterableFieldValidator.validate(new FilterableFieldValidator.Field(terminalField, operator, reqFieldName))
        );
    }
}
