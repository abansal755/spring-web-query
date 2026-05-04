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

package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ValidatorAutoConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					RSQLOperatorsAutoConfig.class, ValidatorAutoConfig.class
			));

	@Test
	void testBeanRegistration() {
		runner.run(ctx -> {
			assertDoesNotThrow(() -> ctx.getBean(SortableFieldValidator.class));
			assertDoesNotThrow(() -> ctx.getBean(FilterableFieldValidator.class));
		});
	}
}
