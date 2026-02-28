package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RsqlOperator;

import java.lang.annotation.*;

/**
 * Composed annotation that allows equality-based filtering on a field.
 * <p>
 * Equivalent to declaring {@link RsqlFilterable} with
 * {@link RsqlOperator#EQUAL} and {@link RsqlOperator#NOT_EQUAL}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RsqlFilterable(operators = { RsqlOperator.EQUAL, RsqlOperator.NOT_EQUAL })
public @interface RsqlFilterableEquality {
}
