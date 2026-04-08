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

package in.co.akshitbansal.springwebquery.validator;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterables;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validator that enforces {@code @RSQLFilterable} constraints for a resolved
 * terminal field and requested comparison operator.
 *
 * <p>This validator supports direct, repeatable, and composed filterability
 * annotations and can resolve referenced custom operators from a prebuilt
 * registry.</p>
 */
@RequiredArgsConstructor
public class FilterableFieldValidator implements Validator<FilterableFieldValidator.FilterableField> {

	/**
	 * Registered custom operators keyed by their implementation class.
	 */
	private final Map<Class<?>, RSQLCustomOperator<?>> customOperators;

	/**
	 * Validates that a field is marked as filterable and that the requested
	 * operator is permitted by its {@link RSQLFilterable} declaration(s).
	 *
	 * @param filterableField field being targeted by the request selector
	 *
	 * @throws QueryFieldValidationException if the field is not filterable
	 * @throws QueryForbiddenOperatorException if the operator is not allowed for the field
	 */
	@Override
	public void validate(FilterableField filterableField) {
		Field field = filterableField.getField();
		ComparisonOperator operator = filterableField.getOperator();
		String fieldPath = filterableField.getFieldPath();

		// Retrieve the RSQLFilterable annotations on the field (if present)
		Set<RSQLFilterable> filterables = collectFilterables(field);
		// Throw exception if the field is not annotated as filterable
		if (filterables.isEmpty()) throw new QueryFieldValidationException(
				MessageFormat.format(
						"Filtering not allowed on field ''{0}''", fieldPath
				), fieldPath
		);

		// Throw exception if the provided operator is not in the allowed set
		Set<ComparisonOperator> allowedOperators = getAllowedOperators(filterables);
		if (!allowedOperators.contains(operator)) {
			throw new QueryForbiddenOperatorException(
					MessageFormat.format("Operator ''{0}'' not allowed on field ''{1}''", operator, fieldPath),
					fieldPath,
					operator,
					allowedOperators
			);
		}
	}

	/**
	 * Aggregates all allowed operators from one or more {@link RSQLFilterable}
	 * declarations attached to the same field.
	 *
	 * @param filterables repeatable filterability declarations
	 *
	 * @return deduplicated set of allowed comparison operators
	 *
	 * @throws QueryConfigurationException if a referenced custom operator is not registered
	 */
	private Set<ComparisonOperator> getAllowedOperators(Set<RSQLFilterable> filterables) {
		// Collect the set of allowed operators for this field from the annotations
		// Stream of default operators defined in the annotation
		Stream<ComparisonOperator> defaultOperators = filterables
				.stream()
				.flatMap(filterable -> Arrays.stream(filterable.value()))
				.map(RSQLDefaultOperator::getOperator);
		// Stream of custom operators defined in the annotation
		// Note: The annotation references classes, which are looked up in the customOperators map
		Stream<ComparisonOperator> customOperators = filterables
				.stream()
				.flatMap(filterable -> Arrays.stream(filterable.customOperators()))
				.map(this::getCustomOperator)
				.map(RSQLCustomOperator::getComparisonOperator);
		return Stream
				.concat(defaultOperators, customOperators)
				.collect(Collectors.toCollection(HashSet::new));
	}

	/**
	 * Retrieves the custom operator instance for the given operator class.
	 *
	 * @param clazz the custom operator class to look up
	 *
	 * @return the registered custom operator instance
	 *
	 * @throws QueryConfigurationException if the custom operator class is not registered
	 */
	private RSQLCustomOperator<?> getCustomOperator(Class<?> clazz) {
		RSQLCustomOperator<?> operator = customOperators.get(clazz);
		if (operator == null) throw new QueryConfigurationException(MessageFormat.format(
				"Custom operator ''{0}'' referenced in @RSQLFilterable is not registered", clazz.getSimpleName()
		));
		return operator;
	}

	/**
	 * Collects all {@link RSQLFilterable} declarations present on a field,
	 * including repeatable and composed annotations.
	 *
	 * @param field field whose annotations are to be inspected
	 *
	 * @return collected filterability declarations
	 */
	private Set<RSQLFilterable> collectFilterables(Field field) {
		Set<RSQLFilterable> filterables = new HashSet<>();
		collectFilterables(field.getAnnotations(), filterables);
		return Collections.unmodifiableSet(filterables);
	}

	/**
	 * Recursively collects {@link RSQLFilterable} declarations from a set of
	 * annotations, supporting both direct and meta-annotation usage.
	 *
	 * @param annotations annotations to inspect
	 * @param filterables accumulator to which discovered filterability declarations are added
	 */
	private void collectFilterables(Annotation[] annotations, Set<RSQLFilterable> filterables) {
		for (Annotation annotation: annotations) {
			Class<? extends Annotation> type = annotation.annotationType();
			if (annotation instanceof RSQLFilterable rsqlFilterable)
				filterables.add(rsqlFilterable);
			else if (annotation instanceof RSQLFilterables rsqlFilterables)
				collectFilterables(rsqlFilterables.value(), filterables);
			else if (type.getName().startsWith("in.co.akshitbansal.springwebquery.annotation"))
				collectFilterables(type.getAnnotations(), filterables);
		}
	}

	@RequiredArgsConstructor
	@Getter
	@EqualsAndHashCode
	@ToString
	public static class FilterableField {

		/**
		 * Reflected terminal field being validated.
		 */
		private final Field field;

		/**
		 * Comparison operator requested for the selector.
		 */
		private final ComparisonOperator operator;

		/**
		 * Original selector path from the incoming request.
		 */
		private final String fieldPath;
	}
}
