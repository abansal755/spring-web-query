package in.co.akshitbansal.springwebquery.util;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Shared field/path resolution helpers used by DTO-aware and entity-aware
 * query validation flows.
 *
 * <p>This utility centralizes:</p>
 * <ul>
 *     <li>DTO selector path resolution and DTO-to-entity path translation.</li>
 *     <li>Entity selector resolution with {@link FieldMapping} alias handling.</li>
 *     <li>Terminal field validation hooks supplied by callers.</li>
 * </ul>
 *
 * <p>Methods throw library-specific query exceptions so resolver and visitor
 * layers can surface consistent validation/configuration errors.</p>
 */
// Private constructor to prevent instantiation
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class FieldResolvingUtil {

    /**
     * Resolves a DTO selector path, validates its terminal DTO field, maps it to
     * an entity path, and validates that the mapped path exists on the entity.
     *
     * @param entityClass entity type used to validate mapped path existence
     * @param dtoClass DTO type used to resolve incoming selector path
     * @param dtoPath incoming selector path from request
     * @param terminalFieldValidator validation callback for the terminal DTO field
     * @return mapped entity path corresponding to {@code dtoPath}
     * @throws QueryFieldValidationException if the DTO selector cannot be resolved
     * @throws QueryConfigurationException if the mapped entity path cannot be resolved
     */
    public static String buildEntityPathFromDtoPath(
            Class<?> entityClass,
            Class<?> dtoClass,
            String dtoPath,
            Consumer<Field> terminalFieldValidator)
    {
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
        // Validate the last field in the path using the provided terminal field validator
        terminalFieldValidator.accept(dtoFields.getLast());

        // Construct the corresponding entity field path using the @MapsTo annotation if present
        List<String> entityPathSegments = new ArrayList<>();
        for(Field dtoField : dtoFields) {
            MapsTo mapsToAnnotation = dtoField.getAnnotation(MapsTo.class);
            if(mapsToAnnotation == null) entityPathSegments.add(dtoField.getName());
            else {
                if(mapsToAnnotation.absolute()) entityPathSegments.clear();
                entityPathSegments.add(mapsToAnnotation.value());
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
        return entityPath;
    }

    /**
     * Resolves an incoming entity-aware selector path by applying configured
     * {@link FieldMapping} aliases and validating the resolved terminal field.
     *
     * @param entityClass entity type used for path resolution
     * @param reqFieldPath incoming selector path from request
     * @param fieldMappingMap mappings keyed by alias name
     * @param originalFieldNameMap mappings keyed by original entity field path
     * @param terminalFieldValidator validation callback for the resolved terminal field
     * @return resolved entity field path after alias substitution
     * @throws QueryFieldValidationException if the selector is disallowed or cannot be resolved
     */
    public static String resolveEntityPath(
            Class<?> entityClass,
            String reqFieldPath,
            Map<String, FieldMapping> fieldMappingMap,
            Map<String, FieldMapping> originalFieldNameMap,
            Consumer<Field> terminalFieldValidator
    ) {
        String fieldPath = reqFieldPath; // Actual entity path to validate against, may be rewritten if field mapping exists

        // If the field name corresponds to an API alias that does not allow using the original field name, reject it
        FieldMapping originalFieldMapping = originalFieldNameMap.get(reqFieldPath);
        if(originalFieldMapping != null && !originalFieldMapping.allowOriginalFieldName()) {
            throw new QueryFieldValidationException(MessageFormat.format(
                    "Unknown field ''{0}''", reqFieldPath
            ), reqFieldPath);
        }

        // Find original field name if field mapping exists to correctly find the field
        FieldMapping fieldMapping = fieldMappingMap.get(fieldPath);
        if(fieldMapping != null) fieldPath = fieldMapping.field();

        // Resolve the field on the entity (including inherited fields)
        Field field;
        try {
            field = ReflectionUtil.resolveField(entityClass, fieldPath);
        }
        catch (Exception ex) {
            throw new QueryFieldValidationException(MessageFormat.format(
                    "Unknown field ''{0}''", reqFieldPath
            ), reqFieldPath, ex);
        }

        terminalFieldValidator.accept(field);

        return fieldPath;
    }
}
