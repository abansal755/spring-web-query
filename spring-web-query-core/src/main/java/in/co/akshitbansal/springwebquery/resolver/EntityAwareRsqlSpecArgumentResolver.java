package in.co.akshitbansal.springwebquery.resolver;

import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import in.co.akshitbansal.springwebquery.EntityValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlSpec;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import io.github.perplexhub.rsql.QuerySupport;
import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entity-based resolver for {@link Specification} parameters annotated with
 * {@link RsqlSpec}.
 *
 * <p>This resolver is active when {@link WebQuery#dtoClass()} is not configured
 * (i.e. equals {@code void.class}). It validates incoming RSQL selectors against
 * entity fields and optional {@link FieldMapping} aliases before producing a
 * JPA specification.</p>
 */
public class EntityAwareRsqlSpecArgumentResolver extends RsqlSpecArgumentResolver {

    /**
     * Creates an entity-aware RSQL specification resolver.
     *
     * @param defaultOperators built-in operators accepted in RSQL expressions
     * @param customOperators custom operators supported by parser and predicates
     * @param annotationUtil utility for resolving annotations and configuration checks
     */
    public EntityAwareRsqlSpecArgumentResolver(Set<RsqlOperator> defaultOperators, Set<? extends RsqlCustomOperator<?>> customOperators, AnnotationUtil annotationUtil) {
        super(defaultOperators, customOperators, annotationUtil);
    }

    /**
     * Determines whether this resolver should handle the given parameter.
     *
     * @param parameter method parameter under inspection
     * @return {@code true} when parameter is a {@code Specification} with
     *         {@link RsqlSpec} and no DTO class is configured
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if(!Specification.class.isAssignableFrom(parameter.getParameterType())) return false;
        if(!parameter.hasParameterAnnotation(RsqlSpec.class)) return false;
        WebQuery webQueryAnnotation = annotationUtil.resolveWebQueryFromParameter(parameter);
        return webQueryAnnotation.dtoClass() == void.class;
    }

    /**
     * Resolves a {@link Specification} from the configured RSQL request parameter.
     *
     * @param parameter controller method parameter being resolved
     * @param mavContainer current MVC container
     * @param webRequest current request
     * @param binderFactory binder factory
     * @return resolved specification, or {@link Specification#unrestricted()} when no filter exists
     * @throws Exception when resolution fails
     */
    @Override
    public @Nullable Object resolveArgument(
            @NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) throws Exception
    {
        try {
            // Extract the RSQL query string from the request using the parameter name defined in @RsqlSpec
            String filter = getRsqlQueryString(parameter, webRequest);
            if(filter == null || filter.isBlank()) return Specification.unrestricted();

            // Retrieve the @WebQuery annotation from the method parameter to access configuration
            WebQuery webQueryAnnotation = annotationUtil.resolveWebQueryFromParameter(parameter);
            // Extract entity class and field mappings from the @WebQuery annotation for validation and specification building
            Class<?> entityClass = webQueryAnnotation.entityClass();
            FieldMapping[] fieldMappings = webQueryAnnotation.fieldMappings();

            // Validate field mappings to ensure they are well-formed and do not contain conflicts
            annotationUtil.validateFieldMappings(fieldMappings);

            // Parse the RSQL query into an Abstract Syntax Tree (AST)
            Node root = rsqlParser.parse(filter);
            // Validate the parsed AST against the target entity and its @RsqlFilterable fields
            EntityValidationRSQLVisitor validationVisitor = new EntityValidationRSQLVisitor(
                    entityClass,
                    fieldMappings,
                    annotationUtil
            );
            root.accept(validationVisitor);

            // Convert field mappings to aliases map which rsql jpa support library accepts
            Map<String, String> fieldMappingsMap = Arrays
                    .stream(fieldMappings)
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
        catch (QueryException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new QueryConfigurationException("Failed to resolve RSQL Specification argument", ex);
        }
    }

    /**
     * Reads the configured RSQL query string from the request.
     *
     * @param parameter method parameter annotated with {@link RsqlSpec}
     * @param webRequest current web request
     * @return raw RSQL query string, or {@code null} if not present
     */
    private String getRsqlQueryString(@NonNull MethodParameter parameter, @NonNull NativeWebRequest webRequest) {
        // Retrieve the @RsqlSpec annotation from the method parameter to access parameter-specific configuration
        RsqlSpec annotation = parameter.getParameterAnnotation(RsqlSpec.class);
        // Null check not required for annotation since supportsParameter() ensures it is present
        // Extract the RSQL query string from the request using the configured parameter name
        return webRequest.getParameter(annotation.paramName());
    }
}
