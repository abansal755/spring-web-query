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

package in.co.akshitbansal.springwebquery.exception;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import lombok.Getter;

import java.util.Set;

/**
 * Indicates that a query uses an operator that is not allowed for a specific field path.
 */
@Getter
public class QueryForbiddenOperatorException extends QueryFieldValidationException {

	/**
	 * Operator used in the query that failed validation.
	 */
	private final ComparisonOperator operator;

	/**
	 * Set of operators allowed for the target field path.
	 */
	private final Set<ComparisonOperator> allowedOperators;

	/**
	 * Creates a new forbidden operator exception.
	 *
	 * @param message validation error details
	 * @param fieldPath query field path associated with the failure
	 * @param operator operator used in the query
	 * @param allowedOperators operators allowed for the field
	 */
	public QueryForbiddenOperatorException(String message, String fieldPath, ComparisonOperator operator, Set<ComparisonOperator> allowedOperators) {
		super(message, fieldPath);
		this.operator = operator;
		this.allowedOperators = Set.copyOf(allowedOperators);
	}

	/**
	 * Creates a new forbidden operator exception with an underlying cause.
	 *
	 * @param message validation error details
	 * @param fieldPath query field path associated with the failure
	 * @param operator operator used in the query
	 * @param allowedOperators operators allowed for the field
	 * @param cause root cause of the validation failure
	 */
	public QueryForbiddenOperatorException(String message, String fieldPath, ComparisonOperator operator, Set<ComparisonOperator> allowedOperators, Throwable cause) {
		super(message, fieldPath, cause);
		this.operator = operator;
		this.allowedOperators = Set.copyOf(allowedOperators);
	}
}
