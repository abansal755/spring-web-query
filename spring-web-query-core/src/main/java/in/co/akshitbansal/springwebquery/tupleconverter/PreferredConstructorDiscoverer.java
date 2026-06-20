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

package in.co.akshitbansal.springwebquery.tupleconverter;

import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Locates a constructor that can materialize a DTO from a JPA {@link Tuple}.
 *
 * <p>Constructor discovery is based entirely on the tuple's positional shape.
 * Parameter names, tuple aliases, and property names are not consulted.</p>
 *
 * <p>A constructor is considered suitable when all of the following are true:</p>
 * <ul>
 *   <li>it is not synthetic</li>
 *   <li>its parameter count exactly matches the tuple element count</li>
 *   <li>for each position, the constructor parameter type is assignable from
 *       the tuple element Java type after primitive types are boxed</li>
 * </ul>
 *
 * <p>The implementation inspects declared constructors, so public and
 * non-public constructors are eligible. Constructor selection is deterministic:
 * a single suitable constructor is selected directly, multiple suitable
 * constructors must be disambiguated by annotating exactly one of the matching
 * constructors with {@link PersistenceCreator}. Discovery throws a
 * {@link QueryConfigurationException} when ambiguity remains unresolved,
 * including when multiple matching constructors are annotated.</p>
 *
 * @param <T> target DTO type
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PreferredConstructorDiscoverer<T> {

	/**
	 * DTO type whose constructors are inspected.
	 */
	@NonNull
	protected final Class<T> clazz;

	/**
	 * Returns the DTO type whose constructors this discoverer inspects.
	 *
	 * @return target DTO type
	 */
	public Class<T> getTargetClass() {
		return clazz;
	}

	/**
	 * Finds a constructor whose parameter list is compatible with the supplied
	 * tuple and makes it accessible for later invocation.
	 *
	 * <p>Matching is performed position by position. For each tuple element, the
	 * constructor parameter at the same index must be assignable from the tuple
	 * element Java type, with primitive parameter types first converted to their
	 * boxed equivalents. Tuple aliases are ignored.</p>
	 *
	 * <p>Public and non-public declared constructors are considered. If exactly
	 * one suitable constructor exists, it is selected. If several suitable
	 * constructors exist, exactly one matching constructor must be annotated with
	 * {@link PersistenceCreator}. If none or more than one matching constructor
	 * is annotated, discovery throws a {@link QueryConfigurationException}.</p>
	 *
	 * @param tuple tuple whose values will be passed to the constructor
	 *
	 * @return matching constructor made accessible for invocation
	 *
	 * @throws QueryConfigurationException if no suitable constructor can be
	 * found for the tuple shape, if multiple suitable constructors cannot be
	 * disambiguated, or if multiple matching constructors are annotated with
	 * {@link PersistenceCreator}
	 */
	public Constructor<T> discover(@NonNull Tuple tuple) {
		// Constructors are of type Constructor<T> only, but the returned array is of type Constructor<?>[]
		// So we can safely cast here
		// noinspection unchecked
		Constructor<T>[] constructors = (Constructor<T>[]) clazz.getDeclaredConstructors();
		List<Constructor<T>> matchingConstructors = Arrays
				.stream(constructors)
				.filter(constructor -> !constructor.isSynthetic() && isConstructorMatchingTuple(constructor, tuple))
				.toList();
		List<Constructor<T>> annotatedMatchingConstructors = matchingConstructors
				.stream()
				.filter(constructor -> constructor.isAnnotationPresent(PersistenceCreator.class))
				.toList();

		// no matching constructors found
		if (matchingConstructors.isEmpty()) {
			throw new QueryConfigurationException(MessageFormat.format(
					"No suitable constructor found for tuple: {0}",
					tupleToString(tuple)
			));
		}
		// matching constructors found

		// only one matching constructor found
		if (matchingConstructors.size() == 1) {
			Constructor<T> constructor = matchingConstructors.get(0);
			constructor.setAccessible(true);
			return constructor;
		}
		// multiple matching constructors found

		// no annotated matching constructors found
		if (annotatedMatchingConstructors.isEmpty()) {
			throw new QueryConfigurationException(MessageFormat.format(
					"Multiple suitable constructors found for tuple: {0}, consider annotating the desired constructor to be used with {1}",
					tupleToString(tuple), PersistenceCreator.class.getName()
			));
		}

		// only one annotated matching constructor found
		if (annotatedMatchingConstructors.size() == 1) {
			Constructor<T> constructor = annotatedMatchingConstructors.get(0);
			constructor.setAccessible(true);
			return constructor;
		}
		// multiple annotated matching constructors found

		throw new QueryConfigurationException(MessageFormat.format(
				"Multiple suitable constructors annotated with {0} found for tuple: {1}, consider annotating only the desired constructor to be used with {0}",
				PersistenceCreator.class.getName(), tupleToString(tuple)
		));
	}

	/**
	 * Checks whether a constructor matches the tuple by parameter count and
	 * positionally aligned parameter types.
	 */
	private boolean isConstructorMatchingTuple(Constructor<?> constructor, Tuple tuple) {
		Parameter[] parameters = constructor.getParameters();
		List<TupleElement<?>> tupleElements = tuple.getElements();
		if (parameters.length != tupleElements.size()) return false;
		for (int idx = 0; idx < parameters.length; idx++) {
			if (!isParameterMatchingTupleElement(parameters[idx], tupleElements.get(idx)))
				return false;
		}
		return true;
	}

	/**
	 * Checks whether one constructor parameter can accept the tuple element at
	 * the same position after primitive boxing.
	 */
	private boolean isParameterMatchingTupleElement(Parameter parameter, TupleElement<?> tupleElement) {
		Class<?> parameterType = wrap(parameter.getType());
		Class<?> tupleElementType = wrap(tupleElement.getJavaType());
		return parameterType.isAssignableFrom(tupleElementType);
	}

	/**
	 * Converts primitive types to their boxed equivalents before assignability
	 * checks are performed.
	 */
	protected Class<?> wrap(Class<?> clazz) {
		if (!clazz.isPrimitive()) return clazz;
		if (clazz == int.class) return Integer.class;
		if (clazz == long.class) return Long.class;
		if (clazz == double.class) return Double.class;
		if (clazz == float.class) return Float.class;
		if (clazz == boolean.class) return Boolean.class;
		if (clazz == byte.class) return Byte.class;
		if (clazz == short.class) return Short.class;
		if (clazz == char.class) return Character.class;
		throw new IllegalArgumentException("Unsupported primitive type: " + clazz);
	}

	/**
	 * Renders tuple element types for error reporting.
	 */
	private String tupleToString(Tuple tuple) {
		return tuple
				.getElements()
				.stream()
				.map(TupleElement::getJavaType)
				.map(Class::toString)
				.collect(Collectors.joining(", "));
	}
}
