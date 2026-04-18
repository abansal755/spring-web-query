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

package in.co.akshitbansal.springwebquery.resolver.field.cache;

import lombok.*;

/**
 * Composite key used for caching DTO-aware field-resolution results.
 *
 * <p>The cache is scoped by the entity type, DTO type, and incoming DTO path so
 * the same selector string can be resolved independently for different query
 * contracts.</p>
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class CacheKey {

	/**
	 * Entity type that the DTO selector is ultimately mapped against.
	 */
	@NonNull
	private final Class<?> entityClass;

	/**
	 * DTO type used as the external selector contract.
	 */
	@NonNull
	private final Class<?> dtoClass;

	/**
	 * Incoming selector path expressed in DTO terms.
	 */
	@NonNull
	private final String dtoPath;
}
