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

import io.github.perplexhub.rsql.RSQLJPAAutoConfiguration;
import io.github.perplexhub.rsql.RSQLJPASupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.support.ConfigurableConversionService;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RSQLJPAConverterRegistrationAutoConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(RSQLJPAAutoConfiguration.class));

	@BeforeEach
	void resetConverters() {
		// Clear static state before each test so they don't bleed into each other
		RSQLJPASupport.getConversionService().removeConvertible(String.class, Timestamp.class);
	}

	// TODO
	// Converter registration doesn't require RSQLJPAConverterRegistrationAutoConfig to be run first (or in fact be present)
	// So should the assertions on auto config order be present?

	@Test
	void testWithoutConverterRegistration() {
		runner.run(ctx -> {
			ConfigurableConversionService conversionService = RSQLJPASupport.getConversionService();
			assertThrows(ConverterNotFoundException.class, () -> conversionService.convert("2025-12-08T00:00:00Z", Timestamp.class));
		});
	}

	@Test
	void testWithConverterRegistration() {
		runner
				.withConfiguration(AutoConfigurations.of(RSQLJPAConverterRegistrationAutoConfig.class))
				.run(ctx -> {
					ConfigurableConversionService conversionService = RSQLJPASupport.getConversionService();
					assertDoesNotThrow(() -> conversionService.convert("2025-12-08T00:00:00Z", Timestamp.class));
				});
	}
}
