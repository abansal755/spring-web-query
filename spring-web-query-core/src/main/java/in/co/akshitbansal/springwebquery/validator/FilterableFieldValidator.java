package in.co.akshitbansal.springwebquery.validator;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterables;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import lombok.*;

import java.lang.annotation.Annotation;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validator that enforces {@code @RSQLFilterable} constraints for a resolved
 * terminal field and requested comparison operator.
 *
 * <p>This validator supports direct, repeatable, and composed filterability
 * annotations and can resolve referenced custom operators from a prebuilt
 * registry.</p>
 */
@RequiredArgsConstructor
public class FilterableFieldValidator implements Validator<FilterableFieldValidator.Field> {

    /**
     * Registered custom operators keyed by their implementation class.
     */
    private final Map<Class<?>, RSQLCustomOperator<?>> customOperators;

    /**
     * Validates that a field is marked as filterable and that the requested
     * operator is permitted by its {@link RSQLFilterable} declaration(s).
     *
     * @param field field being targeted by the request selector
     * @throws QueryFieldValidationException if the field is not filterable
     * @throws QueryForbiddenOperatorException if the operator is not allowed for the field
     */
    @Override
    public void validate(@NonNull FilterableFieldValidator.Field field) {
        java.lang.reflect.Field reflectedField = field.getField();
        ComparisonOperator operator = field.getOperator();
        String fieldPath = field.getFieldPath();

        // Retrieve the RSQLFilterable annotations on the field (if present)
        Set<RSQLFilterable> filterables = collectFilterables(reflectedField);
        // Throw exception if the field is not annotated as filterable
        if(filterables.isEmpty()) throw new QueryFieldValidationException(MessageFormat.format(
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
     * Aggregates all allowed operators from one or more {@link RSQLFilterable}
     * declarations attached to the same field.
     *
     * @param filterables repeatable filterability declarations
     * @return deduplicated set of allowed comparison operators
     * @throws QueryConfigurationException if a referenced custom operator is not registered
     */
    private Set<ComparisonOperator> getAllowedOperators(@NonNull Set<RSQLFilterable> filterables) {
        // Collect the set of allowed operators for this field from the annotations
        // Stream of default operators defined in the annotation
        Stream<ComparisonOperator> defaultOperators = filterables
                .stream()
                .flatMap(filterable -> Arrays.stream(filterable.value()))
                .map(RSQLDefaultOperator::getOperator);
        // Stream of custom operators defined in the annotation
        // Note: The annotation references classes, which are looked up in the customOperators map
        Stream<ComparisonOperator> customOperators = filterables
                .stream()
                .flatMap(filterable -> Arrays.stream(filterable.customOperators()))
                .map(this::getCustomOperator)
                .map(RSQLCustomOperator::getComparisonOperator);
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
    private RSQLCustomOperator<?> getCustomOperator(@NonNull Class<?> clazz) {
        RSQLCustomOperator<?> operator = customOperators.get(clazz);
        if(operator == null) throw new QueryConfigurationException(MessageFormat.format(
                "Custom operator ''{0}'' referenced in @RSQLFilterable is not registered", clazz.getSimpleName()
        ));
        return operator;
    }

    /**
     * Collects all {@link RSQLFilterable} declarations present on a field,
     * including repeatable and composed annotations.
     *
     * @param field field whose annotations are to be inspected
     * @return collected filterability declarations
     */
    private Set<RSQLFilterable> collectFilterables(java.lang.reflect.Field field) {
        return collectFilterables(field.getAnnotations());
    }

    /**
     * Recursively collects {@link RSQLFilterable} declarations from a set of
     * annotations, supporting both direct and meta-annotation usage.
     *
     * @param annotations annotations to inspect
     * @return collected filterability declarations
     */
    private Set<RSQLFilterable> collectFilterables(Annotation[] annotations) {
        Set<RSQLFilterable> filterables = new HashSet<>();
        for(Annotation annotation : annotations) {
            Class<? extends Annotation> type = annotation.annotationType();
            if(annotation instanceof RSQLFilterable rsqlFilterable)
                filterables.add(rsqlFilterable);
            else if(annotation instanceof RSQLFilterables rsqlFilterables)
                filterables.addAll(Arrays.asList(rsqlFilterables.value()));
            else if(type.getName().startsWith("in.co.akshitbansal.springwebquery.annotation"))
                filterables.addAll(collectFilterables(type.getAnnotations()));
        }
        return filterables;
    }

    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class Field {

        /**
         * Reflected terminal field being validated.
         */
        @NonNull
        private final java.lang.reflect.Field field;

        /**
         * Comparison operator requested for the selector.
         */
        @NonNull
        private final ComparisonOperator operator;

        /**
         * Original selector path from the incoming request.
         */
        @NonNull
        private final String fieldPath;
    }
}
