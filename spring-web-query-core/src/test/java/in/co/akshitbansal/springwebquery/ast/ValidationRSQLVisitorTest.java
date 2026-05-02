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

package in.co.akshitbansal.springwebquery.ast;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import in.co.akshitbansal.springwebquery.customoperator.IsLongGreaterThanFiveOperator;
import in.co.akshitbansal.springwebquery.entity.UserEntity;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenLogicalOperatorException;
import in.co.akshitbansal.springwebquery.exception.QueryMaxASTDepthExceededException;
import in.co.akshitbansal.springwebquery.model.User;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapperFactory;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRSQLVisitorTest {

	private final DTOToEntityPathMapperFactory pathMapperFactory = new DTOToEntityPathMapperFactory();
	private final DTOToEntityPathMapper pathMapper = pathMapperFactory.newMapper(UserEntity.class, User.class);

	private final IsLongGreaterThanFiveOperator customOperator = new IsLongGreaterThanFiveOperator();

	private final FilterableFieldValidator fieldValidator = new FilterableFieldValidator(Map.of(IsLongGreaterThanFiveOperator.class, customOperator));
	private final RSQLParser parser = new RSQLParser(getAllowedOperators());

	private Set<ComparisonOperator> getAllowedOperators() {
		Stream<ComparisonOperator> defaultOperators = Arrays
				.stream(RSQLDefaultOperator.values())
				.map(RSQLDefaultOperator::getOperator);
		Stream<ComparisonOperator> customOperators = Stream
				.of(customOperator)
				.map(RSQLCustomOperator::getComparisonOperator);
		return Stream
				.concat(defaultOperators, customOperators)
				.collect(Collectors.toSet());
	}

	@Test
	void testConstructionWithNullPathMapper() {
		assertThrows(NullPointerException.class, () -> new ValidationRSQLVisitor(
				true,
				false,
				1,
				null,
				fieldValidator
		));
	}

	@Test
	void testConstructionWithNullFieldValidator() {
		assertThrows(NullPointerException.class, () -> new ValidationRSQLVisitor(
				true,
				false,
				1,
				pathMapper,
				null
		));
	}

	@Test
	void testConstruction() {
		assertDoesNotThrow(() -> new ValidationRSQLVisitor(
				true,
				false,
				1,
				pathMapper,
				fieldValidator
		));
	}

	@Test
	void testAndNodeNotAllowed() {
		var visitor = new ValidationRSQLVisitor(
				false,
				true,
				1,
				pathMapper,
				fieldValidator
		);
		Node node = parser.parse("id==1;name.firstName=ic=John");
		var nodeMetadata = NodeMetadata.of(0);
		var ex = assertThrows(QueryForbiddenLogicalOperatorException.class, () -> node.accept(visitor, nodeMetadata));
		assertTrue(ex.getMessage().contains("AND operator is not allowed"));
	}

	@Test
	void testOrNodeNotAllowed() {
		var visitor = new ValidationRSQLVisitor(
				true,
				false,
				1,
				pathMapper,
				fieldValidator
		);
		Node node = parser.parse("id==1,id==2");
		var nodeMetadata = NodeMetadata.of(0);
		var ex = assertThrows(QueryForbiddenLogicalOperatorException.class, () -> node.accept(visitor, nodeMetadata));
		assertTrue(ex.getMessage().contains("OR operator is not allowed"));
	}

	@Test
	void testMaxASTDepth() {
		var visitor = new ValidationRSQLVisitor(
				true,
				false,
				0,
				pathMapper,
				fieldValidator
		);
		Node node = parser.parse("id==1;name.firstName=ic=John");
		var nodeMetadata = NodeMetadata.of(0);
		var ex = assertThrows(QueryMaxASTDepthExceededException.class, () -> node.accept(visitor, nodeMetadata));
		assertTrue(ex.getMessage().contains("Exceeded maximum allowed depth"));
	}

	@Test
	void testFieldMappings() {
		var visitor = new ValidationRSQLVisitor(
				true,
				true,
				2,
				pathMapper,
				fieldValidator
		);
		Node node = parser.parse("(name.firstName=ic=John,name.firstName=ic=Doe);(id==1,id==2)");
		var nodeMetadata = NodeMetadata.of(0);
		node.accept(visitor, nodeMetadata);

		// Assertions on the field mappings collected by the visitor
		var mappings = visitor.getFieldMappings();
		assertEquals(2, mappings.size());
		assertEquals("firstName", mappings.get("name.firstName"));
		assertEquals("id", mappings.get("id"));
	}
}
