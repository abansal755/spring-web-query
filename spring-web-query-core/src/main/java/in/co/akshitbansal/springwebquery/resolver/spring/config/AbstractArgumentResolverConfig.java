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
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Shared base configuration for effective {@link WebQuery} metadata resolved
 * from a supported controller method.
 *
 * <p>This type captures the contract information common to pageable and
 * specification resolver flows, including the backing entity type, the optional
 * DTO-facing query contract, and any declared entity-aware field aliases.</p>
 */
@Getter
@SuperBuilder
@EqualsAndHashCode
@ToString
public abstract class AbstractArgumentResolverConfig {

	/**
	 * Entity type against which resolved query paths are ultimately validated
	 * and executed.
	 */
	private final Class<?> entityClass;

	/**
	 * Optional DTO type used as the API-facing selector contract before paths
	 * are translated to entity properties.
	 */
	private final Class<?> dtoClass;

	/**
	 * Explicit field aliases declared on {@link WebQuery}, used by entity-aware
	 * resolver flows when request selectors are validated directly against the
	 * entity model.
	 */
	private final List<FieldMapping> fieldMappings;
}
