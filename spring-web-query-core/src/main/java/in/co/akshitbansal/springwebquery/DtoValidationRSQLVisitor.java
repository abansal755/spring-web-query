package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.*;
import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;

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
     * Creates a DTO-aware validation visitor.
     *
     * @param entityClass target entity type used for final path validation
     * @param dtoClass DTO type used to validate incoming selector paths
     * @param annotationUtil helper for resolving allowed operators from annotations
     */
    public DtoValidationRSQLVisitor(Class<?> entityClass, Class<?> dtoClass, AnnotationUtil annotationUtil) {
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
        this.fieldMappings = new HashMap<>();
        this.annotationUtil = annotationUtil;
    }

    /**
     * Returns immutable selector mappings generated while visiting nodes.
     *
     * @return map from request DTO path to resolved entity path
     */
    public Map<String, String> getFieldMappings() {
        return Map.copyOf(fieldMappings);
    }

    /**
     * Visits a logical AND node and validates each child expression.
     *
     * @param node AND node
     * @param param unused visitor parameter
     * @return {@code null}
     */
    @Override
    public Void visit(AndNode node, Void param) {
        node.forEach(child -> child.accept(this));
        return null;
    }

    /**
     * Visits a logical OR node and validates each child expression.
     *
     * @param node OR node
     * @param param unused visitor parameter
     * @return {@code null}
     */
    @Override
    public Void visit(OrNode node, Void param) {
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

        // Resolve the field path in the DTO class
        List<Field> dtoFields;
        try {
            dtoFields = ReflectionUtil.resolveFieldPath(dtoClass, dtoPath);
        }
        catch (Exception ex) {
            throw new QueryFieldValidationException(MessageFormat.format(
                    "Unknown field ''{0}''", dtoPath
            ), dtoPath, ex);
        }
        // Validate the last field in the path for filterability and allowed operators
        validateField(dtoFields.getLast(), operator, dtoPath);


        // Construct the corresponding entity field path using the @MapsTo annotation if present
        List<String> entityPathSegments = new ArrayList<>();
        for(Field dtoField : dtoFields) {
            MapsTo mapsToAnnotation = dtoField.getAnnotation(MapsTo.class);
            if(mapsToAnnotation == null) entityPathSegments.add(dtoField.getName());
            else {
                if(mapsToAnnotation.absolute()) entityPathSegments.clear();
                entityPathSegments.add(mapsToAnnotation.field());
            }
        }
        String entityPath = String.join(".", entityPathSegments);
        // Validate that the constructed entity field path is resolvable in the entity class
        try {
            ReflectionUtil.resolveField(entityClass, entityPath);
        }
        catch (Exception ex) {
            throw new QueryConfigurationException(MessageFormat.format(
                    "Unable to resolve entity field path ''{0}'' mapped from DTO path ''{1}''", entityPath, dtoPath
            ), ex);
        }

        // Store the mapping from DTO path to entity path for later use during query construction
        fieldMappings.put(dtoPath, entityPath);
    }

    /**
     * Validates filterability and allowed operators for a DTO field.
     *
     * @param field terminal DTO field
     * @param operator requested comparison operator
     * @param fieldPath original request selector path
     */
    private void validateField(Field field, ComparisonOperator operator, String fieldPath) {
        // Retrieve the RsqlFilterable annotation on the field (if present)
        RsqlFilterable filterable = field.getAnnotation(RsqlFilterable.class);
        // Throw exception if the field is not annotated as filterable
        if(filterable == null) throw new QueryFieldValidationException(MessageFormat.format(
                "Filtering not allowed on field ''{0}''", fieldPath
        ), fieldPath);

        // Throw exception if the provided operator is not in the allowed set
        Set<ComparisonOperator> allowedOperators = annotationUtil.getAllowedOperators(filterable);
        if(!allowedOperators.contains(operator)) {
            throw new QueryForbiddenOperatorException(
                    MessageFormat.format("Operator ''{0}'' not allowed on field ''{1}''", operator, fieldPath),
                    fieldPath,
                    operator,
                    allowedOperators
            );
        }
    }
}
