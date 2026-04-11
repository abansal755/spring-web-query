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

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQuerySpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebQuerySpecificationArgumentResolverTest {

	private final WebQuerySpecificationArgumentResolver resolver = new WebQuerySpecificationArgumentResolver(
			"filter",
			true,
			false,
			1,
			parserFor(Set.of(new MockCustomOperator())),
			customPredicates(Set.of(new MockCustomOperator())),
			new QueryParamNameValidator(),
			new ValidationRSQLVisitorFactory(
					new FieldResolverFactory(),
					new FilterableFieldValidator(Map.of(MockCustomOperator.class, new MockCustomOperator()))
			),
			new FieldMappingsValidator()
	);

	@Test
	void supportsParameter_returnsTrueForEntityAwareMethod() throws Exception {
		Method method = TestController.class.getDeclaredMethod("entitySearch", Specification.class);
		assertTrue(resolver.supportsParameter(new MethodParameter(method, 0)));
	}

	@Test
	void supportsParameter_returnsTrueForDtoAwareMethod() throws Exception {
		Method method = TestController.class.getDeclaredMethod("dtoSearch", Specification.class);
		assertTrue(resolver.supportsParameter(new MethodParameter(method, 0)));
	}

	@Test
	void supportsParameter_returnsFalseWhenWebQueryMissing() throws Exception {
		Method method = TestController.class.getDeclaredMethod("missingWebQuery", Specification.class);
		assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
	}

	@Test
	void resolveArgument_returnsUnrestrictedWhenFilterMissing() throws Exception {
		Method method = TestController.class.getDeclaredMethod("entitySearch", Specification.class);
		Object spec = resolver.resolveArgument(new MethodParameter(method, 0), null, new ServletWebRequest(new MockHttpServletRequest()), null);
		assertNotNull(spec);
	}

	@Test
	void resolveArgument_acceptsAliasField() throws Exception {
		Method method = TestController.class.getDeclaredMethod("entitySearchWithMapping", Specification.class);
		resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("filter", "displayName==john"), null);
	}

	@Test
	void resolveArgument_rejectsOriginalMappedFieldWhenNotAllowed() throws Exception {
		Method method = TestController.class.getDeclaredMethod("entitySearchWithMapping", Specification.class);
		assertThrows(
				QueryValidationException.class, () -> resolver.resolveArgument(
						new MethodParameter(method, 0),
						null,
						requestWith("filter", "name==john"),
						null
				)
		);
	}

	@Test
	void resolveArgument_allowsCustomOperator() throws Exception {
		Method method = TestController.class.getDeclaredMethod("entitySearchWithCustom", Specification.class);
		resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("filter", "name=mock=value"), null);
	}

	@Test
	void resolveArgument_usesWebQueryFilterParamName() throws Exception {
		Method method = TestController.class.getDeclaredMethod("dtoSearchWithCustomParam", Specification.class);
		resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("q", "joinedAt==x"), null);
	}

	@Test
	void resolveArgument_rejectsMalformedFilter() throws Exception {
		Method method = TestController.class.getDeclaredMethod("entitySearch", Specification.class);
		assertThrows(
				QueryValidationException.class, () -> resolver.resolveArgument(
						new MethodParameter(method, 0),
						null,
						requestWith("filter", "name=="),
						null
				)
		);
	}

	@Test
	void resolveArgument_returnsUnrestrictedWhenFilterBlank() throws Exception {
		Method method = TestController.class.getDeclaredMethod("entitySearch", Specification.class);
		Object spec = resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("filter", "   "), null);
		assertNotNull(spec);
	}

	@Test
	void resolveArgument_rejectsOrWhenEndpointDisallowsIt() throws Exception {
		Method method = TestController.class.getDeclaredMethod("dtoSearchOrDenied", Specification.class);
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
		Method method = TestController.class.getDeclaredMethod("dtoSearchDepthZero", Specification.class);
		assertThrows(
				QueryValidationException.class, () -> resolver.resolveArgument(
						new MethodParameter(method, 0),
						null,
						requestWith("filter", "joinedAt==x;joinedAt==y"),
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

	private NativeWebRequest requestWith(String key, String value) {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setParameter(key, value);
		return new ServletWebRequest(req);
	}

	private static class MockCustomOperator implements RSQLCustomOperator<String> {

		@Override
		public ComparisonOperator getComparisonOperator() {
			return new ComparisonOperator("=mock=");
		}

		@Override
		public Class<String> getType() {
			return String.class;
		}

		@Override
		public Predicate toPredicate(RSQLCustomPredicateInput input) {
			return dummyPredicate();
		}
	}

	private static Predicate dummyPredicate() {
		return (Predicate) Proxy.newProxyInstance(
				Predicate.class.getClassLoader(),
				new Class[] {Predicate.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "toString" -> "dummyPredicate";
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default ->
							throw new UnsupportedOperationException("Predicate should not be evaluated in this test");
				}
		);
	}

	@SuppressWarnings("unused")
	private static class TestController {

		@WebQuery(entityClass = Entity.class)
		void entitySearch(Specification<Entity> spec) {
		}

		@WebQuery(entityClass = Entity.class, dtoClass = QueryDto.class)
		void dtoSearch(Specification<Entity> spec) {
		}

		@WebQuery(entityClass = Entity.class, fieldMappings = {
				@FieldMapping(name = "displayName", field = "name", allowOriginalFieldName = false)
		})
		void entitySearchWithMapping(Specification<Entity> spec) {
		}

		@WebQuery(entityClass = EntityWithCustom.class)
		void entitySearchWithCustom(Specification<EntityWithCustom> spec) {
		}

		@WebQuery(entityClass = Entity.class, dtoClass = QueryDto.class, filterParamName = "q")
		void dtoSearchWithCustomParam(Specification<Entity> spec) {
		}

		@WebQuery(
				entityClass = Entity.class,
				dtoClass = QueryDto.class,
				allowOrOperator = WebQuery.OperatorPolicy.DENY,
				allowAndOperator = WebQuery.OperatorPolicy.ALLOW
		)
		void dtoSearchOrDenied(Specification<Entity> spec) {
		}

		@WebQuery(entityClass = Entity.class, dtoClass = QueryDto.class, maxASTDepth = 0)
		void dtoSearchDepthZero(Specification<Entity> spec) {
		}

		@WebQuery(entityClass = Entity.class, dtoClass = InvalidMappingDto.class)
		void invalidMapping(Specification<Entity> spec) {
		}

		void missingWebQuery(Specification<Entity> spec) {
		}
	}

	private static class Entity {

		@RSQLFilterable({RSQLDefaultOperator.EQUAL})
		private String name;

		@SuppressWarnings("unused")
		private String createdAt;
	}

	private static class EntityWithCustom {

		@RSQLFilterable(value = {RSQLDefaultOperator.EQUAL}, customOperators = {MockCustomOperator.class})
		private String name;
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

	private static RSQLParser parserFor(Set<? extends RSQLCustomOperator<?>> customOperators) {
		Set<ComparisonOperator> operators = Set.of(RSQLDefaultOperator.values())
				.stream()
				.map(RSQLDefaultOperator::getOperator)
				.collect(Collectors.toSet());
		operators.addAll(customOperators.stream().map(RSQLCustomOperator::getComparisonOperator).collect(Collectors.toSet()));
		return new RSQLParser(operators);
	}

	private static List<RSQLCustomPredicate<?>> customPredicates(Set<? extends RSQLCustomOperator<?>> customOperators) {
		return customOperators.stream()
				.map(operator -> new RSQLCustomPredicate<>(
						operator.getComparisonOperator(),
						operator.getType(),
						operator::toPredicate
				))
				.collect(Collectors.toList());
	}
}
