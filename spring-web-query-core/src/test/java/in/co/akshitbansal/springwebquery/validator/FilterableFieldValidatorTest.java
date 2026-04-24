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

import in.co.akshitbansal.springwebquery.customoperator.IsLongGreaterThanFiveOperator;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException;
import in.co.akshitbansal.springwebquery.model.User;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilterableFieldValidatorTest {

	private final FilterableFieldValidator validator;
	private final Field userIdField;
	private final Field userEmailField;

	FilterableFieldValidatorTest() throws NoSuchFieldException {
		this.validator = newValidator();
		this.userIdField = User.class.getDeclaredField("id");
		this.userEmailField = User.class.getDeclaredField("email");
	}

	@Test
	void testConstructionWithNullOperatorMap() {
		assertThrows(NullPointerException.class, () -> new FilterableFieldValidator(null));
	}

	@Test
	void testConstructionWithNonNullOperatorMap() {
		assertDoesNotThrow(this::newValidator);
	}

	@Test
	void testValidateWithNullField() {
		var operator = RSQLDefaultOperator.IN.getOperator();
		assertThrows(
				NullPointerException.class,
				() -> validator.validate(null, operator, "id")
		);
	}

	@Test
	void testValidateWithNullOperator() {
		assertThrows(
				NullPointerException.class,
				() -> validator.validate(userIdField, null, "id")
		);
	}

	@Test
	void testValidateWithNullFieldPath() {
		var operator = RSQLDefaultOperator.IN.getOperator();
		assertThrows(
				NullPointerException.class,
				() -> validator.validate(userIdField, operator, null)
		);
	}

	@Test
	void testValidateWithNotFilterableField() {
		var operator = RSQLDefaultOperator.IN.getOperator();
		QueryFieldValidationException ex = assertThrows(
				QueryFieldValidationException.class, () -> validator.validate(userEmailField, operator, "email")
		);
		assertEquals("email", ex.getFieldPath());
		assertTrue(ex.getMessage().contains("Filtering not allowed"));
	}

	@Test
	void testValidateWithNotAllowedOperator() {
		var operator = RSQLDefaultOperator.IN.getOperator();
		QueryForbiddenOperatorException ex = assertThrows(
				QueryForbiddenOperatorException.class, () -> validator.validate(userIdField, operator, "id")
		);
		assertEquals("id", ex.getFieldPath());
		assertEquals(operator, ex.getOperator());
		assertTrue(ex.getMessage().contains("not allowed on field"));
	}

	@Test
	void testValidateWithAllowedDefaultOperator() {
		var operator = RSQLDefaultOperator.EQUAL.getOperator();
		assertDoesNotThrow(() -> validator.validate(userIdField, operator, "id"));
	}

	@Test
	void testValidateWithAllowedCustomOperator() {
		var operator = new IsLongGreaterThanFiveOperator().getComparisonOperator();
		assertDoesNotThrow(() -> validator.validate(userIdField, operator, "id"));
	}

	private FilterableFieldValidator newValidator() {
		return new FilterableFieldValidator(Map.of(IsLongGreaterThanFiveOperator.class, new IsLongGreaterThanFiveOperator()));
	}

}
