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

import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;

import java.lang.reflect.Field;
import java.text.MessageFormat;

/**
 * Validator that ensures a resolved terminal field is explicitly marked as sortable.
 */
public class SortableFieldValidator {

	/**
	 * Validates that the requested field is explicitly marked as sortable.
	 *
	 * @param field reflected terminal field being targeted by the sort selector
	 * @param fieldPath original selector path from the incoming request
	 *
	 * @throws QueryFieldValidationException if sorting is not allowed for the field
	 */
	public void validate(Field field, String fieldPath) {
		if (!field.isAnnotationPresent(Sortable.class)) {
			throw new QueryFieldValidationException(
					MessageFormat.format(
							"Sorting is not allowed on the field ''{0}''", fieldPath
					), fieldPath
			);
		}
	}
}
