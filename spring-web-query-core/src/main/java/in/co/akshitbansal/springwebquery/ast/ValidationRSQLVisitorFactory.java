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

import in.co.akshitbansal.springwebquery.resolver.field.DTOAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.EntityAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.enums.ResolutionMode;
import in.co.akshitbansal.springwebquery.resolver.spring.config.SpecificationArgumentResolverConfig;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Factory for creating validation visitors with the shared resolver and
 * filterability infrastructure used across specification resolvers.
 */
@RequiredArgsConstructor
public class ValidationRSQLVisitorFactory {

	/**
	 * Factory for creating the field resolver that matches the active query contract.
	 */
	@NonNull
	private final FieldResolverFactory fieldResolverFactory;

	/**
	 * Validator shared by created visitors for terminal-field filterability checks.
	 */
	@NonNull
	private final FilterableFieldValidator filterableFieldValidator;

	/**
	 * Creates a validation visitor matching the supplied specification resolver configuration.
	 *
	 * <p>DTO-aware configurations produce {@link DTOValidationRSQLVisitor}; otherwise
	 * an {@link EntityValidationRSQLVisitor} is created.</p>
	 *
	 * @param config effective specification resolver configuration
	 *
	 * @return validation visitor aligned with the configured query model
	 */
	public AbstractValidationRSQLVisitor newValidationRSQLVisitor(SpecificationArgumentResolverConfig config) {
		FieldResolver fieldResolver = fieldResolverFactory.newFieldResolver(config);

		// DTOValidationRSQLVisitor
		if (config.getResolutionMode() == ResolutionMode.DTO_AWARE) {
			return new DTOValidationRSQLVisitor(
					(DTOAwareFieldResolver) fieldResolver,
					filterableFieldValidator,
					config.isAndNodeAllowed(),
					config.isOrNodeAllowed(),
					config.getMaxASTDepth()
			);
		}

		// EntityValidationRSQLVisitor
		return new EntityValidationRSQLVisitor(
				(EntityAwareFieldResolver) fieldResolver,
				filterableFieldValidator,
				config.isAndNodeAllowed(),
				config.isOrNodeAllowed(),
				config.getMaxASTDepth()
		);
	}
}
