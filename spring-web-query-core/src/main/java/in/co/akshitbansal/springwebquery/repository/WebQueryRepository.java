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

import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

/**
 * Repository contract for executing DTO-projected, RSQL-filtered, and
 * pageable queries against a backing entity type.
 *
 * <p>The repository expects callers to provide the DTO projection via a
 * {@link SelectionsProvider}. Filtering and sorting selectors are interpreted
 * against the supplied DTO class and then translated to entity paths before
 * query execution.</p>
 *
 * @param <E> entity type backing the repository implementation
 */
public interface WebQueryRepository<E> {

	/**
	 * Executes a projected query with explicit validation settings.
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type that defines the public query contract and result
	 * shape
	 * @param allowAndOperation whether logical {@code AND} is allowed in the
	 * RSQL expression
	 * @param allowOrOperation whether logical {@code OR} is allowed in the RSQL
	 * expression
	 * @param maxASTDepth maximum RSQL AST depth accepted during validation
	 * @param <D> projected DTO type
	 *
	 * @return projected query results
	 */
	<D> List<D> findAll(
			@Nullable String rsqlQuery, Pageable pageable,
			SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<D> dtoClass, boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	);

	/**
	 * Executes a projected query using the repository-wide validation defaults.
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type that defines the public query contract and result
	 * shape
	 * @param <D> projected DTO type
	 *
	 * @return projected query results
	 */
	<D> List<D> findAll(
			@Nullable String rsqlQuery, Pageable pageable,
			SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<D> dtoClass
	);

	/**
	 * Executes a projected query without a specification customizer.
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param dtoClass DTO type that defines the public query contract and result
	 * shape
	 * @param <D> projected DTO type
	 *
	 * @return projected query results
	 */
	default <D> List<D> findAll(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @NonNull Class<D> dtoClass
	) {
		return findAll(rsqlQuery, pageable, selectionsProvider, null, dtoClass);
	}

	/**
	 * Counts entities matching the supplied RSQL filter and customization rules.
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type that defines the public query contract
	 * @param allowAndOperation whether logical {@code AND} is allowed in the
	 * RSQL expression
	 * @param allowOrOperation whether logical {@code OR} is allowed in the RSQL
	 * expression
	 * @param maxASTDepth maximum RSQL AST depth accepted during validation
	 *
	 * @return number of matching rows
	 */
	long count(
			@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<?> dtoClass, boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	);

	/**
	 * Counts entities using the repository-wide validation defaults.
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type that defines the public query contract
	 *
	 * @return number of matching rows
	 */
	long count(@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer, Class<?> dtoClass);

	/**
	 * Counts entities without a specification customizer.
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param dtoClass DTO type that defines the public query contract
	 *
	 * @return number of matching rows
	 */
	default long count(@Nullable String rsqlQuery, @NonNull Class<?> dtoClass) {
		return count(rsqlQuery, null, dtoClass);
	}

	/**
	 * Executes a pageable projected query with explicit validation settings.
	 *
	 * <p>For paged requests this method may issue a separate count query before
	 * executing the content query.</p>
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type that defines the public query contract and result
	 * shape
	 * @param allowAndOperation whether logical {@code AND} is allowed in the
	 * RSQL expression
	 * @param allowOrOperation whether logical {@code OR} is allowed in the RSQL
	 * expression
	 * @param maxASTDepth maximum RSQL AST depth accepted during validation
	 * @param <D> projected DTO type
	 *
	 * @return page of projected results
	 */
	default <D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			@NonNull Class<D> dtoClass, boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	) {
		// If unpaged, there is no need to issue another query for count
		if (pageable.isUnpaged()) {
			return new PageImpl<>(findAll(
					rsqlQuery, pageable,
					selectionsProvider, specificationCustomizer,
					dtoClass, allowAndOperation, allowOrOperation, maxASTDepth
			));
		}

		// Paged, issue a separate query for count
		long count = count(
				rsqlQuery, specificationCustomizer,
				dtoClass, allowAndOperation, allowOrOperation, maxASTDepth
		);

		// If no results, return an empty page
		if (count == 0) return new PageImpl<>(Collections.emptyList(), pageable, 0);

		// Issue a results query to get the actual results
		return new PageImpl<>(
				findAll(
						rsqlQuery, pageable,
						selectionsProvider, specificationCustomizer,
						dtoClass, allowAndOperation, allowOrOperation, maxASTDepth
				),
				pageable, count
		);
	}

	/**
	 * Executes a pageable projected query using the repository-wide validation
	 * defaults.
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type that defines the public query contract and result
	 * shape
	 * @param <D> projected DTO type
	 *
	 * @return page of projected results
	 */
	<D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, Pageable pageable,
			SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<D> dtoClass
	);

	/**
	 * Executes a pageable projected query without a specification customizer.
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param dtoClass DTO type that defines the public query contract and result
	 * shape
	 * @param <D> projected DTO type
	 *
	 * @return page of projected results
	 */
	default <D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider,
			@NonNull Class<D> dtoClass
	) {
		return findAllPaged(rsqlQuery, pageable, selectionsProvider, null, dtoClass);
	}
}
