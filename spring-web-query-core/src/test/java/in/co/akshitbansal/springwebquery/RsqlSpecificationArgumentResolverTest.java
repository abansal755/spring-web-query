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
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RsqlSpecificationArgumentResolverTest {

    private final RsqlSpecificationArgumentResolver resolver = new RsqlSpecificationArgumentResolver(
            Set.of(RsqlOperator.values()),
            Collections.emptySet()
    );

    @Test
    void supportsParameter_returnsTrueForAnnotatedSpecification() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        assertTrue(resolver.supportsParameter(parameter));
    }

    @Test
    void supportsParameter_returnsFalseForNonAnnotatedSpecification() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithoutAnnotation", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        assertFalse(resolver.supportsParameter(parameter));
    }

    @Test
    void resolveArgument_returnsUnrestrictedWhenFilterMissing() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        Specification<?> spec = resolver.resolveArgument(parameter, null, emptyRequest(), null);
        assertNotNull(spec);
    }

    @Test
    void resolveArgument_rejectsInvalidRsqlSyntax() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest webRequest = requestWithFilter("name==");

        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(parameter, null, webRequest, null));
    }

    @Test
    void resolveArgument_acceptsMappedAliasField() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithMapping", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest webRequest = requestWithFilter("displayName==john");

        resolver.resolveArgument(parameter, null, webRequest, null);
    }

    @Test
    void resolveArgument_rejectsOriginalMappedFieldWhenNotAllowed() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithMapping", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest webRequest = requestWithFilter("name==john");

        assertThrows(QueryValidationException.class, () -> resolver.resolveArgument(parameter, null, webRequest, null));
    }

    @Test
    void resolveArgument_allowsOriginalMappedFieldWhenExplicitlyAllowed() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithMappingAllowOriginal", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest webRequest = requestWithFilter("name==john");

        resolver.resolveArgument(parameter, null, webRequest, null);
    }

    @Test
    void resolveArgument_allowsCustomOperator() throws NoSuchMethodException {
        RsqlSpecificationArgumentResolver resolverWithCustom = new RsqlSpecificationArgumentResolver(
                Set.of(RsqlOperator.values()),
                Set.of(new MockCustomOperator())
        );
        Method method = TestController.class.getDeclaredMethod("searchWithCustom", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest webRequest = requestWithFilter("name=mock=value");

        resolverWithCustom.resolveArgument(parameter, null, webRequest, null);
    }

    @Test
    void resolveArgument_usesCustomParameterName() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithCustomParam", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("q", "name==john");

        resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
    }

    @Test
    void resolveArgument_rejectsWhenWebQueryMissing() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithoutWebQuery", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest webRequest = requestWithFilter("name==john");

        assertThrows(QueryConfigurationException.class, () -> resolver.resolveArgument(parameter, null, webRequest, null));
    }

    private static NativeWebRequest emptyRequest() {
        return new ServletWebRequest(new MockHttpServletRequest());
    }

    private static NativeWebRequest requestWithFilter(String filter) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("filter", filter);
        return new ServletWebRequest(request);
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

        @WebQuery(entityClass = TestEntity.class)
        void search(@RsqlSpec Specification<TestEntity> specification) {
        }

        void searchWithoutAnnotation(Specification<TestEntity> specification) {
        }

        @WebQuery(
                entityClass = TestEntity.class,
                fieldMappings = {
                        @FieldMapping(name = "displayName", field = "name")
                }
        )
        void searchWithMapping(@RsqlSpec Specification<TestEntity> specification) {
        }

        @WebQuery(
                entityClass = TestEntity.class,
                fieldMappings = {
                        @FieldMapping(name = "displayName", field = "name", allowOriginalFieldName = true)
                }
        )
        void searchWithMappingAllowOriginal(@RsqlSpec Specification<TestEntity> specification) {
        }

        @WebQuery(entityClass = TestEntityWithCustom.class)
        void searchWithCustom(@RsqlSpec Specification<TestEntityWithCustom> specification) {
        }

        @WebQuery(entityClass = TestEntity.class)
        void searchWithCustomParam(@RsqlSpec(paramName = "q") Specification<TestEntity> specification) {
        }

        void searchWithoutWebQuery(@RsqlSpec Specification<TestEntity> specification) {
        }
    }

    private static class TestEntity {

        @RsqlFilterable(operators = {RsqlOperator.EQUAL})

        private String name;
    }

    private static class TestEntityWithCustom {

        @RsqlFilterable(operators = {RsqlOperator.EQUAL}, customOperators = {MockCustomOperator.class})
        private String name;
    }
}
