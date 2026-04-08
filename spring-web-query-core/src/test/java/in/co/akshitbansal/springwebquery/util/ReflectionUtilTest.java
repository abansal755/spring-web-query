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

package in.co.akshitbansal.springwebquery.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectionUtilTest {

	@Test
	void resolveField_resolvesFromSuperclass() {
		Field field = ReflectionUtil.resolveField(ChildEntity.class, "id");
		assertEquals("id", field.getName());
		assertEquals(BaseEntity.class, field.getDeclaringClass());
	}

	@Test
	void resolveField_resolvesNestedFieldThroughCollection() {
		Field field = ReflectionUtil.resolveField(ParentEntity.class, "children.name");
		assertEquals("name", field.getName());
		assertEquals(NestedEntity.class, field.getDeclaringClass());
	}

	@Test
	void resolveField_resolvesNestedFieldThroughArray() {
		Field field = ReflectionUtil.resolveField(ArrayParentEntity.class, "children.name");
		assertEquals("name", field.getName());
		assertEquals(NestedEntity.class, field.getDeclaringClass());
	}

	@Test
	void resolveFieldPath_returnsFullPath() {
		java.util.List<Field> fields = ReflectionUtil.resolveFieldPath(ParentEntity.class, "children.name");
		assertEquals(2, fields.size());
		// assertEquals("children", fields.getFirst().getName());
		// getFirst() was added in Java 21, using get(0) for compatibility with earlier versions
		assertEquals("children", fields.get(0).getName());
		assertEquals("name", fields.get(fields.size() - 1).getName());
	}

	@Test
	void resolveField_throwsForUnknownSegment() {
		RuntimeException ex = assertThrows(
				RuntimeException.class,
				() -> ReflectionUtil.resolveField(ParentEntity.class, "children.unknown")
		);
		assertEquals("Field 'unknown' not found in class hierarchy of class in.co.akshitbansal.springwebquery.util.ReflectionUtilTest$NestedEntity", ex.getMessage());
	}

	private static class BaseEntity {

		private String id;
	}

	private static class ChildEntity extends BaseEntity {

		private String value;
	}

	private static class ParentEntity {

		private List<NestedEntity> children;
	}

	private static class ArrayParentEntity {

		private NestedEntity[] children;
	}

	private static class NestedEntity {

		private String name;
	}
}
