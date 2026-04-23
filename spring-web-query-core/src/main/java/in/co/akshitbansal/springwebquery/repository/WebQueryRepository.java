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

public interface WebQueryRepository<E> {

	<D> List<D> findAll(
			@Nullable String rsqlQuery, Pageable pageable,
			SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<D> dtoClass, boolean andNodeAllowed, boolean orNodeAllowed, int maxASTDepth
	);

	<D> List<D> findAll(
			@Nullable String rsqlQuery, Pageable pageable,
			SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<D> dtoClass
	);

	default <D> List<D> findAll(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @NonNull Class<D> dtoClass
	) {
		return findAll(rsqlQuery, pageable, selectionsProvider, null, dtoClass);
	}

	long count(
			@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<?> dtoClass, boolean andNodeAllowed, boolean orNodeAllowed, int maxASTDepth
	);

	long count(@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer, Class<?> dtoClass);

	default long count(@Nullable String rsqlQuery, @NonNull Class<?> dtoClass) {
		return count(rsqlQuery, null, dtoClass);
	}

	default <D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			@NonNull Class<D> dtoClass, boolean andNodeAllowed, boolean orNodeAllowed, int maxASTDepth
	) {
		// If unpaged, there is no need to issue another query for count
		if (pageable.isUnpaged()) {
			return new PageImpl<>(findAll(
					rsqlQuery, pageable,
					selectionsProvider, specificationCustomizer,
					dtoClass, andNodeAllowed, orNodeAllowed, maxASTDepth
			));
		}

		// Paged, issue a separate query for count
		long count = count(
				rsqlQuery, specificationCustomizer,
				dtoClass, andNodeAllowed, orNodeAllowed, maxASTDepth
		);

		// If no results, return an empty page
		if (count == 0) return new PageImpl<>(Collections.emptyList(), pageable, 0);

		// Issue a results query to get the actual results
		return new PageImpl<>(
				findAll(
						rsqlQuery, pageable,
						selectionsProvider, specificationCustomizer,
						dtoClass, andNodeAllowed, orNodeAllowed, maxASTDepth
				),
				pageable, count
		);
	}

	<D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, Pageable pageable,
			SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<D> dtoClass
	);

	default <D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider,
			@NonNull Class<D> dtoClass
	) {
		return findAllPaged(rsqlQuery, pageable, selectionsProvider, null, dtoClass);
	}
}
