package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;

import java.lang.annotation.*;

/**
 * Composed annotation that allows set-membership filtering on a field.
 * <p>
 * Equivalent to declaring {@link RSQLFilterable} with
 * {@link RSQLDefaultOperator#IN} and {@link RSQLDefaultOperator#NOT_IN}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RSQLFilterable({ RSQLDefaultOperator.IN, RSQLDefaultOperator.NOT_IN })
public @interface RSQLFilterableMembership {
}
