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

public abstract class RsqlSpecArgumentResolver implements HandlerMethodArgumentResolver {

    protected final RSQLParser rsqlParser;
    protected final List<RSQLCustomPredicate<?>> customPredicates;
    protected final AnnotationUtil annotationUtil;

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
