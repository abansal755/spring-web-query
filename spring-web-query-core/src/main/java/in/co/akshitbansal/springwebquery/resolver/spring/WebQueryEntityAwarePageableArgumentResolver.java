/*
 * Copyright 2026-present Akshit Bansal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.co.akshitbansal.springwebquery.resolver.spring;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.resolver.field.EntityAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.resolver.spring.config.PageableArgumentResolverConfig;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
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
	private final FieldMappingsValidator fieldMappingsValidator;

	/**
	 * Creates an entity-aware pageable resolver.
	 *
	 * @param delegate Spring's pageable resolver used for page and size parsing
	 * @param sortableFieldValidator validator used to enforce {@code @Sortable} constraints
	 * @param fieldMappingsValidator validator used to check declared {@link FieldMapping} aliases
	 */
	public WebQueryEntityAwarePageableArgumentResolver(
			PageableHandlerMethodArgumentResolver delegate,
			SortableFieldValidator sortableFieldValidator,
			FieldMappingsValidator fieldMappingsValidator,
			FieldResolverFactory fieldResolverFactory
	) {
		super(delegate, sortableFieldValidator, fieldResolverFactory);
		this.fieldMappingsValidator = fieldMappingsValidator;
	}

	/**
	 * Determines whether this resolver should handle the given parameter.
	 *
	 * @param parameter method parameter under inspection
	 *
	 * @return {@code true} when parameter is {@code Pageable} with
	 * method-level {@link WebQuery} and no DTO mapping is configured
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
	 *
	 * @return pageable with validated entity sort paths
	 */
	@Override
	protected Pageable resolvePageable(Pageable pageable, PageableArgumentResolverConfig queryConfig) {
		// Validate field mappings to ensure they are well-formed and do not contain conflicts
		fieldMappingsValidator.validate(queryConfig.getFieldMappings());

		FieldResolver fieldResolver = fieldResolverFactory.newFieldResolver(queryConfig);

		List<Sort.Order> newOrders = new ArrayList<>();
		// Validate each requested sort order against entity metadata
		for (Sort.Order order: pageable.getSort()) {
			String reqFieldName = order.getProperty();

			// Resolve the field on the entity class using the requested field name and field mappings
			String fieldName = fieldResolver.resolvePathAndValidateTerminalField(
					reqFieldName,
					terminalField -> sortableFieldValidator.validate(terminalField, reqFieldName)
			);

			newOrders.add(new Sort.Order(order.getDirection(), fieldName));
		}

		Sort sort = Sort.by(newOrders);
		// Reconstruct pageable with mapped sort orders
		if (pageable.isUnpaged()) return Pageable.unpaged(sort);
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}
}
