package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RsqlOperator;

import java.lang.annotation.*;

/**
 * Composed annotation that allows text-oriented filtering on a field.
 * <p>
 * Equivalent to declaring {@link RsqlFilterable} with:
 * {@link RsqlOperator#LIKE},
 * {@link RsqlOperator#NOT_LIKE},
 * {@link RsqlOperator#IGNORE_CASE_LIKE},
 * {@link RsqlOperator#IGNORE_CASE_NOT_LIKE}, and
 * {@link RsqlOperator#IGNORE_CASE}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RsqlFilterable(operators = {
        RsqlOperator.LIKE, RsqlOperator.NOT_LIKE,
        RsqlOperator.IGNORE_CASE_LIKE, RsqlOperator.IGNORE_CASE_NOT_LIKE,
        RsqlOperator.IGNORE_CASE
})
public @interface RsqlFilterableText {
}
