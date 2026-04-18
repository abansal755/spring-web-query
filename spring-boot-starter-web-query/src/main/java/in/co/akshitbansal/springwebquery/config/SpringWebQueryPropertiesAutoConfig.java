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
import in.co.akshitbansal.springwebquery.validator.KeyLockPoolSizeValidator;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
	 * @param dtoAwareFieldResolutionCachingEnabled whether DTO-aware
	 * field-resolution caching is enabled
	 * @param failedDTOAwareResolutionCachingMaxCapacity maximum number of failed
	 * DTO-aware resolutions to cache
	 * @param dtoAwareFieldResolutionCachingKeyLockPoolSize number of striped
	 * locks used to coordinate cache population for selector keys
	 * @param queryParamNameValidator validator used for configured filter parameter names
	 *
	 * @return validated global filtering properties
	 *
	 * @throws QueryConfigurationException if the configured maximum AST depth is
	 * negative or the failed-resolution cache capacity is non-positive
	 * @throws IllegalArgumentException if the configured lock-pool size is not a
	 * positive power of two
	 */
	@Bean
	public SpringWebQueryProperties springWebQueryProperties(
			@Value("${spring-web-query.filtering.filter-param-name:filter}") String globalFilterParamName,
			@Value("${spring-web-query.filtering.allow-and-operation:true}") boolean globalAllowAndOperation,
			@Value("${spring-web-query.filtering.allow-or-operation:false}") boolean globalAllowOrOperation,
			@Value("${spring-web-query.filtering.max-ast-depth:1}") int globalMaxASTDepth,
			@Value("${spring-web-query.field-resolution.dto-aware.caching.enabled:true}") boolean dtoAwareFieldResolutionCachingEnabled,
			@Value("${spring-web-query.field-resolution.dto-aware.caching.failed-resolutions-max-capacity:1000}") int failedDTOAwareResolutionCachingMaxCapacity,
			@Value("${spring-web-query.field-resolution.dto-aware.caching.key-lock-pool-size:128}") int dtoAwareFieldResolutionCachingKeyLockPoolSize,
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

		// Only validate the following properties if caching is enabled
		if(dtoAwareFieldResolutionCachingEnabled) {
			// Validating failedResolutionsMaxCapacity
			if (failedDTOAwareResolutionCachingMaxCapacity <= 0) {
				throw new QueryConfigurationException(MessageFormat.format(
						"Value for spring-web-query.field-resolution.dto-aware.caching.failed-resolutions-max-capacity must be positive. Provided value: {0}",
						failedDTOAwareResolutionCachingMaxCapacity
				));
			}

			// Validating dtoAwareFieldResolutionCachingKeyLockPoolSize
			KeyLockPoolSizeValidator keyLockPoolSizeValidator = new KeyLockPoolSizeValidator();
			keyLockPoolSizeValidator.validate(dtoAwareFieldResolutionCachingKeyLockPoolSize);
		}

		SpringWebQueryProperties properties = new SpringWebQueryProperties(
				globalFilterParamName,
				globalAllowAndOperation,
				globalAllowOrOperation,
				globalMaxASTDepth,
				dtoAwareFieldResolutionCachingEnabled,
				failedDTOAwareResolutionCachingMaxCapacity,
				dtoAwareFieldResolutionCachingKeyLockPoolSize
		);
		log.info("Global spring-web-query configuration: {}", properties);
		return properties;
	}
}
