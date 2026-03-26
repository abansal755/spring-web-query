package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import lombok.*;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        Method controlllerMethod = parameter.getMethod();
        if(controlllerMethod == null) return false;
        WebQuery webQueryAnnotation = controlllerMethod.getAnnotation(WebQuery.class);
        return webQueryAnnotation != null;
    }

    /**
     * Resolves the effective query configuration by combining method-level {@link WebQuery}
     * settings with the configured global fallbacks.
     *
     * @param webQueryAnnotation controller method annotation supplying query settings
     * @return effective configuration used by specification resolvers for validation and parsing
     */
    protected QueryConfiguration getQueryConfiguration(@NonNull WebQuery webQueryAnnotation) {
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
     * Validates {@link FieldMapping} definitions declared in {@link WebQuery}.
     * <p>
     * Validation rules:
     * <ul>
     *     <li>Alias names must be unique ({@link FieldMapping#name()}).</li>
     *     <li>Target entity fields must be unique ({@link FieldMapping#field()}).</li>
     * </ul>
     *
     * @param fieldMappings field mappings to validate
     * @throws QueryConfigurationException if duplicate aliases or duplicate target fields are found
     */
    protected void validateFieldMappings(@NonNull FieldMapping[] fieldMappings) {
        Set<String> nameSet = new HashSet<>();
        for (FieldMapping mapping : fieldMappings) {
            if(!nameSet.add(mapping.name())) throw new QueryConfigurationException(MessageFormat.format(
                    "Duplicate field mapping present for alias ''{0}''. Only one mapping is allowed per alias.",
                    mapping.name()
            ));
        }

        Map<String, FieldMapping> fieldMap = new HashMap<>();
        for (FieldMapping mapping : fieldMappings) {
            fieldMap.compute(mapping.field(), (fieldName, existing) -> {
                if(existing != null) throw new QueryConfigurationException(MessageFormat.format(
                        "Aliases ''{0}'' and ''{1}'' are mapped to same field. Only one mapping is allowed per field.",
                        existing.name(), mapping.name()
                ));
                return mapping;
            });
        }
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

        private final FieldMapping[] fieldMappings;

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
