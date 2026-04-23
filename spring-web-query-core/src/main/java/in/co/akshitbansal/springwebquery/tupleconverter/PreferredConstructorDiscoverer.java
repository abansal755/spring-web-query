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

@RequiredArgsConstructor(staticName = "of")
class PreferredConstructorDiscoverer<T> {

	@NonNull
	private final Class<T> clazz;

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
			throw new IllegalArgumentException(MessageFormat.format(
					"No suitable constructor found for tuple: {0}",
					tupleToString(tuple)
			));
		}
		bestMatch.setAccessible(true);
		return bestMatch;
	}

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

	private boolean isParameterMatchingTupleElement(Parameter parameter, TupleElement<?> tupleElement) {
		Class<?> parameterType = wrap(parameter.getType());
		Class<?> tupleElementType = wrap(tupleElement.getJavaType());
		return parameterType.isAssignableFrom(tupleElementType);
	}

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

	private String tupleToString(Tuple tuple) {
		return tuple
				.getElements()
				.stream()
				.map(TupleElement::getJavaType)
				.map(Class::toString)
				.collect(Collectors.joining(", "));
	}
}
