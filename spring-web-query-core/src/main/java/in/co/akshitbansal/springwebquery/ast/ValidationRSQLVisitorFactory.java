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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ValidationRSQLVisitorFactory {

	private final DTOToEntityPathMapperFactory pathMapperFactory;
	private final FilterableFieldValidator filterableFieldValidator;

	public ValidationRSQLVisitor newValidationRSQLVisitor(
			Class<?> entityClass,
			Class<?> dtoClass,
			boolean allowAndOperation,
			boolean allowOrOperation,
			int maxASTDepth
	) {
		DTOToEntityPathMapper pathMapper = pathMapperFactory.newMapper(entityClass, dtoClass);
		return new ValidationRSQLVisitor(allowAndOperation, allowOrOperation, maxASTDepth, pathMapper, filterableFieldValidator);
	}
}
