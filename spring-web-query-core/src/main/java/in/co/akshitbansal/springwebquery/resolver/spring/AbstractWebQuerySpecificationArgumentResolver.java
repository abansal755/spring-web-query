package in.co.akshitbansal.springwebquery.resolver.spring;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base {@link HandlerMethodArgumentResolver} for resolving RSQL-based
 * {@link org.springframework.data.jpa.domain.Specification} parameters.
 *
 * <p>This class initializes a parser constrained to the configured default and
 * custom operators, adapts custom operators into {@link RSQLCustomPredicate}
 * instances accepted by the underlying {@code rsql-jpa} integration, and
 * merges {@link WebQuery} annotation settings with global defaults before
 * delegating DTO-aware or entity-aware specification creation to subclasses.</p>
 */
public abstract class AbstractWebQuerySpecificationArgumentResolver extends AbstractWebQueryResolver {

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
	protected final RSQLParser rsqlParser;

	/**
	 * Custom predicates adapted for {@code rsql-jpa} specification conversion.
	 */
	protected final List<RSQLCustomPredicate<?>> customPredicates;

	/**
	 * Registered custom operators keyed by implementation class for downstream
	 * validation visitors.
	 */
	protected final Map<Class<?>, RSQLCustomOperator<?>> customOperators;

	/**
	 * Validator used to enforce the supported query-parameter naming contract
	 * for resolved filter parameter names.
	 */
	private final Validator<String> queryParamNameValidator;

	/**
	 * Creates the resolver base with global validation defaults, parser
	 * configuration, and custom predicate adaptation.
	 *
	 * @param globalFilterParamName global default request parameter name used when
	 * {@link WebQuery#filterParamName()} is blank
	 * @param globalAllowAndOperator fallback AND-node policy used when {@code @WebQuery} defers to global settings
	 * @param globalAllowOrOperator fallback OR-node policy used when {@code @WebQuery} defers to global settings
	 * @param globalMaxASTDepth fallback maximum AST depth used when {@code @WebQuery} defers to global settings
	 * @param defaultOperators built-in operators accepted by the parser
	 * @param customOperators custom operators to register for parsing and predicate generation
	 */
	protected AbstractWebQuerySpecificationArgumentResolver(
			String globalFilterParamName,
			boolean globalAllowAndOperator,
			boolean globalAllowOrOperator,
			int globalMaxASTDepth,
			Set<RSQLDefaultOperator> defaultOperators,
			Set<? extends RSQLCustomOperator<?>> customOperators
	) {
		// Global configuration
		this.globalFilterParamName = globalFilterParamName;
		this.globalAllowAndOperator = globalAllowAndOperator;
		this.globalAllowOrOperator = globalAllowOrOperator;
		this.globalMaxASTDepth = globalMaxASTDepth;

		// Combine default and custom operators into a single set of allowed ComparisonOperators for the RSQL parser
		Stream<ComparisonOperator> defaultOperatorsStream = defaultOperators
				.stream()
				.map(RSQLDefaultOperator::getOperator);
		Stream<ComparisonOperator> customOperatorsStream = customOperators
				.stream()
				.map(RSQLCustomOperator::getComparisonOperator);
		HashSet<ComparisonOperator> allowedOperators = Stream
				.concat(defaultOperatorsStream, customOperatorsStream)
				.collect(Collectors.toCollection(HashSet::new));
		this.rsqlParser = new RSQLParser(Collections.unmodifiableSet(allowedOperators));

		// Convert custom operators to the format which rsql jpa support library accepts
		List<RSQLCustomPredicate<?>> customPredicates = new ArrayList<>();
		for (RSQLCustomOperator<?> operator: customOperators) {
			RSQLCustomPredicate<?> predicate = new RSQLCustomPredicate<>(
					operator.getComparisonOperator(),
					operator.getType(),
					operator::toPredicate
			);
			customPredicates.add(predicate);
		}
		this.customPredicates = Collections.unmodifiableList(customPredicates);

		// Register custom operators by class for downstream validation visitors
		this.customOperators = Collections.unmodifiableMap(customOperators
				.stream()
				.collect(Collectors.toMap(
						RSQLCustomOperator::getClass,
						operator -> operator,
						// Might happen in case multiple instances of an operator are registered
						// In that case, we can just keep one of them since they should be functionally equivalent
						(existing, duplicate) -> existing,
						HashMap::new
				)));

		// For query parameter name validation
		this.queryParamNameValidator = new QueryParamNameValidator();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Specification.class.isAssignableFrom(parameter.getParameterType())
				&& super.supportsParameter(parameter);
	}

	@Override
	public Specification<?> resolveArgument(
			MethodParameter parameter,
			@Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			@Nullable WebDataBinderFactory binderFactory
	) {
		try {
			// Resolve effective endpoint settings from the current method parameter
			QueryConfiguration queryConfig = getQueryConfiguration(parameter);

			// Extract the RSQL query string from the request using the parameter name defined in @WebQuery
			String filter = webRequest.getParameter(queryConfig.getFilterParamName());
			if (filter == null || filter.isBlank()) return Specification.unrestricted();

			// Delegate to subclass implementation for actual specification resolution, passing the query configuration and raw filter string
			return resolveSpecification(queryConfig, filter);
		}
		catch (QueryException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new QueryConfigurationException("Failed to resolve RSQL Specification argument", ex);
		}
	}

	/**
	 * Resolves a validated specification for the supplied raw filter value and
	 * effective query configuration.
	 *
	 * @param queryConfig effective query configuration derived from {@link WebQuery}
	 * @param filter raw RSQL filter expression from the request
	 *
	 * @return resolved specification
	 */
	protected abstract Specification<?> resolveSpecification(QueryConfiguration queryConfig, String filter);

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
	protected QueryConfiguration getQueryConfiguration(MethodParameter parameter) {
		// Only runs successfully if supportsParameter has already returned true
		// so we can safely assume the presence of a valid @WebQuery annotation here, thus no exception handling is necessary
		WebQuery webQueryAnnotation = getWebQueryAnnotation(parameter);

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

		return QueryConfiguration
				.builder()
				.entityClass(webQueryAnnotation.entityClass())
				.dtoClass(webQueryAnnotation.dtoClass())
				.fieldMappings(Collections.unmodifiableList(Arrays.asList(webQueryAnnotation.fieldMappings())))
				.filterParamName(filterParamName)
				.andNodeAllowed(andNodeAllowed)
				.orNodeAllowed(orNodeAllowed)
				.maxASTDepth(maxDepth)
				.build();
	}

	/**
	 * Effective specification-specific query metadata derived from a supported
	 * {@link WebQuery}-annotated controller method after global defaults have
	 * been applied.
	 */
	@Getter
	@Builder
	@EqualsAndHashCode
	@ToString
	protected static class QueryConfiguration {

		/**
		 * Target entity used for field validation and specification generation.
		 */
		private final Class<?> entityClass;

		/**
		 * Optional DTO contract used for API-facing field validation and mapping.
		 */
		private final Class<?> dtoClass;

		/**
		 * Field mappings declared on {@link WebQuery}, used only by
		 * entity-aware selector resolution.
		 */
		private final List<FieldMapping> fieldMappings;

		/**
		 * Request parameter name used to read the raw RSQL filter expression
		 * after applying the global default when the annotation value is blank.
		 */
		private final String filterParamName;

		/**
		 * Whether AND nodes are allowed in the effective query configuration.
		 */
		private final boolean andNodeAllowed;

		/**
		 * Whether OR nodes are allowed in the effective query configuration.
		 */
		private final boolean orNodeAllowed;

		/**
		 * Maximum AST depth allowed in the effective query configuration.
		 */
		private final int maxASTDepth;
	}
}
