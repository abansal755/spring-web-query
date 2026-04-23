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

package in.co.akshitbansal.springwebquery.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

import java.util.List;

/**
 * Callback for defining tuple selections for a {@link WebQueryRepository} query.
 *
 * <p>The provided {@code query} may be used to create correlated subqueries or inspect outer-query state. Callers
 * should avoid mutating the outer query in ways that change row cardinality, such as enabling distinct results or
 * adding grouping, because paged execution derives the total count from a separate count query.</p>
 *
 * @param <E> entity type backing the repository
 */
@FunctionalInterface
public interface SelectionsProvider<E> {

	/**
	 * Defines the tuple selections to apply to the result query.
	 *
	 * @param root entity root for the result query
	 * @param query result query being assembled
	 * @param cb criteria builder for creating expressions
	 *
	 * @return selections to project into the tuple result
	 */
	List<Selection<?>> getSelections(
			Root<E> root,
			CriteriaQuery<?> query,
			CriteriaBuilder cb
	);
}
