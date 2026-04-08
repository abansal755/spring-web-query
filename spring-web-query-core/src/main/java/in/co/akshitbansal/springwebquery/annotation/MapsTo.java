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
 * Declares the entity-side property segment that a DTO field maps to for
 * filtering and sorting path translation.
 *
 * <p>When DTO-aware query resolution is enabled, each path segment is resolved
 * on the DTO class and then translated to an entity path using this annotation.
 * If absent, the DTO field name itself is used as the entity segment.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * class UserDto {
 *   @MapsTo("profile")
 *   private ProfileDto details;
 *
 *   class ProfileDto {
 *     @MapsTo("displayName")
 *     private String name;
 *   }
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapsTo {

	/**
	 * Entity-side field or path segment to use for this DTO field.
	 *
	 * @return mapped entity path segment
	 */
	String value();

	/**
	 * Whether this mapping starts a new absolute path from the entity root.
	 *
	 * <p>When {@code true}, previously accumulated parent segments are discarded
	 * before this segment is applied.</p>
	 *
	 * @return {@code true} to reset parent segments, otherwise {@code false}
	 */
	boolean absolute() default false;
}
