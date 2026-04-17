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

import lombok.NonNull;

/**
 * Strategy for resolving API-facing selector paths into concrete entity paths.
 *
 * <p>Implementations encapsulate the path-resolution rules for a particular
 * query contract, such as direct entity-field access or DTO-to-entity mapping.
 * Resolution returns both the resolved entity path and the reflected terminal
 * field so callers can apply validation policies after path resolution.</p>
 */
@FunctionalInterface
public interface FieldResolver {

	/**
	 * Resolves the supplied selector path.
	 *
	 * @param path selector path from the incoming request
	 *
	 * @return resolution result containing the entity-backed path and terminal field
	 */
	ResolutionResult resolvePath(@NonNull String path);
}
