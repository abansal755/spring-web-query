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

package in.co.akshitbansal.springwebquery.resolver.spring.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode( callSuper = true)
@ToString(callSuper = true)
public class SpecificationArgumentResolverConfig extends AbstractArgumentResolverConfig {

	/**
	 * Request parameter name used to read the raw RSQL filter expression
	 * after applying the global default when the annotation value is blank.
	 */
	private final String filterParamName;

	/**
	 * Whether AND nodes are allowed in the effective query configuration.
	 */
	private final boolean andNodeAllowed;

	/**
	 * Whether OR nodes are allowed in the effective query configuration.
	 */
	private final boolean orNodeAllowed;

	/**
	 * Maximum AST depth allowed in the effective query configuration.
	 */
	private final int maxASTDepth;
}
