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

package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwareSpecificationArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebQueryDTOAwareSpecificationArgumentResolverTest {

	private final WebQueryDTOAwareSpecificationArgumentResolver resolver = new WebQueryDTOAwareSpecificationArgumentResolver(
			"filter",
			true,
			false,
			1,
			Set.of(RSQLDefaultOperator.values()),
			Set.of()
	);

	@Test
	void supportsParameter_returnsTrueForDtoAwareMethod() throws Exception {
		Method method = TestController.class.getDeclaredMethod("search", Specification.class);
		assertTrue(resolver.supportsParameter(new MethodParameter(method, 0)));
	}

	@Test
	void supportsParameter_returnsFalseForEntityOnlyMethod() throws Exception {
		Method method = TestController.class.getDeclaredMethod("entityOnly", Specification.class);
		assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
	}

	@Test
	void supportsParameter_returnsFalseWhenWebQueryMissing() throws Exception {
		Method method = TestController.class.getDeclaredMethod("missingWebQuery", Specification.class);
		assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
	}

	@Test
	void resolveArgument_returnsUnrestrictedWhenFilterMissing() throws Exception {
		Method method = TestController.class.getDeclaredMethod("search", Specification.class);
		Object spec = resolver.resolveArgument(new MethodParameter(method, 0), null, new ServletWebRequest(new MockHttpServletRequest()), null);
		assertNotNull(spec);
	}

	@Test
	void resolveArgument_acceptsMappedDtoField() throws Exception {
		Method method = TestController.class.getDeclaredMethod("search", Specification.class);
		resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("filter", "joinedAt==x"), null);
	}

	@Test
	void resolveArgument_rejectsUnknownDtoField() throws Exception {
		Method method = TestController.class.getDeclaredMethod("search", Specification.class);
		assertThrows(
				QueryValidationException.class, () -> resolver.resolveArgument(
						new MethodParameter(method, 0),
						null,
						requestWith("filter", "unknown==x"),
						null
				)
		);
	}

	@Test
	void resolveArgument_rejectsWhenMappedEntityFieldMissing() throws Exception {
		Method method = TestController.class.getDeclaredMethod("invalidMapping", Specification.class);
		assertThrows(
				QueryConfigurationException.class, () -> resolver.resolveArgument(
						new MethodParameter(method, 0),
						null,
						requestWith("filter", "joinedAt==x"),
						null
				)
		);
	}

	@Test
	void resolveArgument_usesWebQueryFilterParamName() throws Exception {
		Method method = TestController.class.getDeclaredMethod("searchWithCustomParam", Specification.class);
		resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("q", "joinedAt==x"), null);
	}

	@Test
	void resolveArgument_returnsUnrestrictedWhenFilterBlank() throws Exception {
		Method method = TestController.class.getDeclaredMethod("search", Specification.class);
		Object spec = resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("filter", " "), null);
		assertNotNull(spec);
	}

	@Test
	void resolveArgument_rejectsOrWhenEndpointDisallowsIt() throws Exception {
		Method method = TestController.class.getDeclaredMethod("searchOrDenied", Specification.class);
		assertThrows(
				QueryValidationException.class, () -> resolver.resolveArgument(
						new MethodParameter(method, 0),
						null,
						requestWith("filter", "joinedAt==x,joinedAt==y"),
						null
				)
		);
	}

	@Test
	void resolveArgument_rejectsWhenEndpointAstDepthExceeded() throws Exception {
		Method method = TestController.class.getDeclaredMethod("searchDepthZero", Specification.class);
		assertThrows(
				QueryValidationException.class, () -> resolver.resolveArgument(
						new MethodParameter(method, 0),
						null,
						requestWith("filter", "joinedAt==x;joinedAt==y"),
						null
				)
		);
	}

	private NativeWebRequest requestWith(String key, String value) {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setParameter(key, value);
		return new ServletWebRequest(req);
	}

	@SuppressWarnings("unused")
	private static class TestController {

		@WebQuery(entityClass = Entity.class, dtoClass = QueryDto.class)
		void search(Specification<Entity> spec) {
		}

		@WebQuery(entityClass = Entity.class)
		void entityOnly(Specification<Entity> spec) {
		}

		@WebQuery(entityClass = Entity.class, dtoClass = QueryDto.class, filterParamName = "q")
		void searchWithCustomParam(Specification<Entity> spec) {
		}

		@WebQuery(entityClass = Entity.class, dtoClass = InvalidMappingDto.class)
		void invalidMapping(Specification<Entity> spec) {
		}

		@WebQuery(
				entityClass = Entity.class,
				dtoClass = QueryDto.class,
				allowOrOperator = WebQuery.OperatorPolicy.DENY,
				allowAndOperator = WebQuery.OperatorPolicy.ALLOW
		)
		void searchOrDenied(Specification<Entity> spec) {
		}

		@WebQuery(entityClass = Entity.class, dtoClass = QueryDto.class, maxASTDepth = 0)
		void searchDepthZero(Specification<Entity> spec) {
		}

		void missingWebQuery(Specification<Entity> spec) {
		}
	}

	private static class Entity {

		@SuppressWarnings("unused")
		private String createdAt;
	}

	private static class QueryDto {

		@RSQLFilterable({RSQLDefaultOperator.EQUAL})
		@MapsTo("createdAt")
		private String joinedAt;
	}

	private static class InvalidMappingDto {

		@RSQLFilterable({RSQLDefaultOperator.EQUAL})
		@MapsTo("missing")
		private String joinedAt;
	}
}
