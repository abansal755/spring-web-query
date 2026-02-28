package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RsqlOperator;

import java.lang.annotation.*;

/**
 * Composed annotation that allows set-membership filtering on a field.
 * <p>
 * Equivalent to declaring {@link RsqlFilterable} with
 * {@link RsqlOperator#IN} and {@link RsqlOperator#NOT_IN}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RsqlFilterable(operators = { RsqlOperator.IN, RsqlOperator.NOT_IN })
public @interface RsqlFilterableMembership {
}
