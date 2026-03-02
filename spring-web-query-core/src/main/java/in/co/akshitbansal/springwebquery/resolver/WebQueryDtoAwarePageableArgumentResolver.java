package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
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
import java.util.List;

/**
 * DTO-based resolver for {@link Pageable} parameters handled via
 * method-level {@link WebQuery}.
 *
 * <p>This resolver validates sort selectors against a DTO contract and maps
 * those selectors to entity paths (using {@link MapsTo} where provided) before
 * returning the final pageable.</p>
 */
@RequiredArgsConstructor
public class WebQueryDtoAwarePageableArgumentResolver implements HandlerMethodArgumentResolver {

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
     *         method-level {@link WebQuery} and a configured DTO class
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if(!Pageable.class.isAssignableFrom(parameter.getParameterType())) return false;
        Method controlllerMethod = parameter.getMethod();
        if(controlllerMethod == null) return false;
        WebQuery webQueryAnnotation = controlllerMethod.getAnnotation(WebQuery.class);
        if(webQueryAnnotation == null) return false;
        return webQueryAnnotation.dtoClass() != void.class;
    }

    /**
     * Resolves and validates a {@link Pageable} argument with DTO-based sorting rules.
     *
     * @param parameter controller method parameter being resolved
     * @param mavContainer current MVC container
     * @param webRequest current request
     * @param binderFactory binder factory
     * @return validated pageable with DTO selectors translated to entity paths
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
            // Extract entity and dto class
            Class<?> entityClass = webQueryAnnotation.entityClass();
            Class<?> dtoClass = webQueryAnnotation.dtoClass();

            List<Sort.Order> newOrders = new ArrayList<>();
            for(Sort.Order order : pageable.getSort()) {
                String dtoPath = order.getProperty();
                // Build the corresponding entity field path from the DTO path and validate the terminal field for sortability
                String entityPath = FieldResolvingUtil.buildEntityPathFromDtoPath(
                        entityClass,
                        dtoClass,
                        dtoPath,
                        terminalField -> annotationUtil.validateSortableField(terminalField, dtoPath)
                );
                newOrders.add(new Sort.Order(order.getDirection(), entityPath));
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
