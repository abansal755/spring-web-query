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

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Effective configuration used by specification argument resolvers after
 * combining method-level {@code @WebQuery} settings with global defaults.
 *
 * <p>In addition to the shared selector-contract state from
 * {@link AbstractArgumentResolverConfig}, this subtype carries the effective
 * filter parameter name and the AST/operator policy used during RSQL
 * validation.</p>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
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

	public SpecificationArgumentResolverConfig(
			Class<?> entityClass,
			Class<?> dtoClass,
			List<FieldMapping> fieldMappings,
			String filterParamName,
			boolean andNodeAllowed,
			boolean orNodeAllowed,
			int maxASTDepth
	) {
		super(entityClass, dtoClass, fieldMappings);
		this.filterParamName = filterParamName;
		this.andNodeAllowed = andNodeAllowed;
		this.orNodeAllowed = orNodeAllowed;
		this.maxASTDepth = maxASTDepth;
	}
}
