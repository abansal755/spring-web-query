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

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.ast.AbstractValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.ast.DTOValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.ast.NodeMetadata;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.enums.ResolutionMode;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.resolver.spring.config.SpecificationArgumentResolverConfig;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import io.github.perplexhub.rsql.RSQLJPAPredicateConverter;
import io.github.perplexhub.rsql.jsonb.JsonbConfiguration;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base {@link HandlerMethodArgumentResolver} for resolving RSQL-based
 * {@link org.springframework.data.jpa.domain.Specification} parameters.
 *
 * <p>This class merges {@link WebQuery} annotation settings with global
 * defaults, reads the raw RSQL filter from the effective request parameter,
 * and delegates DTO-aware or entity-aware specification creation to
 * subclasses.</p>
 */
@RequiredArgsConstructor
public class WebQuerySpecificationArgumentResolver extends AbstractWebQueryResolver {

	/**
	 * Global default request parameter name used when {@link WebQuery#filterParamName()}
	 * is blank.
	 */
	private final String globalFilterParamName;

	/**
	 * Global default applied when {@link WebQuery#allowAndOperator()} is set to
	 * {@link in.co.akshitbansal.springwebquery.annotation.WebQuery.OperatorPolicy#GLOBAL}.
	 */
	private final boolean globalAllowAndOperator;

	/**
	 * Global default applied when {@link WebQuery#allowOrOperator()} is set to
	 * {@link in.co.akshitbansal.springwebquery.annotation.WebQuery.OperatorPolicy#GLOBAL}.
	 */
	private final boolean globalAllowOrOperator;

	/**
	 * Global default applied when {@link WebQuery#maxASTDepth()} is left at its sentinel value.
	 */
	private final int globalMaxASTDepth;

	/**
	 * Parser configured with the allowed default and custom comparison operators.
	 */
	private final RSQLParser rsqlParser;

	/**
	 * Custom predicates adapted for {@code rsql-jpa} specification conversion.
	 */
	private final List<RSQLCustomPredicate<?>> customPredicates;

	/**
	 * Validator used to enforce the supported query-parameter naming contract
	 * for resolved filter parameter names.
	 */
	private final QueryParamNameValidator queryParamNameValidator;

	/**
	 * Factory used to create validation visitors matching the active query contract.
	 */
	private final ValidationRSQLVisitorFactory validationRSQLVisitorFactory;

	private final FieldMappingsValidator fieldMappingsValidator;

	/**
	 * Determines whether the supplied parameter should be resolved as a
	 * {@link Specification} participating in {@link WebQuery}-driven filtering.
	 *
	 * @param parameter method parameter under inspection
	 *
	 * @return {@code true} when the parameter type is assignable to
	 * {@link Specification} and the declaring method is annotated with {@link WebQuery}
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Specification.class.isAssignableFrom(parameter.getParameterType())
				&& super.supportsParameter(parameter);
	}

	/**
	 * Resolves the incoming request into a validated {@link Specification}
	 * using the effective query configuration for the current controller method.
	 *
	 * @param parameter method parameter being resolved
	 * @param mavContainer current model/view container, if any
	 * @param webRequest current native web request
	 * @param binderFactory web data binder factory, if available
	 *
	 * @return resolved specification for the current request, or
	 * {@link Specification#unrestricted()} when no filter is supplied
	 *
	 * @throws QueryException when query-specific validation fails
	 * @throws QueryConfigurationException when specification resolution fails unexpectedly
	 */
	@Override
	public Specification<?> resolveArgument(
			MethodParameter parameter,
			@Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			@Nullable WebDataBinderFactory binderFactory
	) {
		try {
			// Resolve effective endpoint settings from the current method parameter
			SpecificationArgumentResolverConfig queryConfig = getQueryConfiguration(parameter);

			// Extract the RSQL query string from the request using the parameter name defined in @WebQuery
			String filter = webRequest.getParameter(queryConfig.getFilterParamName());
			if (filter == null || filter.isBlank()) return Specification.unrestricted();

			// Parse the RSQL query into an Abstract Syntax Tree (AST)
			Node rootNode = rsqlParser.parse(filter);

			// Validate the parsed AST
			AbstractValidationRSQLVisitor visitor = validationRSQLVisitorFactory.newValidationRSQLVisitor(queryConfig);
			rootNode.accept(visitor, NodeMetadata.of(0));

			// Evaluate property path mapper based on resolution mode
			Map<String, String> propertyPathMapper;
			if (queryConfig.getResolutionMode() == ResolutionMode.DTO_AWARE) {
				// DTO Aware Mode
				propertyPathMapper = ((DTOValidationRSQLVisitor) visitor).getFieldMappings();
			}
			else {
				// Entity Aware Mode
				propertyPathMapper = queryConfig
						.getFieldMappings()
						.stream()
						.collect(Collectors.toMap(FieldMapping::name, FieldMapping::field));
			}
			return (root, query, criteriaBuilder) -> {
				RSQLJPAPredicateConverter converterVisitor = new RSQLJPAPredicateConverter(
						criteriaBuilder,
						propertyPathMapper,
						customPredicates,
						null,
						null,
						null,
						true,
						null,
						JsonbConfiguration.DEFAULT
				);
				return rootNode.accept(converterVisitor, root);
			};
		}
		catch (RSQLParserException ex) {
			throw new QueryValidationException("Unable to parse RSQL query param", ex);
		}
		catch (QueryException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new QueryConfigurationException("Failed to resolve RSQL Specification argument", ex);
		}
	}

	/**
	 * Resolves the effective query configuration by combining method-level {@link WebQuery}
	 * settings with the configured global fallbacks.
	 *
	 * <p>A blank {@link WebQuery#filterParamName()} delegates to the resolver's
	 * configured global default filter parameter name. Non-blank annotation
	 * overrides are validated before they are used for request lookup.</p>
	 *
	 * @param parameter supported method parameter whose declaring method carries
	 * {@link WebQuery}
	 *
	 * @return effective configuration used by specification resolvers for validation and parsing
	 */
	protected SpecificationArgumentResolverConfig getQueryConfiguration(MethodParameter parameter) {
		// Only runs successfully if supportsParameter has already returned true
		// so we can safely assume the presence of a valid @WebQuery annotation here, thus no exception handling is necessary
		WebQuery webQueryAnnotation = getWebQueryAnnotation(parameter);

		// Field Mappings
		List<FieldMapping> fieldMappings = Collections.unmodifiableList(
				Arrays.asList(webQueryAnnotation.fieldMappings())
		);
		fieldMappingsValidator.validate(fieldMappings);

		// Filter Parameter Name
		String filterParamName = webQueryAnnotation.filterParamName();
		if (filterParamName.isBlank()) filterParamName = this.globalFilterParamName;
		else queryParamNameValidator.validate(filterParamName);

		// Determine allowed logical operators based on annotation and global configuration
		// And Operator
		WebQuery.OperatorPolicy andNodePolicy = webQueryAnnotation.allowAndOperator();
		boolean andNodeAllowed;
		if (andNodePolicy == WebQuery.OperatorPolicy.GLOBAL) andNodeAllowed = globalAllowAndOperator;
		else andNodeAllowed = andNodePolicy == WebQuery.OperatorPolicy.ALLOW;

		// Or Operator
		WebQuery.OperatorPolicy orNodePolicy = webQueryAnnotation.allowOrOperator();
		boolean orNodeAllowed;
		if (orNodePolicy == WebQuery.OperatorPolicy.GLOBAL) orNodeAllowed = globalAllowOrOperator;
		else orNodeAllowed = orNodePolicy == WebQuery.OperatorPolicy.ALLOW;

		// Maximum AST Depth
		int maxDepth = webQueryAnnotation.maxASTDepth();
		if (maxDepth < 0) maxDepth = globalMaxASTDepth;

		return new SpecificationArgumentResolverConfig(
				webQueryAnnotation.entityClass(),
				webQueryAnnotation.dtoClass(),
				fieldMappings,
				filterParamName,
				andNodeAllowed,
				orNodeAllowed,
				maxDepth
		);
	}
}
