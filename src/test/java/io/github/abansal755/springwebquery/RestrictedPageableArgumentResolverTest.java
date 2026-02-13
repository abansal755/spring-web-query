package io.github.abansal755.springwebquery;

import io.github.abansal755.springwebquery.annotation.RestrictedPageable;
import io.github.abansal755.springwebquery.annotation.Sortable;
import io.github.abansal755.springwebquery.exception.QueryException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void resolveArgument_rejectsNonSortableField() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest request = requestWithSort("secret,desc");

        assertThrows(QueryException.class, () -> resolver.resolveArgument(parameter, null, request, null));
    }

    private static NativeWebRequest requestWithSort(String sort) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("sort", sort);
        return new ServletWebRequest(request);
    }

    @SuppressWarnings("unused")
    private static class TestController {
        void search(@RestrictedPageable(entityClass = SortEntity.class) Pageable pageable) {
        }

        void searchWithoutAnnotation(Pageable pageable) {
        }
    }

    private static class SortEntity {
        @Sortable
        private String name;
        private String secret;
    }
}
