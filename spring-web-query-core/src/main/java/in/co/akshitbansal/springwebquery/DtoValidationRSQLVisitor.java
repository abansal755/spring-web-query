package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.*;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import in.co.akshitbansal.springwebquery.util.FieldResolvingUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * RSQL AST visitor that validates filters against a DTO contract and maps DTO
 * property paths to entity property paths.
 *
 * <p>This visitor is used when {@link in.co.akshitbansal.springwebquery.annotation.WebQuery#dtoClass()}
 * is configured. It enforces that:</p>
 * <ul>
 *     <li>The requested selector exists on the DTO type.</li>
 *     <li>The terminal DTO field is annotated with
 *         {@link in.co.akshitbansal.springwebquery.annotation.RsqlFilterable}.</li>
 *     <li>The requested operator is allowed for that DTO field.</li>
 *     <li>The resulting mapped entity path can be resolved on the configured entity type.</li>
 * </ul>
 *
 * <p>During validation, selector mappings are captured and exposed via
 * {@link #getFieldMappings()} so downstream query construction can apply
 * DTO-to-entity path translation.</p>
 */
public class DtoValidationRSQLVisitor implements RSQLVisitor<Void, Void> {

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
     * Helper used to resolve allowed operators from annotation metadata.
     */
    private final AnnotationUtil annotationUtil;

    /**
     * Whether logical AND operator is allowed in the query.
     */
    private final boolean andNodeAllowed;

    /**
     * Whether logical OR operator is allowed in the query.
     */
    private final boolean orNodeAllowed;

    /**
     * Creates a DTO-aware validation visitor.
     *
     * @param entityClass target entity type used for final path validation
     * @param dtoClass DTO type used to validate incoming selector paths
     * @param annotationUtil helper for resolving allowed operators from annotations
     * @param andNodeAllowed whether logical AND operator is allowed
     * @param orNodeAllowed whether logical OR operator is allowed
     */
    public DtoValidationRSQLVisitor(Class<?> entityClass, Class<?> dtoClass, AnnotationUtil annotationUtil, boolean andNodeAllowed, boolean orNodeAllowed) {
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
        this.fieldMappings = new HashMap<>();
        this.annotationUtil = annotationUtil;
        this.andNodeAllowed = andNodeAllowed;
        this.orNodeAllowed = orNodeAllowed;
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
     * Visits a logical AND node and validates whether it is allowed.
     *
     * @param node AND node
     * @param param unused visitor parameter
     * @return {@code null}
     * @throws QueryValidationException if AND operator is not allowed
     */
    @Override
    public Void visit(AndNode node, Void param) {
        if(!andNodeAllowed) throw new QueryValidationException("Logical AND operator is not allowed");
        node.forEach(child -> child.accept(this));
        return null;
    }

    /**
     * Visits a logical OR node and validates whether it is allowed.
     *
     * @param node OR node
     * @param param unused visitor parameter
     * @return {@code null}
     * @throws QueryValidationException if OR operator is not allowed
     */
    @Override
    public Void visit(OrNode node, Void param) {
        if(!orNodeAllowed) throw new QueryValidationException("Logical OR operator is not allowed");
        node.forEach(child -> child.accept(this));
        return null;
    }

    /**
     * Visits a comparison node and validates selector/operator compatibility.
     *
     * @param node comparison node
     * @param param unused visitor parameter
     * @return {@code null}
     */
    @Override
    public Void visit(ComparisonNode node, Void param) {
        validate(node);
        return null;
    }

    /**
     * Validates a single comparison expression and records DTO-to-entity mapping.
     *
     * @param node node to validate
     */
    private void validate(ComparisonNode node) {
        // Extract the field name and operator from the RSQL node
        String dtoPath = node.getSelector();
        ComparisonOperator operator = node.getOperator();

        // Build the corresponding entity field path from the DTO path and validate the terminal field for filterability
        String entityPath = FieldResolvingUtil.buildEntityPathFromDtoPath(
                entityClass,
                dtoClass,
                dtoPath,
                terminalField -> annotationUtil.validateFilterableField(terminalField, operator, dtoPath)
        );

        // Store the mapping from DTO path to entity path for later use during query construction
        fieldMappings.put(dtoPath, entityPath);
    }
}
