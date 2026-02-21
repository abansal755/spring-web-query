package in.co.akshitbansal.springwebquery.exception;

/**
 * Exception thrown when an API consumer provides an invalid RSQL query,
 * sorting request, or filter parameters.
 * <p>
 * This exception indicates that the request itself is malformed or violates
 * configured validation rules (e.g., filtering on a non-filterable field,
 * using a disallowed operator, or providing invalid RSQL syntax).
 * </p>
 *
 * <p>This exception is intended to be caught and returned as a 4xx client
 * error to the consumer.</p>
 */
public class QueryValidationException extends QueryException {

    /**
     * Constructs a new query validation exception with the specified detail message.
     *
     * @param message the detail message explaining the reason for the validation failure
     */
    public QueryValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new query validation exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the validation failure
     * @param cause   the underlying cause of the validation failure (e.g., {@link cz.jirutka.rsql.parser.RSQLParserException})
     */
    public QueryValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
