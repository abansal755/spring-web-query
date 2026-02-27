package in.co.akshitbansal.springwebquery.annotation;

import java.lang.annotation.*;

/**
 * Marks a {@link org.springframework.data.domain.Pageable} controller method parameter
 * as subject to field-level sorting restrictions.
 *
 * <p>When applied, the pageable argument is validated so that sorting is only
 * allowed on fields explicitly annotated with {@link Sortable}.</p>
 *
 * <p>Validation behavior depends on {@link WebQuery} configuration on the same method:</p>
 * <ul>
 *     <li>Entity-aware mode ({@code dtoClass = void.class}): sort selectors are validated
 *         against entity fields and optional {@link FieldMapping} aliases.</li>
 *     <li>DTO-aware mode: sort selectors are validated against DTO fields and translated
 *         to entity paths via {@link MapsTo}.</li>
 * </ul>
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
 * @WebQuery(entityClass = User.class)
 * public Page<User> search(
 *     @RestrictedPageable Pageable pageable
 * ) {
 *     ...
 * }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestrictedPageable {

}
