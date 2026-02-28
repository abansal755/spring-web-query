package in.co.akshitbansal.springwebquery.util;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import lombok.NonNull;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Field;
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

    /**
     * Registered custom operators keyed by their implementation class.
     */
    private final Map<Class<?>, RsqlCustomOperator<?>> customOperators;

    /**
     * Creates an annotation utility backed by registered custom operator instances.
     *
     * @param customOperators custom operators available to annotation-driven validation
     */
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
     * Validates that a field is marked as filterable and that the requested
     * operator is permitted by its {@link RsqlFilterable} declaration(s).
     *
     * @param field field being targeted by the request selector
     * @param operator comparison operator requested in the query
     * @param fieldPath original selector path from the request
     * @throws QueryFieldValidationException if the field is not filterable
     * @throws QueryForbiddenOperatorException if the operator is not allowed for the field
     */
    public void validateFilterableField(@NonNull Field field, ComparisonOperator operator, String fieldPath) {
        // Retrieve the RsqlFilterable annotations on the field (if present)
        RsqlFilterable[] filterables = field.getAnnotationsByType(RsqlFilterable.class);
        // Throw exception if the field is not annotated as filterable
        if(filterables.length == 0) throw new QueryFieldValidationException(MessageFormat.format(
                "Filtering not allowed on field ''{0}''", fieldPath
        ), fieldPath);

        // Throw exception if the provided operator is not in the allowed set
        Set<ComparisonOperator> allowedOperators = getAllowedOperators(filterables);
        if(!allowedOperators.contains(operator)) {
            throw new QueryForbiddenOperatorException(
                    MessageFormat.format("Operator ''{0}'' not allowed on field ''{1}''", operator, fieldPath),
                    fieldPath,
                    operator,
                    allowedOperators
            );
        }
    }

    /**
     * Aggregates all allowed operators from one or more {@link RsqlFilterable}
     * declarations attached to the same field.
     *
     * @param filterables repeatable filterability declarations
     * @return deduplicated set of allowed comparison operators
     * @throws QueryConfigurationException if a referenced custom operator is not registered
     */
    private Set<ComparisonOperator> getAllowedOperators(@NonNull RsqlFilterable[] filterables) {
        // Collect the set of allowed operators for this field from the annotations
        // Stream of default operators defined in the annotation
        Stream<ComparisonOperator> defaultOperators = Arrays
                .stream(filterables)
                .flatMap(filterable -> Arrays.stream(filterable.operators()))
                .map(RsqlOperator::getOperator);
        // Stream of custom operators defined in the annotation
        // Note: The annotation references classes, which are looked up in the customOperators map
        Stream<ComparisonOperator> customOperators = Arrays
                .stream(filterables)
                .flatMap(filterable -> Arrays.stream(filterable.customOperators()))
                .map(this::getCustomOperator)
                .map(RsqlCustomOperator::getComparisonOperator);
        return Stream
                .concat(defaultOperators, customOperators)
                .collect(Collectors.toCollection(HashSet::new));
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
