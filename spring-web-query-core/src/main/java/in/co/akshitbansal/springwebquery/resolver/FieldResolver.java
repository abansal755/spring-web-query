package in.co.akshitbansal.springwebquery.resolver;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.reflect.Field;
import java.util.function.Consumer;

@FunctionalInterface
public interface FieldResolver {

    FieldResolverResult resolvePathAndGetTerminalField(String path);

    default String resolvePath(String path) {
        return resolvePathAndGetTerminalField(path).getResolvedPath();
    }

    default String resolvePathAndValidateTerminalField(String path, Consumer<Field> terminalFieldValidator) {
        FieldResolverResult result = resolvePathAndGetTerminalField(path);
        terminalFieldValidator.accept(result.getTerminalField());
        return result.getResolvedPath();
    }

    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    @ToString
    class FieldResolverResult {

        private final String resolvedPath;
        private final Field terminalField;
    }
}
