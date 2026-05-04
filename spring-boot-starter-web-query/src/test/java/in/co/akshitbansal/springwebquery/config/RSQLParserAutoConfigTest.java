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

import cz.jirutka.rsql.parser.RSQLParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RSQLParserAutoConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					RSQLOperatorsAutoConfig.class, RSQLParserAutoConfig.class
			));

	@Test
	void testParserBeanRegistration() {
		runner.run(ctx -> {
			// Assert that the RSQLParser bean is registered
			RSQLParser parser = assertDoesNotThrow(() -> ctx.getBean(RSQLParser.class));

			// Asserting that default operator is getting parsed
			assertDoesNotThrow(() -> parser.parse("name==John"));
		});
	}
}
