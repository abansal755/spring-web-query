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

package in.co.akshitbansal.springwebquery.config.pageable;

import in.co.akshitbansal.springwebquery.resolver.field.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

/**
 * Creates pageable argument resolvers that delegate base pagination parsing to Spring Data.
 */
@AutoConfiguration
public class PageableArgumentResolverAutoConfig {

	/**
	 * Creates the entity-aware pageable resolver used when {@code @WebQuery}
	 * operates directly on entity fields and aliases.
	 *
	 * @param delegate Spring Data's base pageable resolver
	 * @param sortableFieldValidator validator for sortable terminal fields
	 * @param fieldMappingsValidator validator for declared field aliases
	 *
	 * @return entity-aware pageable resolver
	 */
	@Bean
	public WebQueryEntityAwarePageableArgumentResolver entityAwarePageableArgumentResolver(
			PageableHandlerMethodArgumentResolver delegate,
			SortableFieldValidator sortableFieldValidator,
			FieldMappingsValidator fieldMappingsValidator,
			FieldResolverFactory fieldResolverFactory
	) {
		return new WebQueryEntityAwarePageableArgumentResolver(
				delegate,
				sortableFieldValidator,
				fieldMappingsValidator,
				fieldResolverFactory
		);
	}

	/**
	 * Creates the DTO-aware pageable resolver used when {@code @WebQuery}
	 * exposes a DTO query contract.
	 *
	 * @param delegate Spring Data's base pageable resolver
	 * @param sortableFieldValidator validator for sortable terminal fields
	 *
	 * @return DTO-aware pageable resolver
	 */
	@Bean
	public WebQueryDTOAwarePageableArgumentResolver dtoAwarePageableArgumentResolver(
			PageableHandlerMethodArgumentResolver delegate,
			SortableFieldValidator sortableFieldValidator,
			FieldResolverFactory fieldResolverFactory
	) {
		return new WebQueryDTOAwarePageableArgumentResolver(delegate, sortableFieldValidator, fieldResolverFactory);
	}
}
