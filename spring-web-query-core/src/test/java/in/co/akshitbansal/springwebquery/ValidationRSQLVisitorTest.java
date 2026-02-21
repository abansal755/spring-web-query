package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationRSQLVisitorTest {

    private RSQLParser parser = new RSQLParser();

    private RSQLParser configuredParser(Set<RsqlCustomOperator<?>> customOperators) {
        Set<cz.jirutka.rsql.parser.ast.ComparisonOperator> operators = Arrays.stream(RsqlOperator.values())
                .map(RsqlOperator::getOperator)
                .collect(Collectors.toSet());
        operators.addAll(customOperators.stream()
                .map(RsqlCustomOperator::getComparisonOperator)
                .collect(Collectors.toSet()));
        return new RSQLParser(operators);
    }

    @Test
    void visit_allowsFilterableFieldWithAllowedOperator() {
        ValidationRSQLVisitor visitor = new ValidationRSQLVisitor(TestEntity.class, new FieldMapping[]{}, Collections.emptySet());
        parser.parse("name==john").accept(visitor);
    }

    @Test
    void visit_rejectsUnknownField() {
        ValidationRSQLVisitor visitor = new ValidationRSQLVisitor(TestEntity.class, new FieldMapping[]{}, Collections.emptySet());
        QueryValidationException ex = assertThrows(
                QueryValidationException.class,
                () -> parser.parse("missing==x").accept(visitor)
        );
        assertEquals("Unknown field 'missing'", ex.getMessage());
    }

    @Test
    void visit_rejectsNonFilterableField() {
        ValidationRSQLVisitor visitor = new ValidationRSQLVisitor(TestEntity.class, new FieldMapping[]{}, Collections.emptySet());
        QueryValidationException ex = assertThrows(
                QueryValidationException.class,
                () -> parser.parse("age==10").accept(visitor)
        );
        assertEquals("Filtering not allowed on field 'age'", ex.getMessage());
    }

    @Test
    void visit_rejectsDisallowedOperator() {
        ValidationRSQLVisitor visitor = new ValidationRSQLVisitor(TestEntity.class, new FieldMapping[]{}, Collections.emptySet());
        QueryValidationException ex = assertThrows(
                QueryValidationException.class,
                () -> parser.parse("name!=john").accept(visitor)
        );
        assertEquals("Operator '!=' not allowed on field 'name'", ex.getMessage());
    }

    @Test
    void visit_allowsMappedAliasField() {
        ValidationRSQLVisitor visitor = new ValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{mapping("displayName", "name", false)},
                Collections.emptySet()
        );
        parser.parse("displayName==john").accept(visitor);
    }

    @Test
    void visit_rejectsOriginalMappedFieldWhenNotAllowed() {
        ValidationRSQLVisitor visitor = new ValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{mapping("displayName", "name", false)},
                Collections.emptySet()
        );
        QueryValidationException ex = assertThrows(
                QueryValidationException.class,
                () -> parser.parse("name==john").accept(visitor)
        );
        assertEquals("Unknown field 'name'", ex.getMessage());
    }

    @Test
    void visit_allowsOriginalMappedFieldWhenAllowed() {
        ValidationRSQLVisitor visitor = new ValidationRSQLVisitor(
                TestEntity.class,
                new FieldMapping[]{mapping("displayName", "name", true)},
                Collections.emptySet()
        );
        parser.parse("name==john").accept(visitor);
    }

    @Test
    void visit_allowsCustomOperator() {
        Set<RsqlCustomOperator<?>> customOperators = Set.of(new MockCustomOperator());
        ValidationRSQLVisitor visitor = new ValidationRSQLVisitor(TestEntityWithCustom.class, new FieldMapping[]{}, customOperators);
        configuredParser(customOperators).parse("name=mock=value").accept(visitor);
    }

    @Test
    void visit_rejectsUnregisteredCustomOperator() {
        Set<RsqlCustomOperator<?>> customOperators = Set.of(new MockCustomOperator());
        ValidationRSQLVisitor visitor = new ValidationRSQLVisitor(TestEntityWithCustom.class, new FieldMapping[]{}, Collections.emptySet());
        RSQLParser customParser = configuredParser(customOperators);
        QueryConfigurationException ex = assertThrows(
                QueryConfigurationException.class,
                () -> customParser.parse("name=mock=value").accept(visitor)
        );
        assertEquals("Custom operator 'MockCustomOperator' referenced in @RsqlFilterable is not registered", ex.getMessage());
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
