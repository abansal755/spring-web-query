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

import in.co.akshitbansal.springwebquery.resolver.field.cache.CachedDTOAwareFieldResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.config.AbstractArgumentResolverConfig;
import in.co.akshitbansal.springwebquery.enums.ResolutionMode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Factory for creating field resolvers used by validation visitors and
 * pageable/specification resolver flows.
 */
@RequiredArgsConstructor
public class FieldResolverFactory {

	private final boolean useCachedFieldResolvers;

	public FieldResolverFactory() {
		this(false);
	}

	/**
	 * Creates the field resolver that matches the supplied effective query
	 * configuration.
	 *
	 * @param config effective argument-resolver configuration
	 *
	 * @return entity-aware or DTO-aware field resolver
	 */
	public FieldResolver newFieldResolver(@NonNull AbstractArgumentResolverConfig config) {
		if (config.getResolutionMode() == ResolutionMode.DTO_AWARE) {
			if (useCachedFieldResolvers)
				return new CachedDTOAwareFieldResolver(config.getEntityClass(), config.getDtoClass());
			return new DTOAwareFieldResolver(config.getEntityClass(), config.getDtoClass());
		}
		return new EntityAwareFieldResolver(config.getEntityClass(), config.getFieldMappings());
	}
}
