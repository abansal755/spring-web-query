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
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.resolver.FieldResolver;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * RSQL AST visitor that validates filters against a DTO contract and maps DTO
 * property paths to entity property paths via a shared
 * {@link in.co.akshitbansal.springwebquery.resolver.FieldResolver} contract.
 *
 * <p>This visitor is used when {@link in.co.akshitbansal.springwebquery.annotation.WebQuery#dtoClass()}
 * is configured. It enforces that:</p>
 * <ul>
 *     <li>The requested selector exists on the DTO type.</li>
 *     <li>The terminal DTO field is annotated with
 *         {@link RSQLFilterable}.</li>
 *     <li>The requested operator is allowed for that DTO field.</li>
 *     <li>The resulting mapped entity path can be resolved on the configured entity type.</li>
 * </ul>
 *
 * <p>During validation, selector mappings are captured and exposed via
 * {@link #getFieldMappings()} so downstream query construction can apply
 * DTO-to-entity path translation.</p>
 */
public class DTOValidationRSQLVisitor extends AbstractValidationRSQLVisitor {

	/**
	 * Mutable selector map accumulated during traversal.
	 */
	private final Map<String, String> fieldMappings;

	/**
	 * Creates a DTO-aware validation visitor.
	 *
	 * @param entityClass target entity type used for final path validation
	 * @param dtoClass DTO type used to validate incoming selector paths
	 * @param customOperators registered custom operators keyed by implementation class
	 * @param andNodeAllowed whether logical AND operator is allowed
	 * @param orNodeAllowed whether logical OR operator is allowed
	 * @param maxDepth maximum allowed depth for the RSQL AST
	 */
	public DTOValidationRSQLVisitor(
			FieldResolver fieldResolver,
			Validator<FilterableFieldValidator.FilterableField> filterableFieldValidator,
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
		this.fieldMappings = new HashMap<>();
	}

	/**
	 * Returns immutable selector mappings generated while visiting nodes.
	 *
	 * @return map from request DTO path to resolved entity path collected so far
	 */
	public Map<String, String> getFieldMappings() {
		return Collections.unmodifiableMap(fieldMappings);
	}

	/**
	 * Validates a single comparison expression and records DTO-to-entity mapping.
	 *
	 * @param node node to validate
	 */
	@Override
	protected void validateComparisonNode(ComparisonNode node) {
		// Extract the field name and operator from the RSQL node
		String dtoPath = node.getSelector();
		ComparisonOperator operator = node.getOperator();

		// Build the corresponding entity field path from the DTO path and validate the terminal field for filterability
		String entityPath = fieldResolver.resolvePathAndValidateTerminalField(
				dtoPath,
				terminalField -> filterableFieldValidator.validate(new FilterableFieldValidator.FilterableField(terminalField, operator, dtoPath))
		);

		// Store the mapping from DTO path to entity path for later use during query construction
		fieldMappings.put(dtoPath, entityPath);
	}
}
