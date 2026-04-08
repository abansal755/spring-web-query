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

package in.co.akshitbansal.springwebquery.exception;

/**
 * Exception thrown when the library is misconfigured by the developer.
 * <p>
 * This exception indicates an internal configuration error, such as referencing
 * a custom operator that has not been registered with the configured
 * RSQL specification resolver.
 * </p>
 *
 * <p>This exception is intended to be treated as a 5xx server error
 * as it highlights a development-time configuration issue.</p>
 */
public class QueryConfigurationException extends QueryException {

	/**
	 * Constructs a new query configuration exception with the specified detail message.
	 *
	 * @param message the detail message explaining the reason for the configuration error
	 */
	public QueryConfigurationException(String message) {
		super(message);
	}

	/**
	 * Constructs a new query configuration exception with the specified detail message and cause.
	 *
	 * @param message the detail message explaining the reason for the configuration error
	 * @param cause the underlying cause of the configuration error
	 */
	public QueryConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}
