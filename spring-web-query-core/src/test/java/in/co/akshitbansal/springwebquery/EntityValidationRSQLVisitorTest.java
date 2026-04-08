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
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.ast.EntityValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.ast.NodeMetadata;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EntityValidationRSQLVisitorTest {

	private final Map<Class<?>, RSQLCustomOperator<?>> customOperators = Map.of(MockCustomOperator.class, new MockCustomOperator());

	@Test
	void allows_filterableFieldWithAllowedOperator() {
		EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
				TestEntity.class,
				List.of(),
				customOperators,
				true,
				false,
				1
		);

		new RSQLParser().parse("name==john").accept(visitor, NodeMetadata.of(0));
	}

	@Test
	void rejects_unknownField() {
		EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
				TestEntity.class,
				List.of(),
				customOperators,
				true,
				false,
				1
		);

		assertThrows(
				QueryValidationException.class, () ->
						new RSQLParser().parse("missing==x").accept(visitor, NodeMetadata.of(0))
		);
	}

	@Test
	void rejects_disallowedOperator() {
		EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
				TestEntity.class,
				List.of(),
				customOperators,
				true,
				false,
				1
		);

		assertThrows(
				QueryValidationException.class, () ->
						new RSQLParser().parse("name!=x").accept(visitor, NodeMetadata.of(0))
		);
	}

	@Test
	void allows_aliasFieldMapping() {
		EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
				TestEntity.class,
				List.of(mapping("displayName", "name", false)),
				customOperators,
				true,
				false,
				1
		);

		new RSQLParser().parse("displayName==john").accept(visitor, NodeMetadata.of(0));
	}

	@Test
	void rejects_originalMappedFieldWhenNotAllowed() {
		EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
				TestEntity.class,
				List.of(mapping("displayName", "name", false)),
				customOperators,
				true,
				false,
				1
		);

		assertThrows(
				QueryValidationException.class, () ->
						new RSQLParser().parse("name==john").accept(visitor, NodeMetadata.of(0))
		);
	}

	@Test
	void allows_customOperatorWhenWhitelisted() {
		Set<ComparisonOperator> ops = Set.of(RSQLDefaultOperator.EQUAL.getOperator(), new MockCustomOperator().getComparisonOperator());
		EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
				TestEntityWithCustom.class,
				List.of(),
				customOperators,
				true,
				false,
				1
		);

		new RSQLParser(ops).parse("name=mock=value").accept(visitor, NodeMetadata.of(0));
	}

	@Test
	void rejects_unregisteredCustomOperator() {
		Set<ComparisonOperator> ops = Set.of(RSQLDefaultOperator.EQUAL.getOperator(), new MockCustomOperator().getComparisonOperator());
		EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
				TestEntityWithCustom.class,
				List.of(),
				Map.of(),
				true,
				false,
				1
		);

		assertThrows(
				QueryConfigurationException.class, () ->
						new RSQLParser(ops).parse("name=mock=value").accept(visitor, NodeMetadata.of(0))
		);
	}

	@Test
	void rejects_orOperator_whenNotAllowed() {
		EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
				TestEntity.class,
				List.of(),
				customOperators,
				true,
				false,
				1
		);

		assertThrows(
				QueryValidationException.class, () ->
						new RSQLParser().parse("name==john,name==doe").accept(visitor, NodeMetadata.of(0))
		);
	}

	@Test
	void rejects_whenAstDepthExceeded() {
		EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
				TestEntity.class,
				List.of(),
				customOperators,
				true,
				true,
				0
		);

		assertThrows(
				QueryValidationException.class, () ->
						new RSQLParser().parse("name==john;name==doe").accept(visitor, NodeMetadata.of(0))
		);
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

	private static class TestEntity {

		@RSQLFilterable({RSQLDefaultOperator.EQUAL})
		private String name;

		private Integer age;
	}

	private static class TestEntityWithCustom {

		@RSQLFilterable(value = {RSQLDefaultOperator.EQUAL}, customOperators = {MockCustomOperator.class})
		private String name;
	}

	private static FieldMapping mapping(String name, String field, boolean allowOriginalFieldName) {
		return new FieldMapping() {

			@Override
			public String name() {
				return name;
			}

			@Override
			public String field() {
				return field;
			}

			@Override
			public boolean allowOriginalFieldName() {
				return allowOriginalFieldName;
			}

			@Override
			public Class<? extends java.lang.annotation.Annotation> annotationType() {
				return FieldMapping.class;
			}
		};
	}
}
