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
import in.co.akshitbansal.springwebquery.ast.EntityValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.ast.NodeMetadata;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import io.github.perplexhub.rsql.QuerySupport;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import io.github.perplexhub.rsql.RSQLJPASupport;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entity-based resolver for {@link Specification} parameters handled via
 * method-level {@link WebQuery}.
 *
 * <p>This resolver is active when {@link WebQuery#dtoClass()} is not configured
 * (i.e. equals {@code void.class}). It validates incoming RSQL selectors against
 * entity fields and optional {@link FieldMapping} aliases before producing a
 * JPA specification.</p>
 */
public class WebQueryEntityAwareSpecificationArgumentResolver extends AbstractWebQuerySpecificationArgumentResolver {

	/**
	 * Validator used to enforce uniqueness and consistency of declared field mappings.
	 */
	private final Validator<List<FieldMapping>> fieldMappingsValidator;

	/**
	 * Creates an entity-aware RSQL specification resolver.
	 *
	 * @param globalFilterParamName global default request parameter name used when
	 * {@link WebQuery#filterParamName()} is blank
	 * @param globalAllowAndOperator whether AND nodes are allowed by default when {@code @WebQuery}
	 * does not override that behavior
	 * @param globalAllowOrOperator whether OR nodes are allowed by default when {@code @WebQuery}
	 * does not override that behavior
	 * @param globalMaxASTDepth maximum AST depth allowed by default when {@code @WebQuery}
	 * does not override that behavior
	 * @param defaultOperators built-in operators accepted in RSQL expressions
	 * @param customOperators custom operators supported by parser and predicates
	 */
	public WebQueryEntityAwareSpecificationArgumentResolver(
			String globalFilterParamName,
			boolean globalAllowAndOperator,
			boolean globalAllowOrOperator,
			int globalMaxASTDepth,
			RSQLParser rsqlParser,
			List<RSQLCustomPredicate<?>> customPredicates,
			Map<Class<?>, RSQLCustomOperator<?>> customOperatorMap,
			Validator<String> queryParamNameValidator,
			Validator<List<FieldMapping>> fieldMappingsValidator,
			ValidationRSQLVisitorFactory validationRSQLVisitorFactory
	) {
		super(
				globalFilterParamName,
				globalAllowAndOperator,
				globalAllowOrOperator,
				globalMaxASTDepth,
				rsqlParser,
				customPredicates,
				customOperatorMap,
				queryParamNameValidator,
				validationRSQLVisitorFactory
		);
		this.fieldMappingsValidator = fieldMappingsValidator;
	}

	/**
	 * Determines whether this resolver should handle the given parameter.
	 *
	 * @param parameter method parameter under inspection
	 *
	 * @return {@code true} when parameter is a {@code Specification} with
	 * method-level {@link WebQuery} and no DTO class is configured
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return super.supportsParameter(parameter) // supportsParameter in superclass checks for method-level @WebQuery presence
				&& getWebQueryAnnotation(parameter).dtoClass() == void.class; // so no exception handling is needed
	}

	/**
	 * Parses, validates, and converts an entity-oriented RSQL filter into a
	 * JPA {@link Specification}.
	 *
	 * @param queryConfig effective query configuration for the current request
	 * @param filter raw RSQL filter string from the request
	 *
	 * @return resolved specification for the validated filter
	 */
	@Override
	protected Specification<?> resolveSpecification(QueryConfiguration queryConfig, String filter) {
		try {
			// Validate field mappings to ensure they are well-formed and do not contain conflicts
			fieldMappingsValidator.validate(queryConfig.getFieldMappings());

			// Parse the RSQL query into an Abstract Syntax Tree (AST)
			Node root = rsqlParser.parse(filter);
			// Validate the parsed AST against the target entity and its @RSQLFilterable fields
			EntityValidationRSQLVisitor validationVisitor =
			validationRSQLVisitorFactory.newEntityValidationRSQLVisitor(
					queryConfig.getEntityClass(),
					queryConfig.getFieldMappings(),
					queryConfig.isAndNodeAllowed(),
					queryConfig.isOrNodeAllowed(),
					queryConfig.getMaxASTDepth()
			);
			root.accept(validationVisitor, NodeMetadata.of(0));

			// Convert field mappings to aliases map which rsql jpa support library accepts
			Map<String, String> fieldMappingsMap = queryConfig
					.getFieldMappings()
					.stream()
					.collect(Collectors.toMap(FieldMapping::name, FieldMapping::field));

			// Convert the validated RSQL query into a JPA Specification
			QuerySupport querySupport = QuerySupport
					.builder()
					.rsqlQuery(filter)
					.propertyPathMapper(fieldMappingsMap)
					.customPredicates(customPredicates)
					// prevents wildcard parsing for string equality operator
					// so that "name==John*" is treated as: name equals 'John*'
					// rather than: name starts with 'John'
					.strictEquality(true)
					.build();
			return RSQLJPASupport.toSpecification(querySupport);
		}
		catch (RSQLParserException ex) {
			throw new QueryValidationException("Unable to parse RSQL query param", ex);
		}
	}
}
