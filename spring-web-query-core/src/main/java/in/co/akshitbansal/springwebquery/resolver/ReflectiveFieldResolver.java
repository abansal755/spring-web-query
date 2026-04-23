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

package in.co.akshitbansal.springwebquery.resolver;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor(staticName = "of")
public class ReflectiveFieldResolver {

	@NonNull
	private final Class<?> clazz;

	public List<Field> resolveFieldPath(String path) {
		if (path.isEmpty()) throw new IllegalArgumentException("Field path cannot be empty");
		String[] fieldNames = path.split("\\.");
		if (fieldNames.length == 0) throw new IllegalArgumentException("Field path cannot be empty");
		Class<?> current = clazz;
		List<Field> fieldPath = new ArrayList<>();
		for (String fieldName: fieldNames) {
			Field field = resolveFieldUpHierarchy(current, fieldName);
			fieldPath.add(field);
			current = unwrapContainerType(field);
		}
		return Collections.unmodifiableList(fieldPath);
	}

	/**
	 * Resolves a field by name from the given class or any of its superclasses.
	 *
	 * <p>This method attempts to find a declared field with the specified name
	 * in the given class. If the field is not found in the class itself, the
	 * search continues recursively up the inheritance hierarchy until a matching
	 * field is found or there are no more superclasses.</p>
	 *
	 * @param type the class to start searching for the field
	 * @param name the name of the field to resolve
	 *
	 * @return the {@link Field} object representing the resolved field
	 *
	 * @throws RuntimeException if no field with the specified name exists in the class
	 * or any of its superclasses
	 */
	private Field resolveFieldUpHierarchy(Class<?> type, String name) {
		Class<?> current = type;
		while (current != null) {
			try {
				return current.getDeclaredField(name);
			}
			catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new IllegalArgumentException(MessageFormat.format(
				"Field ''{0}'' not found in class hierarchy of {1}", name, type
		));
	}

	/**
	 * Determines the next traversal type for the given field by unwrapping
	 * container types.
	 * <p>
	 * The following unwrapping rules apply:
	 * <ul>
	 *   <li>If the field type is an array, the component type is returned.</li>
	 *   <li>If the field type is a {@link Collection}, the first generic type
	 *       argument is returned.</li>
	 *   <li>Otherwise, the field's declared type is returned as-is.</li>
	 * </ul>
	 *
	 * @param field the field whose type is to be unwrapped
	 *
	 * @return the type to be used for the next traversal step
	 *
	 * @throws UnsupportedOperationException if the collection element type
	 * cannot be determined
	 */
	private Class<?> unwrapContainerType(Field field) {
		Class<?> type = field.getType();
		if (type.isArray()) return type.getComponentType();
		if (Collection.class.isAssignableFrom(type)) return resolveGenericArgument(field, 0);
		return type;
	}

	/**
	 * Resolves a generic type argument from the given field at the specified index.
	 * <p>
	 * This method expects the field to declare a parameterized generic type
	 * (e.g. {@code List<String>} or {@code Map<String, Integer>}).
	 *
	 * @param field the field whose generic type arguments are to be resolved
	 * @param index the index of the desired generic argument
	 *
	 * @return the resolved generic argument as a {@link Class}
	 *
	 * @throws UnsupportedOperationException if the field does not declare
	 * parameterized generic information
	 */
	private Class<?> resolveGenericArgument(Field field, int index) {
		Type type = field.getGenericType();
		if (!(type instanceof ParameterizedType parameterizedType))
			throw new UnsupportedOperationException("Cannot resolve generic type for field: " + field.getName());
		Type arg = parameterizedType.getActualTypeArguments()[index];
		return toClass(arg);
	}

	/**
	 * Converts a {@link Type} into a concrete {@link Class} suitable for
	 * structural traversal.
	 * <p>
	 * Supported type forms:
	 * <ul>
	 *   <li>{@link Class}</li>
	 *   <li>{@link ParameterizedType} (raw type is returned)</li>
	 *   <li>{@link WildcardType} (upper bound is resolved recursively)</li>
	 * </ul>
	 *
	 * <p>
	 * Unsupported types (e.g. {@link java.lang.reflect.TypeVariable},
	 * {@link java.lang.reflect.GenericArrayType}) result in an exception,
	 * as they cannot be safely resolved without additional context.
	 *
	 * @param type the reflective type to convert
	 *
	 * @return the corresponding concrete {@link Class}
	 *
	 * @throws UnsupportedOperationException if the type cannot be safely converted
	 */
	private Class<?> toClass(Type type) {
		if (type instanceof Class<?>) return (Class<?>) type;
		if (type instanceof ParameterizedType parameterizedType) return (Class<?>) parameterizedType.getRawType();
		if (type instanceof WildcardType wt) return toClass(wt.getUpperBounds()[0]);
		throw new UnsupportedOperationException("Unsupported generic type: " + type);
	}
}
