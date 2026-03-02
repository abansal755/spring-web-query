package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.resolver.WebQueryEntityAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebQueryEntityAwarePageableArgumentResolverTest {

    private final WebQueryEntityAwarePageableArgumentResolver resolver = new WebQueryEntityAwarePageableArgumentResolver(
            new PageableHandlerMethodArgumentResolver(),
            new AnnotationUtil(Set.of())
    );

    @Test
    void supportsParameter_returnsTrueForEntityAwarePageable() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        assertTrue(resolver.supportsParameter(new MethodParameter(method, 0)));
    }

    @Test
    void supportsParameter_returnsFalseForDtoAwarePageable() throws Exception {
        Method method = TestController.class.getDeclaredMethod("dtoAware", Pageable.class);
        assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
    }

    @Test
    void supportsParameter_returnsFalseWhenWebQueryMissing() throws Exception {
        Method method = TestController.class.getDeclaredMethod("missingWebQuery", Pageable.class);
        assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
    }

    @Test
    void resolveArgument_allowsSortableField() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        Pageable pageable = (Pageable) resolver.resolveArgument(new MethodParameter(method, 0), null, requestWithSort("name,asc"), null);
        assertEquals("name", pageable.getSort().iterator().next().getProperty());
    }

    @Test
    void resolveArgument_mapsAliasToEntityField() throws Exception {
        Method method = TestController.class.getDeclaredMethod("searchWithMapping", Pageable.class);
        Pageable pageable = (Pageable) resolver.resolveArgument(new MethodParameter(method, 0), null, requestWithSort("displayName,asc"), null);
        assertEquals("name", pageable.getSort().iterator().next().getProperty());
    }

    @Test
    void resolveArgument_rejectsOriginalMappedFieldWhenDisallowed() throws Exception {
        Method method = TestController.class.getDeclaredMethod("searchWithMapping", Pageable.class);
        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWithSort("name,asc"),
                null
        ));
    }

    @Test
    void resolveArgument_rejectsNonSortableField() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWithSort("secret,asc"),
                null
        ));
    }

    private NativeWebRequest requestWithSort(String sort) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("sort", sort);
        return new ServletWebRequest(req);
    }

    @SuppressWarnings("unused")
    private static class TestController {

        @WebQuery(entityClass = Entity.class)
        void search(Pageable pageable) {
        }

        @WebQuery(entityClass = Entity.class, dtoClass = Object.class)
        void dtoAware(Pageable pageable) {
        }

        @WebQuery(entityClass = Entity.class, fieldMappings = {
                @FieldMapping(name = "displayName", field = "name", allowOriginalFieldName = false)
        })
        void searchWithMapping(Pageable pageable) {
        }

        void missingWebQuery(Pageable pageable) {
        }
    }

    private static class Entity {
        @Sortable
        private String name;

        private String secret;
    }
}
