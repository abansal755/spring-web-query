package in.co.akshitbansal.springwebquery.validator;

import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SortableFieldValidatorTest {

    @Test
    void validate_acceptsSortableField() throws Exception {
        SortableFieldValidator validator = new SortableFieldValidator();
        var field = SortableEntity.class.getDeclaredField("name");
        assertDoesNotThrow(() -> validator.validate(new SortableFieldValidator.SortableField(field, "name")));
    }

    @Test
    void validate_rejectsNonSortableField() throws Exception {
        SortableFieldValidator validator = new SortableFieldValidator();
        var field = NonSortableEntity.class.getDeclaredField("name");
        assertThrows(QueryFieldValidationException.class, () -> validator.validate(new SortableFieldValidator.SortableField(field, "name")));
    }

    private static class SortableEntity {
        @Sortable
        private String name;
    }

    private static class NonSortableEntity {
        private String name;
    }
}
