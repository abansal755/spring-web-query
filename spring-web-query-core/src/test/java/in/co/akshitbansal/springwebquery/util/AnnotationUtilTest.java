package in.co.akshitbansal.springwebquery.util;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterableEquality;
import in.co.akshitbansal.springwebquery.annotation.Sortable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryFieldValidationException;
import in.co.akshitbansal.springwebquery.exception.QueryForbiddenOperatorException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
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
        assertDoesNotThrow(() -> util.validateFilterableField(field, RSQLDefaultOperator.EQUAL.getOperator(), "name"));
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
        assertThrows(QueryForbiddenOperatorException.class, () -> util.validateFilterableField(field, RSQLDefaultOperator.NOT_EQUAL.getOperator(), "name"));
    }

    @Test
    void validateFilterableField_rejectsNonFilterableField() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        var field = NonFilterableEntity.class.getDeclaredField("name");
        assertThrows(QueryFieldValidationException.class, () -> util.validateFilterableField(field, RSQLDefaultOperator.EQUAL.getOperator(), "name"));
    }

    @Test
    void validateFilterableField_supportsComposedAnnotations() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        var field = ComposedFilterableEntity.class.getDeclaredField("name");
        assertDoesNotThrow(() -> util.validateFilterableField(field, RSQLDefaultOperator.EQUAL.getOperator(), "name"));
    }

    @Test
    void validateSortableField_acceptsSortableField() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        var field = SortableEntity.class.getDeclaredField("name");
        assertDoesNotThrow(() -> util.validateSortableField(field, "name"));
    }

    @Test
    void validateSortableField_rejectsNonSortableField() throws Exception {
        AnnotationUtil util = new AnnotationUtil(Set.of());
        var field = NonFilterableEntity.class.getDeclaredField("name");
        assertThrows(QueryFieldValidationException.class, () -> util.validateSortableField(field, "name"));
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

    private static class FilterableEntity {
        @RSQLFilterable(value = {RSQLDefaultOperator.EQUAL}, customOperators = {MockCustomOperator.class})
        private String name;
    }

    private static class DefaultOnlyFilterableEntity {
        @RSQLFilterable({RSQLDefaultOperator.EQUAL})
        private String name;
    }

    private static class NonFilterableEntity {
        private String name;
    }

    private static class ComposedFilterableEntity {
        @RSQLFilterableEquality
        private String name;
    }

    private static class SortableEntity {
        @Sortable
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
