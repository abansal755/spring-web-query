package in.co.akshitbansal.springwebquery.resolver;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import lombok.*;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

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
public abstract class WebQuerySpecificationArgumentResolver implements HandlerMethodArgumentResolver {

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
     * Global default applied when {@link WebQuery#allowAndOperator()} is set to
     * {@link in.co.akshitbansal.springwebquery.annotation.WebQuery.OperatorPolicy#GLOBAL}.
     */
    protected final boolean globalAllowAndOperator;

    /**
     * Global default applied when {@link WebQuery#allowOrOperator()} is set to
     * {@link in.co.akshitbansal.springwebquery.annotation.WebQuery.OperatorPolicy#GLOBAL}.
     */
    protected final boolean globalAllowOrOperator;

    /**
     * Global default applied when {@link WebQuery#maxASTDepth()} is left at its sentinel value.
     */
    protected final int globalMaxASTDepth;

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
    protected WebQuerySpecificationArgumentResolver(
            Set<RSQLDefaultOperator> defaultOperators,
            Set<? extends RSQLCustomOperator<?>> customOperators,
            AnnotationUtil annotationUtil,
            boolean globalAllowAndOperator,
            boolean globalAllowOrOperator,
            int globalMaxASTDepth
    ) {
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

        this.globalAllowAndOperator = globalAllowAndOperator;
        this.globalAllowOrOperator = globalAllowOrOperator;
        this.globalMaxASTDepth = globalMaxASTDepth;
    }

    /**
     * Resolves the effective query configuration by combining method-level {@link WebQuery}
     * settings with the configured global fallbacks.
     *
     * @param webQueryAnnotation controller method annotation supplying query settings
     * @return effective configuration used by specification resolvers for validation and parsing
     */
    protected QueryConfiguration getQueryConfiguration(@NonNull WebQuery webQueryAnnotation) {
        Class<?> entityClass = webQueryAnnotation.entityClass();
        Class<?> dtoClass = webQueryAnnotation.dtoClass();
        // Determine allowed logical operators based on annotation and global configuration
        // And Operator
        WebQuery.OperatorPolicy andNodePolicy = webQueryAnnotation.allowAndOperator();
        boolean andNodeAllowed;
        if(andNodePolicy == WebQuery.OperatorPolicy.GLOBAL) andNodeAllowed = globalAllowAndOperator;
        else andNodeAllowed = andNodePolicy == WebQuery.OperatorPolicy.ALLOW;
        // Or Operator
        WebQuery.OperatorPolicy orNodePolicy = webQueryAnnotation.allowOrOperator();
        boolean orNodeAllowed;
        if(orNodePolicy == WebQuery.OperatorPolicy.GLOBAL) orNodeAllowed = globalAllowOrOperator;
        else orNodeAllowed = orNodePolicy == WebQuery.OperatorPolicy.ALLOW;
        // Maximum AST Depth
        int maxDepth = webQueryAnnotation.maxASTDepth();
        if(maxDepth < 0) maxDepth = globalMaxASTDepth;
        return new QueryConfiguration(entityClass, dtoClass, andNodeAllowed, orNodeAllowed, maxDepth);
    }

    @Getter
    @RequiredArgsConstructor
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
