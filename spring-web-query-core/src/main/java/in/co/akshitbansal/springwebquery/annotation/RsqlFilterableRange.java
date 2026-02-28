package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RsqlOperator;

import java.lang.annotation.*;

/**
 * Composed annotation that allows range and comparison filtering on a field.
 * <p>
 * Equivalent to declaring {@link RsqlFilterable} with:
 * {@link RsqlOperator#GREATER_THAN},
 * {@link RsqlOperator#GREATER_THAN_OR_EQUAL},
 * {@link RsqlOperator#LESS_THAN},
 * {@link RsqlOperator#LESS_THAN_OR_EQUAL},
 * {@link RsqlOperator#BETWEEN}, and
 * {@link RsqlOperator#NOT_BETWEEN}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RsqlFilterable(operators = {
        RsqlOperator.GREATER_THAN, RsqlOperator.GREATER_THAN_OR_EQUAL,
        RsqlOperator.LESS_THAN, RsqlOperator.LESS_THAN_OR_EQUAL,
        RsqlOperator.BETWEEN, RsqlOperator.NOT_BETWEEN
})
public @interface RsqlFilterableRange {
}
