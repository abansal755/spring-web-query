package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import in.co.akshitbansal.springwebquery.util.FieldResolvingUtil;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entity-based resolver for {@link Pageable} parameters handled via
 * method-level {@link WebQuery}.
 *
 * <p>This resolver validates requested sort properties directly against the
 * configured entity class and optional {@link FieldMapping} aliases declared
 * on {@link WebQuery}.</p>
 */
@RequiredArgsConstructor
public class WebQueryEntityAwarePageableArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * Delegate used to parse raw pageable parameters from the request.
     */
    private final PageableHandlerMethodArgumentResolver delegate;

    /**
     * Shared annotation utility dependency for resolver-level validation concerns.
     */
    private final AnnotationUtil annotationUtil;

    /**
     * Determines whether this resolver should handle the given parameter.
     *
     * @param parameter method parameter under inspection
     * @return {@code true} when parameter is {@code Pageable} with
     *         method-level {@link WebQuery} and no DTO mapping is configured
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if(!Pageable.class.isAssignableFrom(parameter.getParameterType())) return false;
        Method controlllerMethod = parameter.getMethod();
        if(controlllerMethod == null) return false;
        WebQuery webQueryAnnotation = controlllerMethod.getAnnotation(WebQuery.class);
        if(webQueryAnnotation == null) return false;
        return webQueryAnnotation.dtoClass() == void.class;
    }

    /**
     * Resolves and validates a {@link Pageable} argument with restricted sorting.
     *
     * @param parameter controller method parameter being resolved
     * @param mavContainer current MVC container
     * @param webRequest current request
     * @param binderFactory binder factory
     * @return validated pageable with alias-mapped sort properties
     * @throws Exception when resolution fails
     */
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
            WebQuery webQueryAnnotation = parameter.getMethod().getAnnotation(WebQuery.class);
            // Extract entity class and field mappings from the @WebQuery annotation for validation and pageable building
            Class<?> entityClass = webQueryAnnotation.entityClass();
            FieldMapping[] fieldMappings = webQueryAnnotation.fieldMappings();

            // Validate field mappings to ensure they are well-formed and do not contain conflicts
            annotationUtil.validateFieldMappings(fieldMappings);

            // Create maps for quick lookup of field mappings by both API name and original field name
            Map<String, FieldMapping> fieldMappingMap = Arrays
                    .stream(fieldMappings)
                    .collect(Collectors.toMap(FieldMapping::name, mapping -> mapping));
            Map<String, FieldMapping> originalFieldNameMap = Arrays
                    .stream(fieldMappings)
                    .collect(Collectors.toMap(FieldMapping::field, mapping -> mapping));

            List<Sort.Order> newOrders = new ArrayList<>();
            // Validate each requested sort order against entity metadata
            for(Sort.Order order : pageable.getSort()) {
                String reqFieldName = order.getProperty();

                // Resolve the field on the entity class using the requested field name and field mappings
                String fieldName = FieldResolvingUtil.resolveEntityPath(
                        entityClass,
                        reqFieldName,
                        fieldMappingMap,
                        originalFieldNameMap,
                        terminalField -> annotationUtil.validateSortableField(terminalField, reqFieldName)
                );

                newOrders.add(new Sort.Order(order.getDirection(), fieldName));
            }

            Sort sort = Sort.by(newOrders);
            // Reconstruct pageable with mapped sort orders
            if(pageable.isUnpaged()) return Pageable.unpaged(sort);
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        }
        catch (QueryException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new QueryConfigurationException("Failed to resolve pageable argument", ex);
        }
    }
}
