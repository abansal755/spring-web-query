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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.core.RepositoryMethodContext;
import org.springframework.data.repository.core.support.RepositoryMetadataAccess;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper.MappingResult;

/**
 * JPA-based {@link WebQueryRepository} repository fragment implementation
 * backed by Criteria queries and {@code rsql-jpa}.
 *
 * <p>This implementation handles filtering, sorting, pagination, and
 * projection for a repository method invocation. It builds tuple-based select
 * queries through the supplied {@link SelectionsProvider}, validates and maps
 * filter and sort paths against the supplied DTO type, delegates predicate
 * creation to {@code rsql-jpa}, and converts the resulting tuples into the
 * requested DTO type.</p>
 *
 * <p>When page metadata is needed, the same filter rules are reused for a
 * separate count query. The interface Javadoc remains the main public API
 * description; the helper methods in this class explain how the JPA-backed
 * implementation realizes that behavior.</p>
 *
 * <p>The repository-wide defaults used by the overloads that do not accept
 * explicit validation settings are sourced from the properties
 * {@code spring-web-query.filtering.allow-and-operation},
 * {@code spring-web-query.filtering.allow-or-operation}, and
 * {@code spring-web-query.filtering.max-ast-depth}.</p>
 *
 * @param <E> entity type handled by the repository
 */
public class WebQueryRepositoryImpl<E> implements WebQueryRepository<E>, RepositoryMetadataAccess {

	/**
	 * Entity manager used to build and execute criteria queries.
	 */
	private final EntityManager entityManager;

	/**
	 * Shared RSQL parser configured with the allowed operator set.
	 */
	private final RSQLParser rsqlParser;

	/**
	 * Factory used to create validation visitors for parsed RSQL trees.
	 */
	private final ValidationRSQLVisitorFactory validationRSQLVisitorFactory;

	/**
	 * Custom predicates exposed to the underlying {@code rsql-jpa} converter.
	 */
	private final List<RSQLCustomPredicate<?>> customPredicates;

	/**
	 * Factory used to translate DTO selectors to entity paths.
	 */
	private final DTOToEntityPathMapperFactory pathMapperFactory;

	/**
	 * Validator used to enforce sorting permissions.
	 */
	private final SortableFieldValidator sortableFieldValidator;

	/**
	 * Default repository-wide setting for logical {@code AND} support.
	 */
	private final boolean globalAllowAndOperation;

	/**
	 * Default repository-wide setting for logical {@code OR} support.
	 */
	private final boolean globalAllowOrOperation;

	/**
	 * Default repository-wide setting for maximum RSQL AST depth.
	 */
	private final int globalMaxASTDepth;

	/**
	 * Creates the repository implementation with all collaborating components and
	 * global validation defaults.
	 */
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

	/**
	 * Executes a projected result query using explicit validation settings.
	 */
	@Override
	public <D> List<D> findAll(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			@NonNull Class<D> dtoClass, boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	) {
		Specification<E> spec = createSpecification(
				rsqlQuery, specificationCustomizer,
				dtoClass, allowAndOperation, allowOrOperation, maxASTDepth
		);
		return findAll(spec, pageable, selectionsProvider, dtoClass);
	}

	/**
	 * Executes a projected result query using repository defaults.
	 */
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

	/**
	 * Counts rows matching the supplied filter using explicit validation settings.
	 */
	@Override
	public long count(
			@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			@NonNull Class<?> dtoClass, boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	) {
		Specification<E> spec = createSpecification(
				rsqlQuery, specificationCustomizer, dtoClass,
				allowAndOperation, allowOrOperation, maxASTDepth
		);
		return count(spec);
	}

	/**
	 * Counts rows matching the supplied filter using repository defaults.
	 */
	@Override
	public long count(@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer, @NonNull Class<?> dtoClass) {
		return count(
				rsqlQuery, specificationCustomizer,
				dtoClass, globalAllowAndOperation, globalAllowOrOperation, globalMaxASTDepth
		);
	}

	/**
	 * Executes a paged projected query using explicit validation settings.
	 */
	@Override
	public <D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			@NonNull Class<D> dtoClass, boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	) {
		// Create the specification
		Specification<E> spec = createSpecification(
				rsqlQuery, specificationCustomizer,
				dtoClass, allowAndOperation, allowOrOperation, maxASTDepth
		);

		// If unpaged, there is no need to issue another query for count
		if (pageable.isUnpaged())
			return new PageImpl<>(findAll(spec, pageable, selectionsProvider, dtoClass));

		// Paged, issue a separate query for count
		long count = count(spec);

		// If no results, return an empty page
		if (count == 0) return new PageImpl<>(Collections.emptyList(), pageable, 0);

		// Issue a results query to get the actual results
		return new PageImpl<>(
				findAll(spec, pageable, selectionsProvider, dtoClass),
				pageable, count
		);
	}

	/**
	 * Executes a paged projected query using repository defaults.
	 */
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

	/**
	 * Internal helper to execute a projection query with a pre-built specification.
	 *
	 * <p>This helper manages the full lifecycle of a result query, including
	 * selector projection, sorting, and pagination. It converts the resulting
	 * {@link Tuple} objects into the target DTO type.</p>
	 *
	 * @param specification the filter specification to apply
	 * @param pageable pagination and sorting metadata
	 * @param selectionsProvider callback to define the select clause
	 * @param dtoClass target class for tuple conversion
	 * @param <D> result type
	 * @return projected results for the requested page window
	 */
	private <D> List<D> findAll(
			@Nullable Specification<E> specification, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @NonNull Class<D> dtoClass
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
		if (specification != null) {
			Predicate predicate = specification.toPredicate(root, query, cb);
			if (predicate != null) query.where(predicate);
		}

		// ORDER BY clause
		List<Order> orders = mapSortToJpaOrders(pageable.getSort(), root, cb, entityClass, dtoClass);
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

	/**
	 * Internal helper to execute a count query with a pre-built specification.
	 *
	 * @param specification the filter specification to apply
	 * @return total number of matching rows
	 */
	private long count(@Nullable Specification<E> specification) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Class<E> entityClass = getEntityClass();
		Root<E> root = query.from(entityClass);

		// WHERE clause
		if (specification != null) {
			Predicate predicate = specification.toPredicate(root, query, cb);
			if (predicate != null) query.where(predicate);
		}

		// SELECT clause
		if (query.isDistinct()) query.select(cb.countDistinct(root));
		else query.select(cb.count(root));

		return entityManager
				.createQuery(query)
				.getSingleResult();
	}

	/**
	 * Coordinates the creation of an eager RSQL specification and its optional
	 * customization.
	 *
	 * @param rsqlQuery optional filter string
	 * @param specificationCustomizer optional hook for specification adjustment
	 * @param dtoClass DTO class used for validation and mapping
	 * @param allowAndOperation whether AND nodes are allowed
	 * @param allowOrOperation whether OR nodes are allowed
	 * @param maxASTDepth maximum allowed AST depth
	 * @return final specification ready for use
	 */
	@Nullable
	private Specification<E> createSpecification(
			@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<?> dtoClass, boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	) {
		Class<E> entityClass = getEntityClass();
		Specification<E> filterSpec = createRSQLFilterSpecification(
				rsqlQuery,
				entityClass, dtoClass,
				allowAndOperation, allowOrOperation, maxASTDepth
		);
		if (specificationCustomizer == null) return filterSpec;
		return specificationCustomizer.apply(filterSpec);
	}

	/**
	 * Builds a JPA Specification by eagerly parsing and validating the RSQL query.
	 *
	 * <p>Unlike lazy implementations, this method performs RSQL parsing and
	 * validation immediately. This ensures that these expensive steps are only
	 * executed once per request, even if the resulting specification is used in
	 * both count and content queries.</p>
	 *
	 * @param rsqlQuery optional filter string
	 * @param entityClass entity type for predicate creation
	 * @param dtoClass DTO type for selector contract enforcement
	 * @param allowAndOperation whether AND nodes are allowed
	 * @param allowOrOperation whether OR nodes are allowed
	 * @param maxASTDepth maximum allowed AST depth
	 * @return specification representing the validated RSQL query
	 */
	private Specification<E> createRSQLFilterSpecification(
			@Nullable String rsqlQuery,
			Class<E> entityClass, Class<?> dtoClass,
			boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	) {
		if (rsqlQuery == null) return Specification.unrestricted();
		try {
			// Parse the RSQL query into an Abstract Syntax Tree (AST)
			Node rootNode = rsqlParser.parse(rsqlQuery);
			// Validate the parsed AST
			ValidationRSQLVisitor visitor = validationRSQLVisitorFactory.newValidationRSQLVisitor(
					entityClass,
					dtoClass,
					allowAndOperation,
					allowOrOperation,
					maxASTDepth
			);
			rootNode.accept(visitor, NodeMetadata.of(0));

			return (Root<E> root, CriteriaQuery<?> ignored, CriteriaBuilder cb) -> {
				try {
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
				}
				catch (Exception ex) {
					throw new QueryConfigurationException(MessageFormat.format(
							"Failed to convert RSQL AST to JPA Predicate: {0}", ex.getMessage()
					), ex);
				}
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

	/**
	 * Translates the requested sort orders from DTO selector paths into JPA
	 * {@link Order} instances backed by entity paths.
	 *
	 * <p>Each {@link Sort.Order} is processed independently. The order property
	 * is first mapped from the caller-visible DTO selector path to an entity
	 * attribute path. The terminal DTO field referenced by that selector is then
	 * validated with the {@link SortableFieldValidator}. Once validated, the
	 * mapped entity path is resolved into a chained JPA {@link Path}, and either
	 * an ascending or descending {@link Order} is created to match the original
	 * sort direction.</p>
	 *
	 * <p>An empty {@link Sort} produces an empty order list. Any
	 * {@link QueryException} raised while mapping or validating sort paths is
	 * propagated unchanged. Any other unexpected {@link RuntimeException}
	 * encountered while translating paths or constructing {@link Order}
	 * instances is wrapped in a {@link QueryConfigurationException}.</p>
	 *
	 * @param sort sort specification supplied through the current {@link Pageable}
	 * @param root root entity path for the query being constructed
	 * @param cb criteria builder used to create ascending and descending orders
	 * @param entityClass backing entity type used for selector translation
	 * @param dtoClass DTO type that defines the sortable selector contract
	 *
	 * @return JPA order list corresponding to the requested sort specification
	 */
	private List<Order> mapSortToJpaOrders(Sort sort, Root<E> root, CriteriaBuilder cb, Class<E> entityClass, Class<?> dtoClass) {
		try {
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
		catch (QueryException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new QueryConfigurationException(MessageFormat.format(
					"Failed to construct JPA Orders from Sort: {0}", sort
			), ex);
		}
	}

	/**
	 * Resolves a dotted entity attribute path into a chained JPA {@link Path}.
	 *
	 * <p>The input is expected to already be a validated entity path produced by
	 * the selector-mapping layer. Resolution is purely segment-by-segment through
	 * repeated {@link Path#get(String)} calls starting from the supplied root,
	 * for example turning {@code address.city.name} into
	 * {@code root.get("address").get("city").get("name")}.</p>
	 *
	 * <p>This helper does not perform any additional validation, join creation,
	 * or fallback lookup. It simply mirrors the previously resolved entity path
	 * into the form required by the Criteria API.</p>
	 *
	 * @param root root entity path for the query being constructed
	 * @param entityPath mapped entity attribute path expressed with dot notation
	 *
	 * @return Criteria API path representing the supplied entity path
	 */
	private Path<?> getJPAPathFromEntityPath(Root<E> root, String entityPath) {
		Path<?> path = root;
		for (String part: entityPath.split("\\."))
			path = path.get(part);
		return path;
	}

	/**
	 * Retrieves the repository domain type from the current Spring Data
	 * invocation context.
	 *
	 * <p>The implementation is registered as a repository fragment and implements
	 * {@link RepositoryMetadataAccess}, so Spring Data exposes the current method
	 * invocation metadata through {@link RepositoryMethodContext}. This helper
	 * reads the domain type from that context and casts it to the repository's
	 * generic entity type.</p>
	 *
	 * <p>The returned type is the actual repository domain class currently
	 * invoking this fragment, which allows a single generic implementation class
	 * to serve multiple repository specializations.</p>
	 *
	 * @return runtime entity class for the current repository invocation
	 */
	private Class<E> getEntityClass() {
		// noinspection unchecked
		return (Class<E>) RepositoryMethodContext
				.getContext()
				.getMetadata()
				.getDomainType();
	}
}
