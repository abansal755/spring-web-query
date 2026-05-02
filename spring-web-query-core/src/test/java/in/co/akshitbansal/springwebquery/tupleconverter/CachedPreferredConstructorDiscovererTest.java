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

import in.co.akshitbansal.springwebquery.common.model.Address;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.hibernate.sql.results.internal.TupleElementImpl;
import org.hibernate.sql.results.internal.TupleImpl;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static in.co.akshitbansal.springwebquery.tupleconverter.CachedPreferredConstructorDiscoverer.CacheKey;
import static org.junit.jupiter.api.Assertions.*;

class CachedPreferredConstructorDiscovererTest {

	@Test
	void testConstructionWithNullClass() {
		var constructorCache = new ConcurrentHashMap<CacheKey, Constructor<?>>();
		assertThrows(NullPointerException.class, () -> new CachedPreferredConstructorDiscoverer<>(null, constructorCache));
	}

	@Test
	void testConstructionWithNullMap() {
		assertThrows(NullPointerException.class, () -> new CachedPreferredConstructorDiscoverer<>(Address.class, null));
	}

	@Test
	void testConstruction() {
		var constructorCache = new ConcurrentHashMap<CacheKey, Constructor<?>>();
		assertDoesNotThrow(() -> new CachedPreferredConstructorDiscoverer<>(Address.class, constructorCache));
	}

	@Test
	void testDiscoverWithNullTuple() {
		CachedPreferredConstructorDiscoverer<Address> discoverer =
				new CachedPreferredConstructorDiscoverer<>(Address.class, new ConcurrentHashMap<>());
		assertThrows(NullPointerException.class, () -> discoverer.discover(null));
	}

	@Test
	void testShouldReturnConstructorForRepeatedLookups() {
		// Constructing discoverer
		ConcurrentMap<CacheKey, Constructor<?>> constructorCache = new ConcurrentHashMap<>();
		CachedPreferredConstructorDiscoverer<Address> discoverer =
				new CachedPreferredConstructorDiscoverer<>(Address.class, constructorCache);

		// Asserting that the cache is empty
		assertEquals(0, constructorCache.size());

		// Loading cache with one result
		Constructor<?> result = discoverer.discover(newTuple());
		// Asserting that the cache has one entry
		assertEquals(1, constructorCache.size());
		// Assertions on the constructor
		assertEquals(Address.class, result.getDeclaringClass());

		// Calling the discoverer again with the same tuple shape as before
		Constructor<?> result2 = discoverer.discover(newTuple());
		// The result objects should be the same (== not just equals)
		assertSame(result, result2);
	}

	private Tuple newTuple() {
		TupleElement<String> tupleElement = new TupleElementImpl<>(String.class, "city");
		TupleMetadata metadata = new TupleMetadata(new TupleElement[]{ tupleElement }, new String[]{ "city" });
		return new TupleImpl(metadata, new Object[]{ "city" });
	}
}
