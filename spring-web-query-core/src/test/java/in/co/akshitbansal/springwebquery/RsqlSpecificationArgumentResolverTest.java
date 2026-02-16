package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.RsqlSpec;
import in.co.akshitbansal.springwebquery.enums.RsqlOperator;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RsqlSpecificationArgumentResolverTest {

    private final RsqlSpecificationArgumentResolver resolver = new RsqlSpecificationArgumentResolver(configuredParser());

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

        resolver.resolveArgument(parameter, null, emptyRequest(), null);
    }

    @Test
    void resolveArgument_rejectsInvalidRsqlSyntax() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest webRequest = requestWithFilter("name==");

        assertThrows(QueryException.class, () -> resolver.resolveArgument(parameter, null, webRequest, null));
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

        assertThrows(QueryException.class, () -> resolver.resolveArgument(parameter, null, webRequest, null));
    }

    @Test
    void resolveArgument_allowsOriginalMappedFieldWhenExplicitlyAllowed() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("searchWithMappingAllowOriginal", Specification.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest webRequest = requestWithFilter("name==john");

        resolver.resolveArgument(parameter, null, webRequest, null);
    }

    private static NativeWebRequest emptyRequest() {
        return new ServletWebRequest(new MockHttpServletRequest());
    }

    private static NativeWebRequest requestWithFilter(String filter) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("filter", filter);
        return new ServletWebRequest(request);
    }

    private static RSQLParser configuredParser() {
        Set<ComparisonOperator> allowedOperators = Arrays
                .stream(RsqlOperator.values())
                .map(RsqlOperator::getOperator)
                .collect(Collectors.toSet());
        return new RSQLParser(allowedOperators);
    }

    @SuppressWarnings("unused")
    private static class TestController {
        void search(@RsqlSpec(entityClass = TestEntity.class) Specification<TestEntity> specification) {
        }

        void searchWithoutAnnotation(Specification<TestEntity> specification) {
        }

        void searchWithMapping(@RsqlSpec(
                entityClass = TestEntity.class,
                fieldMappings = {
                        @FieldMapping(name = "displayName", field = "name")
                }
        ) Specification<TestEntity> specification) {
        }

        void searchWithMappingAllowOriginal(@RsqlSpec(
                entityClass = TestEntity.class,
                fieldMappings = {
                        @FieldMapping(name = "displayName", field = "name", allowOriginalFieldName = true)
                }
        ) Specification<TestEntity> specification) {
        }
    }

    private static class TestEntity {
        @RsqlFilterable(operators = {RsqlOperator.EQUAL})
        private String name;
    }
}
