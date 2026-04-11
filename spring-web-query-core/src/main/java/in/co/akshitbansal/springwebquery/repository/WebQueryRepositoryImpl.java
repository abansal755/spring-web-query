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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.core.RepositoryMethodContext;
import org.springframework.data.repository.core.support.RepositoryMetadataAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@RequiredArgsConstructor
public class WebQueryRepositoryImpl<T> implements WebQueryRepository<T>, RepositoryMetadataAccess {

	private final EntityManager entityManager;

	@Override
	public List<Tuple> findAll(
			Specification<T> specification,
			Pageable pageable,
			BiFunction<Root<T>, CriteriaBuilder, List<Selection<?>>> selectionsProvider
	) {
		return createResultsQuery(specification, pageable, selectionsProvider).getResultList();
	}

	@Override
	public Page<Tuple> findAllPaged(
			Specification<T> specification,
			Pageable pageable,
			BiFunction<Root<T>, CriteriaBuilder, List<Selection<?>>> selectionsProvider
	) {
		if (pageable.isUnpaged()) {
			// If unpaged, there is no need to issue another query for count
			return new PageImpl<>(findAll(specification, pageable, selectionsProvider));
		}
		// paged, issue a separate query for count
		long count = createCountQuery(specification).getSingleResult();
		return new PageImpl<>(findAll(specification, pageable, selectionsProvider), pageable, count);
	}

	private TypedQuery<Long> createCountQuery(Specification<T> specification) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Class<T> entityClass = getEntityClass();
		Root<T> root = query.from(entityClass);

		// where clause
		Predicate predicate = specification.toPredicate(root, query, cb);
		if(predicate != null) query.where(predicate);

		return entityManager.createQuery(query);
	}

	private TypedQuery<Tuple> createResultsQuery(
			Specification<T> specification,
			Pageable pageable,
			BiFunction<Root<T>, CriteriaBuilder, List<Selection<?>>> selectionsProvider
	) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Class<T> entityClass = getEntityClass();
		Root<T> root = query.from(entityClass);

		// select columns
		List<Selection<?>> selections = selectionsProvider.apply(root, cb);
		Selection<?>[] selectionsArray = selections.toArray(new Selection<?>[0]);
		query.select(cb.tuple(selectionsArray));

		// where clause
		Predicate predicate = specification.toPredicate(root, query, cb);
		if (predicate != null) query.where(predicate);

		// order by clause
		List<Order> orders = new ArrayList<>();
		for (Sort.Order order: pageable.getSort()) {
			Path<?> path = root;
			for (String part: order.getProperty().split("\\."))
				path = path.get(part);

			Order jpaOrder;
			if (order.isAscending()) jpaOrder = cb.asc(path);
			else jpaOrder = cb.desc(path);
			orders.add(jpaOrder);
		}
		query.orderBy(orders);

		TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);

		// limit clause
		if (pageable.isPaged()) {
			typedQuery.setMaxResults(pageable.getPageSize());
			typedQuery.setFirstResult((int) pageable.getOffset());
		}

		return typedQuery;
	}

	private Class<T> getEntityClass() {
		return (Class<T>) RepositoryMethodContext
				.getContext()
				.getMetadata()
				.getDomainType();
	}
}
