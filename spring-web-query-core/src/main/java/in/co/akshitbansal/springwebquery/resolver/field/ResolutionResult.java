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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.reflect.Field;

/**
 * Value object returned by {@link FieldResolver} implementations after path
 * resolution.
 *
 * <p>It carries the resolved entity-backed field path to use in downstream
 * query construction together with the reflected terminal field that callers
 * may validate for filtering or sorting rules.</p>
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class ResolutionResult {

	/**
	 * Resolved field path understood by downstream entity-backed query code.
	 */
	private final String fieldName;

	/**
	 * Terminal field reached while resolving the request selector path.
	 */
	private final Field terminalField;
}
