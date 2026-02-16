package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.RsqlSpec;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring MVC {@link HandlerMethodArgumentResolver} that resolves controller method
 * parameters annotated with {@link RsqlSpec} into Spring Data JPA
 * {@link Specification Specifications}.
 * <p>
 * This resolver enables transparent usage of RSQL queries in controller methods.
 * When a request contains an RSQL query parameter, the resolver:
 * <ol>
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
 * public List<User> search(
 *     @RsqlSpec(entityClass = User.class) Specification<User> spec
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
@RequiredArgsConstructor
public class RsqlSpecificationArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * RSQL parser used to convert query strings into an AST.
     */
    private final RSQLParser rsqlParser;

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
     * {@link RsqlSpec#paramName()}. The query is then parsed, validated,
     * and converted into a JPA {@link Specification}.
     *
     * @param parameter     the method parameter to resolve
     * @param mavContainer  the model and view container
     * @param webRequest    the current web request
     * @param binderFactory the data binder factory
     * @return a {@link Specification} representing the RSQL query,
     *         or an unrestricted Specification if the query is absent
     * @throws QueryException if the RSQL query is invalid or violates
     *                       {@link RsqlFilterable} constraints
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
            // Retrieve the @RsqlSpec annotation to access configuration
            RsqlSpec annotation = parameter.getParameterAnnotation(RsqlSpec.class);
            // Extract the RSQL query string from the request using the configured parameter name
            String filter = webRequest.getParameter(annotation.paramName());
            // If no filter is provided, return an unrestricted Specification (no filtering)
            if(filter == null || filter.isBlank()) return Specification.unrestricted();

            // Parse the RSQL query into an Abstract Syntax Tree (AST)
            Node root = rsqlParser.parse(filter);
            // Validate the parsed AST against the target entity and its @RsqlFilterable fields
            ValidationRSQLVisitor validationVisitor = new ValidationRSQLVisitor(annotation.entityClass(), annotation.fieldMappings());
            root.accept(validationVisitor);

            // Convert field mappings to aliases map which rsql jpa support library accepts
            Map<String, String> fieldMappings = Arrays
                    .stream(annotation.fieldMappings())
                    .collect(Collectors.toMap(FieldMapping::name, FieldMapping::field));
            // Convert the validated RSQL query into a JPA Specification
            return RSQLJPASupport.toSpecification(filter, fieldMappings);
        }
        catch (RSQLParserException ex) {
            throw new QueryException("Unable to parse rsql query param", ex);
        }
        catch (QueryException ex) {
            throw ex;
        }
    }
}
