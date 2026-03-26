package in.co.akshitbansal.springwebquery.resolver;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import lombok.NonNull;
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
 * custom operators, and adapts custom operators into
 * {@link RSQLCustomPredicate} instances accepted by the underlying
 * {@code rsql-jpa} integration.</p>
 */
public abstract class AbstractWebQuerySpecificationArgumentResolver extends AbstractWebQueryResolver {

    /**
     * Parser configured with the allowed default and custom comparison operators.
     */
    protected final RSQLParser rsqlParser;

    /**
     * Custom predicates adapted for {@code rsql-jpa} specification conversion.
     */
    protected final List<RSQLCustomPredicate<?>> customPredicates;

    /**
     * Utility for annotation resolution and validation support in subclasses.
     */
    protected final AnnotationUtil annotationUtil;

    /**
     * Creates the resolver base with parser and predicate configuration.
     *
     * @param defaultOperators built-in operators accepted by the parser
     * @param customOperators custom operators to register for parsing and predicate generation
     * @param annotationUtil utility for annotation resolution and validation
     * @param globalAllowAndOperator fallback AND-node policy used when {@code @WebQuery} defers to global settings
     * @param globalAllowOrOperator fallback OR-node policy used when {@code @WebQuery} defers to global settings
     * @param globalMaxASTDepth fallback maximum AST depth used when {@code @WebQuery} defers to global settings
     */
    protected AbstractWebQuerySpecificationArgumentResolver(
            Set<RSQLDefaultOperator> defaultOperators,
            Set<? extends RSQLCustomOperator<?>> customOperators,
            AnnotationUtil annotationUtil,
            boolean globalAllowAndOperator,
            boolean globalAllowOrOperator,
            int globalMaxASTDepth
    ) {
        super(globalAllowAndOperator, globalAllowOrOperator, globalMaxASTDepth);
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
        rsqlParser = new RSQLParser(Collections.unmodifiableSet(allowedOperators));

        // Convert custom operators to the format which rsql jpa support library accepts
        List<RSQLCustomPredicate<?>> customPredicates = new ArrayList<>();
        for(RSQLCustomOperator<?> operator : customOperators) {
            RSQLCustomPredicate<?> predicate = new RSQLCustomPredicate<>(
                    operator.getComparisonOperator(),
                    operator.getType(),
                    operator::toPredicate
            );
            customPredicates.add(predicate);
        }
        this.customPredicates = Collections.unmodifiableList(customPredicates);
        this.annotationUtil = annotationUtil;
    }

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        if(!super.supportsParameter(parameter)) return false;
        // Only support parameters of type Specification or its subtypes
        return Specification.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Specification<?> resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        try {
            // Retrieve the @WebQuery annotation from the method parameter to access configuration
            WebQuery webQueryAnnotation = parameter.getMethod().getAnnotation(WebQuery.class);
            // Extract relevant configuration from the annotation
            QueryConfiguration queryConfig = getQueryConfiguration(webQueryAnnotation);

            // Extract the RSQL query string from the request using the parameter name defined in @WebQuery
            String filter = webRequest.getParameter(queryConfig.getFilterParamName());
            if(filter == null || filter.isBlank()) return Specification.unrestricted();

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

    protected abstract Specification<?> resolveSpecification(@NonNull QueryConfiguration queryConfig, @NonNull String filter);
}
