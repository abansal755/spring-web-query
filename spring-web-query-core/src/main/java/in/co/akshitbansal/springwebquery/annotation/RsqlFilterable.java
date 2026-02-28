package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;

import java.lang.annotation.*;

/**
 * Marks a field as filterable via RSQL (RESTful Service Query Language) queries.
 * <p>
 * This annotation allows you to declare which RSQL operators (both default and custom)
 * are permitted on a specific field.
 * When used in combination with a RSQL-to-Spring-Data Specification resolver,
 * only the specified default and custom operators will be allowed for filtering on this field.
 * </p>
 *
 * <p>The annotation is applied to whichever type is used as the filtering contract:
 * entity fields in entity-aware mode, or DTO fields in DTO-aware mode.</p>
 *
 * <p>This annotation is {@linkplain Repeatable repeatable}; multiple declarations
 * can be used on the same field to compose the final set of allowed operators.</p>
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
 * @see RsqlFilterables
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(RsqlFilterables.class)
public @interface RsqlFilterable {

    /**
     * The set of default RSQL operators that are allowed for filtering this field.
     *
     * @return an array of allowed {@link RsqlOperator} values
     */
    RsqlOperator[] operators();

    /**
     * The set of custom RSQL operators that are allowed for filtering this field.
     * Referenced operator classes must be registered in the query resolver configuration.
     *
     * @return an array of custom operator classes
     */
    Class<? extends RsqlCustomOperator<?>>[] customOperators() default {};
}
