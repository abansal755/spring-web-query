package in.co.akshitbansal.springwebquery.util;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import lombok.NonNull;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods for resolving query-related annotations from controller metadata
 * and validating {@link FieldMapping} declarations.
 */
public class AnnotationUtil {

    private final Map<Class<?>, RsqlCustomOperator<?>> customOperators;

    public AnnotationUtil(Set<? extends RsqlCustomOperator<?>> customOperators) {
        this.customOperators = customOperators
                .stream()
                .collect(Collectors.toMap(RsqlCustomOperator::getClass, operator -> operator));
    }

    /**
     * Resolves {@link WebQuery} from the controller method that declares the
     * provided Spring MVC method parameter.
     *
     * @param parameter controller method parameter currently being resolved
     * @return resolved {@link WebQuery} annotation
     * @throws QueryConfigurationException if the method cannot be resolved or is not annotated with {@link WebQuery}
     */
    public WebQuery resolveWebQueryFromParameter(@NonNull MethodParameter parameter) {
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
    public void validateFieldMappings(@NonNull FieldMapping[] fieldMappings) {
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

    /**
     * Computes the full set of operators allowed for a field by combining
     * built-in operators and registered custom operators referenced by
     * {@link RsqlFilterable}.
     *
     * @param filterable field-level filterability metadata
     * @return allowed comparison operators for the field
     * @throws QueryConfigurationException if a referenced custom operator is not registered
     */
    public Set<ComparisonOperator> getAllowedOperators(@NonNull RsqlFilterable filterable) {
        // Collect the set of allowed operators for this field from the annotation
        // Stream of default operators defined in the annotation
        Stream<ComparisonOperator> defaultOperators = Arrays
                .stream(filterable.operators())
                .map(RsqlOperator::getOperator);
        // Stream of custom operators defined in the annotation
        // Note: The annotation references classes, which are looked up in the customOperators map
        Stream<ComparisonOperator> customOperators = Arrays
                .stream(filterable.customOperators())
                .map(this::getCustomOperator)
                .map(RsqlCustomOperator::getComparisonOperator);
        return Stream
                .concat(defaultOperators, customOperators)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves the custom operator instance for the given operator class.
     *
     * @param clazz the custom operator class to look up
     * @return the registered custom operator instance
     * @throws QueryConfigurationException if the custom operator class is not registered
     */
    private RsqlCustomOperator<?> getCustomOperator(@NonNull Class<?> clazz) {
        RsqlCustomOperator<?> operator = customOperators.get(clazz);
        if(operator == null) throw new QueryConfigurationException(MessageFormat.format(
                "Custom operator ''{0}'' referenced in @RsqlFilterable is not registered", clazz.getSimpleName()
        ));
        return operator;
    }
}
