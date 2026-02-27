package in.co.akshitbansal.springwebquery.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares shared web-query metadata for a controller method.
 * <p>
 * This annotation is intended to be placed on handler methods so query-related
 * configuration can be defined once and reused by both filtering and sorting
 * argument resolvers.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebQuery {

    /**
     * Entity class against which filter and sort fields are resolved.
     *
     * @return target entity class
     */
    Class<?> entityClass();

    /**
     * Optional DTO class used as the API-facing query contract.
     *
     * <p>When set, incoming filter/sort paths are validated against this DTO and
     * translated to entity paths (optionally using {@link MapsTo}). When left as
     * {@code void.class}, entity fields are used directly.</p>
     *
     * @return DTO class or {@code void.class} when DTO mapping is disabled
     */
    Class<?> dtoClass() default void.class;

    /**
     * Optional field mappings used to expose API-facing aliases for entity fields.
     *
     * <p>These mappings are applied in entity-aware mode
     * ({@code dtoClass = void.class}). In DTO-aware mode, path translation is
     * driven by {@link MapsTo} annotations on DTO fields.</p>
     *
     * @return mappings between API names and entity paths
     */
    FieldMapping[] fieldMappings() default {};
}
