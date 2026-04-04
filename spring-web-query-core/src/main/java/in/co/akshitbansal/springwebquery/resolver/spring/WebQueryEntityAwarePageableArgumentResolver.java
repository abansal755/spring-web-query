package in.co.akshitbansal.springwebquery.resolver.spring;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.resolver.EntityAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.FieldResolver;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity-based resolver for {@link Pageable} parameters handled via
 * method-level {@link WebQuery}.
 *
 * <p>This resolver validates requested sort properties directly against the
 * configured entity class and optional {@link FieldMapping} aliases declared
 * on {@link WebQuery}, delegating alias and original-name handling to
 * {@link EntityAwareFieldResolver}.</p>
 */
public class WebQueryEntityAwarePageableArgumentResolver extends AbstractWebQueryPageableArgumentResolver {

    /**
     * Validator used to enforce uniqueness and consistency of declared field mappings.
     */
    private final Validator<FieldMapping[]> fieldMappingsValidator;

    /**
     * Creates an entity-aware pageable resolver.
     *
     * @param delegate Spring's pageable resolver used for page and size parsing
     */
    public WebQueryEntityAwarePageableArgumentResolver(PageableHandlerMethodArgumentResolver delegate) {
        super(delegate);
        this.fieldMappingsValidator = new FieldMappingsValidator();
    }

    /**
     * Determines whether this resolver should handle the given parameter.
     *
     * @param parameter method parameter under inspection
     * @return {@code true} when parameter is {@code Pageable} with
     *         method-level {@link WebQuery} and no DTO mapping is configured
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return super.supportsParameter(parameter) // supportsParameter in superclass checks for method-level @WebQuery presence
                    && getWebQueryAnnotation(parameter).dtoClass() == void.class; // so no exception handling is needed
    }

    /**
     * Validates and remaps entity-facing sort properties on the supplied pageable.
     *
     * @param pageable pageable parsed from the request
     * @param queryConfig effective query configuration for the current request
     * @return pageable with validated entity sort paths
     */
    @Override
    protected Pageable resolvePageable(Pageable pageable, QueryConfiguration queryConfig) {
        // Validate field mappings to ensure they are well-formed and do not contain conflicts
        fieldMappingsValidator.validate(queryConfig.getFieldMappings());

        FieldResolver fieldResolver = new EntityAwareFieldResolver(
                queryConfig.getEntityClass(),
                queryConfig.getFieldMappings()
        );

        List<Sort.Order> newOrders = new ArrayList<>();
        // Validate each requested sort order against entity metadata
        for(Sort.Order order : pageable.getSort()) {
            String reqFieldName = order.getProperty();

            // Resolve the field on the entity class using the requested field name and field mappings
            String fieldName = fieldResolver.resolvePathAndValidateTerminalField(
                    reqFieldName,
                    terminalField -> sortableFieldValidator.validate(new SortableFieldValidator.SortableField(terminalField, reqFieldName))
            );

            newOrders.add(new Sort.Order(order.getDirection(), fieldName));
        }

        Sort sort = Sort.by(newOrders);
        // Reconstruct pageable with mapped sort orders
        if(pageable.isUnpaged()) return Pageable.unpaged(sort);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
