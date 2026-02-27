package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.RsqlSpec;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import in.co.akshitbansal.springwebquery.resolver.DtoAwareRsqlSpecArgumentResolver;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DtoAwareRsqlSpecArgumentResolverTest {

    private final DtoAwareRsqlSpecArgumentResolver resolver = new DtoAwareRsqlSpecArgumentResolver(
            Set.of(RsqlOperator.values()),
            Set.of(),
            new AnnotationUtil(Set.of())
    );

    @Test
    void supportsParameter_returnsTrueForDtoAwareMethod() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        assertTrue(resolver.supportsParameter(new MethodParameter(method, 0)));
    }

    @Test
    void supportsParameter_returnsFalseForEntityOnlyMethod() throws Exception {
        Method method = TestController.class.getDeclaredMethod("entityOnly", Specification.class);
        assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
    }

    @Test
    void resolveArgument_acceptsMappedDtoField() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("filter", "joinedAt==x"), null);
    }

    @Test
    void resolveArgument_rejectsUnknownDtoField() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWith("filter", "unknown==x"),
                null
        ));
    }

    @Test
    void resolveArgument_usesCustomParamName() throws Exception {
        Method method = TestController.class.getDeclaredMethod("searchWithCustomParam", Specification.class);
        resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("q", "joinedAt==x"), null);
    }

    @Test
    void resolveArgument_rejectsWhenWebQueryMissing() throws Exception {
        Method method = TestController.class.getDeclaredMethod("missingWebQuery", Specification.class);
        assertThrows(QueryConfigurationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWith("filter", "joinedAt==x"),
                null
        ));
    }

    private NativeWebRequest requestWith(String key, String value) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter(key, value);
        return new ServletWebRequest(req);
    }

    @SuppressWarnings("unused")
    private static class TestController {

        @WebQuery(entityClass = Entity.class, dtoClass = QueryDto.class)
        void search(@RsqlSpec Specification<Entity> spec) {
        }

        @WebQuery(entityClass = Entity.class)
        void entityOnly(@RsqlSpec Specification<Entity> spec) {
        }

        @WebQuery(entityClass = Entity.class, dtoClass = QueryDto.class)
        void searchWithCustomParam(@RsqlSpec(paramName = "q") Specification<Entity> spec) {
        }

        void missingWebQuery(@RsqlSpec Specification<Entity> spec) {
        }
    }

    private static class Entity {
        @SuppressWarnings("unused")
        private String createdAt;
    }

    private static class QueryDto {
        @RsqlFilterable(operators = {RsqlOperator.EQUAL})
        @MapsTo(field = "createdAt")
        private String joinedAt;
    }
}
