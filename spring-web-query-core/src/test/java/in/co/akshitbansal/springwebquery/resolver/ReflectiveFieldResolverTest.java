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

package in.co.akshitbansal.springwebquery.resolver;

import in.co.akshitbansal.springwebquery.model.Address;
import in.co.akshitbansal.springwebquery.model.Name;
import in.co.akshitbansal.springwebquery.model.Phone;
import in.co.akshitbansal.springwebquery.model.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReflectiveFieldResolverTest {

	private final ReflectiveFieldResolver resolver = ReflectiveFieldResolver.of(User.class);

	@Test
	void testConstructionWithNullClass() {
		assertThrows(NullPointerException.class, () -> ReflectiveFieldResolver.of(null));
	}

	@Test
	void testConstructionWithNonNullClass() {
		assertDoesNotThrow( () -> ReflectiveFieldResolver.of(User.class));
	}

	@Test
	void testResolveFieldPathWithNullPath() {
		assertThrows(NullPointerException.class, () -> resolver.resolveFieldPath(null));
	}

	@Test
	void testResolveFieldPathWithEmptyPath() {
		assertThrows(IllegalArgumentException.class, () -> resolver.resolveFieldPath(""));
	}

	@Test
	void testResolveFieldPathWithInvalidPath() {
		assertThrows(IllegalArgumentException.class, () -> resolver.resolveFieldPath("hello"));
	}

	@Test
	void testResolveFieldPathWithValidPath() {
		List<Field> fields = resolver.resolveFieldPath("email");
		assertEquals(1, fields.size());
		assertEquals("email", fields.get(0).getName());
		assertEquals(User.class, fields.get(0).getDeclaringClass());
	}

	@Test
	void testResolveFieldPathWithNestedPath() {
		List<Field> fields = resolver.resolveFieldPath("name.firstName");
		assertEquals(2, fields.size());

		// Assertions on the 1st field
		assertEquals("name", fields.get(0).getName());
		assertEquals(User.class, fields.get(0).getDeclaringClass());

		// Assertions on the 2nd field
		assertEquals("firstName", fields.get(1).getName());
		assertEquals(Name.class, fields.get(1).getDeclaringClass());
	}

	@Test
	void testResolveFieldPathWithArray() {
		List<Field> fields = resolver.resolveFieldPath("phones.number");
		assertEquals(2, fields.size());

		// Assertions on the 1st field
		assertEquals("phones", fields.get(0).getName());
		assertEquals(User.class, fields.get(0).getDeclaringClass());

		// Assertions on the 2nd field
		assertEquals("number", fields.get(1).getName());
		assertEquals(Phone.class, fields.get(1).getDeclaringClass());
	}

	@Test
	void testResolveFieldPathWithCollection() {
		List<Field> fields = resolver.resolveFieldPath("addresses.city");
		assertEquals(2, fields.size());

		// Assertions on the 1st field
		assertEquals("addresses", fields.get(0).getName());
		assertEquals(User.class, fields.get(0).getDeclaringClass());

		// Assertions on the 2nd field
		assertEquals("city", fields.get(1).getName());
		assertEquals(Address.class, fields.get(1).getDeclaringClass());
	}
}
