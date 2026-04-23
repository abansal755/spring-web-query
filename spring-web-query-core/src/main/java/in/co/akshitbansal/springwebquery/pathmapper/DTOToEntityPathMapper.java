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

package in.co.akshitbansal.springwebquery.pathmapper;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.resolver.ReflectiveFieldResolver;
import lombok.*;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates DTO-visible selector paths into entity paths used for query
 * construction.
 *
 * <p>This mapper exists so that filtering and sorting can be expressed against
 * a DTO contract while the underlying query still targets entity attributes.
 * A caller provides a selector path in DTO terms, and the mapper returns the
 * corresponding entity path together with the terminal DTO field that was
 * reached while resolving the selector.</p>
 *
 * <p>Mapping happens in three stages:</p>
 * <ul>
 *   <li>the incoming DTO path is resolved reflectively against the DTO class</li>
 *   <li>each resolved DTO field contributes one entity path segment, either
 *       from its own field name or from {@link MapsTo}</li>
 *   <li>the assembled entity path is resolved reflectively against the entity
 *       class to verify that the DTO mapping configuration is valid</li>
 * </ul>
 *
 * <p>If a DTO field does not declare {@link MapsTo}, its own field name is
 * reused as the entity-side segment. If {@link MapsTo} is present, its
 * {@link MapsTo#value()} is appended instead. When
 * {@link MapsTo#absolute()} is {@code true}, any previously accumulated parent
 * segments are discarded before the mapped segment is added, allowing a nested
 * DTO field to point to a path rooted elsewhere in the entity graph.</p>
 *
 * <p>The mapper distinguishes between caller mistakes and developer
 * misconfiguration:</p>
 * <ul>
 *   <li>if the DTO path itself cannot be resolved, the mapper throws
 *       {@link QueryFieldValidationException}</li>
 *   <li>if the DTO path resolves successfully but the derived entity path does
 *       not, the mapper throws {@link QueryConfigurationException} because the
 *       DTO-to-entity mapping metadata is inconsistent with the entity model</li>
 * </ul>
 */
public class DTOToEntityPathMapper {

	/**
	 * Entity type that receives translated selector paths.
	 */
	protected final Class<?> entityClass;

	/**
	 * DTO type exposed to callers for filtering and sorting.
	 */
	protected final Class<?> dtoClass;

	/**
	 * Resolver used to validate the translated entity path.
	 */
	private final ReflectiveFieldResolver entityFieldResolver;

	/**
	 * Resolver used to walk the incoming DTO path.
	 */
	private final ReflectiveFieldResolver dtoFieldResolver;

	/**
	 * Creates a mapper for one entity/DTO pair.
	 *
	 * <p>The mapper retains reflective resolvers for both sides so that each
	 * mapping request can validate the DTO selector as well as the derived entity
	 * path.</p>
	 *
	 * @param entityClass entity type used for query construction
	 * @param dtoClass DTO type used by callers in selectors
	 */
	DTOToEntityPathMapper(@NonNull Class<?> entityClass, @NonNull Class<?> dtoClass) {
		this.entityClass = entityClass;
		this.dtoClass = dtoClass;
		this.entityFieldResolver = ReflectiveFieldResolver.of(entityClass);
		this.dtoFieldResolver = ReflectiveFieldResolver.of(dtoClass);
	}

	/**
	 * Maps a DTO-visible selector path to the corresponding entity path.
	 *
	 * <p>The incoming path is first resolved against the DTO class using
	 * {@link ReflectiveFieldResolver}. The resolved DTO fields are then processed
	 * from left to right to assemble the entity path:</p>
	 * <ul>
	 *   <li>without {@link MapsTo}, the DTO field name is reused</li>
	 *   <li>with {@link MapsTo}, the annotation value is used instead</li>
	 *   <li>with {@link MapsTo#absolute()} set to {@code true}, any previously
	 *       collected parent segments are cleared before the current mapped
	 *       segment is appended</li>
	 * </ul>
	 *
	 * <p>After the entity path string has been assembled, it is resolved against
	 * the entity class to ensure the mapping metadata actually points to a valid
	 * entity-side path.</p>
	 *
	 * <p>The returned {@link MappingResult} contains both the final entity path
	 * and the terminal DTO field. The terminal DTO field is preserved because
	 * later validation steps, such as checking filterability or sortability,
	 * operate on the DTO contract rather than on the entity field.</p>
	 *
	 * @param dtoPath selector path expressed against the DTO contract
	 *
	 * @return mapped entity path together with the terminal DTO field
	 *
	 * @throws QueryFieldValidationException if the DTO path cannot be resolved
	 * and is therefore invalid from the caller's perspective
	 * @throws QueryConfigurationException if the DTO path resolves but the
	 * derived entity path is invalid for the configured entity type
	 */
	public MappingResult map(String dtoPath) {
		// Resolve the field path in the DTO class
		List<Field> dtoFields;
		try {
			dtoFields = dtoFieldResolver.resolveFieldPath(dtoPath);
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
			entityFieldResolver.resolveFieldPath(entityPath);
		}
		catch (Exception ex) {
			throw new QueryConfigurationException(
					MessageFormat.format(
							"Unable to resolve entity field path ''{0}'' mapped from DTO path ''{1}''", entityPath, dtoPath
					), ex
			);
		}

		return MappingResult.of(entityPath, dtoFields.get(dtoFields.size() - 1));
	}

	/**
	 * Immutable outcome of mapping one DTO selector to an entity path.
	 *
	 * <p>This object keeps both the resolved entity path and the terminal DTO
	 * field because downstream query construction needs the entity path, while
	 * downstream validation still inspects annotations declared on the DTO
	 * contract.</p>
	 */
	@RequiredArgsConstructor(staticName = "of")
	@Getter
	@EqualsAndHashCode
	@ToString
	public static class MappingResult {

		/**
		 * Resolved entity path corresponding to the requested DTO selector.
		 *
		 * <p>This path is suitable for entity-side query construction.</p>
		 */
		@NonNull
		private final String path;

		/**
		 * Terminal DTO field reached while resolving the original selector.
		 *
		 * <p>This is the DTO field against which field-level annotations such as
		 * filterability and sortability are later evaluated.</p>
		 */
		@NonNull
		private final Field terminalDTOField;
	}
}
