package in.co.akshitbansal.springwebquery.annotation;

import java.lang.annotation.*;

/**
 * Container annotation for repeatable {@link RsqlFilterable} declarations on a field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RsqlFilterables {

    /**
     * Repeated {@link RsqlFilterable} entries declared on the same field.
     *
     * @return filterability declarations for the field
     */
    RsqlFilterable[] value();
}
