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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.text.MessageFormat;
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
 * <p>The implementation iterates over {@link Class#getDeclaredConstructors()},
 * keeps track of the latest suitable constructor it has seen, and stops early
 * if it encounters a suitable constructor annotated with
 * {@link PersistenceCreator}. Because Java reflection does not guarantee a
 * stable ordering for declared constructors, constructor selection is
 * unpredictable when multiple suitable constructors are present.</p>
 *
 * <p>To keep selection deterministic, DTOs should ideally expose only one
 * suitable constructor. If {@link PersistenceCreator} is used, it is strongly
 * recommended that at most one suitable constructor be annotated with it.</p>
 *
 * @param <T> target DTO type
 */
@RequiredArgsConstructor(staticName = "of")
class PreferredConstructorDiscoverer<T> {

	/**
	 * DTO type whose constructors are inspected.
	 */
	@NonNull
	private final Class<T> clazz;

	/**
	 * Finds a constructor whose parameter list is compatible with the supplied
	 * tuple and makes it accessible for later invocation.
	 *
	 * <p>Matching is performed position by position. For each tuple element, the
	 * constructor parameter at the same index must be assignable from the tuple
	 * element Java type, with primitive parameter types first converted to their
	 * boxed equivalents. Tuple aliases are ignored.</p>
	 *
	 * <p>If several suitable constructors exist, the outcome depends on the
	 * order returned by {@link Class#getDeclaredConstructors()}. A suitable
	 * constructor annotated with {@link PersistenceCreator} wins immediately when
	 * encountered. Otherwise, the last suitable constructor encountered is
	 * selected.</p>
	 *
	 * @param tuple tuple whose values will be passed to the constructor
	 *
	 * @return matching constructor made accessible for invocation
	 *
	 * @throws QueryConfigurationException if no suitable constructor can be
	 * found for the tuple shape
	 */
	public Constructor<T> discover(@NonNull Tuple tuple) {
		Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		Constructor<T> bestMatch = null;
		for (Constructor<?> rawConstructor: constructors) {
			// Constructors are of type Constructor<T> only, but the returned array is of type Constructor<?>[]
			// So we can safely cast here
			// noinspection unchecked
			Constructor<T> constructor = (Constructor<T>) rawConstructor;
			if (constructor.isSynthetic()) continue;
			if (!isConstructorMatchingTuple(constructor, tuple)) continue;
			bestMatch = constructor;
			if (constructor.isAnnotationPresent(PersistenceCreator.class))
				break;
		}
		if (bestMatch == null) {
			throw new QueryConfigurationException(MessageFormat.format(
					"No suitable constructor found for tuple: {0}",
					tupleToString(tuple)
			));
		}
		bestMatch.setAccessible(true);
		return bestMatch;
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
	private Class<?> wrap(Class<?> clazz) {
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
