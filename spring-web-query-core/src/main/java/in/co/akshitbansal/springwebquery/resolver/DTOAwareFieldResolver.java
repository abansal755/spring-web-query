package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class DTOAwareFieldResolver implements FieldResolver {

    private final Class<?> entityClass;
    private final Class<?> dtoClass;

    @Override
    public FieldResolverResult resolvePathAndGetTerminalField(String dtoPath) {
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

        return new FieldResolverResult(entityPath, dtoFields.get(dtoFields.size() - 1));
    }
}
