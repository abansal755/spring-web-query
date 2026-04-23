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

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import in.co.akshitbansal.springwebquery.ast.NodeMetadata;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapperFactory;
import in.co.akshitbansal.springwebquery.tupleconverter.TupleConverter;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import io.github.perplexhub.rsql.RSQLJPAPredicateConverter;
import io.github.perplexhub.rsql.jsonb.JsonbConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.core.RepositoryMethodContext;
import org.springframework.data.repository.core.support.RepositoryMetadataAccess;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper.MappingResult;

public class WebQueryRepositoryImpl<E> implements WebQueryRepository<E>, RepositoryMetadataAccess {

	private final EntityManager entityManager;
	private final RSQLParser rsqlParser;
	private final ValidationRSQLVisitorFactory validationRSQLVisitorFactory;
	private final List<RSQLCustomPredicate<?>> customPredicates;
	private final DTOToEntityPathMapperFactory pathMapperFactory;
	private final SortableFieldValidator sortableFieldValidator;

	private final boolean globalAllowAndOperation;
	private final boolean globalAllowOrOperation;
	private final int globalMaxASTDepth;

	public WebQueryRepositoryImpl(
			@NonNull EntityManager entityManager,
			@NonNull RSQLParser rsqlParser,
			@NonNull ValidationRSQLVisitorFactory validationRSQLVisitorFactory,
			@NonNull List<RSQLCustomPredicate<?>> customPredicates,
			@NonNull DTOToEntityPathMapperFactory pathMapperFactory,
			@NonNull SortableFieldValidator sortableFieldValidator,
			@Value("${spring-web-query.filtering.allow-and-operation:true}") boolean globalAllowAndOperation,
			@Value("${spring-web-query.filtering.allow-or-operation:false}") boolean globalAllowOrOperation,
			@Value("${spring-web-query.filtering.max-ast-depth:1}") int globalMaxASTDepth
	) {
		this.entityManager = entityManager;
		this.rsqlParser = rsqlParser;
		this.validationRSQLVisitorFactory = validationRSQLVisitorFactory;
		this.customPredicates = customPredicates;
		this.pathMapperFactory = pathMapperFactory;
		this.sortableFieldValidator = sortableFieldValidator;
		this.globalAllowAndOperation = globalAllowAndOperation;
		this.globalAllowOrOperation = globalAllowOrOperation;
		this.globalMaxASTDepth = globalMaxASTDepth;
	}

	@Override
	public <D> List<D> findAll(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			@NonNull Class<D> dtoClass, boolean andNodeAllowed, boolean orNodeAllowed, int maxASTDepth
	) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Class<E> entityClass = getEntityClass();
		Root<E> root = query.from(entityClass);

		// SELECT clause
		List<Selection<?>> selections = selectionsProvider.getSelections(root, query, cb);
		if (selections.isEmpty()) throw new QueryConfigurationException("No selections provided");
		Selection<?>[] selectionsArray = selections.toArray(new Selection<?>[0]);
		query.select(cb.tuple(selectionsArray));

		// WHERE clause
		applyWhereClause(
				root, query, cb,
				rsqlQuery, specificationCustomizer,
				entityClass, dtoClass,
				andNodeAllowed, orNodeAllowed, maxASTDepth
		);

		// ORDER BY clause
		List<Order> orders = mapSortPathsToEntityPaths(pageable.getSort(), root, cb, entityClass, dtoClass);
		query.orderBy(orders);

		TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);

		// LIMIT clause
		if (pageable.isPaged()) {
			typedQuery.setMaxResults(pageable.getPageSize());
			long offset = pageable.getOffset();
			if (offset > Integer.MAX_VALUE) {
				throw new IllegalArgumentException(MessageFormat.format(
						"Pageable offset {0} exceeds maximum allowed value of {1}",
						offset, Integer.MAX_VALUE
				));
			}
			typedQuery.setFirstResult((int) offset);
		}

		// Execute query
		List<Tuple> results = typedQuery.getResultList();
		// Convert the results to the desired DTO class
		TupleConverter<D> converter = TupleConverter.of(dtoClass);
		return results
				.stream()
				.map(converter::convert)
				.toList();
	}

	@Override
	public <D> List<D> findAll(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			@NonNull Class<D> dtoClass
	) {
		return findAll(
				rsqlQuery, pageable,
				selectionsProvider, specificationCustomizer,
				dtoClass, globalAllowAndOperation, globalAllowOrOperation, globalMaxASTDepth
		);
	}

	@Override
	public long count(
			@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			@NonNull Class<?> dtoClass, boolean andNodeAllowed, boolean orNodeAllowed, int maxASTDepth
	) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Class<E> entityClass = getEntityClass();
		Root<E> root = query.from(entityClass);

		// WHERE clause
		applyWhereClause(
				root, query, cb,
				rsqlQuery, specificationCustomizer,
				entityClass, dtoClass,
				andNodeAllowed, orNodeAllowed, maxASTDepth
		);

		// SELECT clause
		if (query.isDistinct()) query.select(cb.countDistinct(root));
		else query.select(cb.count(root));

		return entityManager
				.createQuery(query)
				.getSingleResult();
	}

	@Override
	public long count(@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer, @NonNull Class<?> dtoClass) {
		return count(
				rsqlQuery, specificationCustomizer,
				dtoClass, globalAllowAndOperation, globalAllowOrOperation, globalMaxASTDepth
		);
	}

	@Override
	public <D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			@NonNull Class<D> dtoClass
	) {
		return findAllPaged(
				rsqlQuery, pageable,
				selectionsProvider, specificationCustomizer,
				dtoClass, globalAllowAndOperation, globalAllowOrOperation, globalMaxASTDepth
		);
	}

	private void applyWhereClause(
			Root<E> root, CriteriaQuery<?> query, CriteriaBuilder cb,
			@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<E> entityClass, Class<?> dtoClass,
			boolean andNodeAllowed, boolean orNodeAllowed, int maxASTDepth
	) {
		Specification<E> filterSpec = createSpecification(
				rsqlQuery,
				entityClass, dtoClass,
				andNodeAllowed, orNodeAllowed, maxASTDepth
		);
		if (specificationCustomizer != null) filterSpec = specificationCustomizer.apply(filterSpec);
		if (filterSpec == null) return;
		Predicate predicate = filterSpec.toPredicate(root, query, cb);
		if (predicate != null) query.where(filterSpec.toPredicate(root, query, cb));
	}

	private Specification<E> createSpecification(
			@Nullable String rsqlQuery,
			Class<E> entityClass, Class<?> dtoClass,
			boolean andNodeAllowed, boolean orNodeAllowed, int maxASTDepth
	) {
		if (rsqlQuery == null) return Specification.unrestricted();
		try {
			// Parse the RSQL query into an Abstract Syntax Tree (AST)
			Node rootNode = rsqlParser.parse(rsqlQuery);
			// Validate the parsed AST
			ValidationRSQLVisitor visitor = validationRSQLVisitorFactory.newValidationRSQLVisitor(
					entityClass,
					dtoClass,
					andNodeAllowed,
					orNodeAllowed,
					maxASTDepth
			);
			rootNode.accept(visitor, NodeMetadata.of(0));

			return (Root<E> root, CriteriaQuery<?> ignored, CriteriaBuilder cb) -> {
				// Convert AST into Predicate
				RSQLJPAPredicateConverter predicateConverter = new RSQLJPAPredicateConverter(
						cb,
						visitor.getFieldMappings(),
						customPredicates,
						null,
						null,
						null,
						// prevents wildcard parsing for string equality operator
						// so that "name==John*" is treated as: name equals 'John*'
						// rather than: name starts with 'John'
						true,
						null,
						JsonbConfiguration.DEFAULT
				);
				return rootNode.accept(predicateConverter, root);
			};
		}
		catch (RSQLParserException ex) {
			throw new QueryValidationException(
					MessageFormat.format(
							"Unable to parse RSQL query: {0}", rsqlQuery
					), ex
			);
		}
		catch (QueryException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new QueryConfigurationException(
					MessageFormat.format(
							"Failed to construct Predicate from RSQL query: {0}", rsqlQuery
					), ex
			);
		}
	}

	private List<Order> mapSortPathsToEntityPaths(Sort sort, Root<E> root, CriteriaBuilder cb, Class<E> entityClass, Class<?> dtoClass) {
		DTOToEntityPathMapper pathMapper = pathMapperFactory.newMapper(entityClass, dtoClass);
		List<Order> orders = new ArrayList<>();
		for (Sort.Order order: sort) {
			String dtoPath = order.getProperty();

			// Convert the DTO path to an entity path
			MappingResult mappingResult = pathMapper.map(dtoPath);
			String entityPath = mappingResult.getPath();

			// Validate the terminal field of the mapped entity path
			sortableFieldValidator.validate(mappingResult.getTerminalDTOField(), dtoPath);

			Path<?> path = getJPAPathFromEntityPath(root, entityPath);
			Order jpaOrder;
			if (order.isAscending()) jpaOrder = cb.asc(path);
			else jpaOrder = cb.desc(path);
			orders.add(jpaOrder);
		}
		return orders;
	}

	private Path<?> getJPAPathFromEntityPath(Root<E> root, String entityPath) {
		Path<?> path = root;
		for (String part: entityPath.split("\\."))
			path = path.get(part);
		return path;
	}

	private Class<E> getEntityClass() {
		// noinspection unchecked
		return (Class<E>) RepositoryMethodContext
				.getContext()
				.getMetadata()
				.getDomainType();
	}
}
