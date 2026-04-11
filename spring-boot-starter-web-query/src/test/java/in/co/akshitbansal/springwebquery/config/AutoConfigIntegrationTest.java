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

import in.co.akshitbansal.springwebquery.config.resolver.ArgumentResolverRegistrationAutoConfig;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQuerySpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AutoConfigIntegrationTest {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ArgumentResolverRegistrationAutoConfig argumentResolverRegistrationAutoConfig;

	@Test
	void beansAreRegistered() {
		assertNotNull(context.getBean(WebQueryPageableArgumentResolver.class));
		assertNotNull(context.getBean(WebQuerySpecificationArgumentResolver.class));
		assertNotNull(context.getBean("defaultOperatorSet"));
		assertNotNull(context.getBean("customOperatorSet"));
	}

	@Test
	void customOperatorSetDefaultsToEmpty() {
		@SuppressWarnings("unchecked")
		Set<RSQLCustomOperator<?>> customOperators =
				(Set<RSQLCustomOperator<?>>) context.getBean("customOperatorSet");

		assertTrue(customOperators.isEmpty());
	}

	@Test
	void defaultOperatorSetIsRegistered() {
		@SuppressWarnings("unchecked")
		Set<RSQLDefaultOperator> defaultOperators =
				(Set<RSQLDefaultOperator>) context.getBean("defaultOperatorSet");

		assertTrue(defaultOperators.contains(RSQLDefaultOperator.EQUAL));
		assertTrue(defaultOperators.contains(RSQLDefaultOperator.IN));
	}

	@Test
	void argumentResolverConfigAddsPageableThenSpecificationResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		argumentResolverRegistrationAutoConfig.addArgumentResolvers(resolvers);

		assertEquals(2, resolvers.size());
		assertInstanceOf(WebQueryPageableArgumentResolver.class, resolvers.get(0));
		assertInstanceOf(WebQuerySpecificationArgumentResolver.class, resolvers.get(1));
	}

	@Test
	void specificationConfigRejectsNegativeMaxAstDepth() {
		assertThrows(
				QueryConfigurationException.class,
				() -> new SpringWebQueryPropertiesAutoConfig()
						.springWebQueryProperties("filter", true, false, -1, new QueryParamNameValidator())
		);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(excludeName = {
			"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
			"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
			"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
			"org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
	})
	static class TestApplication {
	}
}
