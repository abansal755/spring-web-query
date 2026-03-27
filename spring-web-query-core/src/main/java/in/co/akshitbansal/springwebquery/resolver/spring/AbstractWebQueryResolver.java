package in.co.akshitbansal.springwebquery.resolver.spring;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import lombok.*;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.lang.reflect.Method;

/**
 * Common base for {@link org.springframework.web.method.support.HandlerMethodArgumentResolver}
 * implementations that participate in {@link WebQuery}-driven request parsing.
 *
 * <p>This class centralizes support detection for controller methods annotated
 * with {@link WebQuery} and computes the effective query configuration by
 * combining method-level annotation values with global defaults.</p>
 */
@RequiredArgsConstructor
public abstract class AbstractWebQueryResolver implements HandlerMethodArgumentResolver {

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
     * Determines whether the supplied method parameter belongs to a controller
     * method annotated with {@link WebQuery}.
     *
     * <p>This base implementation uses {@link #getWebQueryAnnotation(MethodParameter)}
     * as the single source of truth for annotation lookup. Missing methods or
     * missing annotations are treated as "not supported" rather than exceptional
     * conditions so Spring MVC can continue evaluating other resolvers.</p>
     *
     * @param parameter method parameter under inspection
     * @return {@code true} when the declaring method has {@link WebQuery},
     *         otherwise {@code false}
     */
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        try {
            getWebQueryAnnotation(parameter);
            return true;
        }
        catch (RuntimeException e) {
            // Exceptions are expected when the annotation is missing or the parameter is malformed
            // so we catch and return false rather than propagating
            return false;
        }
    }

    /**
     * Resolves the effective query configuration by combining method-level {@link WebQuery}
     * settings with the configured global fallbacks.
     *
     * @param parameter supported method parameter whose declaring method carries
     *                  {@link WebQuery}
     * @return effective configuration used by specification resolvers for validation and parsing
     */
    protected QueryConfiguration getQueryConfiguration(@NonNull MethodParameter parameter) {
        // Only runs successfully if supportsParameter has already returned true
        // so we can safely assume the presence of a valid @WebQuery annotation here, thus no exception handling is necessary
        WebQuery webQueryAnnotation = getWebQueryAnnotation(parameter);
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

        return QueryConfiguration
                .builder()
                .entityClass(webQueryAnnotation.entityClass())
                .dtoClass(webQueryAnnotation.dtoClass())
                .fieldMappings(webQueryAnnotation.fieldMappings())
                .filterParamName(webQueryAnnotation.filterParamName())
                .andNodeAllowed(andNodeAllowed)
                .orNodeAllowed(orNodeAllowed)
                .maxASTDepth(maxDepth)
                .build();
    }

    /**
     * Returns the {@link WebQuery} annotation declared on the supplied method parameter's
     * controller method.
     *
     * <p>This helper centralizes annotation lookup for both support detection and
     * downstream configuration resolution. Callers that need boolean support checks
     * should use {@link #supportsParameter(MethodParameter)}; this method is for
     * subclasses that expect a supported parameter and therefore treat missing
     * method metadata as a programming error.</p>
     *
     * @param parameter method parameter whose declaring method is expected to carry
     *                  {@link WebQuery}
     * @return resolved {@link WebQuery} annotation
     * @throws IllegalStateException if the parameter has no declaring method or the
     *                               declaring method is not annotated with {@link WebQuery}
     */
    protected WebQuery getWebQueryAnnotation(@NonNull MethodParameter parameter) {
        Method method = parameter.getMethod();
        if(method == null) throw new IllegalStateException("MethodParameter does not have an associated method");
        WebQuery webQueryAnnotation = method.getAnnotation(WebQuery.class);
        if(webQueryAnnotation == null) throw new IllegalStateException("Method is not annotated with @WebQuery");
        return webQueryAnnotation;
    }

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
         * Field mappings declared on {@link WebQuery} for alias-based selector resolution.
         */
        private final FieldMapping[] fieldMappings;

        /**
         * Request parameter name used to read the raw RSQL filter expression.
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
