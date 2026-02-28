package in.co.akshitbansal.springwebquery.util;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.RsqlSpec;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationUtilTest {

    @Test
    void resolveWebQueryFromParameter_returnsAnnotation() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        Method method = TestController.class.getDeclaredMethod("search", Specification.class);

        WebQuery annotation = util.resolveWebQueryFromParameter(new MethodParameter(method, 0));
        assertEquals(TestEntity.class, annotation.entityClass());
    }

    @Test
    void resolveWebQueryFromParameter_throwsWhenMissing() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        Method method = TestController.class.getDeclaredMethod("searchWithoutWebQuery", Specification.class);

        assertThrows(QueryConfigurationException.class, () -> util.resolveWebQueryFromParameter(new MethodParameter(method, 0)));
    }

    @Test
    void validateFieldMappings_rejectsDuplicateAlias() {
        AnnotationUtil util = new AnnotationUtil(Set.of());

        assertThrows(QueryConfigurationException.class, () -> util.validateFieldMappings(new FieldMapping[]{
                mapping("name", "a"),
                mapping("name", "b")
        }));
    }

    @Test
    void validateFieldMappings_rejectsDuplicateTargetField() {
        AnnotationUtil util = new AnnotationUtil(Set.of());

        assertThrows(QueryConfigurationException.class, () -> util.validateFieldMappings(new FieldMapping[]{
                mapping("a", "field"),
                mapping("b", "field")
        }));
    }

    @Test
    void validateFilterableField_acceptsDefaultAndCustomOperators() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of(new MockCustomOperator()));
        var field = FilterableEntity.class.getDeclaredField("name");

        assertDoesNotThrow(() -> util.validateFilterableField(field, RsqlOperator.EQUAL.getOperator(), "name"));
        assertDoesNotThrow(() -> util.validateFilterableField(field, new ComparisonOperator("=mock="), "name"));
    }

    @Test
    void validateFilterableField_rejectsUnregisteredCustomOperator() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        var field = FilterableEntity.class.getDeclaredField("name");

        assertThrows(QueryConfigurationException.class, () -> util.validateFilterableField(field, RsqlOperator.EQUAL.getOperator(), "name"));
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

        void searchWithoutWebQuery(@RsqlSpec Specification<TestEntity> specification) {
        }
    }

    private static class FilterableEntity {
        @RsqlFilterable(operators = {RsqlOperator.EQUAL}, customOperators = {MockCustomOperator.class})
        private String name;
    }

    private static class TestEntity {
    }

    private static FieldMapping mapping(String name, String field) {
        return new FieldMapping() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String field() {
                return field;
            }

            @Override
            public boolean allowOriginalFieldName() {
                return false;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return FieldMapping.class;
            }
        };
    }
}
