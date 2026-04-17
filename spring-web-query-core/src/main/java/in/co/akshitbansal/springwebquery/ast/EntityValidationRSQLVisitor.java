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

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.resolver.field.EntityAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.ResolutionResult;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;

/**
 * RSQL AST visitor that validates selectors directly against an entity model.
 *
 * <p>This visitor resolves request selectors to entity fields, applies any
 * configured {@link FieldMapping} aliases, and ensures that the resolved
 * terminal field is filterable for the requested operator.</p>
 *
 * <p>It is typically used by entity-aware specification resolvers before
 * converting a parsed RSQL tree into a Spring Data JPA
 * {@link org.springframework.data.jpa.domain.Specification}.</p>
 *
 * @see RSQLFilterable
 * @see RSQLDefaultOperator
 * @see cz.jirutka.rsql.parser.ast.Node
 */
public class EntityValidationRSQLVisitor extends AbstractValidationRSQLVisitor {

	/**
	 * Creates a new entity validation visitor with the specified configuration.
	 *
	 * @param fieldResolver resolver that validates selectors against the effective entity contract
	 * @param filterableFieldValidator validator used to enforce {@link RSQLFilterable} constraints
	 * @param andNodeAllowed whether logical AND operator is allowed
	 * @param orNodeAllowed whether logical OR operator is allowed
	 * @param maxDepth maximum allowed depth for the RSQL AST
	 */
	protected EntityValidationRSQLVisitor(
			EntityAwareFieldResolver fieldResolver,
			FilterableFieldValidator filterableFieldValidator,
			boolean andNodeAllowed,
			boolean orNodeAllowed,
			int maxDepth
	) {
		super(
				fieldResolver,
				filterableFieldValidator,
				andNodeAllowed,
				orNodeAllowed,
				maxDepth
		);
	}

	/**
	 * Validates a comparison node against the entity class.
	 *
	 * @param node the comparison node to validate
	 *
	 * @throws QueryValidationException if the field is unknown, not allowed,
	 * or the operator is invalid for the
	 * resolved terminal field
	 * @throws QueryConfigurationException if the field mapping is misconfigured
	 */
	@Override
	protected void validateComparisonNode(ComparisonNode node) {
		// Extract the field name and operator from the RSQL node
		String reqFieldName = node.getSelector();
		ComparisonOperator operator = node.getOperator();

		// Resolve the field on the entity class using the requested field name and field mappings
		ResolutionResult result = fieldResolver.resolvePath(reqFieldName);
		filterableFieldValidator.validate(result.getTerminalField(), operator, reqFieldName);
	}
}
