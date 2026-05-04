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

import in.co.akshitbansal.springwebquery.config.customoperator.IsEqualsOperator;
import in.co.akshitbansal.springwebquery.config.customoperator.IsLongGreaterThanFiveOperator;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RSQLOperatorsAutoConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RSQLOperatorsAutoConfig.class));

	@Test
	void testDefaultOperatorRegistration() {
		runner
				.run(ctx -> {
					// Assert that the default operator set bean is registered
					// noinspection unchecked
					Set<RSQLDefaultOperator> defaultOperatorSet = assertDoesNotThrow(() -> ctx.getBean("defaultOperatorSet", Set.class));

					// Assert that the default operator set is populated with all the default operators
					int numDefaultOperators = RSQLDefaultOperator.values().length;
					assertEquals(numDefaultOperators, defaultOperatorSet.size());
				});
	}

	@Test
	void testCustomOperatorRegistration() {
		runner
				.withBean(IsLongGreaterThanFiveOperator.class)
				.run(ctx -> {
					// Assert that the custom operator bean is registered
					IsLongGreaterThanFiveOperator customOperator = assertDoesNotThrow(() -> ctx.getBean(IsLongGreaterThanFiveOperator.class));


					// Assert that the custom operator set bean is registered
					// noinspection unchecked
					Set<? extends RSQLCustomOperator<?>> customOperatorSet = assertDoesNotThrow(() -> ctx.getBean("customOperatorSet", Set.class));

					// Assert that the custom operator set is populated with the custom operator bean
					assertTrue(customOperatorSet.contains(customOperator));

					// Assert that the custom operator map bean is registered
					// noinspection unchecked
					Map<Class<?>, RSQLCustomOperator<?>> customOperatorMap = assertDoesNotThrow(() -> ctx.getBean("customOperatorMap", Map.class));

					// Assert that the custom operator map is populated with the custom operator bean
					assertTrue(customOperatorMap.containsKey(IsLongGreaterThanFiveOperator.class));
				});
	}

	@Test
	void testCustomOperatorRegistrationWithConflicts() {
		runner
				.withBean(IsEqualsOperator.class)
				.run(ctx -> {
					// Asserting that the application context failed to start
					Throwable th = ctx.getStartupFailure();
					assertNotNull(th);

					// Asserting on the root cause
					Throwable rootCause = getRootCause(th);
					assertInstanceOf(QueryConfigurationException.class, rootCause);
					assertTrue(rootCause.getMessage().contains("Duplicate operator"));
				});
	}

	private static Throwable getRootCause(Throwable th) {
		while (th.getCause() != null)
			th = th.getCause();
		return th;
	}
}
