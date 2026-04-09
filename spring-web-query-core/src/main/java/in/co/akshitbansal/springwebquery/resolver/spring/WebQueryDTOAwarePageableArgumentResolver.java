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

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.resolver.DTOAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.FieldResolver;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
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
	 */
	public WebQueryDTOAwarePageableArgumentResolver(
			PageableHandlerMethodArgumentResolver delegate,
			SortableFieldValidator sortableFieldValidator
	) {
		super(delegate, sortableFieldValidator);
	}

	/**
	 * Determines whether this resolver should handle the given parameter.
	 *
	 * @param parameter method parameter under inspection
	 *
	 * @return {@code true} when parameter is {@code Pageable} with
	 * method-level {@link WebQuery} and a configured DTO class
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return super.supportsParameter(parameter) // supportsParameter in superclass checks for method-level @WebQuery presence
				&& getWebQueryAnnotation(parameter).dtoClass() != void.class; // so no exception handling is needed
	}

	/**
	 * Validates DTO-facing sort properties and maps them to entity paths.
	 *
	 * @param pageable pageable parsed from the request
	 * @param queryConfig effective query configuration for the current request
	 *
	 * @return pageable with validated entity sort paths derived from DTO selectors
	 */
	@Override
	protected Pageable resolvePageable(Pageable pageable, QueryConfiguration queryConfig) {
		FieldResolver fieldResolver = new DTOAwareFieldResolver(
				queryConfig.getEntityClass(),
				queryConfig.getDtoClass()
		);

		List<Sort.Order> newOrders = new ArrayList<>();
		for (Sort.Order order: pageable.getSort()) {
			String dtoPath = order.getProperty();
			// Build the corresponding entity field path from the DTO path and validate the terminal field for sortability
			String entityPath = fieldResolver.resolvePathAndValidateTerminalField(
					dtoPath,
					terminalField -> sortableFieldValidator.validate(new SortableFieldValidator.SortableField(terminalField, dtoPath))
			);
			newOrders.add(new Sort.Order(order.getDirection(), entityPath));
		}
		Sort sort = Sort.by(newOrders);
		// Reconstruct pageable with mapped sort orders
		if (pageable.isUnpaged()) return Pageable.unpaged(sort);
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}
}
