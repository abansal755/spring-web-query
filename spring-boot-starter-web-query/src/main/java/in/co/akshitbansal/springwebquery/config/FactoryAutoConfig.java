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
import in.co.akshitbansal.springwebquery.resolver.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Publishes shared infrastructure beans used by the starter's query resolvers.
 */
@AutoConfiguration
public class FactoryAutoConfig {

	/**
	 * Creates the shared factory for field resolver instances.
	 *
	 * @return field resolver factory
	 */
	@Bean
	public FieldResolverFactory fieldResolverFactory() {
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
	public ValidationRSQLVisitorFactory validationRSQLVisitorFactory(
			FieldResolverFactory fieldResolverFactory,
			FilterableFieldValidator filterableFieldValidator
	) {
		return new ValidationRSQLVisitorFactory(fieldResolverFactory, filterableFieldValidator);
	}
}
