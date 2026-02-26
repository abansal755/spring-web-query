package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.annotation.*;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A custom {@link HandlerMethodArgumentResolver} that wraps a standard
 * {@link PageableHandlerMethodArgumentResolver} to enforce restrictions
 * on pageable sorting fields based on entity metadata.
 * <p>
 * This resolver only supports controller method parameters that:
 * <ul>
 *     <li>Are of type {@link Pageable}.</li>
 *     <li>Are annotated with {@link RestrictedPageable}.</li>
 * </ul>
 * <p>
 * The resolver delegates the initial parsing of {@code page}, {@code size},
 * and {@code sort} parameters to Spring's {@link PageableHandlerMethodArgumentResolver}.
 * It then validates each requested {@link Sort.Order} against the target entity class
 * specified in {@link WebQuery} on the controller method. Sorting is only allowed
 * on fields explicitly annotated with {@link Sortable}.
 * Alias mappings from {@link WebQuery#fieldMappings()} are also applied so API-facing
 * sort names can be rewritten to entity field paths.
 * <p>
 * If a requested sort field is not annotated as {@link Sortable}, a
 * {@link QueryValidationException} is thrown.
 */
@RequiredArgsConstructor
public class RestrictedPageableArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * The delegate {@link PageableHandlerMethodArgumentResolver} used to
     * parse standard pageable parameters.
     */
    private final PageableHandlerMethodArgumentResolver delegate;

    private final AnnotationUtil annotationUtil;

    /**
     * Determines whether the given method parameter is supported by this resolver.
     * <p>
     * Supported parameters must:
     * <ul>
     *     <li>Be assignable to {@link Pageable}.</li>
     *     <li>Be annotated with {@link RestrictedPageable}.</li>
     * </ul>
     *
     * @param parameter the method parameter to check
     * @return {@code true} if the parameter is supported, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Pageable.class.isAssignableFrom(parameter.getParameterType())
                && parameter.hasParameterAnnotation(RestrictedPageable.class);
    }

    /**
     * Resolves the given {@link Pageable} argument from the web request.
     * <p>
     * The process is as follows:
     * <ol>
     *     <li>Delegate parsing of page, size, and sort parameters to {@link #delegate}.</li>
     *     <li>Resolve {@link WebQuery} metadata from the controller method.</li>
     *     <li>Validate each requested {@link Sort.Order} against the entity's sortable fields.
     *     If a field is not annotated with {@link Sortable}, a {@link QueryValidationException} is thrown.</li>
     *     <li>Rewrite alias sort properties to real entity field paths using field mappings.</li>
     * </ol>
     *
     * @param methodParameter the method parameter for which the value should be resolved
     * @param mavContainer the ModelAndViewContainer (can be {@code null})
     * @param webRequest the current request
     * @param binderFactory a factory for creating WebDataBinder instances (can be {@code null})
     * @return a {@link Pageable} object containing page, size, validated sort information,
     *         and mapped sort field paths
     * @throws QueryValidationException if any requested sort field is not marked as {@link Sortable}
     * @throws QueryConfigurationException if resolver metadata or alias configuration cannot be processed
     */
    @Override
    public Pageable resolveArgument(
            @NonNull MethodParameter methodParameter,
            ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        try {
            // Delegate parsing of page, size and sort parameters to Spring
            Pageable pageable = delegate.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);

            // Resolve the @WebQuery annotation to access entity metadata for validation
            WebQuery webQueryAnnotation = annotationUtil.resolveWebQueryFromParameter(methodParameter);
            // Extract entity and dto class
            Class<?> entityClass = webQueryAnnotation.entityClass();
            Class<?> dtoClass = webQueryAnnotation.dtoClass();

            // Entity mode
            if(dtoClass == void.class) {
                FieldMapping[] fieldMappings = webQueryAnnotation.fieldMappings();
                return buildEntityModePageable(entityClass, fieldMappings, pageable);
            }

            // DTO mode
            return builtDtoModePageable(entityClass, dtoClass, pageable);
        }
        catch (QueryException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new QueryConfigurationException("Failed to resolve pageable argument", ex);
        }
    }

    private Pageable builtDtoModePageable(Class<?> entityClass, Class<?> dtoClass, Pageable pageable) {
        List<Sort.Order> newOrders = new ArrayList<>();
        for(Sort.Order order : pageable.getSort()) {
            String dtoPath = order.getProperty();
            // Resolve the field path in the DTO class
            List<Field> dtoFields;
            try {
                dtoFields = ReflectionUtil.resolveFieldPath(dtoClass, dtoPath);
            }
            catch (Exception ex) {
                throw new QueryValidationException(MessageFormat.format(
                        "Unknown field ''{0}''", dtoPath
                ), ex);
            }
            // Validate the last field in the path for sortability
            if(!dtoFields.getLast().isAnnotationPresent(Sortable.class)) {
                throw new QueryValidationException(MessageFormat.format(
                        "Sorting is not allowed on the field ''{0}''", dtoPath
                ));
            }
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
            catch (Exception x) {
                throw new QueryConfigurationException(MessageFormat.format(
                        "Unable to resolve entity field path ''{0}'' mapped from DTO path ''{1}''", entityPath, dtoPath
                ));
            }
            newOrders.add(new Sort.Order(order.getDirection(), entityPath));
        }
        Sort sort = Sort.by(newOrders);
        // Reconstruct pageable with mapped sort orders
        if(pageable.isUnpaged()) return Pageable.unpaged(sort);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Pageable buildEntityModePageable(Class<?> entityClass, FieldMapping[] fieldMappings, Pageable pageable) {
        // Create maps for quick lookup of field mappings by both API name and original field name
        Map<String, FieldMapping> fieldMappingMap = Arrays
                .stream(fieldMappings)
                .collect(Collectors.toMap(FieldMapping::name, mapping -> mapping));
        Map<String, FieldMapping> originalFieldNameMap = Arrays
                .stream(fieldMappings)
                .collect(Collectors.toMap(FieldMapping::field, mapping -> mapping));

        // Validate each requested sort order against entity metadata
        for(Sort.Order order : pageable.getSort()) {
            String reqFieldName = order.getProperty();
            String fieldName = reqFieldName; // Actual entity path to validate against, may be rewritten if field mapping exists

            // If the field name corresponds to an API alias that does not allow using the original field name, reject it
            FieldMapping originalFieldMapping = originalFieldNameMap.get(reqFieldName);
            if(originalFieldMapping != null && !originalFieldMapping.allowOriginalFieldName())
                throw new QueryValidationException(MessageFormat.format(
                        "Unknown field ''{0}''", reqFieldName
                ));

            // Find original field name if field mapping exists to correctly find the field
            FieldMapping fieldMapping = fieldMappingMap.get(reqFieldName);
            if(fieldMapping != null) fieldName = fieldMapping.field();

            // Resolve the field on the entity (including inherited fields)
            Field field;
            try {
                field = ReflectionUtil.resolveField(entityClass, fieldName);
            }
            catch (Exception ex) {
                throw new QueryValidationException(MessageFormat.format(
                        "Unknown field ''{0}''", reqFieldName
                ), ex);
            }
            // Reject sorting on fields not explicitly marked as sortable
            if(!field.isAnnotationPresent(Sortable.class))
                throw new QueryValidationException(MessageFormat.format(
                        "Sorting is not allowed on the field ''{0}''", reqFieldName
                ));
        }

        return reconstructPageable(pageable, fieldMappingMap);
    }

    /**
     * Rebuilds the resolved {@link Pageable} with sort properties rewritten from
     * public aliases to entity field paths.
     *
     * @param pageable the original pageable resolved by Spring
     * @param fieldMappingMap map of API alias to {@link FieldMapping}
     * @return a pageable with mapped sort property paths
     */
    private Pageable reconstructPageable(Pageable pageable, Map<String, FieldMapping> fieldMappingMap) {
        // Reconstruct sort orders with mapped field names
        List<Sort.Order> newOrders = pageable
                .getSort()
                .stream()
                .map(order -> reconstructSortOrder(order, fieldMappingMap))
                .toList();
        Sort sort = Sort.by(newOrders);
        // Reconstruct pageable with mapped sort orders
        if(pageable.isUnpaged()) return Pageable.unpaged(sort);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    /**
     * Rewrites one sort order from alias property name to entity field path when
     * a matching mapping is present.
     *
     * @param order original sort order from request
     * @param fieldMappingMap map of API alias to {@link FieldMapping}
     * @return rewritten sort order
     */
    private Sort.Order reconstructSortOrder(Sort.Order order, Map<String, FieldMapping> fieldMappingMap) {
        String fieldName = order.getProperty();
        FieldMapping fieldMapping = fieldMappingMap.get(fieldName);
        if(fieldMapping != null) fieldName = fieldMapping.field();
        return new Sort.Order(order.getDirection(), fieldName);
    }
}
