package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RestrictedPageable;
import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class EntityAwareRestrictedPageableArgumentResolver implements HandlerMethodArgumentResolver {

    private final PageableHandlerMethodArgumentResolver delegate;
    private final AnnotationUtil annotationUtil;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if(!Pageable.class.isAssignableFrom(parameter.getParameterType())) return false;
        if(!parameter.hasParameterAnnotation(RestrictedPageable.class)) return false;
        WebQuery webQueryAnnotation = annotationUtil.resolveWebQueryFromParameter(parameter);
        return webQueryAnnotation.dtoClass() == void.class;
    }

    @Override
    public @Nullable Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) throws Exception
    {
        try {
            // Delegate parsing of page, size and sort parameters to Spring
            Pageable pageable = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

            // Resolve the @WebQuery annotation to access entity metadata for validation
            WebQuery webQueryAnnotation = annotationUtil.resolveWebQueryFromParameter(parameter);
            // Extract entity class and field mappings from the @WebQuery annotation for validation and pageable building
            Class<?> entityClass = webQueryAnnotation.entityClass();
            FieldMapping[] fieldMappings = webQueryAnnotation.fieldMappings();

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
                if(!field.isAnnotationPresent(Sortable.class)) {
                    throw new QueryValidationException(MessageFormat.format(
                            "Sorting is not allowed on the field ''{0}''", reqFieldName
                    ));
                }
            }

            return reconstructPageable(pageable, fieldMappingMap);
        }
        catch (QueryException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new QueryConfigurationException("Failed to resolve pageable argument", ex);
        }
    }

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

    private Sort.Order reconstructSortOrder(Sort.Order order, Map<String, FieldMapping> fieldMappingMap) {
        String fieldName = order.getProperty();
        FieldMapping fieldMapping = fieldMappingMap.get(fieldName);
        if(fieldMapping != null) fieldName = fieldMapping.field();
        return new Sort.Order(order.getDirection(), fieldName);
    }
}
