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
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.resolver.field.cache.DTOAwareFieldResolutionCache;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Publishes shared infrastructure beans used by the starter's query resolvers.
 */
@AutoConfiguration
@Slf4j
public class FactoryAutoConfig {

	/**
	 * Creates the shared cache used by DTO-aware field resolvers when caching is
	 * enabled.
	 *
	 * @param properties validated global starter properties
	 *
	 * @return DTO-aware field-resolution cache
	 */
	@Bean
	@ConditionalOnProperty(
			name = "spring-web-query.field-resolution.dto-aware.caching.enabled",
			havingValue = "true",
			matchIfMissing = true
	)
	@ConditionalOnMissingBean
	public DTOAwareFieldResolutionCache dtoAwareFieldResolutionCache(SpringWebQueryProperties properties) {
		log.info(
				"Registered {} for caching DTO-aware field resolutions with max capacity for failed resolutions: {}",
				DTOAwareFieldResolutionCache.class.getSimpleName(), properties.getFailedDTOAwareResolutionCachingMaxCapacity()
		);
		return new DTOAwareFieldResolutionCache(properties.getFailedDTOAwareResolutionCachingMaxCapacity(), properties.getDtoAwareFieldResolutionCachingKeyLockPoolSize());
	}

	/**
	 * Creates the shared field-resolver factory backed by the DTO-resolution
	 * cache.
	 *
	 * @param cache cache for memoizing DTO-aware field-resolution results
	 *
	 * @return field-resolver factory that produces cached DTO-aware resolvers
	 */
	@Bean
	@ConditionalOnBean(DTOAwareFieldResolutionCache.class)
	@ConditionalOnMissingBean
	public FieldResolverFactory fieldResolverFactoryWithDTOCache(DTOAwareFieldResolutionCache cache) {
		return new FieldResolverFactory(cache);
	}

	/**
	 * Creates the shared factory for field resolver instances when caching is not
	 * enabled.
	 *
	 * @return field resolver factory
	 */
	@Bean
	@ConditionalOnMissingBean
	public FieldResolverFactory fieldResolverFactoryWithoutDTOCache() {
		return new FieldResolverFactory();
	}

	/**
	 * Creates the shared factory for validation visitors used during RSQL
	 * specification resolution.
	 *
	 * @param fieldResolverFactory factory for DTO-aware and entity-aware field resolvers
	 * @param filterableFieldValidator validator used for terminal field/operator checks
	 *
	 * @return validation visitor factory
	 */
	@Bean
	@ConditionalOnMissingBean
	public ValidationRSQLVisitorFactory validationRSQLVisitorFactory(
			FieldResolverFactory fieldResolverFactory,
			FilterableFieldValidator filterableFieldValidator
	) {
		return new ValidationRSQLVisitorFactory(fieldResolverFactory, filterableFieldValidator);
	}
}
