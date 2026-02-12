package io.github.abansal755.spring_query.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity field as eligible for sorting in API query requests.
 *
 * <p>Fields annotated with {@code @Sortable} may be referenced in
 * {@code sort} query parameters when pagination is enabled via
 * {@link RestrictedPageable}.</p>
 *
 * <p>This annotation is purely declarative and does not impose any
 * persistence or indexing requirements. Its sole purpose is to
 * explicitly whitelist fields that are safe and supported for sorting
 * at the API level.</p>
 *
 * <p>Sorting requests targeting fields not annotated with
 * {@code @Sortable} will be rejected during request resolution.</p>
 *
 * <p>Example usage:</p>
 *
 * <pre>{@code
 * @Entity
 * public class User {
 *
 *     @Sortable
 *     private String username;
 *
 *     @Sortable
 *     private Instant createdAt;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sortable {
}
