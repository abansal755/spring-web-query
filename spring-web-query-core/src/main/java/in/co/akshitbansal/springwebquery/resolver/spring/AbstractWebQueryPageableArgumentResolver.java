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

import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.resolver.spring.config.PageableArgumentResolverConfig;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.Collections;

/**
 * Base resolver for {@link Pageable} parameters participating in
 * {@link WebQuery}-aware sorting.
 *
 * <p>This class delegates standard page/size parsing to Spring's
 * {@link PageableHandlerMethodArgumentResolver} and lets subclasses validate
 * and remap sort properties against either entity or DTO metadata.</p>
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractWebQueryPageableArgumentResolver extends AbstractWebQueryResolver {

	/**
	 * Delegate used to parse raw pageable parameters from the request.
	 */
	protected final PageableHandlerMethodArgumentResolver delegate;

	/**
	 * Validator used to enforce {@code @Sortable} constraints on resolved sort fields.
	 */
	protected final SortableFieldValidator sortableFieldValidator;

	protected final FieldResolverFactory fieldResolverFactory;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Pageable.class.isAssignableFrom(parameter.getParameterType())
				&& super.supportsParameter(parameter);
	}

	@Override
	public Pageable resolveArgument(
			MethodParameter parameter,
			@Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			@Nullable WebDataBinderFactory binderFactory
	) {
		try {
			// Delegate parsing of page, size and sort parameters to Spring
			Pageable pageable = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
			// Resolve effective endpoint settings from the current method parameter
			PageableArgumentResolverConfig queryConfig = getQueryConfiguration(parameter);
			// Perform pageable resolution and validation based on the extracted configuration
			return resolvePageable(pageable, queryConfig);
		}
		catch (QueryException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new QueryConfigurationException("Failed to resolve pageable argument", ex);
		}
	}

	/**
	 * Validates and remaps pageable sorting according to the effective query configuration.
	 *
	 * @param pageable pageable parsed from the request
	 * @param queryConfig effective query configuration derived from {@link WebQuery}
	 *
	 * @return pageable with validated and possibly remapped sort orders
	 */
	protected abstract Pageable resolvePageable(Pageable pageable, PageableArgumentResolverConfig queryConfig);

	/**
	 * Extracts pageable-specific query metadata directly from the
	 * {@link WebQuery} annotation declared on the supplied controller method.
	 *
	 * <p>Unlike specification resolution, pageable handling does not consume
	 * operator policies or AST settings, so this configuration contains only
	 * the entity type, optional DTO type, and the declared field mappings
	 * retained for entity-aware sort validation and remapping.</p>
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
		return new PageableArgumentResolverConfig(
				webQueryAnnotation.entityClass(),
				webQueryAnnotation.dtoClass(),
				Collections.unmodifiableList(Arrays.asList(webQueryAnnotation.fieldMappings()))
		);
	}
}
