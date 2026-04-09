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

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.resolver.FieldResolver;
import in.co.akshitbansal.springwebquery.resolver.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ValidationRSQLVisitorFactory {

	private final FieldResolverFactory fieldResolverFactory;
	private final Validator<FilterableFieldValidator.FilterableField> filterableFieldValidator;

	public DTOValidationRSQLVisitor newDTOValidationRSQLVisitor(
			Class<?> entityClass,
			Class<?> dtoClass,
			boolean andNodeAllowed,
			boolean orNodeAllowed,
			int maxDepth
	) {
		FieldResolver fieldResolver = fieldResolverFactory.newDtoAwareFieldResolver(entityClass, dtoClass);
		return new DTOValidationRSQLVisitor(
				fieldResolver,
				filterableFieldValidator,
				andNodeAllowed,
				orNodeAllowed,
				maxDepth
		);
	}

	public EntityValidationRSQLVisitor newEntityValidationRSQLVisitor(
			Class<?> entityClass,
			List<FieldMapping> fieldMappings,
			boolean andNodeAllowed,
			boolean orNodeAllowed,
			int maxDepth
	) {
		FieldResolver fieldResolver = fieldResolverFactory.newEntityAwareFieldResolver(entityClass, fieldMappings);
		return new EntityValidationRSQLVisitor(
				fieldResolver,
				filterableFieldValidator,
				andNodeAllowed,
				orNodeAllowed,
				maxDepth
		);
	}
}
