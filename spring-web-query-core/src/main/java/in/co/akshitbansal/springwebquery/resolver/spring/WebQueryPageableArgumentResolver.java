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

package in.co.akshitbansal.springwebquery.resolver.spring;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolver;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.resolver.field.ResolutionResult;
import in.co.akshitbansal.springwebquery.resolver.spring.config.PageableArgumentResolverConfig;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unified resolver for {@link Pageable} parameters participating in
 * {@link WebQuery}-aware sorting.
 *
 * <p>This resolver delegates standard page/size parsing to Spring's
 * {@link PageableHandlerMethodArgumentResolver} and then validates and remaps
 * sort properties against either entity or DTO metadata.</p>
 */
@RequiredArgsConstructor
public class WebQueryPageableArgumentResolver extends AbstractWebQueryResolver {

	/**
	 * Delegate used to parse raw pageable parameters from the request.
	 */
	@NonNull
	private final PageableHandlerMethodArgumentResolver delegate;

	/**
	 * Validator used to enforce {@code @Sortable} constraints on resolved sort fields.
	 */
	@NonNull
	private final SortableFieldValidator sortableFieldValidator;

	/**
	 * Factory used to create entity-aware or DTO-aware field resolvers for sort paths.
	 */
	@NonNull
	private final FieldResolverFactory fieldResolverFactory;

	/**
	 * Validator used to fail fast on malformed declared field mappings before
	 * the effective query mode is resolved.
	 */
	@NonNull
	private final FieldMappingsValidator fieldMappingsValidator;

	/**
	 * Determines whether the supplied parameter should be resolved as a
	 * {@link Pageable} participating in {@link WebQuery}-aware sorting.
	 *
	 * @param parameter method parameter under inspection
	 *
	 * @return {@code true} when the parameter type is assignable to
	 * {@link Pageable} and the declaring method is annotated with {@link WebQuery}
	 */
	@Override
	public boolean supportsParameter(@NonNull MethodParameter parameter) {
		return Pageable.class.isAssignableFrom(parameter.getParameterType())
				&& super.supportsParameter(parameter);
	}

	/**
	 * Resolves the request into a validated {@link Pageable} using Spring's
	 * standard parsing first and mode-aware sort validation/remapping afterward.
	 *
	 * @param parameter method parameter being resolved
	 * @param mavContainer current model/view container, if any
	 * @param webRequest current native web request
	 * @param binderFactory web data binder factory, if available
	 *
	 * @return validated pageable instance for the current request
	 *
	 * @throws QueryException when query-specific validation fails
	 * @throws QueryConfigurationException when pageable resolution fails unexpectedly
	 */
	@Override
	public Pageable resolveArgument(
			@NonNull MethodParameter parameter,
			@Nullable ModelAndViewContainer mavContainer,
			@NonNull NativeWebRequest webRequest,
			@Nullable WebDataBinderFactory binderFactory
	) {
		try {
			// Delegate parsing of page, size and sort parameters to Spring
			Pageable pageable = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
			// Resolve effective endpoint settings from the current method parameter
			PageableArgumentResolverConfig queryConfig = getQueryConfiguration(parameter);
			// Get field resolver based on current config
			FieldResolver fieldResolver = fieldResolverFactory.newFieldResolver(queryConfig);

			// Iterate over sort orders and build new sort orders with mapped field paths
			List<Sort.Order> newOrders = new ArrayList<>();
			for (Sort.Order order: pageable.getSort()) {
				String reqFieldPath = order.getProperty();
				// Build the corresponding entity field path
				ResolutionResult result = fieldResolver.resolvePath(reqFieldPath);
				sortableFieldValidator.validate(result.getTerminalField(), reqFieldPath);
				newOrders.add(new Sort.Order(order.getDirection(), result.getFieldName()));
			}

			Sort sort = Sort.by(newOrders);
			// Reconstruct pageable with mapped sort orders
			if (pageable.isUnpaged()) return Pageable.unpaged(sort);
			return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
		}
		catch (QueryException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new QueryConfigurationException("Failed to resolve pageable argument", ex);
		}
	}

	/**
	 * Extracts pageable-specific query metadata directly from the
	 * {@link WebQuery} annotation declared on the supplied controller method.
	 *
	 * <p>Unlike specification resolution, pageable handling does not consume
	 * operator policies or AST settings, so this configuration contains only
	 * the entity type, optional DTO type, and the declared field mappings used
	 * for eager validation and entity-aware sort remapping.</p>
	 *
	 * @param parameter supported method parameter whose declaring method carries
	 * {@link WebQuery}
	 *
	 * @return effective configuration used by pageable resolvers for sort validation
	 */
	protected PageableArgumentResolverConfig getQueryConfiguration(MethodParameter parameter) {
		// Only runs successfully if supportsParameter has already returned true
		// so we can safely assume the presence of a valid @WebQuery annotation here, thus no exception handling is necessary
		WebQuery webQueryAnnotation = getWebQueryAnnotation(parameter);

		// Validate field mappings once so configuration issues fail before request parsing
		List<FieldMapping> fieldMappings = Collections.unmodifiableList(Arrays.asList(webQueryAnnotation.fieldMappings()));
		fieldMappingsValidator.validate(fieldMappings);

		return new PageableArgumentResolverConfig(
				webQueryAnnotation.entityClass(),
				webQueryAnnotation.dtoClass(),
				fieldMappings
		);
	}
}
