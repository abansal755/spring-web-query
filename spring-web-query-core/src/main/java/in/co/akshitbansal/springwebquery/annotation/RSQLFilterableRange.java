package in.co.akshitbansal.springwebquery.annotation;

import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;

import java.lang.annotation.*;

/**
 * Composed annotation that allows range and comparison filtering on a field.
 * <p>
 * Equivalent to declaring {@link RSQLFilterable} with:
 * {@link RSQLDefaultOperator#GREATER_THAN},
 * {@link RSQLDefaultOperator#GREATER_THAN_OR_EQUAL},
 * {@link RSQLDefaultOperator#LESS_THAN},
 * {@link RSQLDefaultOperator#LESS_THAN_OR_EQUAL},
 * {@link RSQLDefaultOperator#BETWEEN}, and
 * {@link RSQLDefaultOperator#NOT_BETWEEN}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RSQLFilterable({
        RSQLDefaultOperator.GREATER_THAN, RSQLDefaultOperator.GREATER_THAN_OR_EQUAL,
        RSQLDefaultOperator.LESS_THAN, RSQLDefaultOperator.LESS_THAN_OR_EQUAL,
        RSQLDefaultOperator.BETWEEN, RSQLDefaultOperator.NOT_BETWEEN
})
public @interface RSQLFilterableRange {
}
