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
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Effective configuration used by pageable argument resolvers after extracting
 * the relevant {@code @WebQuery} metadata from the controller method.
 *
 * <p>Pageable resolution needs only the shared contract state from
 * {@link AbstractArgumentResolverConfig}, so this subtype intentionally adds no
 * extra fields.</p>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PageableArgumentResolverConfig extends AbstractArgumentResolverConfig {

	public PageableArgumentResolverConfig(Class<?> entityClass, Class<?> dtoClass, List<FieldMapping> fieldMappings) {
		// null checks present in superclass constructor
		super(entityClass, dtoClass, fieldMappings);
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

		@SuppressWarnings("NullAway")
		public PageableArgumentResolverConfig build() {
			// this constructor calls the superclass constructor which has null checks present
			return new PageableArgumentResolverConfig(
					entityClass,
					dtoClass,
					fieldMappings
			);
		}
	}
}
