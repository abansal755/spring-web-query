package in.co.akshitbansal.springwebquery.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for performing reflection-based operations on entity classes.
 * <p>
 * This class provides methods for resolving fields from dot-separated paths,
 * handling inheritance hierarchies, and unwrapping container types (arrays and collections).
 * </p>
 */
public class ReflectionUtil {

    /**
     * Resolves a {@link Field} for the given dot-separated field path, starting from
     * the supplied root type and traversing the type hierarchy and container types
     * as needed.
     * <p>
     * The resolution rules are:
     * <ul>
     *   <li>Each path segment is resolved using {@code getDeclaredField} while
     *       walking up the superclass hierarchy.</li>
     *   <li>If a resolved field is an array, traversal continues with its component type.</li>
     *   <li>If a resolved field is a {@link Collection}, traversal continues with the
     *       collection's generic element type.</li>
     *   <li>Only the last field in the path is returned.</li>
     * </ul>
     *
     * <p>
     * Examples:
     * <pre>{@code
     * resolveField(User.class, "name.first");
     * resolveField(Order.class, "items.product.id");
     * resolveField(Foo.class, "values"); // List<List<String>> resolves to List
     * }</pre>
     *
     * <p>
     * This method performs <strong>structural type resolution only</strong>. It does not
     * access or inspect runtime values.
     *
     * @param type the root class from which resolution starts
     * @param name a dot-separated field path (e.g. {@code "a.b.c"})
     * @return the {@link Field} corresponding to the last segment in the path
     * @throws RuntimeException if any segment of the path cannot be resolved
     * @throws UnsupportedOperationException if an intermediate collection type
     *         does not expose resolvable generic information
     */
    public static Field resolveField(Class<?> type, String name) {
        String[] fieldNames = name.split("\\.");
        Class<?> current = type;
        Field field = null;
        for(String fieldName : fieldNames) {
            field = resolveFieldUpHierarchy(current, fieldName);
            current = unwrapContainerType(field);
        }
        return field;
    }

    public static List<Field> resolveFieldPath(Class<?> type, String name) {
        String[] fieldNames = name.split("\\.");
        Class<?> current = type;
        List<Field> path = new ArrayList<>();
        for(String fieldName : fieldNames) {
            Field field = resolveFieldUpHierarchy(current, fieldName);
            path.add(field);
            current = unwrapContainerType(field);
        }
        return path;
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
     * @return the {@link Field} object representing the resolved field
     * @throws RuntimeException if no field with the specified name exists in the class
     *                        or any of its superclasses
     */
    private static Field resolveFieldUpHierarchy(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            }
            catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException(MessageFormat.format(
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
     * @return the type to be used for the next traversal step
     * @throws UnsupportedOperationException if the collection element type
     *         cannot be determined
     */
    private static Class<?> unwrapContainerType(Field field) {
        Class<?> type = field.getType();
        if(type.isArray()) return type.getComponentType();
        if(Collection.class.isAssignableFrom(type)) return resolveGenericArgument(field, 0);
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
     * @return the resolved generic argument as a {@link Class}
     * @throws UnsupportedOperationException if the field does not declare
     *         parameterized generic information
     */
    private static Class<?> resolveGenericArgument(Field field, int index) {
        Type type = field.getGenericType();
        if(type instanceof ParameterizedType parameterizedType) {
            Type arg = parameterizedType.getActualTypeArguments()[index];
            return toClass(arg);
        }
        throw new UnsupportedOperationException("Cannot resolve generic type for field: " + field.getName());
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
     * @return the corresponding concrete {@link Class}
     * @throws UnsupportedOperationException if the type cannot be safely converted
     */
    private static Class<?> toClass(Type type) {
        if(type instanceof Class<?>) return (Class<?>) type;
        if(type instanceof ParameterizedType parameterizedType) return (Class<?>) parameterizedType.getRawType();
        if (type instanceof WildcardType wt) return toClass(wt.getUpperBounds()[0]);
        throw new UnsupportedOperationException("Unsupported generic type: " + type);
    }
}
