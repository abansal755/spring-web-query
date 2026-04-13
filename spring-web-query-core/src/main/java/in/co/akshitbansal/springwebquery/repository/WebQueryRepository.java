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

import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Repository fragment for executing {@link Specification}-based tuple selections.
 *
 * @param <T> entity type backing the repository
 */
public interface WebQueryRepository<T> {

	/**
	 * Executes a tuple query for the given specification, sort/page request, and selection definition.
	 *
	 * <p>The selection callback receives the live result {@code CriteriaQuery} so it can build correlated subqueries or
	 * inspect query state while defining projected columns.</p>
	 *
	 * @param specification filtering criteria to apply
	 * @param pageable paging and sorting information; sorting is always applied and limits are applied when paged
	 * @param selectionsProvider callback that defines the tuple selections for the query
	 *
	 * @return tuples matching the requested filter, sort, and selection set
	 */
	List<Tuple> findAll(
			Specification<T> specification,
			Pageable pageable,
			SelectionsProvider<T> selectionsProvider
	);

	/**
	 * Executes a tuple query and wraps the results in a page.
	 *
	 * <p>When {@code pageable} is unpaged, the returned page contains all matching tuples without issuing a separate
	 * count query.</p>
	 *
	 * <p>The selection callback receives the live result {@code CriteriaQuery}. For paged execution, avoid mutating the
	 * outer query in ways that change row cardinality, such as enabling distinct results or adding grouping, because
	 * the total count is derived from a separate count query built from the {@link Specification}.</p>
	 *
	 * @param specification filtering criteria to apply
	 * @param pageable paging and sorting information; sorting is always applied and limits are applied when paged
	 * @param selectionsProvider callback that defines the tuple selections for the query
	 *
	 * @return a page of tuples matching the requested filter, sort, and selection set
	 */
	Page<Tuple> findAllPaged(
			Specification<T> specification,
			Pageable pageable,
			SelectionsProvider<T> selectionsProvider
	);

	/**
	 * Executes a tuple query and converts each result row into an instance of the requested DTO type.
	 *
	 * <p>Use this overload when the caller wants constructor-backed DTO projection instead of working with raw
	 * {@link Tuple} instances. The provided {@link SelectionsProvider} still defines the tuple projection, but each
	 * resulting row is converted into {@code dtoClass} immediately before being returned to the caller.</p>
	 *
	 * <p>Conversion is positional. The selected tuple elements are passed to the DTO constructor in the same order as
	 * they are returned by {@link SelectionsProvider#getSelections}. Tuple aliases or selection names are ignored.</p>
	 *
	 * <p>A constructor is considered compatible when it has the same number of parameters as projected tuple elements
	 * and each parameter type is assignable from the corresponding tuple element runtime Java type after primitive
	 * types are normalized to their wrapper equivalents. When multiple constructors are compatible, a constructor
	 * annotated with {@link org.springframework.data.annotation.PersistenceCreator} is preferred. Callers should define
	 * at most one such annotated constructor; if more than one constructor is annotated, or if multiple compatible
	 * constructors exist and selection is not uniquely determined, behavior is unpredictable.</p>
	 *
	 * <p>If no compatible constructor can be found, or if reflective instantiation fails for any row, the conversion
	 * fails at runtime.</p>
	 *
	 * @param specification filtering criteria to apply
	 * @param pageable paging and sorting information; sorting is always applied and limits are applied when paged
	 * @param selectionsProvider callback that defines the tuple selections for the query
	 * @param dtoClass target DTO type to instantiate for each tuple row
	 * @param <U> DTO projection type
	 *
	 * @return DTO instances matching the requested filter, sort, selection set, and constructor mapping rules
	 */
	<U> List<U> findAll(
			Specification<T> specification,
			Pageable pageable,
			SelectionsProvider<T> selectionsProvider,
			Class<U> dtoClass
	);

	/**
	 * Executes a tuple query, converts each result row into an instance of the requested DTO type, and wraps the
	 * converted results in a page.
	 *
	 * <p>Use this overload when the caller wants constructor-backed DTO projection together with Spring Data paging
	 * metadata. The provided {@link SelectionsProvider} still defines the tuple projection, and each tuple row is
	 * converted into {@code dtoClass} before being exposed in the returned page.</p>
	 *
	 * <p>Conversion is positional. The selected tuple elements are supplied to the DTO constructor in the same order as
	 * they are returned by {@link SelectionsProvider#getSelections}. Tuple aliases or selection names do not
	 * participate in constructor binding.</p>
	 *
	 * <p>A constructor is considered compatible when it has the same number of parameters as projected tuple elements
	 * and each parameter type is assignable from the corresponding tuple element runtime Java type after primitive
	 * types are normalized to their wrapper equivalents. When multiple constructors are compatible, a constructor
	 * annotated with {@link org.springframework.data.annotation.PersistenceCreator} is preferred. Callers should define
	 * at most one such annotated constructor; if more than one constructor is annotated, or if multiple compatible
	 * constructors exist and selection is not uniquely determined, behavior is unpredictable.</p>
	 *
	 * <p>This method inherits the same count-query caveats as {@link #findAllPaged(Specification, Pageable,
	 * SelectionsProvider)}. In particular, callers should avoid mutating the outer query in ways that change row
	 * cardinality, because the total count is derived from a separate count query built from the
	 * {@link Specification}.</p>
	 *
	 * <p>If no compatible constructor can be found, or if reflective instantiation fails for any row, the conversion
	 * fails at runtime.</p>
	 *
	 * @param specification filtering criteria to apply
	 * @param pageable paging and sorting information; sorting is always applied and limits are applied when paged
	 * @param selectionsProvider callback that defines the tuple selections for the query
	 * @param dtoClass target DTO type to instantiate for each tuple row
	 * @param <U> DTO projection type
	 *
	 * @return a page of DTO instances matching the requested filter, sort, selection set, and constructor mapping rules
	 */
	<U> Page<U> findAllPaged(
			Specification<T> specification,
			Pageable pageable,
			SelectionsProvider<T> selectionsProvider,
			Class<U> dtoClass
	);
}
