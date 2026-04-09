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
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwareSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwareSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.text.MessageFormat;
import java.util.List;

/**
 * Creates specification resolvers using the starter's global filtering configuration.
 */
@AutoConfiguration
@Slf4j
public class SpecificationArgumentResolverAutoConfig {

	private final String GLOBAL_FILTER_PARAM_NAME;
	private final boolean GLOBAL_ALLOW_OR_OPERATION;
	private final boolean GLOBAL_ALLOW_AND_OPERATION;
	private final int GLOBAL_MAX_AST_DEPTH;

	/**
	 * Creates the auto-configuration and validates the global filtering
	 * properties contributed through application configuration.
	 *
	 * @param GLOBAL_FILTER_PARAM_NAME global default request parameter for RSQL filters
	 * @param GLOBAL_ALLOW_OR_OPERATION whether logical OR is allowed by default
	 * @param GLOBAL_ALLOW_AND_OPERATION whether logical AND is allowed by default
	 * @param GLOBAL_MAX_AST_DEPTH maximum AST depth allowed by default
	 * @param queryParamNameValidator validator used for the configured filter parameter name
	 */
	public SpecificationArgumentResolverAutoConfig(
			@Value("${spring-web-query.filtering.filter-param-name:filter}") String GLOBAL_FILTER_PARAM_NAME,
			@Value("${spring-web-query.filtering.allow-or-operation:false}") boolean GLOBAL_ALLOW_OR_OPERATION,
			@Value("${spring-web-query.filtering.allow-and-operation:true}") boolean GLOBAL_ALLOW_AND_OPERATION,
			@Value("${spring-web-query.filtering.max-ast-depth:1}") int GLOBAL_MAX_AST_DEPTH,
			QueryParamNameValidator queryParamNameValidator
	) {
		// Validating GLOBAL_FILTER_PARAM_NAME
		queryParamNameValidator.validate(GLOBAL_FILTER_PARAM_NAME);
		this.GLOBAL_FILTER_PARAM_NAME = GLOBAL_FILTER_PARAM_NAME;

		this.GLOBAL_ALLOW_OR_OPERATION = GLOBAL_ALLOW_OR_OPERATION;
		this.GLOBAL_ALLOW_AND_OPERATION = GLOBAL_ALLOW_AND_OPERATION;

		// Validating GLOBAL_MAX_AST_DEPTH
		if (GLOBAL_MAX_AST_DEPTH < 0) {
			throw new QueryConfigurationException(MessageFormat.format(
					"Value for spring-web-query.filtering.max-ast-depth must be non-negative. Provided value: {0}",
					GLOBAL_MAX_AST_DEPTH
			));
		}
		this.GLOBAL_MAX_AST_DEPTH = GLOBAL_MAX_AST_DEPTH;

		log.info(
				"Found global filtering configuration: filterParamName = {}, allowOrOperation = {}, allowAndOperation = {}, maxAstDepth = {}",
				GLOBAL_FILTER_PARAM_NAME, GLOBAL_ALLOW_OR_OPERATION, GLOBAL_ALLOW_AND_OPERATION, GLOBAL_MAX_AST_DEPTH
		);
	}

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
	public WebQueryEntityAwareSpecificationArgumentResolver entityAwareSpecArgumentResolver(
			RSQLParser rsqlParser,
			List<RSQLCustomPredicate<?>> customPredicates,
			QueryParamNameValidator queryParamNameValidator,
			ValidationRSQLVisitorFactory validationRSQLVisitorFactory,
			FieldMappingsValidator fieldMappingsValidator
	) {
		return new WebQueryEntityAwareSpecificationArgumentResolver(
				GLOBAL_FILTER_PARAM_NAME,
				GLOBAL_ALLOW_AND_OPERATION,
				GLOBAL_ALLOW_OR_OPERATION,
				GLOBAL_MAX_AST_DEPTH,
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
	public WebQueryDTOAwareSpecificationArgumentResolver dtoAwareSpecArgumentResolver(
			RSQLParser rsqlParser,
			List<RSQLCustomPredicate<?>> customPredicates,
			QueryParamNameValidator queryParamNameValidator,
			ValidationRSQLVisitorFactory validationRSQLVisitorFactory
	) {
		return new WebQueryDTOAwareSpecificationArgumentResolver(
				GLOBAL_FILTER_PARAM_NAME,
				GLOBAL_ALLOW_AND_OPERATION,
				GLOBAL_ALLOW_OR_OPERATION,
				GLOBAL_MAX_AST_DEPTH,
				rsqlParser,
				customPredicates,
				queryParamNameValidator,
				validationRSQLVisitorFactory
		);
	}
}
