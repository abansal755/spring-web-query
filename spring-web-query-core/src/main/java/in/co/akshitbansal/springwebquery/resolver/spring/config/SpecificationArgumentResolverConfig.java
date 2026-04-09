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
import lombok.NonNull;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

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
			Class<?> entityClass, // null check present in superclass constructor
			Class<?> dtoClass, // null check present in superclass constructor
			List<FieldMapping> fieldMappings, // null check present in superclass constructor
			@NonNull String filterParamName,
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

	public static Builder builder() {
		return new Builder();
	}

	@EqualsAndHashCode
	@ToString
	public static class Builder {

		@Nullable
		private Class<?> entityClass;

		@Nullable
		private Class<?> dtoClass;

		@Nullable
		private List<FieldMapping> fieldMappings;

		@Nullable
		private String filterParamName;

		@Nullable
		private Boolean andNodeAllowed;

		@Nullable
		private Boolean orNodeAllowed;

		@Nullable
		private Integer maxASTDepth;

		public Builder entityClass(Class<?> entityClass) {
			this.entityClass = entityClass;
			return this;
		}

		public Builder dtoClass(Class<?> dtoClass) {
			this.dtoClass = dtoClass;
			return this;
		}

		public Builder fieldMappings(List<FieldMapping> fieldMappings) {
			this.fieldMappings = fieldMappings;
			return this;
		}

		public Builder filterParamName(String filterParamName) {
			this.filterParamName = filterParamName;
			return this;
		}

		public Builder andNodeAllowed(boolean andNodeAllowed) {
			this.andNodeAllowed = andNodeAllowed;
			return this;
		}

		public Builder orNodeAllowed(boolean orNodeAllowed) {
			this.orNodeAllowed = orNodeAllowed;
			return this;
		}

		public Builder maxASTDepth(int maxASTDepth) {
			this.maxASTDepth = maxASTDepth;
			return this;
		}

		@SuppressWarnings("NullAway")
		public SpecificationArgumentResolverConfig build() {
			return new SpecificationArgumentResolverConfig(
					entityClass,
					dtoClass,
					fieldMappings,
					filterParamName,
					// null check for the rest of the fields to check if the builder's setter got called
					Objects.requireNonNull(andNodeAllowed, "andNodeAllowed must not be null"),
					Objects.requireNonNull(orNodeAllowed, "orNodeAllowed must not be null"),
					Objects.requireNonNull(maxASTDepth, "maxASTDepth must not be null")
			);
		}
	}
}
