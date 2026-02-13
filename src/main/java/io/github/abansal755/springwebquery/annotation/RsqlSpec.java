package io.github.abansal755.springwebquery.annotation;

import java.lang.annotation.*;

/**
 * Marks a controller method parameter to be automatically resolved as a
 * {@link org.springframework.data.jpa.domain.Specification} from an RSQL query string.
 * <p>
 * When applied to a method parameter, the annotated parameter will receive a Specification
 * built from the RSQL query provided in the HTTP request. The RSQL query is parsed,
 * validated against the target entity's {@link RsqlFilterable} annotations, and converted
 * into a Spring Data JPA {@link org.springframework.data.jpa.domain.Specification}.
 * </p>
 *
 * <p><b>Example usage in a controller:</b></p>
 * <pre>{@code
 * @GetMapping("/users")
 * public List<User> search(
 *     @RsqlSpec(entityClass = User.class, paramName = "filter") Specification<User> spec
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
     * The entity class for which the Specification should be built.
     * This class is used to validate the RSQL query fields and operators
     * against the {@link RsqlFilterable} annotations.
     *
     * @return the target entity class
     */
    Class<?> entityClass();

    FieldMapping[] fieldMappings() default {};

    /**
     * The name of the query parameter that contains the RSQL string.
     * Defaults to "filter".
     *
     * @return the HTTP request query parameter name
     */
    String paramName() default "filter";
}
