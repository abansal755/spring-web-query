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

import lombok.Getter;

/**
 * Indicates that query validation failed for a specific field path.
 */
@Getter
public class QueryFieldValidationException extends QueryValidationException {

	/**
	 * Query field path for which validation failed.
	 */
	private final String fieldPath;

	/**
	 * Creates a new field validation exception.
	 *
	 * @param message validation error details
	 * @param fieldPath query field path associated with the failure
	 */
	public QueryFieldValidationException(String message, String fieldPath) {
		super(message);
		this.fieldPath = fieldPath;
	}

	/**
	 * Creates a new field validation exception with an underlying cause.
	 *
	 * @param message validation error details
	 * @param fieldPath query field path associated with the failure
	 * @param cause root cause of the validation failure
	 */
	public QueryFieldValidationException(String message, String fieldPath, Throwable cause) {
		super(message, cause);
		this.fieldPath = fieldPath;
	}
}
