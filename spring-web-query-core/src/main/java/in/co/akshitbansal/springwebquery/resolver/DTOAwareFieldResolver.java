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

package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * {@link FieldResolver} implementation that treats a DTO type as the public
 * query contract and maps DTO selector paths to entity selector paths.
 *
 * <p>Resolution proceeds in three steps:</p>
 * <ul>
 *     <li>Resolve the incoming path against the DTO class structure.</li>
 *     <li>Validate the terminal DTO field via the supplied callback.</li>
 *     <li>Translate the DTO path to an entity path using {@link MapsTo}, then
 *     verify that the resulting entity path exists.</li>
 * </ul>
 */
@RequiredArgsConstructor
public class DTOAwareFieldResolver implements FieldResolver {

	/**
	 * Entity type used to validate the translated path.
	 */
	private final Class<?> entityClass;

	/**
	 * DTO type used as the external selector contract.
	 */
	private final Class<?> dtoClass;

	/**
	 * Resolves a DTO selector path, validates its terminal DTO field, and maps
	 * the selector to the corresponding entity path.
	 *
	 * @param dtoPath selector path from the incoming request
	 * @param terminalFieldValidator callback used to validate the terminal DTO
	 * field; when {@code null}, terminal-field
	 * validation is skipped
	 *
	 * @return resolved entity path corresponding to the DTO selector
	 */
	@Override
	public String resolvePathAndValidateTerminalField(String dtoPath, @Nullable Consumer<Field> terminalFieldValidator) {
		// Resolve the field path in the DTO class
		List<Field> dtoFields;
		try {
			dtoFields = ReflectionUtil.resolveFieldPath(dtoClass, dtoPath);
		}
		catch (Exception ex) {
			throw new QueryFieldValidationException(
					MessageFormat.format(
							"Unknown field ''{0}''", dtoPath
					), dtoPath, ex
			);
		}

		// Validate the last field in the path using the provided terminal field validator
		if (terminalFieldValidator != null)
			terminalFieldValidator.accept(dtoFields.get(dtoFields.size() - 1));

		// Construct the corresponding entity field path using the @MapsTo annotation if present
		List<String> entityPathSegments = new ArrayList<>();
		for (Field dtoField: dtoFields) {
			MapsTo mapsToAnnotation = dtoField.getAnnotation(MapsTo.class);
			if (mapsToAnnotation == null) entityPathSegments.add(dtoField.getName());
			else {
				if (mapsToAnnotation.absolute()) entityPathSegments.clear();
				entityPathSegments.add(mapsToAnnotation.value());
			}
		}
		String entityPath = String.join(".", entityPathSegments);
		// Validate that the constructed entity field path is resolvable in the entity class
		try {
			ReflectionUtil.resolveField(entityClass, entityPath);
		}
		catch (Exception ex) {
			throw new QueryConfigurationException(
					MessageFormat.format(
							"Unable to resolve entity field path ''{0}'' mapped from DTO path ''{1}''", entityPath, dtoPath
					), ex
			);
		}

		return entityPath;
	}
}
