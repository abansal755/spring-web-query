package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
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
import java.util.ArrayList;
import java.util.List;

/**
 * DTO-based resolver for {@link Pageable} parameters annotated with
 * {@link RestrictedPageable}.
 *
 * <p>This resolver validates sort selectors against a DTO contract and maps
 * those selectors to entity paths (using {@link MapsTo} where provided) before
 * returning the final pageable.</p>
 */
@RequiredArgsConstructor
public class DtoAwareRestrictedPageableArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * Delegate used to parse raw pageable parameters from the request.
     */
    private final PageableHandlerMethodArgumentResolver delegate;

    /**
     * Utility used to resolve {@link WebQuery} metadata.
     */
    private final AnnotationUtil annotationUtil;

    /**
     * Determines whether this resolver should handle the given parameter.
     *
     * @param parameter method parameter under inspection
     * @return {@code true} when parameter is {@code Pageable} with
     *         {@link RestrictedPageable} and {@link WebQuery} has a DTO class
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if(!Pageable.class.isAssignableFrom(parameter.getParameterType())) return false;
        if(!parameter.hasParameterAnnotation(RestrictedPageable.class)) return false;
        WebQuery webQueryAnnotation = annotationUtil.resolveWebQueryFromParameter(parameter);
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
            WebQuery webQueryAnnotation = annotationUtil.resolveWebQueryFromParameter(parameter);
            // Extract entity and dto class
            Class<?> entityClass = webQueryAnnotation.entityClass();
            Class<?> dtoClass = webQueryAnnotation.dtoClass();

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
        catch (QueryException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new QueryConfigurationException("Failed to resolve pageable argument", ex);
        }
    }
}
