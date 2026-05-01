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

import in.co.akshitbansal.springwebquery.model.Address;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreferredConstructorDiscovererFactoryTest {

	@Test
	void testConstructionWithCachingEnabled() {
		assertDoesNotThrow(() -> new PreferredConstructorDiscovererFactory(true));
	}

	@Test
	void testConstructionWithCachingDisabled() {
		assertDoesNotThrow(() -> new PreferredConstructorDiscovererFactory(false));
	}

	@Test
	void testNewDiscovererWithNullClass() {
		var factory = new PreferredConstructorDiscovererFactory(false);
		assertThrows(NullPointerException.class, () -> factory.newDiscoverer(null));
	}

	@Test
	void testNewDiscovererWithCachingEnabled() {
		var factory = new PreferredConstructorDiscovererFactory(true);
		assertSame(CachedPreferredConstructorDiscoverer.class, factory.newDiscoverer(Address.class).getClass());
	}

	@Test
	void testNewDiscovererWithCachingDisabled() {
		var factory = new PreferredConstructorDiscovererFactory(false);
		assertSame(PreferredConstructorDiscoverer.class, factory.newDiscoverer(Address.class).getClass());
	}
}
