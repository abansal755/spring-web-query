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

package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;

import java.util.Set;

/**
 * Configurer interface for providing custom RSQL operators.
 * <p>
 * Applications can implement this interface and register it as a bean to
 * contribute custom operators to the RSQL query resolution process.
 * </p>
 */
@FunctionalInterface
public interface RSQLCustomOperatorsConfigurer {

	/**
	 * Returns a set of custom RSQL operators to be registered.
	 * <p>
	 * If multiple {@code RSQLCustomOperatorsConfigurer} beans are present in the
	 * Spring context, their results will be combined into a single set of
	 * operators available for query resolution.
	 * </p>
	 *
	 * @return a set of custom operators, or an empty set if none
	 */
	Set<? extends RSQLCustomOperator<?>> getCustomOperators();
}
