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
 * Marks a field as eligible for request-driven sorting.
 *
 * <p>Sortability is validated against the terminal field of the requested sort
 * selector. In practice, this annotation belongs on the fields that make up
 * the public sorting contract for the query.</p>
 *
 * <p>Fields without this annotation are rejected by the sorting validator even
 * if the selector path itself is valid and the underlying entity path exists.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code @Sortable
 * private String displayName;}</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sortable {
}
