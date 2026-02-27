package in.co.akshitbansal.springwebquery.exception;

import lombok.Getter;

/**
 * Indicates that query validation failed for a specific field path.
 */
@Getter
public class QueryFieldValidationException extends QueryValidationException {

    /**
     * Query field path for which validation failed.
     */
    private final String fieldPath;

    /**
     * Creates a new field validation exception.
     *
     * @param message validation error details
     * @param fieldPath query field path associated with the failure
     */
    public QueryFieldValidationException(String message, String fieldPath) {
        super(message);
        this.fieldPath = fieldPath;
    }

    /**
     * Creates a new field validation exception with an underlying cause.
     *
     * @param message validation error details
     * @param fieldPath query field path associated with the failure
     * @param cause root cause of the validation failure
     */
    public QueryFieldValidationException(String message, String fieldPath, Throwable cause) {
        super(message, cause);
        this.fieldPath = fieldPath;
    }
}
