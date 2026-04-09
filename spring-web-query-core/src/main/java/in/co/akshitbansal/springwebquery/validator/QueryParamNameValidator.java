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

package in.co.akshitbansal.springwebquery.validator;

import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;

import java.text.MessageFormat;

/**
 * Validates HTTP query parameter names used by web-query configuration.
 *
 * <p>The accepted format is restricted to a conservative identifier-style
 * subset so configured names remain unambiguous in URLs and compatible with
 * Spring request parameter lookup.</p>
 */
public class QueryParamNameValidator {

	/**
	 * Safe character set allowed for configured query parameter names.
	 */
	private static final String regex = "^[A-Za-z0-9._-]+$";

	/**
	 * Validates that the supplied parameter name matches the supported query
	 * parameter naming pattern.
	 *
	 * @param paramName query parameter name to validate
	 *
	 * @throws QueryConfigurationException if the parameter name contains
	 * unsupported characters or is otherwise invalid
	 */
	public void validate(String paramName) {
		if (!paramName.matches(regex)) {
			throw new QueryConfigurationException(MessageFormat.format(
					"Invalid query parameter name: {0}. Must match regex: {1}", paramName, regex
			));
		}
	}
}
