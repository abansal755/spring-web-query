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

package in.co.akshitbansal.springwebquery.util;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import lombok.NonNull;
import org.springframework.data.annotation.PersistenceCreator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Discovers the constructor that should be used to convert a projected JPA {@link Tuple} into a DTO instance.
 *
 * <p>Compatibility is determined by constructor parameter order, parameter count, and runtime type assignability for
 * each tuple element. Primitive constructor parameter types are normalized to their wrapper types before
 * compatibility checks are performed.</p>
 *
 * <p>When multiple constructors are compatible, a constructor annotated with
 * {@link org.springframework.data.annotation.PersistenceCreator} is preferred. Callers should keep at most one such
 * annotated constructor. If multiple annotated constructors are present, or if multiple non-annotated constructors
 * are compatible, constructor selection is not guaranteed to be stable and behavior is unpredictable.</p>
 */
public class PreferredConstructorDiscoveryUtil {

	/**
	 * Finds a compatible constructor for the given tuple-backed DTO projection.
	 *
	 * <p>The selected constructor must declare the same number of parameters as the tuple contains elements, and each
	 * parameter must be assignable from the corresponding tuple element runtime Java type after primitive-wrapper
	 * normalization. Compatible constructors are evaluated in declared-constructor order, with a compatible constructor
	 * annotated with {@link PersistenceCreator} taking precedence over any previously found compatible constructor.</p>
	 *
	 * <p>If no compatible constructor exists, this method throws an {@link IllegalArgumentException}. If multiple
	 * compatible constructors exist without a uniquely determining preference, the returned constructor depends on the
	 * current implementation details of discovery and should not be treated as stable API behavior.</p>
	 *
	 * @param clazz target DTO type whose constructors should be inspected
	 * @param tuple tuple whose projected element count and runtime types drive compatibility checks
	 *
	 * @return the compatible constructor selected for tuple-backed instantiation
	 *
	 * @throws IllegalArgumentException if no suitable constructor can be found
	 */
	public static Constructor<?> discover(@NonNull Class<?> clazz, @NonNull Tuple tuple) {
		Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		Constructor<?> bestMatch = null;
		for(Constructor<?> constructor: constructors) {
			if(constructor.isSynthetic()) continue;
			if(!isConstructorMatchingTuple(constructor, tuple)) continue;
			bestMatch = constructor;
			if(constructor.isAnnotationPresent(PersistenceCreator.class))
				break;
		}
		if(bestMatch == null) {
			throw new IllegalArgumentException(MessageFormat.format(
					"No suitable constructor found for tuple: {0}",
					tupleToString(tuple)
			));
		}
		bestMatch.setAccessible(true);
		return bestMatch;
	}

	private static boolean isConstructorMatchingTuple(Constructor<?> constructor, Tuple tuple) {
		Parameter[] parameters = constructor.getParameters();
		List<TupleElement<?>> tupleElements = tuple.getElements();
		if(parameters.length != tupleElements.size()) return false;
		for(int idx = 0; idx < parameters.length; idx++) {
			if(!isParameterMatchingTupleElement(parameters[idx], tupleElements.get(idx)))
				return false;
		}
		return true;
	}

	private static boolean isParameterMatchingTupleElement(Parameter parameter, TupleElement<?> tupleElement) {
		Class<?> parameterType = wrap(parameter.getType());
		Class<?> tupleElementType = wrap(tupleElement.getJavaType());
		return parameterType.isAssignableFrom(tupleElementType);
	}

	private static Class<?> wrap(Class<?> clazz) {
		if(!clazz.isPrimitive()) return clazz;
		if(clazz == int.class) return Integer.class;
		if(clazz == long.class) return Long.class;
		if(clazz == double.class) return Double.class;
		if(clazz == float.class) return Float.class;
		if(clazz == boolean.class) return Boolean.class;
		if(clazz == byte.class) return Byte.class;
		if(clazz == short.class) return Short.class;
		if(clazz == char.class) return Character.class;
		throw new IllegalArgumentException("Unsupported primitive type: " + clazz);
	}

	private static String tupleToString(Tuple tuple) {
		return tuple
				.getElements()
				.stream()
				.map(TupleElement::getJavaType)
				.map(Class::toString)
				.collect(Collectors.joining(", "));
	}
}
