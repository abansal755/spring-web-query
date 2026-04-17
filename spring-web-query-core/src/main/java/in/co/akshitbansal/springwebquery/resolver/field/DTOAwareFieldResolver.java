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

package in.co.akshitbansal.springwebquery.resolver.field;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.util.ReflectionUtil;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link FieldResolver} implementation that treats a DTO type as the public
 * query contract and maps DTO selector paths to entity selector paths.
 *
 * <p>Resolution proceeds in three steps:</p>
 * <ul>
 *     <li>Resolve the incoming path against the DTO class structure.</li>
 *     <li>Translate the DTO path to an entity path using {@link MapsTo}, then
 *     verify that the resulting entity path exists.</li>
 *     <li>Return the mapped entity path together with the terminal DTO field so
 *     callers can apply validation separately.</li>
 * </ul>
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class DTOAwareFieldResolver implements FieldResolver {

	/**
	 * Entity type used to validate the translated path.
	 */
	@NonNull
	protected final Class<?> entityClass;

	/**
	 * DTO type used as the external selector contract.
	 */
	@NonNull
	protected final Class<?> dtoClass;

	/**
	 * Resolves a DTO selector path and maps the selector to the corresponding
	 * entity path.
	 *
	 * @param dtoPath selector path from the incoming request
	 *
	 * @return resolution result containing the mapped entity path and terminal DTO field
	 */
	@Override
	public ResolutionResult resolvePath(@NonNull String dtoPath) {
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

		return new ResolutionResult(entityPath, dtoFields.get(dtoFields.size() - 1));
	}
}
