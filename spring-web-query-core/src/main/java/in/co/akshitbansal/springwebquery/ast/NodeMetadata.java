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

package in.co.akshitbansal.springwebquery.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Carries metadata while traversing the RSQL AST.
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class NodeMetadata {

	/**
	 * Current depth in the AST traversal.
	 */
	private final int depth;

	/**
	 * Creates metadata for the given depth.
	 *
	 * @param depth current depth in the AST
	 *
	 * @return metadata instance
	 */
	public static NodeMetadata of(int depth) {
		return new NodeMetadata(depth);
	}
}
