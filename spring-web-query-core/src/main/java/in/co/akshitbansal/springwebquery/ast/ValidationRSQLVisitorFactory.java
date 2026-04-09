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

package in.co.akshitbansal.springwebquery.ast;

import in.co.akshitbansal.springwebquery.resolver.FieldResolver;
import in.co.akshitbansal.springwebquery.resolver.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.resolver.spring.config.SpecificationArgumentResolverConfig;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import lombok.RequiredArgsConstructor;

/**
 * Factory for creating validation visitors with the shared resolver and
 * filterability infrastructure used across specification resolvers.
 */
@RequiredArgsConstructor
public class ValidationRSQLVisitorFactory {

	private final FieldResolverFactory fieldResolverFactory;
	private final FilterableFieldValidator filterableFieldValidator;

	public AbstractValidationRSQLVisitor newValidationRSQLVisitor(SpecificationArgumentResolverConfig config) {
		FieldResolver fieldResolver = fieldResolverFactory.newFieldResolver(config);
		if(config.getDtoClass() != void.class)
			return new DTOValidationRSQLVisitor(fieldResolver, filterableFieldValidator, config.isAndNodeAllowed(), config.isOrNodeAllowed(), config.getMaxASTDepth());
		return new EntityValidationRSQLVisitor(fieldResolver, filterableFieldValidator, config.isAndNodeAllowed(), config.isOrNodeAllowed(), config.getMaxASTDepth());
	}
}
