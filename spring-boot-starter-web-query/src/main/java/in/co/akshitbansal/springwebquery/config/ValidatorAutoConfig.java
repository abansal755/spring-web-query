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

package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * Publishes the validator components shared by pageable and specification
 * resolver auto-configuration.
 */
@AutoConfiguration
public class ValidatorAutoConfig {

	/**
	 * Registers the validator used for configured filter parameter names.
	 *
	 * @return query parameter name validator
	 */
	@Bean
	@ConditionalOnMissingBean
	public QueryParamNameValidator queryParamNameValidator() {
		return new QueryParamNameValidator();
	}

	/**
	 * Registers the validator used to enforce {@code @Sortable} constraints.
	 *
	 * @return sortable field validator
	 */
	@Bean
	@ConditionalOnMissingBean
	public SortableFieldValidator sortableFieldValidator() {
		return new SortableFieldValidator();
	}

	/**
	 * Registers the validator used to check explicit entity field aliases.
	 *
	 * @return field-mappings validator
	 */
	@Bean
	@ConditionalOnMissingBean
	public FieldMappingsValidator fieldMappingsValidator() {
		return new FieldMappingsValidator();
	}

	/**
	 * Registers the validator used to enforce {@code @RSQLFilterable}
	 * constraints, backed by the current custom operator registry.
	 *
	 * @param customOperatorMap custom operators keyed by implementation class
	 *
	 * @return filterable field validator
	 */
	@Bean
	@ConditionalOnMissingBean
	public FilterableFieldValidator filterableFieldValidator(Map<Class<?>, RSQLCustomOperator<?>> customOperatorMap) {
		return new FilterableFieldValidator(customOperatorMap);
	}
}
