package in.co.akshitbansal.springwebquery.util;

import in.co.akshitbansal.springwebquery.exception.QueryException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectionUtilTest {

    @Test
    void resolveField_resolvesFromSuperclass() {
        Field field = ReflectionUtil.resolveField(ChildEntity.class, "id");
        assertEquals("id", field.getName());
        assertEquals(BaseEntity.class, field.getDeclaringClass());
    }

    @Test
    void resolveField_resolvesNestedFieldThroughCollection() {
        Field field = ReflectionUtil.resolveField(ParentEntity.class, "children.name");
        assertEquals("name", field.getName());
        assertEquals(NestedEntity.class, field.getDeclaringClass());
    }

    @Test
    void resolveField_resolvesNestedFieldThroughArray() {
        Field field = ReflectionUtil.resolveField(ArrayParentEntity.class, "children.name");
        assertEquals("name", field.getName());
        assertEquals(NestedEntity.class, field.getDeclaringClass());
    }

    @Test
    void resolveField_throwsForUnknownSegment() {
        QueryException ex = assertThrows(
                QueryException.class,
                () -> ReflectionUtil.resolveField(ParentEntity.class, "children.unknown")
        );
        assertEquals("Unknown field 'unknown'", ex.getMessage());
    }

    private static class BaseEntity {
        private String id;
    }

    private static class ChildEntity extends BaseEntity {
        private String value;
    }

    private static class ParentEntity {
        private List<NestedEntity> children;
    }

    private static class ArrayParentEntity {
        private NestedEntity[] children;
    }

    private static class NestedEntity {
        private String name;
    }
}
