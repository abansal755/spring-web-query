package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.resolver.WebQueryEntityAwareSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebQueryEntityAwareSpecificationArgumentResolverTest {

    private final WebQueryEntityAwareSpecificationArgumentResolver resolver = new WebQueryEntityAwareSpecificationArgumentResolver(
            Set.of(RSQLDefaultOperator.values()),
            Set.of(new MockCustomOperator()),
            new AnnotationUtil(Set.of(new MockCustomOperator())),
            true,
            false,
            1
    );

    @Test
    void supportsParameter_returnsTrueForEntityAwareMethod() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        assertTrue(resolver.supportsParameter(new MethodParameter(method, 0)));
    }

    @Test
    void supportsParameter_returnsFalseForDtoAwareMethod() throws Exception {
        Method method = TestController.class.getDeclaredMethod("dtoSearch", Specification.class);
        assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
    }

    @Test
    void supportsParameter_returnsFalseWhenWebQueryMissing() throws Exception {
        Method method = TestController.class.getDeclaredMethod("missingWebQuery", Specification.class);
        assertFalse(resolver.supportsParameter(new MethodParameter(method, 0)));
    }

    @Test
    void resolveArgument_returnsUnrestrictedWhenFilterMissing() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        Object spec = resolver.resolveArgument(new MethodParameter(method, 0), null, new ServletWebRequest(new MockHttpServletRequest()), null);
        assertNotNull(spec);
    }

    @Test
    void resolveArgument_acceptsAliasField() throws Exception {
        Method method = TestController.class.getDeclaredMethod("searchWithMapping", Specification.class);
        resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("filter", "displayName==john"), null);
    }

    @Test
    void resolveArgument_rejectsOriginalMappedFieldWhenNotAllowed() throws Exception {
        Method method = TestController.class.getDeclaredMethod("searchWithMapping", Specification.class);
        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWith("filter", "name==john"),
                null
        ));
    }

    @Test
    void resolveArgument_allowsCustomOperator() throws Exception {
        Method method = TestController.class.getDeclaredMethod("searchWithCustom", Specification.class);
        resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("filter", "name=mock=value"), null);
    }

    @Test
    void resolveArgument_usesWebQueryFilterParamName() throws Exception {
        Method method = TestController.class.getDeclaredMethod("searchWithCustomParam", Specification.class);
        resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("q", "name==john"), null);
    }

    @Test
    void resolveArgument_rejectsMalformedFilter() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWith("filter", "name=="),
                null
        ));
    }

    @Test
    void resolveArgument_returnsUnrestrictedWhenFilterBlank() throws Exception {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        Object spec = resolver.resolveArgument(new MethodParameter(method, 0), null, requestWith("filter", "   "), null);
        assertNotNull(spec);
    }

    @Test
    void resolveArgument_rejectsOrWhenEndpointDisallowsIt() throws Exception {
        Method method = TestController.class.getDeclaredMethod("searchOrDenied", Specification.class);
        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWith("filter", "name==john,name==doe"),
                null
        ));
    }

    @Test
    void resolveArgument_rejectsWhenEndpointAstDepthExceeded() throws Exception {
        Method method = TestController.class.getDeclaredMethod("searchDepthZero", Specification.class);
        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWith("filter", "name==john;name==doe"),
                null
        ));
    }

    private NativeWebRequest requestWith(String key, String value) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter(key, value);
        return new ServletWebRequest(req);
    }

    private static class MockCustomOperator implements RSQLCustomOperator<String> {
        @Override
        public ComparisonOperator getComparisonOperator() {
            return new ComparisonOperator("=mock=");
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }

        @Override
        public Predicate toPredicate(RSQLCustomPredicateInput input) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static class TestController {

        @WebQuery(entityClass = Entity.class)
        void search(Specification<Entity> spec) {
        }

        @WebQuery(entityClass = Entity.class, dtoClass = Object.class)
        void dtoSearch(Specification<Entity> spec) {
        }

        @WebQuery(entityClass = Entity.class, fieldMappings = {
                @FieldMapping(name = "displayName", field = "name", allowOriginalFieldName = false)
        })
        void searchWithMapping(Specification<Entity> spec) {
        }

        @WebQuery(entityClass = EntityWithCustom.class)
        void searchWithCustom(Specification<EntityWithCustom> spec) {
        }

        @WebQuery(entityClass = Entity.class, filterParamName = "q")
        void searchWithCustomParam(Specification<Entity> spec) {
        }

        @WebQuery(
                entityClass = Entity.class,
                allowOrOperator = WebQuery.OperatorPolicy.DENY,
                allowAndOperator = WebQuery.OperatorPolicy.ALLOW
        )
        void searchOrDenied(Specification<Entity> spec) {
        }

        @WebQuery(entityClass = Entity.class, maxASTDepth = 0)
        void searchDepthZero(Specification<Entity> spec) {
        }

        void missingWebQuery(Specification<Entity> spec) {
        }
    }

    private static class Entity {
        @RSQLFilterable({RSQLDefaultOperator.EQUAL})
        private String name;
    }

    private static class EntityWithCustom {
        @RSQLFilterable(value = {RSQLDefaultOperator.EQUAL}, customOperators = {MockCustomOperator.class})
        private String name;
    }
}
