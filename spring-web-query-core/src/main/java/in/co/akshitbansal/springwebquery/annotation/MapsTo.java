package in.co.akshitbansal.springwebquery.annotation;

import java.lang.annotation.*;

/**
 * Declares the entity-side property segment that a DTO field maps to for
 * filtering and sorting path translation.
 *
 * <p>When DTO-aware query resolution is enabled, each path segment is resolved
 * on the DTO class and then translated to an entity path using this annotation.
 * If absent, the DTO field name itself is used as the entity segment.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * class UserDto {
 *   @MapsTo(field = "profile")
 *   private ProfileDto details;
 *
 *   class ProfileDto {
 *     @MapsTo(field = "displayName")
 *     private String name;
 *   }
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapsTo {

    /**
     * Entity-side field or path segment to use for this DTO field.
     *
     * @return mapped entity path segment
     */
    String field();

    /**
     * Whether this mapping starts a new absolute path from the entity root.
     *
     * <p>When {@code true}, previously accumulated parent segments are discarded
     * before this segment is applied.</p>
     *
     * @return {@code true} to reset parent segments, otherwise {@code false}
     */
    boolean absolute() default false;
}
