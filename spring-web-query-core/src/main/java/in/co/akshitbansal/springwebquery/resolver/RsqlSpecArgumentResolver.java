package in.co.akshitbansal.springwebquery.resolver;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.List;
import java.util.Set;
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
public abstract class RsqlSpecArgumentResolver implements HandlerMethodArgumentResolver {

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
     */
    public RsqlSpecArgumentResolver(Set<RsqlOperator> defaultOperators, Set<? extends RsqlCustomOperator<?>> customOperators, AnnotationUtil annotationUtil) {
        // Combine default and custom operators into a single set of allowed ComparisonOperators for the RSQL parser
        Stream<ComparisonOperator> defaultOperatorsStream = defaultOperators
                .stream()
                .map(RsqlOperator::getOperator);
        Stream<ComparisonOperator> customOperatorsStream = customOperators
                .stream()
                .map(RsqlCustomOperator::getComparisonOperator);
        Set<ComparisonOperator> allowedOperators = Stream
                .concat(defaultOperatorsStream, customOperatorsStream)
                .collect(Collectors.toSet());
        rsqlParser = new RSQLParser(allowedOperators);

        // Convert custom operators to the format which rsql jpa support library accepts
        this.customPredicates = customOperators
                .stream()
                .map(operator -> new RSQLCustomPredicate<>(
                        operator.getComparisonOperator(),
                        operator.getType(),
                        operator::toPredicate
                ))
                .collect(Collectors.toList());
        this.annotationUtil = annotationUtil;
    }
}
