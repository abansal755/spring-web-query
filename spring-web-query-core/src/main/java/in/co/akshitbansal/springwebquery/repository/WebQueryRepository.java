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

import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper;
import in.co.akshitbansal.springwebquery.tupleconverter.TupleConverter;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import io.github.perplexhub.rsql.RSQLJPAPredicateConverter;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Primary repository entry point of Spring Web Query.
 *
 * <p>This interface is exposed as a Spring Data repository fragment. Although
 * it is declared as an interface for fragment composition, the Javadoc here
 * describes the concrete behavior provided by the library's built-in
 * JPA-backed implementation.</p>
 *
 * <p>For each invocation, the fragment resolves the backing entity type from
 * the current repository method call, obtains a tuple projection from the
 * supplied {@link SelectionsProvider}, parses the optional RSQL filter,
 * validates the parsed AST with a {@link ValidationRSQLVisitor}, maps
 * selector paths from {@code dtoClass} to entity paths through
 * {@link DTOToEntityPathMapper}, validates sortable fields through
 * {@link SortableFieldValidator}, and delegates predicate construction to
 * {@link RSQLJPAPredicateConverter}.</p>
 *
 * <p>The supplied DTO class therefore controls both which field paths may be
 * used for filtering and sorting and how the projected result is materialized.
 * The resulting Criteria query applies sorting and pagination from
 * {@link Pageable}. Query rows are fetched as tuples and converted to the
 * requested DTO type through {@link TupleConverter}, so the selections
 * produced by {@link SelectionsProvider} must be compatible in order and type
 * with the conversion strategy discoverable for {@code dtoClass}. If the
 * selections provider returns no selections, query execution fails with a
 * {@link QueryConfigurationException}.</p>
 *
 * <p>{@link #count(String, SpecificationCustomizer, Class, boolean, boolean, int)}
 * reuses the same filter construction pipeline without tuple projection.
 * {@link #findAllPaged(String, Pageable, SelectionsProvider, SpecificationCustomizer, Class, boolean, boolean, int)}
 * uses the same filtering, sorting, pagination, and projection behavior as
 * {@link #findAll(String, Pageable, SelectionsProvider, SpecificationCustomizer, Class, boolean, boolean, int)};
 * for paged requests it first executes a count query and skips the content
 * query when the count is zero, while unpaged requests are wrapped directly in
 * a {@link PageImpl} without a separate count query.</p>
 *
 * <p>The overloads that do not accept explicit validation settings use the
 * repository-wide defaults sourced from the properties
 * {@code spring-web-query.filtering.allow-and-operation},
 * {@code spring-web-query.filtering.allow-or-operation}, and
 * {@code spring-web-query.filtering.max-ast-depth}.</p>
 *
 * @param <E> entity type backing the repository implementation
 */
public interface WebQueryRepository<E> {

	/**
	 * Executes the full Spring Web Query pipeline and returns only the projected
	 * content rows for the requested page window.
	 *
	 * <p>If {@code rsqlQuery} is not {@code null}, the current implementation
	 * parses it, validates the AST against the supplied DTO type and the
	 * provided logical-operator and depth settings, translates validated
	 * selectors to entity paths, and creates a JPA {@code Specification} from
	 * the validated tree. The optional {@code specificationCustomizer} is then
	 * applied to that specification and may augment it, replace it, or remove it
	 * completely by returning {@code null}.</p>
	 *
	 * <p>The {@code selectionsProvider} defines the tuple select clause. The
	 * built-in implementation rejects an empty selection list, applies the sort
	 * orders and pagination window from {@code pageable}, executes the query, and
	 * converts each returned tuple into an instance of {@code dtoClass} through
	 * {@link TupleConverter}. Even though this method returns a {@link List},
	 * the offset and page size from {@code pageable} are still applied to the
	 * query when {@code pageable} is paged.</p>
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * specification before it is applied
	 * @param dtoClass DTO type whose fields are used for filtering and sorting
	 * and whose shape is used for result projection
	 * @param allowAndOperation whether logical {@code AND} is allowed in the
	 * RSQL expression
	 * @param allowOrOperation whether logical {@code OR} is allowed in the RSQL
	 * expression
	 * @param maxASTDepth maximum RSQL AST depth accepted during validation
	 * @param <D> projected DTO type
	 *
	 * @return projected results for the requested page window, ordered according
	 * to the translated sort instructions
	 *
	 * @throws QueryValidationException if the RSQL expression cannot be parsed
	 * or violates the configured validation rules
	 * @throws QueryConfigurationException if no selections are provided, selector
	 * translation fails because of invalid configuration, JPA sort order
	 * construction fails, or predicate creation fails after parsing and
	 * validation
	 * @throws QueryException if another Spring Web Query exception is raised
	 * during processing
	 * @throws IllegalArgumentException if {@code pageable.getOffset()} exceeds
	 * {@link Integer#MAX_VALUE}
	 */
	<D> List<D> findAll(
			@Nullable String rsqlQuery, Pageable pageable,
			SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<D> dtoClass, boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	);

	/**
	 * Executes {@link #findAll(String, Pageable, SelectionsProvider, SpecificationCustomizer, Class, boolean, boolean, int)}
	 * using the repository-wide validation defaults configured for the
	 * implementation.
	 *
	 * <p>This overload keeps the same filtering, sorting, projection, and
	 * pagination semantics as the fully configurable variant, but derives the
	 * logical-operator and AST-depth settings from repository configuration
	 * instead of requiring them from the caller.</p>
	 *
	 * <p>The default values come from the properties
	 * {@code spring-web-query.filtering.allow-and-operation},
	 * {@code spring-web-query.filtering.allow-or-operation}, and
	 * {@code spring-web-query.filtering.max-ast-depth}.</p>
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type whose fields are used for filtering and sorting
	 * and whose shape is used for result projection
	 * @param <D> projected DTO type
	 *
	 * @return projected results for the requested page window, ordered according
	 * to the translated sort instructions
	 *
	 * @throws QueryValidationException if the RSQL expression cannot be parsed
	 * or violates the configured validation rules
	 * @throws QueryConfigurationException if no selections are provided, selector
	 * translation fails because of invalid configuration, JPA sort order
	 * construction fails, or predicate creation fails after parsing and
	 * validation
	 * @throws QueryException if another Spring Web Query exception is raised
	 * during processing
	 * @throws IllegalArgumentException if {@code pageable.getOffset()} exceeds
	 * {@link Integer#MAX_VALUE}
	 */
	<D> List<D> findAll(
			@Nullable String rsqlQuery, Pageable pageable,
			SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<D> dtoClass
	);

	/**
	 * Executes a filtered, sorted, and paginated projection query without a
	 * specification customizer.
	 *
	 * <p>This is a convenience overload equivalent to invoking the repository
	 * default-settings variant with a {@code null} customizer.</p>
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param dtoClass DTO type whose fields are used for filtering and sorting
	 * and whose shape is used for result projection
	 * @param <D> projected DTO type
	 *
	 * @return projected results for the requested page window, ordered according
	 * to the translated sort instructions
	 *
	 * @throws QueryValidationException if the RSQL expression cannot be parsed
	 * or violates the configured validation rules
	 * @throws QueryConfigurationException if no selections are provided, selector
	 * translation fails because of invalid configuration, JPA sort order
	 * construction fails, or predicate creation fails after parsing and
	 * validation
	 * @throws QueryException if another Spring Web Query exception is raised
	 * during processing
	 * @throws IllegalArgumentException if {@code pageable.getOffset()} exceeds
	 * {@link Integer#MAX_VALUE}
	 */
	default <D> List<D> findAll(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @NonNull Class<D> dtoClass
	) {
		return findAll(rsqlQuery, pageable, selectionsProvider, null, dtoClass);
	}

	/**
	 * Executes the same filtering pipeline as
	 * {@link #findAll(String, Pageable, SelectionsProvider, SpecificationCustomizer, Class, boolean, boolean, int)}
	 * but produces a row count instead of projected content.
	 *
	 * <p>If {@code rsqlQuery} is {@code null}, the base specification is
	 * unrestricted and only the optional {@code specificationCustomizer} can add
	 * restrictions. Otherwise the RSQL expression is parsed, validated, and
	 * translated exactly as it is for {@code findAll}. The current implementation
	 * applies the resulting specification to a count query and uses
	 * {@code countDistinct(root)} when the underlying criteria query has been
	 * marked distinct; otherwise it uses a regular {@code count(root)}.</p>
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type whose fields are used for filter validation and
	 * path translation
	 * @param allowAndOperation whether logical {@code AND} is allowed in the
	 * RSQL expression
	 * @param allowOrOperation whether logical {@code OR} is allowed in the RSQL
	 * expression
	 * @param maxASTDepth maximum RSQL AST depth accepted during validation
	 *
	 * @return number of matching rows after the generated and customized filter
	 * specification has been applied
	 *
	 * @throws QueryValidationException if the RSQL expression cannot be parsed
	 * or violates the configured validation rules
	 * @throws QueryConfigurationException if selector translation fails because
	 * of invalid configuration or predicate creation fails after parsing and
	 * validation
	 * @throws QueryException if another Spring Web Query exception is raised
	 * during processing
	 */
	long count(
			@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<?> dtoClass, boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	);

	/**
	 * Counts rows using the repository-wide validation defaults configured for
	 * the implementation.
	 *
	 * <p>This overload retains the same counting semantics as
	 * {@link #count(String, SpecificationCustomizer, Class, boolean, boolean, int)}
	 * while sourcing the logical-operator and AST-depth settings from repository
	 * configuration.</p>
	 *
	 * <p>The default values come from the properties
	 * {@code spring-web-query.filtering.allow-and-operation},
	 * {@code spring-web-query.filtering.allow-or-operation}, and
	 * {@code spring-web-query.filtering.max-ast-depth}.</p>
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type whose fields are used for filter validation and
	 * path translation
	 *
	 * @return number of matching rows after the generated and customized filter
	 * specification has been applied
	 *
	 * @throws QueryValidationException if the RSQL expression cannot be parsed
	 * or violates the configured validation rules
	 * @throws QueryConfigurationException if selector translation fails because
	 * of invalid configuration or predicate creation fails after parsing and
	 * validation
	 * @throws QueryException if another Spring Web Query exception is raised
	 * during processing
	 */
	long count(@Nullable String rsqlQuery, @Nullable SpecificationCustomizer<E> specificationCustomizer, Class<?> dtoClass);

	/**
	 * Counts rows without a specification customizer.
	 *
	 * <p>This is a convenience overload equivalent to invoking the repository
	 * default-settings variant with a {@code null} customizer.</p>
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param dtoClass DTO type whose fields are used for filter validation and
	 * path translation
	 *
	 * @return number of matching rows after applying only the repository-built
	 * filter specification
	 *
	 * @throws QueryValidationException if the RSQL expression cannot be parsed
	 * or violates the configured validation rules
	 * @throws QueryConfigurationException if selector translation fails because
	 * of invalid configuration or predicate creation fails after parsing and
	 * validation
	 * @throws QueryException if another Spring Web Query exception is raised
	 * during processing
	 */
	default long count(@Nullable String rsqlQuery, @NonNull Class<?> dtoClass) {
		return count(rsqlQuery, null, dtoClass);
	}

	/**
	 * Executes the same query pipeline as
	 * {@link #findAll(String, Pageable, SelectionsProvider, SpecificationCustomizer, Class, boolean, boolean, int)}
	 * but returns a {@link Page} with count metadata.
	 *
	 * <p>The current implementation behaves in two modes. When
	 * {@code pageable.isUnpaged()} is {@code true}, it runs only
	 * {@code findAll(...)} and wraps the resulting content in a {@link PageImpl}.
	 * When {@code pageable} is paged, it first executes
	 * {@link #count(String, SpecificationCustomizer, Class, boolean, boolean, int)}.
	 * If that count is zero, it returns an empty page without issuing the content
	 * query. Otherwise it executes {@code findAll(...)} for the requested page
	 * window and combines the content with the total row count.</p>
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type whose fields are used for filtering and sorting
	 * and whose shape is used for result projection
	 * @param allowAndOperation whether logical {@code AND} is allowed in the
	 * RSQL expression
	 * @param allowOrOperation whether logical {@code OR} is allowed in the RSQL
	 * expression
	 * @param maxASTDepth maximum RSQL AST depth accepted during validation
	 * @param <D> projected DTO type
	 *
	 * @return page of projected results together with paging metadata derived
	 * from the matching-row count
	 *
	 * @throws QueryValidationException if the RSQL expression cannot be parsed
	 * or violates the configured validation rules
	 * @throws QueryConfigurationException if no selections are provided, selector
	 * translation fails because of invalid configuration, JPA sort order
	 * construction fails, or predicate creation fails after parsing and
	 * validation
	 * @throws QueryException if another Spring Web Query exception is raised
	 * during processing
	 * @throws IllegalArgumentException if {@code pageable.getOffset()} exceeds
	 * {@link Integer#MAX_VALUE}
	 */
	<D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			@NonNull Class<D> dtoClass, boolean allowAndOperation, boolean allowOrOperation, int maxASTDepth
	);

	/**
	 * Executes a filtered, sorted, and paginated projection query using the
	 * repository-wide validation defaults and returns a {@link Page}.
	 *
	 * <p>This overload keeps the same page-assembly behavior as the fully
	 * configurable variant while sourcing the logical-operator and AST-depth
	 * settings from repository configuration.</p>
	 *
	 * <p>The default values come from the properties
	 * {@code spring-web-query.filtering.allow-and-operation},
	 * {@code spring-web-query.filtering.allow-or-operation}, and
	 * {@code spring-web-query.filtering.max-ast-depth}.</p>
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param specificationCustomizer optional hook to amend the generated filter
	 * @param dtoClass DTO type whose fields are used for filtering and sorting
	 * and whose shape is used for result projection
	 * @param <D> projected DTO type
	 *
	 * @return page of projected results together with paging metadata derived
	 * from the matching-row count
	 *
	 * @throws QueryValidationException if the RSQL expression cannot be parsed
	 * or violates the configured validation rules
	 * @throws QueryConfigurationException if no selections are provided, selector
	 * translation fails because of invalid configuration, JPA sort order
	 * construction fails, or predicate creation fails after parsing and
	 * validation
	 * @throws QueryException if another Spring Web Query exception is raised
	 * during processing
	 * @throws IllegalArgumentException if {@code pageable.getOffset()} exceeds
	 * {@link Integer#MAX_VALUE}
	 */
	<D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, Pageable pageable,
			SelectionsProvider<E> selectionsProvider, @Nullable SpecificationCustomizer<E> specificationCustomizer,
			Class<D> dtoClass
	);

	/**
	 * Executes a filtered, sorted, and paginated projection query without a
	 * specification customizer and returns a {@link Page}.
	 *
	 * <p>This is a convenience overload equivalent to invoking the repository
	 * default-settings variant with a {@code null} customizer.</p>
	 *
	 * @param rsqlQuery optional RSQL filter expression
	 * @param pageable requested paging and sorting information
	 * @param selectionsProvider callback that defines the tuple projection
	 * @param dtoClass DTO type whose fields are used for filtering and sorting
	 * and whose shape is used for result projection
	 * @param <D> projected DTO type
	 *
	 * @return page of projected results together with paging metadata derived
	 * from the matching-row count
	 *
	 * @throws QueryValidationException if the RSQL expression cannot be parsed
	 * or violates the configured validation rules
	 * @throws QueryConfigurationException if no selections are provided, selector
	 * translation fails because of invalid configuration, JPA sort order
	 * construction fails, or predicate creation fails after parsing and
	 * validation
	 * @throws QueryException if another Spring Web Query exception is raised
	 * during processing
	 * @throws IllegalArgumentException if {@code pageable.getOffset()} exceeds
	 * {@link Integer#MAX_VALUE}
	 */
	default <D> Page<D> findAllPaged(
			@Nullable String rsqlQuery, @NonNull Pageable pageable,
			@NonNull SelectionsProvider<E> selectionsProvider,
			@NonNull Class<D> dtoClass
	) {
		return findAllPaged(rsqlQuery, pageable, selectionsProvider, null, dtoClass);
	}
}
