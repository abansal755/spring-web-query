package in.co.akshitbansal.springwebquery.util;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import lombok.NonNull;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for resolving query-related annotations from controller metadata
 * and validating {@link FieldMapping} declarations.
 */
public class AnnotationUtil {

    /**
     * Resolves {@link WebQuery} from the controller method that declares the
     * provided Spring MVC method parameter.
     *
     * @param parameter controller method parameter currently being resolved
     * @return resolved {@link WebQuery} annotation
     * @throws QueryConfigurationException if the method cannot be resolved or is not annotated with {@link WebQuery}
     */
    public static WebQuery resolveWebQueryFromParameter(@NonNull MethodParameter parameter) {
        // Retrieve the controller method
        Method controllerMethod = parameter.getMethod();
        // Ensure that the method is not null (should not happen for valid controller parameters)
        if(controllerMethod == null) throw new QueryConfigurationException(MessageFormat.format(
                "Unable to resolve controller method for parameter {0}", parameter
        ));
        // Retrieve the @WebQuery annotation from the controller method to access query configuration
        WebQuery webQueryAnnotation = controllerMethod.getAnnotation(WebQuery.class);
        // Ensure that the method is annotated with @WebQuery to access query configuration
        if(webQueryAnnotation == null)
            throw new QueryConfigurationException(MessageFormat.format(
                    "Controller method {0} must be annotated with @WebQuery to use query argument resolvers",
                    controllerMethod
            ));
        return webQueryAnnotation;
    }

    /**
     * Validates {@link FieldMapping} definitions declared in {@link WebQuery}.
     * <p>
     * Validation rules:
     * <ul>
     *     <li>Alias names must be unique ({@link FieldMapping#name()}).</li>
     *     <li>Target entity fields must be unique ({@link FieldMapping#field()}).</li>
     * </ul>
     *
     * @param fieldMappings field mappings to validate
     * @throws QueryConfigurationException if duplicate aliases or duplicate target fields are found
     */
    public static void validateFieldMappings(@NonNull FieldMapping[] fieldMappings) {
        Set<String> nameSet = new HashSet<>();
        for (FieldMapping mapping : fieldMappings) {
            if(!nameSet.add(mapping.name())) throw new QueryConfigurationException(MessageFormat.format(
                    "Duplicate field mapping present for alias ''{0}''. Only one mapping is allowed per alias.",
                    mapping.name()
            ));
        }

        Map<String, FieldMapping> fieldMap = new HashMap<>();
        for (FieldMapping mapping : fieldMappings) {
            fieldMap.compute(mapping.field(), (fieldName, existing) -> {
                if(existing != null) throw new QueryConfigurationException(MessageFormat.format(
                        "Aliases ''{0}'' and ''{1}'' are mapped to same field. Only one mapping is allowed per field.",
                        existing.name(), mapping.name()
                ));
                return mapping;
            });
        }
    }
}
