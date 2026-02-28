package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RsqlOperator;

import java.lang.annotation.*;

/**
 * Composed annotation that allows null-check filtering on a field.
 * <p>
 * Equivalent to declaring {@link RsqlFilterable} with
 * {@link RsqlOperator#IS_NULL} and {@link RsqlOperator#NOT_NULL}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RsqlFilterable(operators = {RsqlOperator.IS_NULL, RsqlOperator.NOT_NULL})
public @interface RsqlFilterableNull {
}
