package in.co.akshitbansal.springwebquery.resolver;

import java.lang.reflect.Field;
import java.util.function.Consumer;

@FunctionalInterface
public interface FieldResolver {

    String resolvePathAndValidateTerminalField(String path, Consumer<Field> terminalFieldValidator);

    default String resolvePath(String path) {
        return resolvePathAndValidateTerminalField(path, field -> {}); // No-op validator
    }
}
