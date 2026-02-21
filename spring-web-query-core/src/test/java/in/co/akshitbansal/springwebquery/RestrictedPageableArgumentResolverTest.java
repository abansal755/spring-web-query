package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RestrictedPageable;
import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class RestrictedPageableArgumentResolverTest {

    private final RestrictedPageableArgumentResolver resolver =
            new RestrictedPageableArgumentResolver(new PageableHandlerMethodArgumentResolver());

    @Test
    void supportsParameter_returnsTrueForRestrictedPageable() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        assertTrue(resolver.supportsParameter(parameter));
    }

    @Test
    void supportsParameter_returnsFalseForPlainPageable() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithoutAnnotation", Pageable.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        assertFalse(resolver.supportsParameter(parameter));
    }

    @Test
    void resolveArgument_allowsSortableField() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest request = requestWithSort("name,asc");

        Pageable pageable = resolver.resolveArgument(parameter, null, request, null);
        assertEquals("name", pageable.getSort().iterator().next().getProperty());
    }

    @Test
    void resolveArgument_rewritesMappedAliasField() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithMapping", Pageable.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest request = requestWithSort("displayName,asc");

        Pageable pageable = resolver.resolveArgument(parameter, null, request, null);
        assertEquals("name", pageable.getSort().iterator().next().getProperty());
    }

    @Test
    void resolveArgument_rejectsNonSortableField() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest request = requestWithSort("secret,desc");

        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(parameter, null, request, null));
    }

    @Test
    void resolveArgument_rejectsOriginalMappedFieldWhenNotAllowed() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithMapping", Pageable.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest request = requestWithSort("name,asc");

        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(parameter, null, request, null));
    }

    @Test
    void resolveArgument_allowsOriginalMappedFieldWhenAllowed() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithMappingAllowOriginal", Pageable.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest request = requestWithSort("name,asc");

        Pageable pageable = resolver.resolveArgument(parameter, null, request, null);
        assertEquals("name", pageable.getSort().iterator().next().getProperty());
    }

    @Test
    void resolveArgument_rejectsWhenWebQueryMissing() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithoutWebQuery", Pageable.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest request = requestWithSort("name,asc");

        assertThrows(QueryConfigurationException.class, () -> resolver.resolveArgument(parameter, null, request, null));
    }

    private static NativeWebRequest requestWithSort(String sort) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("sort", sort);
        return new ServletWebRequest(request);
    }

    @SuppressWarnings("unused")
    private static class TestController {

        @WebQuery(entityClass = SortEntity.class)
        void search(@RestrictedPageable Pageable pageable) {
        }

        void searchWithoutAnnotation(Pageable pageable) {
        }

        @WebQuery(
                entityClass = SortEntity.class,
                fieldMappings = {@FieldMapping(name = "displayName", field = "name")}
        )
        void searchWithMapping(@RestrictedPageable Pageable pageable) {
        }

        @WebQuery(
                entityClass = SortEntity.class,
                fieldMappings = {@FieldMapping(name = "displayName", field = "name", allowOriginalFieldName = true)}
        )
        void searchWithMappingAllowOriginal(@RestrictedPageable Pageable pageable) {
        }

        void searchWithoutWebQuery(@RestrictedPageable Pageable pageable) {
        }
    }

    private static class SortEntity {

        @Sortable
        private String name;

        private String secret;
    }
}
