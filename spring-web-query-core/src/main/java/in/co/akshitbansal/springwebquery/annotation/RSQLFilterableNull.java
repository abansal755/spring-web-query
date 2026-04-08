package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;

import java.lang.annotation.*;

/**
 * Composed annotation that allows null-check filtering on a field.
 * <p>
 * Equivalent to declaring {@link RSQLFilterable} with
 * {@link RSQLDefaultOperator#IS_NULL} and {@link RSQLDefaultOperator#NOT_NULL}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RSQLFilterable({RSQLDefaultOperator.IS_NULL, RSQLDefaultOperator.NOT_NULL})
public @interface RSQLFilterableNull {
}
