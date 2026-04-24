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

/**
 * Resolves dotted field paths against a root class using reflection.
 *
 * <p>The resolver is used for structural path traversal rather than direct
 * value access. Given a path such as {@code profile.address.city}, it resolves
 * each segment in order and returns the corresponding {@link Field} objects as
 * an immutable list.</p>
 *
 * <p>Resolution is performed segment by segment starting from the configured
 * root class. For each segment, the resolver:</p>
 * <ul>
 *   <li>looks for a declared field on the current class</li>
 *   <li>if not found, continues searching up the superclass hierarchy</li>
 *   <li>uses the resolved field's type as the basis for the next segment</li>
 * </ul>
 *
 * <p>Container-like field types are unwrapped before the next segment is
 * resolved:</p>
 * <ul>
 *   <li>array types resolve to their component type</li>
 *   <li>{@link Collection} types resolve to their first generic type argument</li>
 *   <li>all other types are used as declared</li>
 * </ul>
 *
 * <p>The resolver works with declared fields, including non-public ones, but it
 * does not make them accessible. It also searches only the class/superclass
 * hierarchy; interfaces are not inspected for fields.</p>
 *
 * <p>No special path syntax is supported beyond dot-separated field names.
 * Empty or unresolvable segments fail immediately.</p>
 */
@RequiredArgsConstructor(staticName = "of")
public class ReflectiveFieldResolver {

	/**
	 * Root class against which field-path resolution starts.
	 */
	@NonNull
	private final Class<?> clazz;

	/**
	 * Resolves a dotted field path from the configured root class.
	 *
	 * <p>The path is split on dots and processed left to right. Each resolved
	 * field becomes the structural context for the next segment after array and
	 * collection unwrapping has been applied. For example, when resolving
	 * {@code accounts.portfolios.code}, if {@code accounts} is a
	 * {@code List<Account>}, the next segment is resolved against {@code Account}
	 * rather than {@code List}.</p>
	 *
	 * <p>The returned list contains one {@link Field} per path segment in the
	 * same order as the input path. The final element therefore represents the
	 * terminal field in the path.</p>
	 *
	 * @param path dotted field path to resolve
	 *
	 * @return immutable list of fields representing the resolved path
	 *
	 * @throws IllegalArgumentException if the path is empty or any segment
	 * cannot be resolved in the current structural context
	 * @throws UnsupportedOperationException if traversal reaches a collection
	 * whose element type cannot be resolved reflectively
	 */
	public List<Field> resolveFieldPath(@NonNull String path) {
		if (path.isEmpty()) throw new IllegalArgumentException("Field path cannot be empty");
		String[] fieldNames = path.split("\\.", -1);
		Class<?> current = clazz;
		List<Field> fieldPath = new ArrayList<>();
		for (String fieldName: fieldNames) {
			if (fieldName.isEmpty()) throw new IllegalArgumentException("Field path cannot contain empty segments");
			Field field = resolveFieldUpHierarchy(current, fieldName);
			fieldPath.add(field);
			current = unwrapContainerType(field);
		}
		return Collections.unmodifiableList(fieldPath);
	}

	/**
	 * Resolves one field name against a class and its superclasses.
	 *
	 * <p>The search starts at {@code type} and walks upward using
	 * {@link Class#getSuperclass()}. Because the lookup is based on
	 * {@link Class#getDeclaredField(String)}, non-public fields are eligible, and
	 * a field declared on a subclass takes precedence over a field with the same
	 * name declared higher in the hierarchy.</p>
	 *
	 * @param type the class to start searching for the field
	 * @param name the field name to resolve
	 *
	 * @return the resolved field
	 *
	 * @throws IllegalArgumentException if no declared field with the given name
	 * exists in the class hierarchy
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
	 * Determines which type should be used to resolve the next path segment.
	 *
	 * <p>Arrays are traversed through their component type. Collections are
	 * traversed through their first declared generic type argument. All other
	 * fields are traversed through their declared raw type.</p>
	 *
	 * <p>This means nested collection traversal relies on reflective generic type
	 * information being present on the field declaration. Raw collections, type
	 * variables, and other non-concrete generic forms may therefore fail to
	 * unwrap.</p>
	 *
	 * @param field the field whose type is to be unwrapped
	 *
	 * @return the type to use for the next traversal step
	 *
	 * @throws UnsupportedOperationException if the next traversal type cannot be
	 * resolved from the field declaration
	 */
	private Class<?> unwrapContainerType(Field field) {
		Class<?> type = field.getType();
		if (type.isArray()) return type.getComponentType();
		if (Collection.class.isAssignableFrom(type)) return resolveGenericArgument(field, 0);
		return type;
	}

	/**
	 * Resolves one generic type argument declared on a field.
	 *
	 * <p>This helper expects the field to expose parameterized generic
	 * information such as {@code List<Address>}. The selected type argument is
	 * then normalized into a concrete {@link Class} via {@link #toClass(Type)}.
	 * In the current resolver flow this is used only with index {@code 0} for
	 * collection element traversal.</p>
	 *
	 * @param field the field whose generic type arguments are to be resolved
	 * @param index the index of the desired generic argument
	 *
	 * @return the resolved generic argument as a class
	 *
	 * @throws UnsupportedOperationException if the field does not declare
	 * parameterized generic information or the selected argument cannot be
	 * converted into a concrete class
	 */
	private Class<?> resolveGenericArgument(Field field, int index) {
		Type type = field.getGenericType();
		if (!(type instanceof ParameterizedType parameterizedType))
			throw new UnsupportedOperationException("Cannot resolve generic type for field: " + field.getName());
		Type arg = parameterizedType.getActualTypeArguments()[index];
		return toClass(arg);
	}

	/**
	 * Converts a reflective {@link Type} into a concrete {@link Class} that can
	 * be used for subsequent path traversal.
	 *
	 * <p>The conversion rules are intentionally conservative:</p>
	 * <ul>
	 *   <li>a plain {@link Class} is returned as-is</li>
	 *   <li>a {@link ParameterizedType} resolves to its raw type</li>
	 *   <li>a {@link WildcardType} resolves recursively through its first upper
	 *       bound</li>
	 * </ul>
	 *
	 * <p>Other reflective forms such as type variables and generic array types
	 * are rejected because the resolver cannot safely determine a concrete class
	 * for structural navigation without additional type context.</p>
	 *
	 * @param type the reflective type to convert
	 *
	 * @return the concrete class used for traversal
	 *
	 * @throws UnsupportedOperationException if the type cannot be converted into
	 * a concrete traversal class
	 */
	private Class<?> toClass(Type type) {
		if (type instanceof Class<?>) return (Class<?>) type;
		if (type instanceof ParameterizedType parameterizedType) return (Class<?>) parameterizedType.getRawType();
		if (type instanceof WildcardType wt) return toClass(wt.getUpperBounds()[0]);
		throw new UnsupportedOperationException("Unsupported generic type: " + type);
	}
}
