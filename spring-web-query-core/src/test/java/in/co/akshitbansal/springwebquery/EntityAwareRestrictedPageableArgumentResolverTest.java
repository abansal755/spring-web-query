package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RestrictedPageable;
import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.resolver.EntityAwareRestrictedPageableArgumentResolver;
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

import static org.junit.jupiter.api.Assertions.*;

class EntityAwareRestrictedPageableArgumentResolverTest {

    private final EntityAwareRestrictedPageableArgumentResolver resolver = new EntityAwareRestrictedPageableArgumentResolver(
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
    void resolveArgument_rejectsNonSortableField() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWithSort("secret,asc"),
                null
        ));
    }

    @Test
    void resolveArgument_rejectsWhenWebQueryMissing() throws Exception {
        Method method = TestController.class.getDeclaredMethod("missingWebQuery", Pageable.class);
        assertThrows(QueryConfigurationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWithSort("name,asc"),
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
        void search(@RestrictedPageable Pageable pageable) {
        }

        @WebQuery(entityClass = Entity.class, dtoClass = Object.class)
        void dtoAware(@RestrictedPageable Pageable pageable) {
        }

        @WebQuery(entityClass = Entity.class, fieldMappings = {
                @FieldMapping(name = "displayName", field = "name")
        })
        void searchWithMapping(@RestrictedPageable Pageable pageable) {
        }

        void missingWebQuery(@RestrictedPageable Pageable pageable) {
        }
    }

    private static class Entity {
        @Sortable
        private String name;

        private String secret;
    }
}
