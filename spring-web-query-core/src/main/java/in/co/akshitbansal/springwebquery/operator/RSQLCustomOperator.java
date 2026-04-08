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

package in.co.akshitbansal.springwebquery.operator;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;

/**
 * Interface for defining custom RSQL operators.
 * <p>
 * Implementing this interface allows you to add new operators to the RSQL parser
 * and define how they should be converted into JPA {@link Predicate}s.
 * </p>
 *
 * @param <T> the type of the operand that this operator handles
 */
public interface RSQLCustomOperator<T extends Comparable<?>> {

	/**
	 * Returns the RSQL {@link ComparisonOperator} for this custom operator.
	 *
	 * @return the comparison operator
	 */
	ComparisonOperator getComparisonOperator();

	/**
	 * Returns the Java type of the operand that this operator handles.
	 *
	 * @return the operand type
	 */
	Class<T> getType();

	/**
	 * Converts the RSQL operator and its input into a JPA {@link Predicate}.
	 *
	 * @param input the input containing the criteria builder, root, and values
	 *
	 * @return the JPA predicate
	 */
	Predicate toPredicate(RSQLCustomPredicateInput input);
}
