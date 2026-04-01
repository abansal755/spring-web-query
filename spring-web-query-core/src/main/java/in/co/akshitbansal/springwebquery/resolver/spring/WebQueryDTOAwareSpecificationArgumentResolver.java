package in.co.akshitbansal.springwebquery.resolver.spring;

import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.ast.DTOValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.ast.NodeMetadata;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import io.github.perplexhub.rsql.QuerySupport;
import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

/**
 * DTO-based resolver for {@link Specification} parameters handled via
 * method-level {@link WebQuery}.
 *
 * <p>This resolver is active when {@link WebQuery#dtoClass()} is configured.
 * Incoming RSQL selectors are validated against DTO fields and then translated
 * to entity paths before the specification is produced.</p>
 */
public class WebQueryDTOAwareSpecificationArgumentResolver extends AbstractWebQuerySpecificationArgumentResolver {

    /**
     * Creates a DTO-aware RSQL specification resolver.
     *
     * @param defaultOperators built-in operators accepted in RSQL expressions
     * @param customOperators custom operators supported by parser and predicates
     * @param globalAllowAndOperator whether AND nodes are allowed by default when {@code @WebQuery}
     *                               does not override that behavior
     * @param globalAllowOrOperator whether OR nodes are allowed by default when {@code @WebQuery}
     *                              does not override that behavior
     * @param globalMaxASTDepth maximum AST depth allowed by default when {@code @WebQuery}
     *                          does not override that behavior
     */
    public WebQueryDTOAwareSpecificationArgumentResolver(
            Set<RSQLDefaultOperator> defaultOperators,
            Set<? extends RSQLCustomOperator<?>> customOperators,
            boolean globalAllowAndOperator,
            boolean globalAllowOrOperator,
            int globalMaxASTDepth
    ) {
        super(defaultOperators, customOperators, globalAllowAndOperator, globalAllowOrOperator, globalMaxASTDepth);
    }

    /**
     * Determines whether this resolver should handle the given parameter.
     *
     * @param parameter method parameter under inspection
     * @return {@code true} when parameter is a {@code Specification} with
     *         method-level {@link WebQuery} and a configured DTO class
     */
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return super.supportsParameter(parameter) // supportsParameter in superclass checks for method-level @WebQuery presence
                && getWebQueryAnnotation(parameter).dtoClass() != void.class; // so no exception handling is needed
    }

    /**
     * Parses, validates, and converts a DTO-oriented RSQL filter into a JPA
     * {@link Specification}.
     *
     * @param queryConfig effective query configuration for the current request
     * @param filter raw RSQL filter string from the request
     * @return resolved specification for the validated filter
     */
    @Override
    protected Specification<?> resolveSpecification(@NonNull QueryConfiguration queryConfig, @NonNull String filter) {
        try {
            // Parse the RSQL query into an Abstract Syntax Tree (AST)
            Node root = rsqlParser.parse(filter);
            // Validate the parsed AST against the target DTO and its @RSQLFilterable fields, while also building field mappings from DTO to entity
            DTOValidationRSQLVisitor visitor = new DTOValidationRSQLVisitor(
                    queryConfig.getEntityClass(),
                    queryConfig.getDtoClass(),
                    customOperators,
                    queryConfig.isAndNodeAllowed(),
                    queryConfig.isOrNodeAllowed(),
                    queryConfig.getMaxASTDepth()
            );
            root.accept(visitor, NodeMetadata.of(0));

            // Convert the validated RSQL query into a JPA Specification
            QuerySupport querySupport = QuerySupport
                    .builder()
                    .rsqlQuery(filter)
                    .propertyPathMapper(visitor.getFieldMappings())
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
