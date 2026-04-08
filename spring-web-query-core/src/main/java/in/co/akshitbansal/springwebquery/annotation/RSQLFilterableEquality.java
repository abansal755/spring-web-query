package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;

import java.lang.annotation.*;

/**
 * Composed annotation that allows equality-based filtering on a field.
 * <p>
 * Equivalent to declaring {@link RSQLFilterable} with
 * {@link RSQLDefaultOperator#EQUAL} and {@link RSQLDefaultOperator#NOT_EQUAL}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RSQLFilterable({RSQLDefaultOperator.EQUAL, RSQLDefaultOperator.NOT_EQUAL})
public @interface RSQLFilterableEquality {
}
