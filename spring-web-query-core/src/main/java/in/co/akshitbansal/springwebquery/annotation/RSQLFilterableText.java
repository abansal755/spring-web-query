package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;

import java.lang.annotation.*;

/**
 * Composed annotation that allows text-oriented filtering on a field.
 * <p>
 * Equivalent to declaring {@link RSQLFilterable} with:
 * {@link RSQLDefaultOperator#LIKE},
 * {@link RSQLDefaultOperator#NOT_LIKE},
 * {@link RSQLDefaultOperator#IGNORE_CASE_LIKE},
 * {@link RSQLDefaultOperator#IGNORE_CASE_NOT_LIKE}, and
 * {@link RSQLDefaultOperator#IGNORE_CASE}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RSQLFilterable({
        RSQLDefaultOperator.LIKE, RSQLDefaultOperator.NOT_LIKE,
        RSQLDefaultOperator.IGNORE_CASE_LIKE, RSQLDefaultOperator.IGNORE_CASE_NOT_LIKE,
        RSQLDefaultOperator.IGNORE_CASE
})
public @interface RSQLFilterableText {
}
