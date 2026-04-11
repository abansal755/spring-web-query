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

package in.co.akshitbansal.springwebquery.config.resolver;

import cz.jirutka.rsql.parser.RSQLParser;
import in.co.akshitbansal.springwebquery.SpringWebQueryProperties;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQuerySpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.List;

/**
 * Publishes the merged WebQuery argument resolvers used for pageable and
 * specification handling.
 */
@AutoConfiguration
public class ArgumentResolverAutoConfig {

	/**
	 * Creates the unified resolver for {@code Specification} parameters.
	 *
	 * @param properties global WebQuery filtering defaults
	 * @param rsqlParser shared RSQL parser
	 * @param customPredicates custom predicates used during JPA conversion
	 * @param queryParamNameValidator validator for configured filter names
	 * @param validationRSQLVisitorFactory factory for mode-aware validation visitors
	 * @param fieldMappingsValidator validator for declared field mappings
	 *
	 * @return resolver for {@code Specification} parameters
	 */
	@Bean
	@ConditionalOnMissingBean
	public WebQuerySpecificationArgumentResolver webQuerySpecificationArgumentResolver(
			SpringWebQueryProperties properties,
			RSQLParser rsqlParser,
			List<RSQLCustomPredicate<?>> customPredicates,
			QueryParamNameValidator queryParamNameValidator,
			ValidationRSQLVisitorFactory validationRSQLVisitorFactory,
			FieldMappingsValidator fieldMappingsValidator
	) {
		return new WebQuerySpecificationArgumentResolver(
				properties.getGlobalFilterParamName(),
				properties.isGlobalAllowAndOperation(),
				properties.isGlobalAllowOrOperation(),
				properties.getGlobalMaxASTDepth(),
				rsqlParser,
				customPredicates,
				queryParamNameValidator,
				validationRSQLVisitorFactory,
				fieldMappingsValidator
		);
	}

	/**
	 * Creates the unified resolver for {@code Pageable} parameters.
	 *
	 * @param delegate Spring's pageable argument resolver
	 * @param sortableFieldValidator validator for sortable terminal fields
	 * @param fieldResolverFactory factory for DTO-aware and entity-aware field resolution
	 * @param fieldMappingsValidator validator for declared field mappings
	 *
	 * @return resolver for {@code Pageable} parameters
	 */
	@Bean
	@ConditionalOnMissingBean
	public WebQueryPageableArgumentResolver webQueryPageableArgumentResolver(
			PageableHandlerMethodArgumentResolver delegate,
			SortableFieldValidator sortableFieldValidator,
			FieldResolverFactory fieldResolverFactory,
			FieldMappingsValidator fieldMappingsValidator
	) {
		return new WebQueryPageableArgumentResolver(
				delegate,
				sortableFieldValidator,
				fieldResolverFactory,
				fieldMappingsValidator
		);
	}
}
