package in.co.akshitbansal.springwebquery.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a mapping between an API-facing field name and an actual entity field name.
 * <p>
 * This annotation is used within {@link WebQuery#fieldMappings()} to create aliases
 * for entity fields in filtering and sorting requests. It allows clients to use
 * simpler or more intuitive names while mapping them to the actual field names
 * on the entity.
 * </p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * @GetMapping("/users")
 * @WebQuery(
 *     entityClass = User.class,
 *     fieldMappings = {
 *         @FieldMapping(name = "id", field = "userId"),
 *         @FieldMapping(name = "fullName", field = "profile.name")
 *     }
 * )
 * public List<User> search(
 *     @RsqlSpec Specification<User> spec,
 *     @RestrictedPageable Pageable pageable
 * ) {
 *     return userRepository.findAll(spec);
 * }
 * }</pre>
 *
 * <p>In the example above, clients can use {@code id==123} or {@code fullName==John}
 * in their RSQL queries, which will be translated to {@code userId==123} and
 * {@code profile.name==John} respectively.</p>
 *
 * @see WebQuery
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface FieldMapping {

    /**
     * The alias name to use in API query strings.
     * <p>
     * This is the name that clients will use when constructing their queries.
     * </p>
     *
     * @return the query parameter field name
     */
    String name();

    /**
     * The actual field name or path on the entity.
     * <p>
     * This can be a simple field name (e.g., {@code "userId"}) or a nested path
     * using dot notation (e.g., {@code "profile.name"}).
     * </p>
     *
     * @return the entity field name or path
     */
    String field();

    /**
     * Whether to allow the use of the original field name in addition to the alias.
     * <p>
     * When {@code false} (default), only the alias name defined in {@link #name()}
     * can be used in filter and sort expressions. When {@code true}, both the alias
     * and the original field name are allowed.
     * </p>
     *
     * @return {@code true} if the original field name should remain usable,
     *         {@code false} to enforce exclusive use of the alias
     */
    boolean allowOriginalFieldName() default false;
}
