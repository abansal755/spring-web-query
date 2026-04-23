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

package in.co.akshitbansal.springwebquery.annotation;

import java.lang.annotation.*;

/**
 * Declares how a DTO field contributes to the entity path derived from a DTO
 * selector.
 *
 * <p>This annotation is consulted only while mapping selector paths from the
 * DTO query contract to the underlying entity model. For each field in the
 * resolved DTO path, the mapper contributes either the DTO field name itself or
 * the value declared here.</p>
 *
 * <p>If {@code @MapsTo} is absent, the DTO field name is reused as the
 * corresponding entity-side segment. If {@code @MapsTo} is present, its
 * {@link #value()} is appended instead. When {@link #absolute()} is
 * {@code true}, any previously accumulated parent segments are discarded before
 * the mapped value is appended.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code class UserDto {
 *     @MapsTo("profile")
 *     private ProfileDto details;
 *
 *     class ProfileDto {
 *         @MapsTo("displayName")
 *         private String name;
 *     }
 * }}</pre>
 *
 * <p>With that contract, the DTO selector {@code details.name} maps to the
 * entity path {@code profile.displayName}.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapsTo {

	/**
	 * Entity-side field segment or dotted subpath to append for this DTO field.
	 *
	 * <p>The declared value is inserted into the mapped entity path in place of
	 * the DTO field name.</p>
	 *
	 * @return mapped entity field segment or subpath
	 */
	String value();

	/**
	 * Whether this mapping starts a new absolute path from the entity root.
	 *
	 * <p>When {@code true}, previously accumulated parent segments are discarded
	 * before {@link #value()} is applied.</p>
	 *
	 * @return {@code true} to reset parent segments, otherwise {@code false}
	 */
	boolean absolute() default false;
}
