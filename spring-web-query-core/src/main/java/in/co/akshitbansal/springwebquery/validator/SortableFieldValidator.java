package in.co.akshitbansal.springwebquery.validator;

import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import lombok.*;

import java.text.MessageFormat;

/**
 * Validator that ensures a resolved terminal field is explicitly marked as sortable.
 */
public class SortableFieldValidator implements Validator<SortableFieldValidator.Field> {

    /**
     * Validates that the requested field is explicitly marked as sortable.
     *
     * @param field field being targeted by sort selector
     * @param fieldPath original selector path from the request
     * @throws QueryFieldValidationException if sorting is not allowed for the field
     */
    @Override
    public void validate(@NonNull SortableFieldValidator.Field field) {
        java.lang.reflect.Field reflectedField = field.getField();
        String fieldPath = field.getFieldPath();
        if(!reflectedField.isAnnotationPresent(Sortable.class)) {
            throw new QueryFieldValidationException(MessageFormat.format(
                    "Sorting is not allowed on the field ''{0}''", fieldPath
            ), fieldPath);
        }
    }

    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class Field {

        /**
         * Reflected terminal field being validated.
         */
        @NonNull
        private final java.lang.reflect.Field field;

        /**
         * Original selector path from the incoming request.
         */
        @NonNull
        private final String fieldPath;
    }
}
