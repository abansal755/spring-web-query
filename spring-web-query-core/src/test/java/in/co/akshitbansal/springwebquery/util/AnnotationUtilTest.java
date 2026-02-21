package in.co.akshitbansal.springwebquery.util;

import in.co.akshitbansal.springwebquery.annotation.RsqlSpec;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnnotationUtilTest {

    @Test
    void resolveWebQueryFromParameter_returnsAnnotation() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        WebQuery annotation = AnnotationUtil.resolveWebQueryFromParameter(parameter);

        assertEquals(TestEntity.class, annotation.entityClass());
    }

    @Test
    void resolveWebQueryFromParameter_throwsWhenMissing() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithoutWebQuery", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        assertThrows(QueryConfigurationException.class, () -> AnnotationUtil.resolveWebQueryFromParameter(parameter));
    }

    @SuppressWarnings("unused")
    private static class TestController {

        @WebQuery(entityClass = TestEntity.class)
        void search(@RsqlSpec Specification<TestEntity> specification) {
        }

        void searchWithoutWebQuery(@RsqlSpec Specification<TestEntity> specification) {
        }
    }

    private static class TestEntity {
    }
}
