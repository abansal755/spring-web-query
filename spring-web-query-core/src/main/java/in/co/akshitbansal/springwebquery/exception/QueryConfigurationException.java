package in.co.akshitbansal.springwebquery.exception;

/**
 * Exception thrown when the library is misconfigured by the developer.
 * <p>
 * This exception indicates an internal configuration error, such as referencing
 * a custom operator that has not been registered with the {@code RsqlSpecificationArgumentResolver}.
 * </p>
 *
 * <p>This exception is intended to be treated as a 5xx server error
 * as it highlights a development-time configuration issue.</p>
 */
public class QueryConfigurationException extends QueryException {

    /**
     * Constructs a new query configuration exception with the specified detail message.
     *
     * @param message the detail message explaining the reason for the configuration error
     */
    public QueryConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new query configuration exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the configuration error
     * @param cause   the underlying cause of the configuration error
     */
    public QueryConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
