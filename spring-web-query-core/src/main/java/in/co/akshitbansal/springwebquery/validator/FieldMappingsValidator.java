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

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;

import java.text.MessageFormat;
import java.util.*;

/**
 * Validator for {@link FieldMapping} declarations supplied via {@link WebQuery}.
 *
 * <p>This validator ensures that aliases are unique and that no two aliases map
 * to the same underlying entity field.</p>
 */
public class FieldMappingsValidator {

	/**
	 * Validates {@link FieldMapping} definitions declared in {@link WebQuery}.
	 * <p>
	 * Validation rules:
	 * <ul>
	 *     <li>Alias names must be unique ({@link FieldMapping#name()}).</li>
	 *     <li>Target entity fields must be unique ({@link FieldMapping#field()}).</li>
	 * </ul>
	 *
	 * @param fieldMappings field mappings to validate
	 *
	 * @throws QueryConfigurationException if duplicate aliases or duplicate target fields are found
	 */
	public void validate(List<FieldMapping> fieldMappings) {
		Set<String> nameSet = new HashSet<>();
		for (FieldMapping mapping: fieldMappings) {
			if (!nameSet.add(mapping.name())) throw new QueryConfigurationException(MessageFormat.format(
					"Duplicate field mapping present for alias ''{0}''. Only one mapping is allowed per alias.",
					mapping.name()
			));
		}

		Map<String, FieldMapping> fieldMap = new HashMap<>();
		for (FieldMapping mapping: fieldMappings) {
			fieldMap.compute(
					mapping.field(), (fieldName, existing) -> {
						if (existing != null) throw new QueryConfigurationException(MessageFormat.format(
								"Aliases ''{0}'' and ''{1}'' are mapped to same field. Only one mapping is allowed per field.",
								existing.name(), mapping.name()
						));
						return mapping;
					}
			);
		}
	}
}
