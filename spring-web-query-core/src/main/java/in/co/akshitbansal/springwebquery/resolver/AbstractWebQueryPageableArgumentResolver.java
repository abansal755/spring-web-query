package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public abstract class AbstractWebQueryPageableArgumentResolver extends AbstractWebQueryResolver {

    /**
     * Delegate used to parse raw pageable parameters from the request.
     */
    protected final PageableHandlerMethodArgumentResolver delegate;

    protected final Validator<SortableFieldValidator.Field> sortableFieldValidator;

    public AbstractWebQueryPageableArgumentResolver(
            PageableHandlerMethodArgumentResolver delegate,
            boolean globalAllowAndOperator,
            boolean globalAllowOrOperator,
            int globalMaxASTDepth
    ) {
        super(globalAllowAndOperator, globalAllowOrOperator, globalMaxASTDepth);
        this.delegate = delegate;
        this.sortableFieldValidator = new SortableFieldValidator();
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
}
