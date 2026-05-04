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

import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.pathmapper.CachedDTOToEntityPathMapper;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapperFactory;
import in.co.akshitbansal.springwebquery.tupleconverter.PreferredConstructorDiscovererFactory;
import in.co.akshitbansal.springwebquery.tupleconverter.TupleConverterFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

class FactoryAutoConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					RSQLOperatorsAutoConfig.class, ValidatorAutoConfig.class, FactoryAutoConfig.class
			));

	@Test
	void testBeanRegistration() {
		runner.run(ctx -> {
			assertDoesNotThrow(() -> ctx.getBean(DTOToEntityPathMapperFactory.class));
			assertDoesNotThrow(() -> ctx.getBean(ValidationRSQLVisitorFactory.class));
			assertDoesNotThrow(() -> ctx.getBean(PreferredConstructorDiscovererFactory.class));
			assertDoesNotThrow(() -> ctx.getBean(TupleConverterFactory.class));
		});
	}

	@Test
	void testFieldResolutionCachingWithNoPropertyDefined() {
		runner.run(this::assertFieldResolutionIsCached);
	}

	@Test
	void testWithFieldResolutionCachingEnabled() {
		runner
				.withPropertyValues("spring-web-query.field-resolution.caching.enabled=true")
				.run(this::assertFieldResolutionIsCached);
	}

	@Test
	void testWithFieldResolutionCachingDisabled() {
		runner
				.withPropertyValues("spring-web-query.field-resolution.caching.enabled=false")
				.run(ctx -> {
					// Assert that the DTOToEntityPathMapperFactory bean is registered
					DTOToEntityPathMapperFactory factory = assertDoesNotThrow(() -> ctx.getBean(DTOToEntityPathMapperFactory.class));

					// Assert that the mapper returned by the factory is not cached
					DTOToEntityPathMapper pathMapper = factory.newMapper(Object.class, Object.class);
					assertSame(DTOToEntityPathMapper.class, pathMapper.getClass());
				});
	}

	private void assertFieldResolutionIsCached(AssertableApplicationContext ctx) {
		// Assert that the DTOToEntityPathMapperFactory bean is registered
		DTOToEntityPathMapperFactory factory = assertDoesNotThrow(() -> ctx.getBean(DTOToEntityPathMapperFactory.class));

		// Assert that the mapper returned by the factory is not cached
		DTOToEntityPathMapper pathMapper = factory.newMapper(Object.class, Object.class);
		assertSame(CachedDTOToEntityPathMapper.class, pathMapper.getClass());
	}
}
