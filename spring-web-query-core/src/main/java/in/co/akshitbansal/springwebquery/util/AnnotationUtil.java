package in.co.akshitbansal.springwebquery.util;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.*;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods for validating query-related annotation metadata.
 *
 * <p>This utility validates {@link FieldMapping} declarations and resolves
 * filterability/operator constraints from {@link RSQLFilterable} annotations,
 * including composed annotations in this library's annotation package.</p>
 */
public class AnnotationUtil {

    /**
     * Registered custom operators keyed by their implementation class.
     */
    private final Map<Class<?>, RSQLCustomOperator<?>> customOperators;

    /**
     * Creates an annotation utility backed by registered custom operator instances.
     *
     * @param customOperators custom operators available to annotation-driven validation
     */
    public AnnotationUtil(Set<? extends RSQLCustomOperator<?>> customOperators) {
        this.customOperators = Collections.unmodifiableMap(customOperators
                .stream()
                .collect(Collectors.toMap(
                        RSQLCustomOperator::getClass,
                        operator -> operator,
                        // Might happen in case multiple instances of an operator are registered
                        // In that case, we can just keep one of them since they should be functionally equivalent
                        (existing, duplicate) -> existing,
                        HashMap::new
                )));
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
     * operator is permitted by its {@link RSQLFilterable} declaration(s).
     *
     * @param field field being targeted by the request selector
     * @param operator comparison operator requested in the query
     * @param fieldPath original selector path from the request
     * @throws QueryFieldValidationException if the field is not filterable
     * @throws QueryForbiddenOperatorException if the operator is not allowed for the field
     */
    public void validateFilterableField(@NonNull Field field, ComparisonOperator operator, String fieldPath) {
        // Retrieve the RSQLFilterable annotations on the field (if present)
        Set<RSQLFilterable> filterables = collectFilterables(field);
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
     * Validates that the requested field is explicitly marked as sortable.
     *
     * @param field field being targeted by sort selector
     * @param fieldPath original selector path from the request
     * @throws QueryFieldValidationException if sorting is not allowed for the field
     */
    public void validateSortableField(@NonNull Field field, String fieldPath) {
        if(!field.isAnnotationPresent(Sortable.class)) {
            throw new QueryFieldValidationException(MessageFormat.format(
                    "Sorting is not allowed on the field ''{0}''", fieldPath
            ), fieldPath);
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
    private Set<RSQLFilterable> collectFilterables(Field field) {
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
}
