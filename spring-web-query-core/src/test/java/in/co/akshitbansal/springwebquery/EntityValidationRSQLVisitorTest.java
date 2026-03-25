package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RSQLFilterable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EntityValidationRSQLVisitorTest {

    private final AnnotationUtil annotationUtil = new AnnotationUtil(Set.of(new MockCustomOperator()));

    @Test
    void allows_filterableFieldWithAllowedOperator() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{},
                annotationUtil,
                true,
                false,
                1
        );

        new RSQLParser().parse("name==john").accept(visitor, NodeMetadata.of(0));
    }

    @Test
    void rejects_unknownField() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{},
                annotationUtil,
                true,
                false,
                1
        );

        assertThrows(QueryValidationException.class, () ->
                new RSQLParser().parse("missing==x").accept(visitor, NodeMetadata.of(0))
        );
    }

    @Test
    void rejects_disallowedOperator() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{},
                annotationUtil,
                true,
                false,
                1
        );

        assertThrows(QueryValidationException.class, () ->
                new RSQLParser().parse("name!=x").accept(visitor, NodeMetadata.of(0))
        );
    }

    @Test
    void allows_aliasFieldMapping() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{mapping("displayName", "name", false)},
                annotationUtil,
                true,
                false,
                1
        );

        new RSQLParser().parse("displayName==john").accept(visitor, NodeMetadata.of(0));
    }

    @Test
    void rejects_originalMappedFieldWhenNotAllowed() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{mapping("displayName", "name", false)},
                annotationUtil,
                true,
                false,
                1
        );

        assertThrows(QueryValidationException.class, () ->
                new RSQLParser().parse("name==john").accept(visitor, NodeMetadata.of(0))
        );
    }

    @Test
    void allows_customOperatorWhenWhitelisted() {
        Set<ComparisonOperator> ops = Set.of(RSQLDefaultOperator.EQUAL.getOperator(), new MockCustomOperator().getComparisonOperator());
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntityWithCustom.class,
                new FieldMapping[]{},
                annotationUtil,
                true,
                false,
                1
        );

        new RSQLParser(ops).parse("name=mock=value").accept(visitor, NodeMetadata.of(0));
    }

    @Test
    void rejects_unregisteredCustomOperator() {
        Set<ComparisonOperator> ops = Set.of(RSQLDefaultOperator.EQUAL.getOperator(), new MockCustomOperator().getComparisonOperator());
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntityWithCustom.class,
                new FieldMapping[]{},
                new AnnotationUtil(Set.of()),
                true,
                false,
                1
        );

        assertThrows(QueryConfigurationException.class, () ->
                new RSQLParser(ops).parse("name=mock=value").accept(visitor, NodeMetadata.of(0))
        );
    }

    @Test
    void rejects_orOperator_whenNotAllowed() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{},
                annotationUtil,
                true,
                false,
                1
        );

        assertThrows(QueryValidationException.class, () ->
                new RSQLParser().parse("name==john,name==doe").accept(visitor, NodeMetadata.of(0))
        );
    }

    @Test
    void rejects_whenAstDepthExceeded() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{},
                annotationUtil,
                true,
                true,
                0
        );

        assertThrows(QueryValidationException.class, () ->
                new RSQLParser().parse("name==john;name==doe").accept(visitor, NodeMetadata.of(0))
        );
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

    private static class TestEntity {

        @RSQLFilterable({RSQLDefaultOperator.EQUAL})
        private String name;

        private Integer age;
    }

    private static class TestEntityWithCustom {

        @RSQLFilterable(value = {RSQLDefaultOperator.EQUAL}, customOperators = {MockCustomOperator.class})
        private String name;
    }

    private static FieldMapping mapping(String name, String field, boolean allowOriginalFieldName) {
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
                return allowOriginalFieldName;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return FieldMapping.class;
            }
        };
    }
}
