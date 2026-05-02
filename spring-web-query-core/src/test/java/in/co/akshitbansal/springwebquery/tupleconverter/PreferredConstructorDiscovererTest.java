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
import in.co.akshitbansal.springwebquery.common.model.Address;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.hibernate.sql.results.internal.TupleElementImpl;
import org.hibernate.sql.results.internal.TupleImpl;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.PersistenceCreator;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class PreferredConstructorDiscovererTest {

	private final PreferredConstructorDiscoverer<Address> discoverer = new PreferredConstructorDiscoverer<>(Address.class);

	@Test
	void testConstructionWithNullClass() {
		assertThrows(NullPointerException.class, () -> new PreferredConstructorDiscoverer<>(null));
	}

	@Test
	void testConstructionWithNonNullClass() {
		assertDoesNotThrow(() -> new PreferredConstructorDiscoverer<>(Address.class));
	}

	@Test
	void testWithNullTuple() {
		assertThrows(NullPointerException.class, () -> discoverer.discover(null));
	}

	@Test
	void testWithInvalidTuple() {
		TupleElement<Integer> tupleElement = new TupleElementImpl<>(int.class, "id");
		TupleMetadata metadata = new TupleMetadata(new TupleElement[]{ tupleElement }, new String[]{ "id" });
		Tuple tuple = new TupleImpl(metadata, new Object[]{ 1 });
		QueryConfigurationException ex = assertThrows(QueryConfigurationException.class, () -> discoverer.discover(tuple));
		assertTrue(ex.getMessage().contains("No suitable constructor"));
	}

	@Test
	void testWithValidTuple() {
		TupleElement<String> tupleElement = new TupleElementImpl<>(String.class, "city");
		TupleMetadata metadata = new TupleMetadata(new TupleElement[]{ tupleElement }, new String[]{ "city" });
		Tuple tuple = new TupleImpl(metadata, new Object[]{ "city" });
		Constructor<Address> constructor = discoverer.discover(tuple);
		assertTrue(constructor.isAnnotationPresent(PersistenceCreator.class));
		assertEquals(1, constructor.getParameterCount());
		assertEquals(String.class, constructor.getParameterTypes()[0]);
	}
}
