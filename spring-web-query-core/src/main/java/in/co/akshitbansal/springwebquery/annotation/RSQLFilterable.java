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
 * Marks a field as filterable and declares which comparison operators are
 * allowed when that field is the terminal field of an RSQL selector path.
 *
 * <p>During query validation, the incoming selector path is first resolved
 * against the active query contract and then mapped to the underlying entity
 * path if necessary. Operator validation is performed against the terminal
 * field of that resolved selector path. In practice, this annotation belongs on
 * the fields that make up the public filtering contract for the query.</p>
 *
 * <p>The effective allowed operator set is the union of:</p>
 * <ul>
 *   <li>all {@link #value()} entries declared directly on the field</li>
 *   <li>all {@link #customOperators()} entries declared directly on the
 *       field</li>
 *   <li>all repeated declarations collected through {@link RSQLFilterables}</li>
 *   <li>all built-in composed annotations from this package, such as
 *       {@link RSQLFilterableEquality} and {@link RSQLFilterableRange}</li>
 * </ul>
 *
 * <p>Fields without any discovered {@code @RSQLFilterable} declaration are not
 * filterable and cause validation to fail when targeted by a query.</p>
 *
 * <p>This annotation also targets annotation types so the library's composed
 * annotations in this package can build on top of it.</p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code @RSQLFilterable({RSQLDefaultOperator.EQUAL, RSQLDefaultOperator.IN})
 * private String status;}</pre>
 *
 * <p><b>Example queries allowed by that declaration:</b></p>
 * <pre>{@code status==ACTIVE
 * status=in=(ACTIVE,PENDING)}</pre>
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
	 * Default built-in operators allowed for this field.
	 *
	 * <p>These operators are unioned with any operators contributed by
	 * {@link #customOperators()} and by repeated declarations on the same
	 * field.</p>
	 *
	 * @return an array of allowed {@link RSQLDefaultOperator} values
	 */
	RSQLDefaultOperator[] value() default {};

	/**
	 * Custom operators allowed for this field.
	 *
	 * <p>Referenced operator classes are looked up by implementation class in the
	 * configured custom-operator registry and are unioned with the operators from
	 * {@link #value()}.</p>
	 *
	 * @return an array of custom operator classes
	 */
	Class<? extends RSQLCustomOperator<?>>[] customOperators() default {};
}
