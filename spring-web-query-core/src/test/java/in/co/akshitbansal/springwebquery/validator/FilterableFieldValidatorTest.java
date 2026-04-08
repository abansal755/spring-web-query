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

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterableEquality;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilterableFieldValidatorTest {

	@Test
	void validate_acceptsDefaultOperator() throws Exception {
		FilterableFieldValidator validator = new FilterableFieldValidator(Map.of());
		var field = DefaultOnlyFilterableEntity.class.getDeclaredField("name");
		assertDoesNotThrow(() -> validator.validate(new FilterableFieldValidator.FilterableField(field, RSQLDefaultOperator.EQUAL.getOperator(), "name")));
	}

	@Test
	void validate_acceptsCustomOperatorWhenRegistered() throws Exception {
		FilterableFieldValidator validator = new FilterableFieldValidator(Map.of(MockCustomOperator.class, new MockCustomOperator()));
		var field = FilterableEntity.class.getDeclaredField("name");
		assertDoesNotThrow(() -> validator.validate(new FilterableFieldValidator.FilterableField(field, new ComparisonOperator("=mock="), "name")));
	}

	@Test
	void validate_rejectsUnregisteredCustomOperator() throws Exception {
		FilterableFieldValidator validator = new FilterableFieldValidator(Map.of());
		var field = FilterableEntity.class.getDeclaredField("name");
		assertThrows(QueryConfigurationException.class, () -> validator.validate(new FilterableFieldValidator.FilterableField(field, new ComparisonOperator("=mock="), "name")));
	}

	@Test
	void validate_rejectsDisallowedOperator() throws Exception {
		FilterableFieldValidator validator = new FilterableFieldValidator(Map.of());
		var field = DefaultOnlyFilterableEntity.class.getDeclaredField("name");
		assertThrows(QueryForbiddenOperatorException.class, () -> validator.validate(new FilterableFieldValidator.FilterableField(field, RSQLDefaultOperator.NOT_EQUAL.getOperator(), "name")));
	}

	@Test
	void validate_rejectsNonFilterableField() throws Exception {
		FilterableFieldValidator validator = new FilterableFieldValidator(Map.of());
		var field = NonFilterableEntity.class.getDeclaredField("name");
		assertThrows(QueryFieldValidationException.class, () -> validator.validate(new FilterableFieldValidator.FilterableField(field, RSQLDefaultOperator.EQUAL.getOperator(), "name")));
	}

	@Test
	void validate_supportsComposedAnnotations() throws Exception {
		FilterableFieldValidator validator = new FilterableFieldValidator(Map.of());
		var field = ComposedFilterableEntity.class.getDeclaredField("name");
		assertDoesNotThrow(() -> validator.validate(new FilterableFieldValidator.FilterableField(field, RSQLDefaultOperator.EQUAL.getOperator(), "name")));
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

	private static class FilterableEntity {

		@RSQLFilterable(value = {RSQLDefaultOperator.EQUAL}, customOperators = {MockCustomOperator.class})
		private String name;
	}

	private static class DefaultOnlyFilterableEntity {

		@RSQLFilterable({RSQLDefaultOperator.EQUAL})
		private String name;
	}

	private static class NonFilterableEntity {

		private String name;
	}

	private static class ComposedFilterableEntity {

		@RSQLFilterableEquality
		private String name;
	}
}
