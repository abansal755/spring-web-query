package in.co.akshitbansal.springwebquery.resolver;

import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import in.co.akshitbansal.springwebquery.EntityValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.NodeMetadata;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import io.github.perplexhub.rsql.QuerySupport;
import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
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
     * Creates an entity-aware RSQL specification resolver.
     *
     * @param defaultOperators built-in operators accepted in RSQL expressions
     * @param customOperators custom operators supported by parser and predicates
     * @param annotationUtil utility for resolving annotations and configuration checks
     * @param globalAllowAndOperator whether AND nodes are allowed by default when {@code @WebQuery}
     *                               does not override that behavior
     * @param globalAllowOrOperator whether OR nodes are allowed by default when {@code @WebQuery}
     *                              does not override that behavior
     * @param globalMaxASTDepth maximum AST depth allowed by default when {@code @WebQuery}
     *                          does not override that behavior
     */
    public WebQueryEntityAwareSpecificationArgumentResolver(
            Set<RSQLDefaultOperator> defaultOperators,
            Set<? extends RSQLCustomOperator<?>> customOperators,
            AnnotationUtil annotationUtil,
            boolean globalAllowAndOperator,
            boolean globalAllowOrOperator,
            int globalMaxASTDepth
    ) {
        super(defaultOperators, customOperators, annotationUtil, globalAllowAndOperator, globalAllowOrOperator, globalMaxASTDepth);
    }

    /**
     * Determines whether this resolver should handle the given parameter.
     *
     * @param parameter method parameter under inspection
     * @return {@code true} when parameter is a {@code Specification} with
     *         method-level {@link WebQuery} and no DTO class is configured
     */
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        if(!super.supportsParameter(parameter)) return false;
        // supportsParameter in superclass checks for method-level @WebQuery presence, so we can safely assume that here
        return parameter.getMethod().getAnnotation(WebQuery.class).dtoClass() == void.class;
    }

    @Override
    protected Specification<?> resolveSpecification(@NonNull QueryConfiguration queryConfig, @NonNull String filter) {
        try {
            // Parse the RSQL query into an Abstract Syntax Tree (AST)
            Node root = rsqlParser.parse(filter);
            // Validate the parsed AST against the target entity and its @RSQLFilterable fields
            EntityValidationRSQLVisitor validationVisitor = new EntityValidationRSQLVisitor(
                    queryConfig.getEntityClass(),
                    queryConfig.getFieldMappings(),
                    annotationUtil,
                    queryConfig.isAndNodeAllowed(),
                    queryConfig.isOrNodeAllowed(),
                    queryConfig.getMaxASTDepth()
            );
            root.accept(validationVisitor, NodeMetadata.of(0));

            // Convert field mappings to aliases map which rsql jpa support library accepts
            Map<String, String> fieldMappingsMap = Arrays
                    .stream(queryConfig.getFieldMappings())
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
