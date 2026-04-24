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

import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapperFactory;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Creates fully configured {@link ValidationRSQLVisitor} instances for a
 * specific entity/DTO pair.
 */
@RequiredArgsConstructor
public class ValidationRSQLVisitorFactory {

	/**
	 * Factory used to create DTO-to-entity path mappers.
	 */
	@NonNull
	private final DTOToEntityPathMapperFactory pathMapperFactory;

	/**
	 * Validator used to enforce field-level filterability rules.
	 */
	@NonNull
	private final FilterableFieldValidator filterableFieldValidator;

	/**
	 * Creates a validation visitor for the supplied query contract.
	 *
	 * @param entityClass entity type that ultimately backs predicate creation
	 * @param dtoClass DTO type exposed to callers for filtering
	 * @param allowAndOperation whether logical {@code AND} is allowed
	 * @param allowOrOperation whether logical {@code OR} is allowed
	 * @param maxASTDepth maximum AST depth accepted during validation
	 *
	 * @return configured validation visitor
	 */
	public ValidationRSQLVisitor newValidationRSQLVisitor(
			@NonNull Class<?> entityClass,
			@NonNull Class<?> dtoClass,
			boolean allowAndOperation,
			boolean allowOrOperation,
			int maxASTDepth
	) {
		DTOToEntityPathMapper pathMapper = pathMapperFactory.newMapper(entityClass, dtoClass);
		return new ValidationRSQLVisitor(allowAndOperation, allowOrOperation, maxASTDepth, pathMapper, filterableFieldValidator);
	}
}
