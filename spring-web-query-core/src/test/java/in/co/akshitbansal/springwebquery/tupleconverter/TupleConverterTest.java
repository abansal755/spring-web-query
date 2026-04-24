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

package in.co.akshitbansal.springwebquery.tupleconverter;

import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.model.Address;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.hibernate.sql.results.internal.TupleElementImpl;
import org.hibernate.sql.results.internal.TupleImpl;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.MessageFormat;

import static org.junit.jupiter.api.Assertions.*;

class TupleConverterTest {

	private final TupleConverter<Address> converter = TupleConverter.of(Address.class);

	@Test
	void testConstructionWithNullClass() {
		assertThrows(NullPointerException.class, () -> TupleConverter.of(null));
	}

	@Test
	void testConstructionWithNonNullClass() {
		assertDoesNotThrow(() -> TupleConverter.of(Address.class));
	}

	@Test
	void testConvertWithNullTuple() {
		assertThrows(NullPointerException.class, () -> converter.convert(null));
	}

	@Test
	void testConvertWithInvalidTuple() {
		TupleElement<Integer> tupleElement = new TupleElementImpl<>(int.class, "id");
		TupleMetadata metadata = new TupleMetadata(new TupleElement[]{ tupleElement }, new String[]{ "id" });
		Tuple tuple = new TupleImpl(metadata, new Object[]{ 1 });
		QueryConfigurationException ex = assertThrows(QueryConfigurationException.class, () -> converter.convert(tuple));
		assertTrue(ex.getMessage().contains("Failed to convert"));
	}

	@Test
	void testCachedConstructorShouldRemainSameAfterRepeatedCalls() {
		TupleElement<String> tupleElement = new TupleElementImpl<>(String.class, "city");
		TupleMetadata metadata = new TupleMetadata(new TupleElement[]{ tupleElement }, new String[]{ "city" });
		Tuple tuple = new TupleImpl(metadata, new Object[]{ "city" });

		// New instance to test caching
		TupleConverter<Address> converter = TupleConverter.of(Address.class);

		// Should be null initially
		var constructor = getCachedConstructor(converter);
		assertNull(constructor);

		// First call should populate the cache
		Address address = converter.convert(tuple);
		assertEquals("city", address.getCity());
		var constructor2 = getCachedConstructor(converter);
		assertNotNull(constructor2);

		// Subsequent calls should use the cached constructor
		Address address2 = converter.convert(tuple);
		assertEquals("city", address2.getCity());
		var constructor3 = getCachedConstructor(converter);
		assertEquals(constructor2, constructor3);
	}

	private Constructor<Address> getCachedConstructor(TupleConverter<Address> converter) {
		try {
			Field field = TupleConverter.class.getDeclaredField("cachedConstructor");
			field.setAccessible(true);
			return (Constructor<Address>) field.get(converter);
		}
		catch (Exception ex) {
			throw new RuntimeException(MessageFormat.format(
					"Failed to get cached constructor: {0}", ex.getMessage()
			), ex);
		}
	}
}
