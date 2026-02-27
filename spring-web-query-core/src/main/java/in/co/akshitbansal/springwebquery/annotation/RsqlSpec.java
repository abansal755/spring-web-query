package in.co.akshitbansal.springwebquery.annotation;

import java.lang.annotation.*;

/**
 * Marks a controller method parameter to be automatically resolved as a
 * {@link org.springframework.data.jpa.domain.Specification} from an RSQL query string.
 * <p>
 * When applied to a method parameter, the annotated parameter will receive a Specification
 * built from the RSQL query provided in the HTTP request. The RSQL query is parsed,
 * validated against the configured query contract, and converted into a Spring Data JPA
 * {@link org.springframework.data.jpa.domain.Specification}.
 * </p>
 *
 * <p>Validation behavior depends on {@link WebQuery} configuration on the same method:</p>
 * <ul>
 *     <li>Entity-aware mode ({@code dtoClass = void.class}): selectors are validated
 *         against entity fields annotated with {@link RsqlFilterable}, with optional
 *         alias support from {@link FieldMapping}.</li>
 *     <li>DTO-aware mode: selectors are validated against DTO fields annotated with
 *         {@link RsqlFilterable} and translated to entity paths through {@link MapsTo}.</li>
 * </ul>
 *
 * <p><b>Example usage in a controller:</b></p>
 * <pre>{@code
 * @GetMapping("/users")
 * @WebQuery(entityClass = User.class)
 * public List<User> search(
 *     @RsqlSpec(paramName = "filter") Specification<User> spec
 * ) {
 *     return userRepository.findAll(spec);
 * }
 * }</pre>
 *
 * <p>If the query parameter is not present in the request, the Specification
 * will be equivalent to {@code Specification.unrestricted()}, returning all results.</p>
 *
 * @see RsqlFilterable
 * @see org.springframework.data.jpa.domain.Specification
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RsqlSpec {

    /**
     * The name of the query parameter that contains the RSQL string.
     * Defaults to "filter".
     *
     * @return the HTTP request query parameter name
     */
    String paramName() default "filter";
}
