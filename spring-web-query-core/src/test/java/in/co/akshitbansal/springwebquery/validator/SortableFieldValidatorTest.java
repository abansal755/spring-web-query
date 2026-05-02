/*
 * Copyright 2026-present Akshit Bansal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.co.akshitbansal.springwebquery.validator;

import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.common.model.Name;
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
