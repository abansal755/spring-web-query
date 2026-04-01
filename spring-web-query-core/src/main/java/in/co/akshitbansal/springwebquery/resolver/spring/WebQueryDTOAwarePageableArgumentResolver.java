package in.co.akshitbansal.springwebquery.resolver.spring;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.resolver.DTOAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.FieldResolver;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

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
public class WebQueryDTOAwarePageableArgumentResolver extends AbstractWebQueryPageableArgumentResolver {

    /**
     * Creates a DTO-aware pageable resolver.
     *
     * @param delegate Spring's pageable resolver used for page and size parsing
     * @param globalAllowAndOperator global fallback for logical AND allowance
     * @param globalAllowOrOperator global fallback for logical OR allowance
     * @param globalMaxASTDepth global fallback for maximum AST depth
     */
    public WebQueryDTOAwarePageableArgumentResolver(
            PageableHandlerMethodArgumentResolver delegate,
            boolean globalAllowAndOperator,
            boolean globalAllowOrOperator,
            int globalMaxASTDepth
    ) {
        super(delegate, globalAllowAndOperator, globalAllowOrOperator, globalMaxASTDepth);
    }

    /**
     * Determines whether this resolver should handle the given parameter.
     *
     * @param parameter method parameter under inspection
     * @return {@code true} when parameter is {@code Pageable} with
     *         method-level {@link WebQuery} and a configured DTO class
     */
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return super.supportsParameter(parameter) // supportsParameter in superclass checks for method-level @WebQuery presence
                && getWebQueryAnnotation(parameter).dtoClass() != void.class; // so no exception handling is needed
    }

    /**
     * Validates DTO-facing sort properties and maps them to entity paths.
     *
     * @param pageable pageable parsed from the request
     * @param queryConfig effective query configuration for the current request
     * @return pageable with validated entity sort paths derived from DTO selectors
     */
    @Override
    protected Pageable resolvePageable(Pageable pageable, QueryConfiguration queryConfig) {
        FieldResolver fieldResolver = new DTOAwareFieldResolver(
                queryConfig.getEntityClass(),
                queryConfig.getDtoClass()
        );

        List<Sort.Order> newOrders = new ArrayList<>();
        for(Sort.Order order : pageable.getSort()) {
            String dtoPath = order.getProperty();
            // Build the corresponding entity field path from the DTO path and validate the terminal field for sortability
            String entityPath = fieldResolver.resolvePathAndValidateTerminalField(dtoPath,
                    terminalField -> sortableFieldValidator.validate(new SortableFieldValidator.SortableField(terminalField, dtoPath))
            );
            newOrders.add(new Sort.Order(order.getDirection(), entityPath));
        }
        Sort sort = Sort.by(newOrders);
        // Reconstruct pageable with mapped sort orders
        if(pageable.isUnpaged()) return Pageable.unpaged(sort);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
