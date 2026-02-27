package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
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
                annotationUtil
        );

        new RSQLParser().parse("name==john").accept(visitor);
    }

    @Test
    void rejects_unknownField() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{},
                annotationUtil
        );

        assertThrows(QueryValidationException.class, () -> new RSQLParser().parse("missing==x").accept(visitor));
    }

    @Test
    void rejects_disallowedOperator() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{},
                annotationUtil
        );

        assertThrows(QueryValidationException.class, () -> new RSQLParser().parse("name!=x").accept(visitor));
    }

    @Test
    void allows_aliasFieldMapping() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{mapping("displayName", "name", false)},
                annotationUtil
        );

        new RSQLParser().parse("displayName==john").accept(visitor);
    }

    @Test
    void rejects_originalMappedFieldWhenNotAllowed() {
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{mapping("displayName", "name", false)},
                annotationUtil
        );

        assertThrows(QueryValidationException.class, () -> new RSQLParser().parse("name==john").accept(visitor));
    }

    @Test
    void allows_customOperatorWhenWhitelisted() {
        Set<ComparisonOperator> ops = Set.of(RsqlOperator.EQUAL.getOperator(), new MockCustomOperator().getComparisonOperator());
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntityWithCustom.class,
                new FieldMapping[]{},
                annotationUtil
        );

        new RSQLParser(ops).parse("name=mock=value").accept(visitor);
    }

    @Test
    void rejects_unregisteredCustomOperator() {
        Set<ComparisonOperator> ops = Set.of(RsqlOperator.EQUAL.getOperator(), new MockCustomOperator().getComparisonOperator());
        EntityValidationRSQLVisitor visitor = new EntityValidationRSQLVisitor(
                TestEntityWithCustom.class,
                new FieldMapping[]{},
                new AnnotationUtil(Set.of())
        );

        assertThrows(QueryConfigurationException.class, () -> new RSQLParser(ops).parse("name=mock=value").accept(visitor));
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

    private static class TestEntity {

        @RsqlFilterable(operators = {RsqlOperator.EQUAL})
        private String name;

        private Integer age;
    }

    private static class TestEntityWithCustom {

        @RsqlFilterable(operators = {RsqlOperator.EQUAL}, customOperators = {MockCustomOperator.class})
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
