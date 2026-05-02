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

package in.co.akshitbansal.springwebquery.common.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterableEquality;
import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.customoperator.IsLongGreaterThanFiveOperator;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

import java.util.List;

@Data
@NoArgsConstructor
public class User {

	@RSQLFilterable(customOperators = IsLongGreaterThanFiveOperator.class)
	@RSQLFilterableEquality
	@Sortable
	private Long id;

	// Explicitly not annotated with @RSQLFilterable
	private String email;

	private Name name;
	private Phone[] phones;
	private List<Address> addresses;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@PersistenceCreator
	private User(Long id, String email, String firstName, String lastName, String phones, String addresses) throws JsonProcessingException {
		this.id = id;
		this.email = email;
		this.name = new Name(firstName, lastName);

		this.phones = objectMapper.readValue(phones, new TypeReference<Phone[]>() {});
		this.addresses = objectMapper.readValue(addresses, new TypeReference<List<Address>>() {});
	}
}
