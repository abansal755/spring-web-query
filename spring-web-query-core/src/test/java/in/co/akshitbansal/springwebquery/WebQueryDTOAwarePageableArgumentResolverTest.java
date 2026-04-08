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
import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwarePageableArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebQueryDTOAwarePageableArgumentResolverTest {

	private final WebQueryDTOAwarePageableArgumentResolver resolver = new WebQueryDTOAwarePageableArgumentResolver(
			new PageableHandlerMethodArgumentResolver()
	);

	@Test
	void supportsParameter_returnsTrueForDtoAwarePageable() throws Exception {
		Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
		assertTrue(resolver.supportsParameter(new MethodParameter(method, 0)));
	}

	@Test
	void supportsParameter_returnsFalseForEntityAwarePageable() throws Exception {
		Method method = TestController.class.getDeclaredMethod("entityAware", Pageable.class);
		assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
	}

	@Test
	void supportsParameter_returnsFalseWhenWebQueryMissing() throws Exception {
		Method method = TestController.class.getDeclaredMethod("missingWebQuery", Pageable.class);
		assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
	}

	@Test
	void resolveArgument_mapsDtoSortToEntityPath() throws Exception {
		Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
		Pageable pageable = (Pageable) resolver.resolveArgument(new MethodParameter(method, 0), null, requestWithSort("joinedAt,desc"), null);
		assertEquals("createdAt", pageable.getSort().iterator().next().getProperty());
	}

	@Test
	void resolveArgument_rejectsUnknownDtoField() throws Exception {
		Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
		assertThrows(
				QueryValidationException.class, () -> resolver.resolveArgument(
						new MethodParameter(method, 0),
						null,
						requestWithSort("unknown,asc"),
						null
				)
		);
	}

	@Test
	void resolveArgument_rejectsWhenMappedEntityFieldMissing() throws Exception {
		Method method = TestController.class.getDeclaredMethod("invalidMapping", Pageable.class);
		assertThrows(
				QueryConfigurationException.class, () -> resolver.resolveArgument(
						new MethodParameter(method, 0),
						null,
						requestWithSort("joinedAt,asc"),
						null
				)
		);
	}

	@Test
	void resolveArgument_returnsUnsortedPageableWhenSortMissing() throws Exception {
		Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
		Pageable pageable = (Pageable) resolver.resolveArgument(
				new MethodParameter(method, 0),
				null,
				new ServletWebRequest(new MockHttpServletRequest()),
				null
		);

		assertTrue(pageable.getSort().isUnsorted());
	}

	private NativeWebRequest requestWithSort(String sort) {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setParameter("sort", sort);
		return new ServletWebRequest(req);
	}

	@SuppressWarnings("unused")
	private static class TestController {

		@WebQuery(entityClass = Entity.class, dtoClass = QueryDto.class)
		void search(Pageable pageable) {
		}

		@WebQuery(entityClass = Entity.class)
		void entityAware(Pageable pageable) {
		}

		@WebQuery(entityClass = Entity.class, dtoClass = InvalidMappingDto.class)
		void invalidMapping(Pageable pageable) {
		}

		void missingWebQuery(Pageable pageable) {
		}
	}

	private static class Entity {

		@SuppressWarnings("unused")
		private String createdAt;
	}

	private static class QueryDto {

		@Sortable
		@MapsTo("createdAt")
		private String joinedAt;
	}

	private static class InvalidMappingDto {

		@Sortable
		@MapsTo("missing")
		private String joinedAt;
	}
}
