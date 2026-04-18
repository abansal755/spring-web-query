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

import in.co.akshitbansal.springwebquery.SpringWebQueryProperties;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.text.MessageFormat;

/**
 * Publishes the validated global filtering configuration used by the starter's
 * specification resolver auto-configuration.
 */
@AutoConfiguration
@Slf4j
public class SpringWebQueryPropertiesAutoConfig {

	/**
	 * Creates the shared immutable container for global filtering defaults
	 * contributed through application configuration.
	 *
	 * @param globalFilterParamName configured request parameter name for filter expressions
	 * @param globalAllowAndOperation whether logical AND nodes are allowed by default
	 * @param globalAllowOrOperation whether logical OR nodes are allowed by default
	 * @param globalMaxASTDepth configured maximum allowed depth for parsed RSQL ASTs
	 * @param queryParamNameValidator validator used for configured filter parameter names
	 *
	 * @return validated global filtering properties
	 *
	 * @throws QueryConfigurationException if the configured maximum AST depth is negative
	 */
	@Bean
	@ConditionalOnMissingBean
	public SpringWebQueryProperties springWebQueryProperties(
			@Value("${spring-web-query.filtering.filter-param-name:filter}") String globalFilterParamName,
			@Value("${spring-web-query.filtering.allow-and-operation:true}") boolean globalAllowAndOperation,
			@Value("${spring-web-query.filtering.allow-or-operation:false}") boolean globalAllowOrOperation,
			@Value("${spring-web-query.filtering.max-ast-depth:1}") int globalMaxASTDepth,
			@Value("${spring-web-query.field-resolution.caching.enabled:false}") boolean fieldResolutionCachingEnabled,
			@Value("${spring-web-query.field-resolution.caching.failed-resolutions-max-capacity:1000}") int failedResolutionsMaxCapacity,
			QueryParamNameValidator queryParamNameValidator
	) {
		// Validating globalFilterParamName
		queryParamNameValidator.validate(globalFilterParamName);

		// Validating globalMaxASTDepth
		if (globalMaxASTDepth < 0) {
			throw new QueryConfigurationException(MessageFormat.format(
					"Value for spring-web-query.filtering.max-ast-depth must be non-negative. Provided value: {0}",
					globalMaxASTDepth
			));
		}

		SpringWebQueryProperties properties = new SpringWebQueryProperties(
				globalFilterParamName,
				globalAllowAndOperation,
				globalAllowOrOperation,
				globalMaxASTDepth,
				fieldResolutionCachingEnabled,
				failedResolutionsMaxCapacity
		);
		log.info("Global spring-web-query configuration: {}", properties);
		return properties;
	}
}
