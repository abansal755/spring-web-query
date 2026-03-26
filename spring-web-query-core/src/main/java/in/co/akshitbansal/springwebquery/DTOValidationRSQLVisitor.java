package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.util.FieldResolvingUtil;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * RSQL AST visitor that validates filters against a DTO contract and maps DTO
 * property paths to entity property paths.
 *
 * <p>This visitor is used when {@link in.co.akshitbansal.springwebquery.annotation.WebQuery#dtoClass()}
 * is configured. It enforces that:</p>
 * <ul>
 *     <li>The requested selector exists on the DTO type.</li>
 *     <li>The terminal DTO field is annotated with
 *         {@link RSQLFilterable}.</li>
 *     <li>The requested operator is allowed for that DTO field.</li>
 *     <li>The resulting mapped entity path can be resolved on the configured entity type.</li>
 * </ul>
 *
 * <p>During validation, selector mappings are captured and exposed via
 * {@link #getFieldMappings()} so downstream query construction can apply
 * DTO-to-entity path translation.</p>
 */
public class DTOValidationRSQLVisitor extends ValidationRSQLVisitor {

    /**
     * Target entity type used for mapped-path validation.
     */
    private final Class<?> entityClass;

    /**
     * DTO type used as the external query contract.
     */
    private final Class<?> dtoClass;

    /**
     * Mutable selector map accumulated during traversal.
     */
    private final Map<String, String> fieldMappings;

    /**
     * Creates a DTO-aware validation visitor.
     *
     * @param entityClass target entity type used for final path validation
     * @param dtoClass DTO type used to validate incoming selector paths
     * @param annotationUtil helper for resolving allowed operators from annotations
     * @param andNodeAllowed whether logical AND operator is allowed
     * @param orNodeAllowed whether logical OR operator is allowed
     * @param maxDepth maximum allowed depth for the RSQL AST
     */
    public DTOValidationRSQLVisitor(
            Class<?> entityClass,
            Class<?> dtoClass,
            Set<? extends RSQLCustomOperator<?>> customOperators,
            boolean andNodeAllowed,
            boolean orNodeAllowed,
            int maxDepth
    ) {
        super(customOperators, andNodeAllowed, orNodeAllowed, maxDepth);
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
        this.fieldMappings = new HashMap<>();
    }

    /**
     * Returns immutable selector mappings generated while visiting nodes.
     *
     * @return map from request DTO path to resolved entity path
     */
    public Map<String, String> getFieldMappings() {
        return Collections.unmodifiableMap(fieldMappings);
    }

    /**
     * Validates a single comparison expression and records DTO-to-entity mapping.
     *
     * @param node node to validate
     */
    @Override
    protected void validateComparisonNode(ComparisonNode node) {
        // Extract the field name and operator from the RSQL node
        String dtoPath = node.getSelector();
        ComparisonOperator operator = node.getOperator();

        // Build the corresponding entity field path from the DTO path and validate the terminal field for filterability
        String entityPath = FieldResolvingUtil.buildEntityPathFromDtoPath(
                entityClass,
                dtoClass,
                dtoPath,
                terminalField -> filterableFieldValidator.validate(new FilterableFieldValidator.Field(terminalField, operator, dtoPath))
        );

        // Store the mapping from DTO path to entity path for later use during query construction
        fieldMappings.put(dtoPath, entityPath);
    }
}
