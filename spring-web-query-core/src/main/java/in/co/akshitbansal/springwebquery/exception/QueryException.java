package in.co.akshitbansal.springwebquery.exception;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.Sortable;

/**
 * Exception thrown when an RSQL query or pagination request violates
 * configured field or operator restrictions.
 * <p>
 * This exception is typically thrown in the following scenarios:
 * <ul>
 *     <li>A query attempts to filter on a field not annotated with
 *         {@link RsqlFilterable}</li>
 *     <li>A query uses a default or custom operator not allowed for a specific field</li>
 *     <li>A query uses original field name when the behavior is disabled via {@link FieldMapping#allowOriginalFieldName()}</li>
 *     <li>A sort request targets a field not annotated with
 *         {@link Sortable}</li>
 *     <li>A field referenced in a query does not exist on the entity</li>
 *     <li>A custom operator referenced in {@link RsqlFilterable} is not registered</li>
 *     <li>The RSQL query syntax is malformed or cannot be parsed</li>
 * </ul>
 *
 * <p>This is a runtime exception and is intended to be caught and handled
 * at the controller or advice layer to provide meaningful error responses
 * to API clients.</p>
 *
 * @see RsqlFilterable
 * @see Sortable
 */
public class QueryException extends RuntimeException {

    /**
     * Constructs a new query exception with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public QueryException(String message) {
        super(message);
    }

    /**
     * Constructs a new query exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the underlying cause of the exception
     */
    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
