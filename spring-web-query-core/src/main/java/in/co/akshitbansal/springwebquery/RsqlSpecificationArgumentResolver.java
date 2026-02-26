package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.RsqlSpec;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import io.github.perplexhub.rsql.QuerySupport;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring MVC {@link HandlerMethodArgumentResolver} that resolves controller method
 * parameters annotated with {@link RsqlSpec} into Spring Data JPA
 * {@link Specification Specifications}.
 * <p>
 * This resolver enables transparent usage of RSQL queries in controller methods.
 * When a request contains an RSQL query parameter, the resolver:
 * <ol>
 *     <li>Resolves entity metadata and aliases from {@link WebQuery} on the controller method</li>
 *     <li>Parses the RSQL query string into an AST</li>
 *     <li>Validates the AST against the target entity using {@link ValidationRSQLVisitor}</li>
 *     <li>Converts the validated query into a {@link Specification} using
 *         {@link RSQLJPASupport}</li>
 * </ol>
 *
 * <p>If the RSQL query parameter is missing or blank, the resolver returns an
 * unrestricted Specification (equivalent to no filtering).</p>
 *
 * <p><b>Example controller usage:</b></p>
 * <pre>{@code
 * @GetMapping("/users")
 * @WebQuery(entityClass = User.class)
 * public List<User> search(
 *     @RsqlSpec Specification<User> spec
 * ) {
 *     return userRepository.findAll(spec);
 * }
 * }</pre>
 *
 * <p>This resolver must be registered via
 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurer}
 * to be active.</p>
 *
 * @see RsqlSpec
 * @see ValidationRSQLVisitor
 * @see RSQLJPASupport
 * @see Specification
 */
public class RsqlSpecificationArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * RSQL parser used to convert query strings into an AST.
     */
    private final RSQLParser rsqlParser;

    /**
     * List of RSQLCustomPredicate corresponding to the custom operators.
     */
    private final List<RSQLCustomPredicate<?>> customPredicates;

    private final AnnotationUtil annotationUtil;

    /**
     * Creates a new RsqlSpecificationArgumentResolver with the specified operators.
     *
     * @param defaultOperators set of default RSQL operators to support
     * @param customOperators  set of custom RSQL operators to support
     */
    public RsqlSpecificationArgumentResolver(Set<RsqlOperator> defaultOperators, Set<? extends RsqlCustomOperator<?>> customOperators, AnnotationUtil annotationUtil) {
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

    /**
     * Determines whether this resolver supports the given method parameter.
     * <p>
     * A parameter is supported if:
     * <ul>
     *     <li>It is assignable to {@link Specification}</li>
     *     <li>It is annotated with {@link RsqlSpec}</li>
     * </ul>
     *
     * @param parameter the method parameter to check
     * @return {@code true} if this resolver supports the parameter
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Specification.class.isAssignableFrom(parameter.getParameterType())
                && parameter.hasParameterAnnotation(RsqlSpec.class);
    }

    /**
     * Resolves the controller method argument into a {@link Specification}.
     * <p>
     * The RSQL query is read from the request parameter defined by
     * {@link RsqlSpec#paramName()}. Entity metadata and alias mappings are read
     * from {@link WebQuery} on the same controller method. The query is then
     * parsed, validated, and converted into a JPA {@link Specification}.
     *
     * @param parameter     the method parameter to resolve
     * @param mavContainer  the model and view container
     * @param webRequest    the current web request
     * @param binderFactory the data binder factory
     * @return a {@link Specification} representing the RSQL query,
     *         or an unrestricted Specification if the query is absent
     * @throws QueryValidationException if the RSQL query is invalid or violates
     *                       {@link RsqlFilterable} constraints
     * @throws QueryConfigurationException if query metadata or field mapping configuration is invalid
     */
    @Override
    public Specification<?> resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    )
    {
        try {
            // Extract the RSQL query string from the request using the parameter name defined in @RsqlSpec
            String filter = getRsqlQueryString(parameter, webRequest);
            if(filter == null || filter.isBlank()) return Specification.unrestricted();

            // Retrieve the @WebQuery annotation from the method parameter to access configuration
            WebQuery webQueryAnnotation = annotationUtil.resolveWebQueryFromParameter(parameter);
            // Extract entity and dto class
            Class<?> entityClass = webQueryAnnotation.entityClass();
            Class<?> dtoClass = webQueryAnnotation.dtoClass();

            // Entity mode
            if(dtoClass == void.class) {
                FieldMapping[] fieldMappings = webQueryAnnotation.fieldMappings();
                return buildEntityModeSpecification(entityClass, fieldMappings, filter);
            }

            // DTO mode
            return buildDtoModeSpecification(entityClass, dtoClass, filter);
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
     * Reads the raw RSQL query string from the request parameter declared by
     * {@link RsqlSpec#paramName()}.
     *
     * @param parameter method parameter annotated with {@link RsqlSpec}
     * @param webRequest current web request
     * @return raw query string, or {@code null} if the parameter is missing
     */
    private String getRsqlQueryString(@NonNull MethodParameter parameter, @NonNull NativeWebRequest webRequest) {
        // Retrieve the @RsqlSpec annotation from the method parameter to access parameter-specific configuration
        RsqlSpec annotation = parameter.getParameterAnnotation(RsqlSpec.class);
        // Null check not required for annotation since supportsParameter() ensures it is present
        // Extract the RSQL query string from the request using the configured parameter name
        return webRequest.getParameter(annotation.paramName());
    }

    private Specification<?> buildEntityModeSpecification(Class<?> entityClass, FieldMapping[] fieldMappings, String filter) {
        // Validate field mappings to ensure they are well-formed and do not contain conflicts
        annotationUtil.validateFieldMappings(fieldMappings);

        // Parse the RSQL query into an Abstract Syntax Tree (AST)
        Node root = rsqlParser.parse(filter);
        // Validate the parsed AST against the target entity and its @RsqlFilterable fields
        ValidationRSQLVisitor validationVisitor = new ValidationRSQLVisitor(
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

    private Specification<?> buildDtoModeSpecification(Class<?> entityClass, Class<?> dtoClass, String filter) {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(entityClass, dtoClass, annotationUtil);
        // Parse the RSQL query into an Abstract Syntax Tree (AST)
        Node root = rsqlParser.parse(filter);
        // Validate the parsed AST against the target DTO and its @RsqlFilterable fields, while also building field mappings from DTO to entity
        root.accept(visitor);

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
}
