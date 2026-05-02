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

package in.co.akshitbansal.springwebquery.repository;

import in.co.akshitbansal.springwebquery.common.entity.AddressEntity;
import in.co.akshitbansal.springwebquery.common.entity.PhoneEntity;
import in.co.akshitbansal.springwebquery.common.entity.UserEntity;
import in.co.akshitbansal.springwebquery.common.model.User;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class WebQueryRepositoryTest {

	@Container
	private static final MySQLContainer mysqlContainer = new MySQLContainer("mysql:9.7.0")
			.withInitScript("init.sql");

	@DynamicPropertySource
	private static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
		registry.add("spring.datasource.username", mysqlContainer::getUsername);
		registry.add("spring.datasource.password", mysqlContainer::getPassword);
	}

	@Autowired
	private UserRepository userRepository;

	@Test
	void testWithNullPageable() {
		assertThrows(NullPointerException.class, () ->
				userRepository.findAll(null, null, this::getSelections, User.class));
		assertThrows(NullPointerException.class, () ->
				userRepository.findAllPaged(null, null, this::getSelections, User.class));
	}

	@Test
	void testWithNullSelectionsProvider() {
		assertThrows(NullPointerException.class, () ->
				userRepository.findAll(null, Pageable.unpaged(), null, User.class));
		assertThrows(NullPointerException.class, () ->
				userRepository.findAllPaged(null, Pageable.unpaged(), null, User.class));
	}

	@Test
	void testWithNullDTOClass() {
		assertThrows(NullPointerException.class, () ->
				userRepository.findAll(null, Pageable.unpaged(), this::getSelections, null));
		assertThrows(NullPointerException.class, () ->
				userRepository.findAllPaged(null, Pageable.unpaged(), this::getSelections, null));
		assertThrows(NullPointerException.class, () ->
				userRepository.count(null, null));
	}

	@Test
	void testWithNullQueryAndUnpagedPageable() {
		List<User> results = userRepository.findAll(null, Pageable.unpaged(), this::getSelections, User.class);
		assertEquals(3, results.size());

		Page<User> page =  userRepository.findAllPaged(null, Pageable.unpaged(), this::getSelections, User.class);
		Pageable pageable = page.getPageable();
		assertTrue(pageable.isUnpaged());
		assertEquals(3, page.getTotalElements());

		long count = userRepository.count(null, User.class);
		assertEquals(3, count);
	}

	@Test
	void testWithNullQueryAndPageableWithSort() {
		// Asserting 1st page returns the last record
		var pageable = PageRequest.of(0, 1, Sort.by("id").descending());
		List<User> results = userRepository.findAll(null, pageable, this::getSelections, User.class);
		assertEquals(1, results.size());
		assertEquals(3, results.get(0).getId());

		// Asserting 2nd page returns the 2nd last record
		pageable = PageRequest.of(1, 1, Sort.by("id").descending());
		results = userRepository.findAll(null, pageable, this::getSelections, User.class);
		assertEquals(1, results.size());
		assertEquals(2, results.get(0).getId());
	}

	@Test
	void testWithIdEqualsFilter() {
		List<User> results = userRepository.findAll("id==1", Pageable.unpaged(), this::getSelections, User.class);
		assertEquals(1, results.size());
		assertEquals(1, results.get(0).getId());

		long count = userRepository.count("id==1", User.class);
		assertEquals(1, count);
	}

	@Test
	void testWithNestedOneToManyFilter() {
		List<User> results = userRepository.findAll("phones.number=like=0101", Pageable.unpaged(), this::getSelections, User.class);
		assertEquals(1, results.size());
		assertEquals(2, results.get(0).getPhones().length);
	}

	private List<Selection<?>> getSelections(Root<UserEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
		Subquery<String> phonesQuery = query.subquery(String.class);
		Root<PhoneEntity> phone = phonesQuery.from(PhoneEntity.class);
		phonesQuery.select(
				cb.function("JSON_ARRAYAGG", String.class,
					cb.function("JSON_OBJECT", String.class,
						cb.literal("number"), phone.get("phoneNumber")))
		);
		phonesQuery.where(cb.equal(root.get("id"), phone.get("user").get("id")));

		Subquery<String> addressesQuery = query.subquery(String.class);
		Root<AddressEntity> address = addressesQuery.from(AddressEntity.class);
		addressesQuery.select(
				cb.function("JSON_ARRAYAGG", String.class,
					cb.function("JSON_OBJECT", String.class,
						cb.literal("city"), address.get("userCity")))
		);
		addressesQuery.where(cb.equal(root.get("id"), address.get("user").get("id")));

		return List.of(
				root.get("id"),
				root.get("email"),
				root.get("firstName"),
				root.get("lastName"),
				phonesQuery,
				addressesQuery
		);
	}
}
