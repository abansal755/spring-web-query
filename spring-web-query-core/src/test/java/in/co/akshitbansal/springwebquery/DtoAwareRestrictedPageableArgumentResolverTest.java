package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.RestrictedPageable;
import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.resolver.DtoAwareRestrictedPageableArgumentResolver;
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

class DtoAwareRestrictedPageableArgumentResolverTest {

    private final DtoAwareRestrictedPageableArgumentResolver resolver = new DtoAwareRestrictedPageableArgumentResolver(
            new PageableHandlerMethodArgumentResolver(),
            new AnnotationUtil(Set.of())
    );

    @Test
    void supportsParameter_returnsTrueForDtoAwarePageable() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        assertTrue(resolver.supportsParameter(new MethodParameter(method, 0)));
    }

    @Test
    void supportsParameter_returnsFalseForEntityAwarePageable() throws Exception {
        Method method = TestController.class.getDeclaredMethod("entityAware", Pageable.class);
        assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
    }

    @Test
    void resolveArgument_mapsDtoSortToEntityPath() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        Pageable pageable = (Pageable) resolver.resolveArgument(new MethodParameter(method, 0), null, requestWithSort("joinedAt,desc"), null);
        assertEquals("createdAt", pageable.getSort().iterator().next().getProperty());
    }

    @Test
    void resolveArgument_rejectsUnknownDtoField() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);
        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWithSort("unknown,asc"),
                null
        ));
    }

    @Test
    void resolveArgument_rejectsWhenMappedEntityFieldMissing() throws Exception {
        Method method = TestController.class.getDeclaredMethod("invalidMapping", Pageable.class);
        assertThrows(QueryConfigurationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWithSort("joinedAt,asc"),
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

        @WebQuery(entityClass = Entity.class, dtoClass = QueryDto.class)
        void search(@RestrictedPageable Pageable pageable) {
        }

        @WebQuery(entityClass = Entity.class)
        void entityAware(@RestrictedPageable Pageable pageable) {
        }

        @WebQuery(entityClass = Entity.class, dtoClass = InvalidMappingDto.class)
        void invalidMapping(@RestrictedPageable Pageable pageable) {
        }
    }

    private static class Entity {
        @SuppressWarnings("unused")
        private String createdAt;
    }

    private static class QueryDto {
        @Sortable
        @MapsTo(field = "createdAt")
        private String joinedAt;
    }

    private static class InvalidMappingDto {
        @Sortable
        @MapsTo(field = "missing")
        private String joinedAt;
    }
}
