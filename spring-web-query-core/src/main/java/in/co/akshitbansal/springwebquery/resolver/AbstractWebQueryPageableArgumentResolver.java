package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Field;
import java.text.MessageFormat;

public abstract class AbstractWebQueryPageableArgumentResolver extends AbstractWebQueryResolver {

    /**
     * Delegate used to parse raw pageable parameters from the request.
     */
    protected final PageableHandlerMethodArgumentResolver delegate;

    public AbstractWebQueryPageableArgumentResolver(
            PageableHandlerMethodArgumentResolver delegate,
            boolean globalAllowAndOperator,
            boolean globalAllowOrOperator,
            int globalMaxASTDepth
    ) {
        super(globalAllowAndOperator, globalAllowOrOperator, globalMaxASTDepth);
        this.delegate = delegate;
    }

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        if(!super.supportsParameter(parameter)) return false;
        // Supported if parameter is of type Pageable
        return Pageable.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Pageable resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        try {
            // Delegate parsing of page, size and sort parameters to Spring
            Pageable pageable = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
            // Retrieve the @WebQuery annotation from the method parameter to access configuration
            WebQuery webQueryAnnotation = parameter.getMethod().getAnnotation(WebQuery.class);
            // Extract relevant configuration from the annotation
            QueryConfiguration queryConfig = getQueryConfiguration(webQueryAnnotation);
            // Perform pageable resolution and validation based on the extracted configuration
            return resolvePageable(pageable, queryConfig);
        }
        catch (QueryException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new QueryConfigurationException("Failed to resolve pageable argument", ex);
        }
    }

    protected abstract Pageable resolvePageable(Pageable pageable, QueryConfiguration queryConfig);

    /**
     * Validates that the requested field is explicitly marked as sortable.
     *
     * @param field field being targeted by sort selector
     * @param fieldPath original selector path from the request
     * @throws QueryFieldValidationException if sorting is not allowed for the field
     */
    protected void validateSortableField(@NonNull Field field, String fieldPath) {
        if(!field.isAnnotationPresent(Sortable.class)) {
            throw new QueryFieldValidationException(MessageFormat.format(
                    "Sorting is not allowed on the field ''{0}''", fieldPath
            ), fieldPath);
        }
    }
}
