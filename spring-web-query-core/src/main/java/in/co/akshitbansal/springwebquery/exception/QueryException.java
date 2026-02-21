package in.co.akshitbansal.springwebquery.exception;

import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.Sortable;

/**
 * Base exception thrown for all RSQL query or pagination related errors.
 * <p>
 * This class serves as the parent for more specific exceptions that distinguish
 * between client-side validation errors and developer-side configuration errors:
 * </p>
 * <ul>
 *     <li>{@link QueryValidationException}: Thrown when an API consumer provides
 *         an invalid query or violates validation rules (e.g., malformed RSQL,
 *         disallowed operators, non-filterable fields).</li>
 *     <li>{@link QueryConfigurationException}: Thrown when the library or
 *         entity mapping is misconfigured by the developer (e.g., unregistered
 *         custom operators).</li>
 * </ul>
 *
 * <p>Using this base exception in a controller advice or catch block allows
 * handling all query-related errors in a unified manner.</p>
 *
 * @see QueryValidationException
 * @see QueryConfigurationException
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
