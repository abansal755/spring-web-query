package in.co.akshitbansal.springwebquery.validator;

import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import lombok.NonNull;

import java.text.MessageFormat;

/**
 * Validates HTTP query parameter names used by web-query configuration.
 *
 * <p>The accepted format is restricted to a conservative identifier-style
 * subset so configured names remain unambiguous in URLs and compatible with
 * Spring request parameter lookup.</p>
 */
public class QueryParamNameValidator implements Validator<String> {

    /**
     * Safe character set allowed for configured query parameter names.
     */
    private static final String regex = "^[A-Za-z0-9._-]+$";

    /**
     * Validates that the supplied parameter name matches the supported query
     * parameter naming pattern.
     *
     * @param paramName query parameter name to validate
     * @throws QueryConfigurationException if the parameter name contains
     *                                     unsupported characters or is otherwise invalid
     */
    @Override
    public void validate(@NonNull String paramName) {
        if(!paramName.matches(regex)) {
            throw new QueryConfigurationException(MessageFormat.format(
                    "Invalid query parameter name: {0}. Must match regex: {1}", paramName, regex
            ));
        }
    }
}
