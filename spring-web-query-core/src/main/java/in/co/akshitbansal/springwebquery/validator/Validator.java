package in.co.akshitbansal.springwebquery.validator;

/**
 * Generic contract for validators that enforce library-specific constraints on
 * an input object.
 *
 * @param <T> input type accepted by the validator
 */
public interface Validator<T> {

	/**
	 * Validates the supplied object and throws an exception when a constraint is violated.
	 *
	 * @param object object to validate
	 */
	void validate(T object);
}
