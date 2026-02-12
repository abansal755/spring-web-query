package io.github.abansal755.spring_query;

import io.github.abansal755.spring_query.annotation.RestrictedPageable;
import io.github.abansal755.spring_query.annotation.Sortable;
import io.github.abansal755.spring_query.exception.QueryException;
import io.github.abansal755.spring_query.util.ReflectionUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Field;
import java.text.MessageFormat;

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
 * specified in the {@link RestrictedPageable} annotation. Sorting is only allowed
 * on fields explicitly annotated with {@link Sortable}.
 * <p>
 * If a requested sort field is not annotated as {@link Sortable}, a
 * {@link QueryException} is thrown.
 */
@RequiredArgsConstructor
public class RestrictedPageableArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * The delegate {@link PageableHandlerMethodArgumentResolver} used to
     * parse standard pageable parameters.
     */
    private final PageableHandlerMethodArgumentResolver delegate;

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
     *     <li>Retrieve the target entity class from the {@link RestrictedPageable} annotation.</li>
     *     <li>Validate each requested {@link Sort.Order} against the entity's sortable fields.
     *     If a field is not annotated with {@link Sortable}, a {@link QueryException} is thrown.</li>
     * </ol>
     *
     * @param methodParameter the method parameter for which the value should be resolved
     * @param mavContainer the ModelAndViewContainer (can be {@code null})
     * @param webRequest the current request
     * @param binderFactory a factory for creating WebDataBinder instances (can be {@code null})
     * @return a {@link Pageable} object containing page, size, and validated sort information
     * @throws QueryException if any requested sort field is not marked as {@link Sortable}
     */
    @Override
    public Pageable resolveArgument(
            @NonNull MethodParameter methodParameter,
            ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        // Delegate parsing of page, size and sort parameters to Spring
        Pageable pageable = delegate.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);

        // Retrieve annotation to determine the target entity
        RestrictedPageable restrictedPageable = methodParameter.getParameterAnnotation(RestrictedPageable.class);
        Class<?> entityClass = restrictedPageable.entityClass();

        // Validate each requested sort order against entity metadata
        for(Sort.Order order : pageable.getSort()) {
            String fieldName = order.getProperty();
            // Resolve the field on the entity (including inherited fields)
            Field field = ReflectionUtil.resolveField(entityClass, fieldName);
            // Reject sorting on fields not explicitly marked as sortable
            if(!field.isAnnotationPresent(Sortable.class))
                throw new QueryException(MessageFormat.format("Sorting is not allowed on the field ''{0}''", fieldName));
        }

        return pageable;
    }
}
