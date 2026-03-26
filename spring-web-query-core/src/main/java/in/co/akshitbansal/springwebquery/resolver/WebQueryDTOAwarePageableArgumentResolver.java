package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import in.co.akshitbansal.springwebquery.util.FieldResolvingUtil;
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

    public WebQueryDTOAwarePageableArgumentResolver(
            PageableHandlerMethodArgumentResolver delegate,
            AnnotationUtil annotationUtil,
            boolean globalAllowAndOperator,
            boolean globalAllowOrOperator,
            int globalMaxASTDepth
    ) {
        super(delegate, annotationUtil, globalAllowAndOperator, globalAllowOrOperator, globalMaxASTDepth);
    }

    /**
     * Determines whether this resolver should handle the given parameter.
     *
     * @param parameter method parameter under inspection
     * @return {@code true} when parameter is {@code Pageable} with
     *         method-level {@link WebQuery} and a configured DTO class
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if(!super.supportsParameter(parameter)) return false;
        // supportsParameter in superclass checks for method-level @WebQuery presence, so we can safely assume that here
        return parameter.getMethod().getAnnotation(WebQuery.class).dtoClass() != void.class;
    }

    @Override
    protected Pageable resolvePageable(Pageable pageable, QueryConfiguration queryConfig) {
        List<Sort.Order> newOrders = new ArrayList<>();
        for(Sort.Order order : pageable.getSort()) {
            String dtoPath = order.getProperty();
            // Build the corresponding entity field path from the DTO path and validate the terminal field for sortability
            String entityPath = FieldResolvingUtil.buildEntityPathFromDtoPath(
                    queryConfig.getEntityClass(),
                    queryConfig.getDtoClass(),
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
}
