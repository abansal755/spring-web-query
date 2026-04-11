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
}
