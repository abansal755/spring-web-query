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

package in.co.akshitbansal.springwebquery.config.specification;

import cz.jirutka.rsql.parser.RSQLParser;
import in.co.akshitbansal.springwebquery.SpringWebQueryProperties;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwareSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwareSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Creates specification resolvers using the starter's global filtering configuration.
 */
@AutoConfiguration
@Slf4j
public class SpecificationArgumentResolverAutoConfig {

	/**
	 * Creates the entity-aware specification resolver used when {@code @WebQuery}
	 * resolves selectors directly against entity fields and aliases.
	 *
	 * @param rsqlParser shared RSQL parser
	 * @param customPredicates shared custom predicate adapters
	 * @param queryParamNameValidator validator for request parameter name overrides
	 * @param validationRSQLVisitorFactory factory for entity-aware validation visitors
	 * @param fieldMappingsValidator validator for declared field aliases
	 *
	 * @return entity-aware specification resolver
	 */
	@Bean
	@ConditionalOnMissingBean
	public WebQueryEntityAwareSpecificationArgumentResolver entityAwareSpecArgumentResolver(
			SpringWebQueryProperties properties,
			RSQLParser rsqlParser,
			List<RSQLCustomPredicate<?>> customPredicates,
			QueryParamNameValidator queryParamNameValidator,
			ValidationRSQLVisitorFactory validationRSQLVisitorFactory,
			FieldMappingsValidator fieldMappingsValidator
	) {
		return new WebQueryEntityAwareSpecificationArgumentResolver(
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
	 * Creates the DTO-aware specification resolver used when {@code @WebQuery}
	 * exposes a DTO query contract.
	 *
	 * @param rsqlParser shared RSQL parser
	 * @param customPredicates shared custom predicate adapters
	 * @param queryParamNameValidator validator for request parameter name overrides
	 * @param validationRSQLVisitorFactory factory for DTO-aware validation visitors
	 *
	 * @return DTO-aware specification resolver
	 */
	@Bean
	@ConditionalOnMissingBean
	public WebQueryDTOAwareSpecificationArgumentResolver dtoAwareSpecArgumentResolver(
			SpringWebQueryProperties properties,
			RSQLParser rsqlParser,
			List<RSQLCustomPredicate<?>> customPredicates,
			QueryParamNameValidator queryParamNameValidator,
			ValidationRSQLVisitorFactory validationRSQLVisitorFactory
	) {
		return new WebQueryDTOAwareSpecificationArgumentResolver(
				properties.getGlobalFilterParamName(),
				properties.isGlobalAllowAndOperation(),
				properties.isGlobalAllowOrOperation(),
				properties.getGlobalMaxASTDepth(),
				rsqlParser,
				customPredicates,
				queryParamNameValidator,
				validationRSQLVisitorFactory
		);
	}
}
