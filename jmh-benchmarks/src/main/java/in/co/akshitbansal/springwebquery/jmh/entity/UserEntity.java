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

package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

import java.util.List;

// @Entity
// @Table(name = "app_user")
@Data
public class UserEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "user_id", nullable = false, unique = true)
	private String userId;

	// @OneToOne(cascade = CascadeType.ALL, optional = false)
	// @JoinColumn(name = "profile_id", nullable = false)
	private UserProfileEntity profile;

	// @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	// @JoinColumn(name = "user_id_fk", nullable = false)
	private List<AccountEntity> accounts;
}
