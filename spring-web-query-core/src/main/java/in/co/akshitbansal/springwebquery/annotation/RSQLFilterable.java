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

import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;

import java.lang.annotation.*;

/**
 * Marks a field as filterable via RSQL (RESTful Service Query Language) queries.
 * <p>
 * This annotation allows you to declare which RSQL operators (both default and custom)
 * are permitted on a specific field.
 * When used in combination with a RSQL-to-Spring-Data Specification resolver,
 * only the specified default and custom operators will be allowed for filtering on this field.
 * </p>
 *
 * <p>The annotation is applied to whichever type is used as the filtering contract:
 * entity fields in entity-aware mode, or DTO fields in DTO-aware mode.</p>
 *
 * <p>This annotation is {@linkplain Repeatable repeatable}; multiple declarations
 * can be used on the same field to compose the final set of allowed operators.</p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * @RSQLFilterable({RSQLDefaultOperator.EQUAL, RSQLDefaultOperator.IN})
 * private String status;
 * }</pre>
 *
 * <p>Fields without this annotation are considered <em>not filterable</em> and attempts
 * to filter them via RSQL queries will result in an exception.</p>
 *
 * <p>This annotation is retained at runtime and can be inspected via reflection.</p>
 *
 * @see RSQLDefaultOperator
 * @see RSQLFilterables
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(RSQLFilterables.class)
public @interface RSQLFilterable {

	/**
	 * The set of default RSQL operators that are allowed for filtering this field.
	 *
	 * @return an array of allowed {@link RSQLDefaultOperator} values
	 */
	RSQLDefaultOperator[] value() default {};

	/**
	 * The set of custom RSQL operators that are allowed for filtering this field.
	 * Referenced operator classes must be registered in the query resolver configuration.
	 *
	 * @return an array of custom operator classes
	 */
	Class<? extends RSQLCustomOperator<?>>[] customOperators() default {};
}
