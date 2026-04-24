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

import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;

import java.lang.annotation.*;

/**
 * Convenience annotation that allows null-check filtering on a query contract
 * field.
 *
 * <p>This annotation is equivalent to declaring {@link RSQLFilterable} with
 * {@link RSQLDefaultOperator#IS_NULL} and
 * {@link RSQLDefaultOperator#NOT_NULL}. It is useful for nullable attributes
 * where callers need to test presence or absence explicitly.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RSQLFilterable({RSQLDefaultOperator.IS_NULL, RSQLDefaultOperator.NOT_NULL})
public @interface RSQLFilterableNull {
}
