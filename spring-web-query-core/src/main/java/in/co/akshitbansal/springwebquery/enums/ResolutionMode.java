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

package in.co.akshitbansal.springwebquery.enums;

/**
 * Selects whether request selectors are resolved against DTO metadata first
 * or directly against entity metadata.
 */
public enum ResolutionMode {

	/**
	 * Resolve request selectors through the DTO contract and translate them to
	 * entity paths.
	 */
	DTO_AWARE,

	/**
	 * Resolve request selectors directly against the entity model.
	 */
	ENTITY_AWARE
}
