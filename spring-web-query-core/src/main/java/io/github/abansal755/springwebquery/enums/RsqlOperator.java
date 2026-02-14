package io.github.abansal755.springwebquery.enums;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import io.github.abansal755.springwebquery.ValidationRSQLVisitor;
import io.github.abansal755.springwebquery.annotation.RsqlFilterable;
import io.github.perplexhub.rsql.RSQLOperators;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of supported RSQL comparison operators.
 * <p>
 * This enum provides a type-safe wrapper around the built-in
 * {@link cz.jirutka.rsql.parser.ast.RSQLOperators} supplied by the RSQL library.
 * Each enum constant maps directly to a corresponding {@link cz.jirutka.rsql.parser.ast.ComparisonOperator}
 * instance and represents a single logical comparison operation supported by RSQL.
 *
 * <p>
 * The primary purpose of this enum is to:
 * <ul>
 *     <li>Expose RSQL operators in a form that can be safely used in Java annotations</li>
 *     <li>Allow fine-grained control over which operators are permitted for a given entity field</li>
 *     <li>Decouple application code from direct usage of {@link RSQLOperators}</li>
 * </ul>
 *
 * <p>
 * {@link RsqlOperator} is typically used in conjunction with
 * {@link RsqlFilterable} to declare the set of allowed operators on an entity field,
 * and with {@link ValidationRSQLVisitor} to enforce these constraints at runtime.
 * </p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * @RsqlFilterable(operators = {RsqlOperator.EQUAL, RsqlOperator.IN})
 * private String status;
 * }</pre>
 *
 * @see RsqlFilterable
 * @see ValidationRSQLVisitor
 * @see RSQLOperators
 * @see cz.jirutka.rsql.parser.ast.ComparisonOperator
 */
@RequiredArgsConstructor
@Getter
public enum RsqlOperator {

    // Equality
    EQUAL(RSQLOperators.EQUAL), // ==
    NOT_EQUAL(RSQLOperators.NOT_EQUAL), // !=

    // Comparison
    GREATER_THAN(RSQLOperators.GREATER_THAN), // > or =gt=
    GREATER_THAN_OR_EQUAL(RSQLOperators.GREATER_THAN_OR_EQUAL),// >= or =ge=
    LESS_THAN(RSQLOperators.LESS_THAN), // < or =lt=
    LESS_THAN_OR_EQUAL(RSQLOperators.LESS_THAN_OR_EQUAL), // <= or =le=

    // Set membership
    IN(RSQLOperators.IN), // =in=
    NOT_IN(RSQLOperators.NOT_IN), // =out=

    // Null checks
    IS_NULL(RSQLOperators.IS_NULL), // =null= or =isnull= or =na=
    NOT_NULL(RSQLOperators.NOT_NULL), // =notnull= or =isnotnull= or =nn=

    // Like
    LIKE(RSQLOperators.LIKE), // =like= or =ke=
    NOT_LIKE(RSQLOperators.NOT_LIKE), // =notlike= or =nk=

    // Case-insensitive
    IGNORE_CASE(RSQLOperators.IGNORE_CASE), // =icase= or =ic=
    IGNORE_CASE_LIKE(RSQLOperators.IGNORE_CASE_LIKE), // =ilike= or =ik=
    IGNORE_CASE_NOT_LIKE(RSQLOperators.IGNORE_CASE_NOT_LIKE), // =inotlike= or =ni=

    // Range
    BETWEEN(RSQLOperators.BETWEEN), // =between= or =bt=
    NOT_BETWEEN(RSQLOperators.NOT_BETWEEN); // =notbetween= or =nb=

    private final ComparisonOperator operator;
}
