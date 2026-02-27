package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.RsqlSpec;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import in.co.akshitbansal.springwebquery.resolver.EntityAwareRsqlSpecArgumentResolver;
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

import static org.junit.jupiter.api.Assertions.*;

class EntityAwareRsqlSpecArgumentResolverTest {

    private final EntityAwareRsqlSpecArgumentResolver resolver = new EntityAwareRsqlSpecArgumentResolver(
            Set.of(RsqlOperator.values()),
            Set.of(new MockCustomOperator()),
            new AnnotationUtil(Set.of(new MockCustomOperator()))
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
    void resolveArgument_rejectsWhenWebQueryMissing() throws Exception {
        Method method = TestController.class.getDeclaredMethod("missingWebQuery", Specification.class);
        assertThrows(QueryConfigurationException.class, () -> resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                requestWith("filter", "name==john"),
                null
        ));
    }

    private NativeWebRequest requestWith(String key, String value) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter(key, value);
        return new ServletWebRequest(req);
    }

    private static class MockCustomOperator implements RsqlCustomOperator<String> {
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
        void search(@RsqlSpec Specification<Entity> spec) {
        }

        @WebQuery(entityClass = Entity.class, dtoClass = Object.class)
        void dtoSearch(@RsqlSpec Specification<Entity> spec) {
        }

        @WebQuery(entityClass = Entity.class, fieldMappings = {
                @FieldMapping(name = "displayName", field = "name", allowOriginalFieldName = false)
        })
        void searchWithMapping(@RsqlSpec Specification<Entity> spec) {
        }

        @WebQuery(entityClass = EntityWithCustom.class)
        void searchWithCustom(@RsqlSpec Specification<EntityWithCustom> spec) {
        }

        void missingWebQuery(@RsqlSpec Specification<Entity> spec) {
        }
    }

    private static class Entity {
        @RsqlFilterable(operators = {RsqlOperator.EQUAL})
        private String name;
    }

    private static class EntityWithCustom {
        @RsqlFilterable(operators = {RsqlOperator.EQUAL}, customOperators = {MockCustomOperator.class})
        private String name;
    }
}
