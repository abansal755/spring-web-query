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

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Strategy for resolving API-facing selector paths into concrete entity paths.
 *
 * <p>Implementations encapsulate the path-resolution rules for a particular
 * query contract, such as direct entity-field access or DTO-to-entity mapping.
 * They also expose a hook for validating the resolved terminal field before the
 * final path is returned to the caller.</p>
 */
@FunctionalInterface
public interface FieldResolver {

	/**
	 * Resolves the supplied selector path, validates the resolved terminal field,
	 * and returns the corresponding path understood by downstream query builders.
	 *
	 * @param path selector path from the incoming request
	 * @param terminalFieldValidator callback used to validate the resolved
	 * terminal field; when {@code null}, terminal
	 * field validation is skipped
	 *
	 * @return resolved path suitable for entity-backed query execution
	 */
	String resolvePathAndValidateTerminalField(String path, @Nullable Consumer<Field> terminalFieldValidator);

	/**
	 * Resolves the supplied selector path without performing terminal-field validation.
	 *
	 * @param path selector path from the incoming request
	 *
	 * @return resolved path suitable for entity-backed query execution
	 */
	default String resolvePath(String path) {
		return resolvePathAndValidateTerminalField(path, null);
	}
}
