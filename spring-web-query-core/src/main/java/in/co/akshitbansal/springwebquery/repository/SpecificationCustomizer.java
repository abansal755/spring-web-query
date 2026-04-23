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

package in.co.akshitbansal.springwebquery.repository;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Callback that can amend or replace the generated filtering
 * {@link Specification} before execution.
 *
 * @param <E> entity type targeted by the specification
 */
@FunctionalInterface
public interface SpecificationCustomizer<E> {

	/**
	 * Applies additional specification logic to the generated filter.
	 *
	 * @param specification generated specification, which may already be
	 * unrestricted
	 *
	 * @return specification to execute, or {@code null} to remove filtering
	 */
	@Nullable
	Specification<E> apply(Specification<E> specification);
}
