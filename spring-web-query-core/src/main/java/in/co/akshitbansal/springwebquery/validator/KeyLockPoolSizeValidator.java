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

/**
 * Validator for the striped lock-pool size used by DTO-aware field-resolution
 * caching.
 *
 * <p>The cache indexes locks by masking hash codes, so the pool size must be a
 * positive power of two.</p>
 */
public class KeyLockPoolSizeValidator {

	/**
	 * Validates that the configured lock-pool size is positive and a power of
	 * two.
	 *
	 * @param size configured lock-pool size
	 *
	 * @throws IllegalArgumentException when the size is non-positive or not a
	 * power of two
	 */
	public void validate(int size) {
		if(size <= 0)
			throw new IllegalArgumentException("Key lock pool size must be a positive integer");
		if((size & (size - 1)) != 0)
			throw new IllegalArgumentException("Key lock pool size must be a power of 2");
	}
}
