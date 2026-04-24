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

package in.co.akshitbansal.springwebquery.pathmapper;

import in.co.akshitbansal.springwebquery.entity.UserEntity;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DTOToEntityPathMapperFactoryTest {

	@Test
	void testConstructionWithNegativeFailedResolutionsMaxCapacity() {
		QueryConfigurationException ex = assertThrows(
				QueryConfigurationException.class,
				() -> new DTOToEntityPathMapperFactory(-1, 2)
		);
		Throwable cause = ex.getCause();
		assertInstanceOf(IllegalArgumentException.class, cause);
		assertTrue(cause.getMessage().contains("must not be negative"));
	}

	@Test
	void testConstructionWithNonPositiveLockStripeCount() {
		QueryConfigurationException ex = assertThrows(
				QueryConfigurationException.class,
				() -> new DTOToEntityPathMapperFactory(10, 0)
		);
		Throwable cause = ex.getCause();
		assertInstanceOf(IllegalArgumentException.class, cause);
		assertTrue(cause.getMessage().contains("must be positive"));
	}

	@Test
	void testMapperConstructionWithNullEntityClass() {
		DTOToEntityPathMapperFactory factory = new DTOToEntityPathMapperFactory();
		assertThrows(NullPointerException.class, () -> factory.newMapper(null, User.class));
	}

	@Test
	void testMapperConstructionWithNullDTOClass() {
		DTOToEntityPathMapperFactory factory = new DTOToEntityPathMapperFactory();
		assertThrows(NullPointerException.class, () -> factory.newMapper(UserEntity.class, null));
	}

	@Test
	void testUncachedMapperConstruction() {
		DTOToEntityPathMapperFactory factory = new DTOToEntityPathMapperFactory();
		DTOToEntityPathMapper mapper = factory.newMapper(UserEntity.class, User.class);
		assertSame(DTOToEntityPathMapper.class, mapper.getClass());
	}

	@Test
	void testCachedMapperConstruction() {
		DTOToEntityPathMapperFactory factory = new DTOToEntityPathMapperFactory(10, 2);
		DTOToEntityPathMapper mapper = factory.newMapper(UserEntity.class, User.class);
		assertSame(CachedDTOToEntityPathMapper.class, mapper.getClass());
	}
}
