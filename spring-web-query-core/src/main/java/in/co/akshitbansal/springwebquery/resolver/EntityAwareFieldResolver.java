package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Map;
import java.util.function.Consumer;

/**
 * {@link FieldResolver} implementation that resolves selectors directly
 * against an entity model, with optional alias support via {@link FieldMapping}.
 *
 * <p>This resolver can rewrite request selectors using declared aliases,
 * reject access to original field names when a mapping forbids them, and
 * validate the resolved terminal entity field via the supplied callback.</p>
 */
@RequiredArgsConstructor
public class EntityAwareFieldResolver implements FieldResolver {

    /**
     * Entity type used for structural path resolution.
     */
    private final Class<?> entityClass;

    /**
     * Field mappings keyed by public alias name.
     */
    private final Map<String, FieldMapping> fieldMappingMap;

    /**
     * Field mappings keyed by original entity field path.
     */
    private final Map<String, FieldMapping> originalFieldNameMap;

    /**
     * Resolves an entity-facing selector path, validates the resolved terminal
     * field, and returns the final entity path.
     *
     * @param reqFieldPath selector path from the incoming request
     * @param terminalFieldValidator callback used to validate the resolved terminal field
     * @return resolved entity path after alias translation
     */
    @Override
    public String resolvePathAndValidateTerminalField(String reqFieldPath, Consumer<Field> terminalFieldValidator) {
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
