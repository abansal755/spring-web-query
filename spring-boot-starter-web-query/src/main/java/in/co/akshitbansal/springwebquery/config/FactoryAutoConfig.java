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

import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapperFactory;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@Slf4j
public class FactoryAutoConfig {

	@Bean
	@ConditionalOnProperty(
			name = "spring-web-query.field-resolution.caching.enabled",
			havingValue = "true",
			matchIfMissing = true
	)
	@ConditionalOnMissingBean
	public DTOToEntityPathMapperFactory dtoToEntityPathMapperFactoryWithCaching(
			@Value("${spring-web-query.field-resolution.caching.failed-resolutions-max-capacity:1000}") int failedResolutionsMaxCapacity,
			@Value("${spring-web-query.field-resolution.caching.lock-stripe-count:32}") int lockStripeCount
	) {
		return new DTOToEntityPathMapperFactory(failedResolutionsMaxCapacity, lockStripeCount);
	}

	@Bean
	@ConditionalOnProperty(
			name = "spring-web-query.field-resolution.caching.enabled",
			havingValue = "false"
	)
	@ConditionalOnMissingBean
	public DTOToEntityPathMapperFactory dtoToEntityPathMapperFactoryWithoutCaching() {
		return new DTOToEntityPathMapperFactory();
	}

	@Bean
	@ConditionalOnMissingBean
	public ValidationRSQLVisitorFactory validationRSQLVisitorFactory(
			DTOToEntityPathMapperFactory pathMapperFactory,
			FilterableFieldValidator filterableFieldValidator
	) {
		return new ValidationRSQLVisitorFactory(pathMapperFactory, filterableFieldValidator);
	}
}
