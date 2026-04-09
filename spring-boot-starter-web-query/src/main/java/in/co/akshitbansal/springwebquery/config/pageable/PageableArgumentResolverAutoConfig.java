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

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.List;

/**
 * Creates pageable argument resolvers that delegate base pagination parsing to Spring Data.
 */
@AutoConfiguration
public class PageableArgumentResolverAutoConfig {

	@Bean
	public WebQueryEntityAwarePageableArgumentResolver entityAwarePageableArgumentResolver(
			PageableHandlerMethodArgumentResolver delegate,
			Validator<SortableFieldValidator.SortableField> sortableFieldValidator,
			Validator<List<FieldMapping>> fieldMappingsValidator
	) {
		return new WebQueryEntityAwarePageableArgumentResolver(delegate, sortableFieldValidator, fieldMappingsValidator);
	}

	@Bean
	public WebQueryDTOAwarePageableArgumentResolver dtoAwarePageableArgumentResolver(
			PageableHandlerMethodArgumentResolver delegate,
			Validator<SortableFieldValidator.SortableField> sortableFieldValidator
	) {
		return new WebQueryDTOAwarePageableArgumentResolver(delegate, sortableFieldValidator);
	}
}
