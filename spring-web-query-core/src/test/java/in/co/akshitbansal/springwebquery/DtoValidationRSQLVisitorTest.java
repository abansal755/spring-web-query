package in.co.akshitbansal.springwebquery;

import cz.jirutka.rsql.parser.RSQLParser;
import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.RsqlFilterable;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryValidationException;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DtoValidationRSQLVisitorTest {

    private final AnnotationUtil annotationUtil = new AnnotationUtil(Set.of());

    @Test
    void builds_fieldMappingsForValidDtoPath() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, QueryDto.class, annotationUtil);
        new RSQLParser().parse("profile.city==London").accept(visitor);

        assertEquals(Map.of("profile.city", "profile.address.city"), visitor.getFieldMappings());
    }

    @Test
    void rejects_unknownDtoPath() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, QueryDto.class, annotationUtil);

        assertThrows(QueryValidationException.class, () -> new RSQLParser().parse("missing==x").accept(visitor));
    }

    @Test
    void rejects_nonFilterableDtoField() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, QueryDto.class, annotationUtil);

        assertThrows(QueryValidationException.class, () -> new RSQLParser().parse("unfilterable==x").accept(visitor));
    }

    @Test
    void rejects_whenMappedEntityPathCannotBeResolved() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, InvalidMappingDto.class, annotationUtil);

        assertThrows(QueryConfigurationException.class, () -> new RSQLParser().parse("city==x").accept(visitor));
    }

    @Test
    void supports_absoluteMapReset() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, AbsoluteDto.class, annotationUtil);
        new RSQLParser().parse("nested.city==x").accept(visitor);

        assertEquals(Map.of("nested.city", "profile.address.city"), visitor.getFieldMappings());
    }

    private static class TestEntity {
        private Profile profile;
    }

    private static class Profile {
        private Address address;
    }

    private static class Address {
        @SuppressWarnings("unused")
        private String city;
    }

    private static class QueryDto {
        @MapsTo(field = "profile")
        private ProfileDto profile;

        @SuppressWarnings("unused")
        private String unfilterable;
    }

    private static class ProfileDto {
        @MapsTo(field = "address")
        private AddressDto address;

        @RsqlFilterable(operators = {RsqlOperator.EQUAL})
        @MapsTo(field = "address.city")
        private String city;
    }

    private static class AddressDto {
        @RsqlFilterable(operators = {RsqlOperator.EQUAL})
        @MapsTo(field = "city")
        private String city;
    }

    private static class InvalidMappingDto {
        @RsqlFilterable(operators = {RsqlOperator.EQUAL})
        @MapsTo(field = "doesNotExist")
        private String city;
    }

    private static class AbsoluteDto {
        private NestedDto nested;
    }

    private static class NestedDto {
        @RsqlFilterable(operators = {RsqlOperator.EQUAL})
        @MapsTo(field = "profile.address.city", absolute = true)
        private String city;
    }
}
