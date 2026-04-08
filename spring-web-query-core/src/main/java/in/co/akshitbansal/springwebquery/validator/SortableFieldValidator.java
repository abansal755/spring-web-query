package in.co.akshitbansal.springwebquery.validator;

import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.reflect.Field;
import java.text.MessageFormat;

/**
 * Validator that ensures a resolved terminal field is explicitly marked as sortable.
 */
public class SortableFieldValidator implements Validator<SortableFieldValidator.SortableField> {

	/**
	 * Validates that the requested field is explicitly marked as sortable.
	 *
	 * @param sortableField field being targeted by sort selector
	 *
	 * @throws QueryFieldValidationException if sorting is not allowed for the field
	 */
	@Override
	public void validate(SortableField sortableField) {
		Field field = sortableField.getField();
		String fieldPath = sortableField.getFieldPath();
		if (!field.isAnnotationPresent(Sortable.class)) {
			throw new QueryFieldValidationException(
					MessageFormat.format(
							"Sorting is not allowed on the field ''{0}''", fieldPath
					), fieldPath
			);
		}
	}

	@RequiredArgsConstructor
	@Getter
	@EqualsAndHashCode
	@ToString
	public static class SortableField {

		/**
		 * Reflected terminal field being validated.
		 */
		private final Field field;

		/**
		 * Original selector path from the incoming request.
		 */
		private final String fieldPath;
	}
}
