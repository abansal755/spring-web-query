package in.co.akshitbansal.springwebquery.util;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterableEquality;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnnotationUtilTest {

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
    void validateFilterableField_acceptsDefaultOperator() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        var field = DefaultOnlyFilterableEntity.class.getDeclaredField("name");
        assertDoesNotThrow(() -> util.validateFilterableField(field, RsqlOperator.EQUAL.getOperator(), "name"));
    }

    @Test
    void validateFilterableField_acceptsCustomOperatorWhenRegistered() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of(new MockCustomOperator()));
        var field = FilterableEntity.class.getDeclaredField("name");
        assertDoesNotThrow(() -> util.validateFilterableField(field, new ComparisonOperator("=mock="), "name"));
    }

    @Test
    void validateFilterableField_rejectsUnregisteredCustomOperator() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        var field = FilterableEntity.class.getDeclaredField("name");
        assertThrows(QueryConfigurationException.class, () -> util.validateFilterableField(field, new ComparisonOperator("=mock="), "name"));
    }

    @Test
    void validateFilterableField_rejectsDisallowedOperator() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        var field = DefaultOnlyFilterableEntity.class.getDeclaredField("name");
        assertThrows(QueryForbiddenOperatorException.class, () -> util.validateFilterableField(field, RsqlOperator.NOT_EQUAL.getOperator(), "name"));
    }

    @Test
    void validateFilterableField_rejectsNonFilterableField() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        var field = NonFilterableEntity.class.getDeclaredField("name");
        assertThrows(QueryFieldValidationException.class, () -> util.validateFilterableField(field, RsqlOperator.EQUAL.getOperator(), "name"));
    }

    @Test
    void validateFilterableField_supportsComposedAnnotations() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        var field = ComposedFilterableEntity.class.getDeclaredField("name");
        assertDoesNotThrow(() -> util.validateFilterableField(field, RsqlOperator.EQUAL.getOperator(), "name"));
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

    private static class FilterableEntity {
        @RsqlFilterable(value = {RsqlOperator.EQUAL}, customOperators = {MockCustomOperator.class})
        private String name;
    }

    private static class DefaultOnlyFilterableEntity {
        @RsqlFilterable({RsqlOperator.EQUAL})
        private String name;
    }

    private static class NonFilterableEntity {
        private String name;
    }

    private static class ComposedFilterableEntity {
        @RsqlFilterableEquality
        private String name;
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
