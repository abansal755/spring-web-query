package io.github.abansal755.spring_query.annotation;

import java.lang.annotation.*;

/**
 * Marks a {@link org.springframework.data.domain.Pageable} controller method parameter
 * as subject to field-level sorting restrictions.
 *
 * <p>When applied, the pageable argument is validated so that sorting is only
 * allowed on entity fields explicitly annotated with {@link Sortable}.</p>
 *
 * <p>This annotation does <strong>not</strong> affect pagination parameters
 * such as page number or page size. It only governs which fields may be used
 * in {@code sort} query parameters.</p>
 *
 * <p>The validation is enforced by a custom
 * {@link org.springframework.web.method.support.HandlerMethodArgumentResolver}
 * before the controller method is invoked.</p>
 *
 * <p>Example usage:</p>
 *
 * <pre>{@code
 * @GetMapping
 * public Page<User> search(
 *     @RestrictedPageable(entityClass = User.class) Pageable pageable
 * ) {
 *     ...
 * }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestrictedPageable {

    /**
     * Entity class whose fields define the set of properties
     * that may be used for sorting.
     *
     * <p>Only fields annotated with {@link Sortable} on this entity
     * are permitted in the {@code sort} request parameter.</p>
     *
     * @return the entity class used for sort validation
     */
    Class<?> entityClass();
}
