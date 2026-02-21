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
     * Optional field mappings used to expose API-facing aliases for entity fields.
     *
     * @return mappings between API names and entity paths
     */
    FieldMapping[] fieldMappings() default {};
}
