package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Map;

@RequiredArgsConstructor
public class EntityAwareFieldResolver implements FieldResolver {

    private final Class<?> entityClass;
    private final Map<String, FieldMapping> fieldMappingMap;
    private final Map<String, FieldMapping> originalFieldNameMap;

    @Override
    public FieldResolverResult resolvePathAndGetTerminalField(String reqFieldPath) {
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

        return new FieldResolverResult(fieldPath, field);
    }
}
