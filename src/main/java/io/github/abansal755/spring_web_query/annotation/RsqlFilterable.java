package io.github.abansal755.spring_web_query.annotation;

import io.github.abansal755.spring_web_query.enums.RsqlOperator;

import java.lang.annotation.*;

/**
 * Marks an entity field as filterable via RSQL (RESTful Service Query Language) queries.
 * <p>
 * This annotation allows you to declare which RSQL operators are permitted on a specific field.
 * When used in combination with a RSQL-to-Spring-Data Specification resolver,
 * only the specified operators will be allowed for filtering on this field.
 * </p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * @RsqlFilterable(operators = {RsqlOperator.EQUAL, RsqlOperator.IN})
 * private String status;
 * }</pre>
 *
 * <p>Fields without this annotation are considered <em>not filterable</em> and attempts
 * to filter them via RSQL queries will result in an exception.</p>
 *
 * <p>This annotation is retained at runtime and can be inspected via reflection.</p>
 *
 * @see RsqlOperator
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RsqlFilterable {

    /**
     * The set of RSQL operators that are allowed for filtering this field.
     *
     * @return an array of allowed {@link RsqlOperator} values
     */
    RsqlOperator[] operators();
}
