package in.co.akshitbansal.springwebquery.validator;

import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.model.Name;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class SortableFieldValidatorTest {

	private final SortableFieldValidator validator;
	private final Field firstNameField;
	private final Field lastNameField;

	SortableFieldValidatorTest() throws NoSuchFieldException {
		this.validator = new SortableFieldValidator();
		this.firstNameField = Name.class.getDeclaredField("firstName");
		this.lastNameField = Name.class.getDeclaredField("lastName");
	}

	@Test
	void testValidateWithNullField() {
		assertThrows(NullPointerException.class, () -> validator.validate(null, "hello"));
	}

	@Test
	void testValidateWithNullFieldPath() {
		assertThrows(NullPointerException.class, () -> validator.validate(firstNameField, null));
	}

	@Test
	void testValidateWithSortableField() {
		assertDoesNotThrow(() -> validator.validate(firstNameField, "firstName"));
	}

	@Test
	void testValidateWithNonSortableField() {
		QueryFieldValidationException ex = assertThrows(QueryFieldValidationException.class, () -> validator.validate(lastNameField, "lastName"));
		assertEquals("lastName", ex.getFieldPath());
		assertTrue(ex.getMessage().contains("Sorting is not allowed"));
	}
}
