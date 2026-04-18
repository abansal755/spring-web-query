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
import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.ast.DTOValidationRSQLVisitor;
import in.co.akshitbansal.springwebquery.ast.NodeMetadata;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.resolver.spring.config.SpecificationArgumentResolverConfig;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DTOValidationRSQLVisitorTest {

	private final Map<Class<?>, RSQLCustomOperator<?>> customOperators = Map.of();

	@Test
	void builds_fieldMappingsForValidDtoPath() {
		DTOValidationRSQLVisitor visitor = newVisitor(TestEntity.class, QueryDto.class, customOperators, true, false, 1);
		new RSQLParser().parse("profile.city==London").accept(visitor, NodeMetadata.of(0));

		assertEquals(Map.of("profile.city", "profile.address.city"), visitor.getFieldMappings());
	}

	@Test
	void rejects_unknownDtoPath() {
		DTOValidationRSQLVisitor visitor = newVisitor(TestEntity.class, QueryDto.class, customOperators, true, false, 1);

		assertThrows(QueryValidationException.class, () -> new RSQLParser().parse("missing==x").accept(visitor, NodeMetadata.of(0)));
	}

	@Test
	void rejects_nonFilterableDtoField() {
		DTOValidationRSQLVisitor visitor = newVisitor(TestEntity.class, QueryDto.class, customOperators, true, false, 1);

		assertThrows(QueryValidationException.class, () -> new RSQLParser().parse("unfilterable==x").accept(visitor, NodeMetadata.of(0)));
	}

	@Test
	void rejects_whenMappedEntityPathCannotBeResolved() {
		DTOValidationRSQLVisitor visitor = newVisitor(TestEntity.class, InvalidMappingDto.class, customOperators, true, false, 1);

		assertThrows(QueryConfigurationException.class, () -> new RSQLParser().parse("city==x").accept(visitor, NodeMetadata.of(0)));
	}

	@Test
	void supports_absoluteMapReset() {
		DTOValidationRSQLVisitor visitor = newVisitor(TestEntity.class, AbsoluteDto.class, customOperators, true, false, 1);
		new RSQLParser().parse("nested.city==x").accept(visitor, NodeMetadata.of(0));

		assertEquals(Map.of("nested.city", "profile.address.city"), visitor.getFieldMappings());
	}

	@Test
	void rejects_andOperator_whenNotAllowed() {
		DTOValidationRSQLVisitor visitor = newVisitor(TestEntity.class, QueryDto.class, customOperators, false, false, 1);
		RSQLParser parser = new RSQLParser();

		assertThrows(QueryValidationException.class, () -> parser.parse("profile.city==London;unfilterable==x").accept(visitor, NodeMetadata.of(0)));
	}

	@Test
	void rejects_orOperator_whenNotAllowed() {
		DTOValidationRSQLVisitor visitor = newVisitor(TestEntity.class, QueryDto.class, customOperators, true, false, 1);
		RSQLParser parser = new RSQLParser();

		assertThrows(QueryValidationException.class, () -> parser.parse("profile.city==London,unfilterable==x").accept(visitor, NodeMetadata.of(0)));
	}

	@Test
	void allows_orOperator_whenExplicitlyEnabled() {
		DTOValidationRSQLVisitor visitor = newVisitor(TestEntity.class, QueryDto.class, customOperators, true, true, 1);
		new RSQLParser().parse("profile.city==London,profile.city==Paris").accept(visitor, NodeMetadata.of(0));
	}

	private DTOValidationRSQLVisitor newVisitor(
			Class<?> entityClass,
			Class<?> dtoClass,
			Map<Class<?>, RSQLCustomOperator<?>> customOperators,
			boolean andNodeAllowed,
			boolean orNodeAllowed,
			int maxDepth
	) {
		ValidationRSQLVisitorFactory factory = new ValidationRSQLVisitorFactory(
				new FieldResolverFactory(),
				new FilterableFieldValidator(customOperators)
		);
		SpecificationArgumentResolverConfig config = new SpecificationArgumentResolverConfig(
				entityClass,
				dtoClass,
				List.of(),
				"filter",
				andNodeAllowed,
				orNodeAllowed,
				maxDepth
		);
		return (DTOValidationRSQLVisitor) factory.newValidationRSQLVisitor(config);
	}

	private static class TestEntity {

		private Profile profile;

		@SuppressWarnings("unused")
		private String unfilterable;
	}

	private static class Profile {

		private Address address;
	}

	private static class Address {

		@SuppressWarnings("unused")
		private String city;
	}

	private static class QueryDto {

		@MapsTo("profile")
		private ProfileDto profile;

		@SuppressWarnings("unused")
		private String unfilterable;
	}

	private static class ProfileDto {

		@MapsTo("address")
		private AddressDto address;

		@RSQLFilterable({RSQLDefaultOperator.EQUAL})
		@MapsTo("address.city")
		private String city;
	}

	private static class AddressDto {

		@RSQLFilterable({RSQLDefaultOperator.EQUAL})
		@MapsTo("city")
		private String city;
	}

	private static class InvalidMappingDto {

		@RSQLFilterable({RSQLDefaultOperator.EQUAL})
		@MapsTo("doesNotExist")
		private String city;
	}

	private static class AbsoluteDto {

		private NestedDto nested;
	}

	private static class NestedDto {

		@RSQLFilterable({RSQLDefaultOperator.EQUAL})
		@MapsTo(value = "profile.address.city", absolute = true)
		private String city;
	}
}
