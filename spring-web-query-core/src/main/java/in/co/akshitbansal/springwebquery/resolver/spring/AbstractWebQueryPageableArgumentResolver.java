package in.co.akshitbansal.springwebquery.resolver.spring;

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

/**
 * Base resolver for {@link Pageable} parameters participating in
 * {@link WebQuery}-aware sorting.
 *
 * <p>This class delegates standard page/size parsing to Spring's
 * {@link PageableHandlerMethodArgumentResolver} and lets subclasses validate
 * and remap sort properties against either entity or DTO metadata.</p>
 */
public abstract class AbstractWebQueryPageableArgumentResolver extends AbstractWebQueryResolver {

    /**
     * Delegate used to parse raw pageable parameters from the request.
     */
    protected final PageableHandlerMethodArgumentResolver delegate;

    /**
     * Validator used to enforce {@code @Sortable} constraints on resolved sort fields.
     */
    protected final Validator<SortableFieldValidator.Field> sortableFieldValidator;

    /**
     * Creates a pageable resolver base with shared global defaults.
     *
     * @param delegate Spring's pageable resolver used for base pagination parsing
     * @param globalAllowAndOperator global fallback for logical AND allowance
     * @param globalAllowOrOperator global fallback for logical OR allowance
     * @param globalMaxASTDepth global fallback for maximum AST depth
     */
    protected AbstractWebQueryPageableArgumentResolver(
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
            // Resolve effective endpoint settings from the current method parameter
            QueryConfiguration queryConfig = getQueryConfiguration(parameter);
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

    /**
     * Validates and remaps pageable sorting according to the effective query configuration.
     *
     * @param pageable pageable parsed from the request
     * @param queryConfig effective query configuration derived from {@link WebQuery}
     * @return pageable with validated and possibly remapped sort orders
     */
    protected abstract Pageable resolvePageable(Pageable pageable, QueryConfiguration queryConfig);
}
