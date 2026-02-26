package in.co.akshitbansal.springwebquery.operator;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.EntityValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
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
 * and with {@link EntityValidationRSQLVisitor} to enforce these constraints at runtime.
 * </p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * @RsqlFilterable(operators = {RsqlOperator.EQUAL, RsqlOperator.IN})
 * private String status;
 * }</pre>
 *
 * @see RsqlFilterable
 * @see EntityValidationRSQLVisitor
 * @see RSQLOperators
 * @see cz.jirutka.rsql.parser.ast.ComparisonOperator
 */
@RequiredArgsConstructor
@Getter
public enum RsqlOperator {

    /**
     * Strict equality operator ({@code ==}).
     * <p>
     * Note: This operator enforces strict equality. Wildcards (e.g., {@code *}) are not supported
     * and will be treated as literal characters.
     * </p>
     * <p><b>Example:</b> {@code name=="John Doe"}</p>
     * <p><b>SQL Equivalent:</b> {@code name = 'John Doe'}</p>
     */
    EQUAL(RSQLOperators.EQUAL),
    /**
     * Inequality operator ({@code !=}).
     * <p><b>Example:</b> {@code status!="DELETED"}</p>
     * <p><b>SQL Equivalent:</b> {@code status <> 'DELETED'}</p>
     */
    NOT_EQUAL(RSQLOperators.NOT_EQUAL),

    /**
     * Greater than operator ({@code >} or {@code =gt=}).
     * <p><b>Example:</b> {@code age>18} or {@code age=gt=18}</p>
     * <p><b>SQL Equivalent:</b> {@code age > 18}</p>
     */
    GREATER_THAN(RSQLOperators.GREATER_THAN),
    /**
     * Greater than or equal to operator ({@code >=} or {@code =ge=}).
     * <p><b>Example:</b> {@code price>=100.0} or {@code price=ge=100.0}</p>
     * <p><b>SQL Equivalent:</b> {@code price >= 100.0}</p>
     */
    GREATER_THAN_OR_EQUAL(RSQLOperators.GREATER_THAN_OR_EQUAL),
    /**
     * Less than operator ({@code <} or {@code =lt=}).
     * <p><b>Example:</b> {@code score<50} or {@code score=lt=50}</p>
     * <p><b>SQL Equivalent:</b> {@code score < 50}</p>
     */
    LESS_THAN(RSQLOperators.LESS_THAN),
    /**
     * Less than or equal to operator ({@code <=} or {@code =le=}).
     * <p><b>Example:</b> {@code count<=10} or {@code count=le=10}</p>
     * <p><b>SQL Equivalent:</b> {@code count <= 10}</p>
     */
    LESS_THAN_OR_EQUAL(RSQLOperators.LESS_THAN_OR_EQUAL),

    /**
     * Set membership operator ({@code =in=}).
     * Expects a list of values.
     * <p><b>Example:</b> {@code role=in=(ADMIN,USER)}</p>
     * <p><b>SQL Equivalent:</b> {@code role IN ('ADMIN', 'USER')}</p>
     */
    IN(RSQLOperators.IN),
    /**
     * Set non-membership operator ({@code =out=}).
     * Expects a list of values.
     * <p><b>Example:</b> {@code department=out=(HR,FINANCE)}</p>
     * <p><b>SQL Equivalent:</b> {@code department NOT IN ('HR', 'FINANCE')}</p>
     */
    NOT_IN(RSQLOperators.NOT_IN),

    /**
     * Null check operator ({@code =null=}, {@code =isnull=}, or {@code =na=}).
     * <p><b>Example:</b> {@code middleName=null=} or {@code middleName=null=true} or {@code middleName=isnull=true} or {@code middleName=na=true}</p>
     * <p><b>SQL Equivalent:</b> {@code middleName IS NULL}</p>
     */
    IS_NULL(RSQLOperators.IS_NULL),
    /**
     * Non-null check operator ({@code =notnull=}, {@code =isnotnull=}, or {@code =nn=}).
     * <p><b>Example:</b> {@code email=notnull=} or {@code email=notnull=true} or {@code email=isnotnull=true} or {@code email=nn=true}</p>
     * <p><b>SQL Equivalent:</b> {@code email IS NOT NULL}</p>
     */
    NOT_NULL(RSQLOperators.NOT_NULL),

    /**
     * Like operator ({@code =like=} or {@code =ke=}).
     * <p><b>Example:</b> {@code description=like="spring"}</p>
     * <p><b>SQL Equivalent:</b> {@code description LIKE '%spring%'}</p>
     */
    LIKE(RSQLOperators.LIKE),
    /**
     * Not like operator ({@code =notlike=} or {@code =nk=}).
     * <p><b>Example:</b> {@code title=notlike="Draft"}</p>
     * <p><b>SQL Equivalent:</b> {@code title NOT LIKE '%Draft%'}</p>
     */
    NOT_LIKE(RSQLOperators.NOT_LIKE),

    /**
     * Case-insensitive equality operator ({@code =icase=} or {@code =ic=}).
     * <p><b>Example:</b> {@code city=icase=london}</p>
     * <p><b>SQL Equivalent:</b> {@code LOWER(city) = LOWER('london')}</p>
     */
    IGNORE_CASE(RSQLOperators.IGNORE_CASE),
    /**
     * Case-insensitive like operator ({@code =ilike=} or {@code =ik=}).
     * <p><b>Example:</b> {@code username=ilike="admin"}</p>
     * <p><b>SQL Equivalent:</b> {@code LOWER(username) LIKE LOWER('%admin%')}</p>
     */
    IGNORE_CASE_LIKE(RSQLOperators.IGNORE_CASE_LIKE),
    /**
     * Case-insensitive not like operator ({@code =inotlike=} or {@code =ni=}).
     * <p><b>Example:</b> {@code tag=inotlike="test"}</p>
     * <p><b>SQL Equivalent:</b> {@code LOWER(tag) NOT LIKE LOWER('%test%')}</p>
     */
    IGNORE_CASE_NOT_LIKE(RSQLOperators.IGNORE_CASE_NOT_LIKE),

    /**
     * Range operator ({@code =between=} or {@code =bt=}).
     * Expects exactly two values.
     * <p><b>Example:</b> {@code createdAt=between=(2023-01-01,2023-12-31)}</p>
     * <p><b>SQL Equivalent:</b> {@code createdAt BETWEEN '2023-01-01' AND '2023-12-31'}</p>
     */
    BETWEEN(RSQLOperators.BETWEEN),
    /**
     * Not between operator ({@code =notbetween=} or {@code =nb=}).
     * Expects exactly two values.
     * <p><b>Example:</b> {@code age=notbetween=(18,65)}</p>
     * <p><b>SQL Equivalent:</b> {@code age NOT BETWEEN 18 AND 65}</p>
     */
    NOT_BETWEEN(RSQLOperators.NOT_BETWEEN);

    private final ComparisonOperator operator;
}
