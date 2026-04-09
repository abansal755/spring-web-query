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

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;

import java.util.List;

/**
 * Factory for creating field resolvers used by validation visitors and
 * pageable/specification resolver flows.
 */
public class FieldResolverFactory {

	/**
	 * Creates a resolver that validates API-facing paths against a DTO contract
	 * and maps them to entity paths.
	 *
	 * @param entityClass backing entity type used for final path validation
	 * @param dtoClass DTO type exposed as the query contract
	 *
	 * @return DTO-aware field resolver
	 */
	public DTOAwareFieldResolver newDtoAwareFieldResolver(Class<?> entityClass, Class<?> dtoClass) {
		return new DTOAwareFieldResolver(entityClass, dtoClass);
	}

	/**
	 * Creates a resolver that validates selectors directly against the entity
	 * model while honoring explicit field aliases.
	 *
	 * @param entityClass backing entity type used for selector validation
	 * @param fieldMappings aliases declared for entity-aware resolution
	 *
	 * @return entity-aware field resolver
	 */
	public EntityAwareFieldResolver newEntityAwareFieldResolver(Class<?> entityClass, List<FieldMapping> fieldMappings) {
		return new EntityAwareFieldResolver(entityClass, fieldMappings);
	}
}
