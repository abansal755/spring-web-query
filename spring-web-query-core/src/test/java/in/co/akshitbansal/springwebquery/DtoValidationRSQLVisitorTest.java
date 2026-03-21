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
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, QueryDto.class, annotationUtil, true, false);
        new RSQLParser().parse("profile.city==London").accept(visitor);

        assertEquals(Map.of("profile.city", "profile.address.city"), visitor.getFieldMappings());
    }

    @Test
    void rejects_unknownDtoPath() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, QueryDto.class, annotationUtil, true, false);

        assertThrows(QueryValidationException.class, () -> new RSQLParser().parse("missing==x").accept(visitor));
    }

    @Test
    void rejects_nonFilterableDtoField() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, QueryDto.class, annotationUtil, true, false);

        assertThrows(QueryValidationException.class, () -> new RSQLParser().parse("unfilterable==x").accept(visitor));
    }

    @Test
    void rejects_whenMappedEntityPathCannotBeResolved() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, InvalidMappingDto.class, annotationUtil, true, false);

        assertThrows(QueryConfigurationException.class, () -> new RSQLParser().parse("city==x").accept(visitor));
    }

    @Test
    void supports_absoluteMapReset() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, AbsoluteDto.class, annotationUtil, true, false);
        new RSQLParser().parse("nested.city==x").accept(visitor);

        assertEquals(Map.of("nested.city", "profile.address.city"), visitor.getFieldMappings());
    }

    @Test
    void rejects_andOperator_whenNotAllowed() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, QueryDto.class, annotationUtil, false, false);
        RSQLParser parser = new RSQLParser();

        assertThrows(QueryValidationException.class, () -> parser.parse("profile.city==London;unfilterable==x").accept(visitor));
    }

    @Test
    void rejects_orOperator_whenNotAllowed() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, QueryDto.class, annotationUtil, true, false);
        RSQLParser parser = new RSQLParser();

        assertThrows(QueryValidationException.class, () -> parser.parse("profile.city==London,unfilterable==x").accept(visitor));
    }

    @Test
    void allows_orOperator_whenExplicitlyEnabled() {
        DtoValidationRSQLVisitor visitor = new DtoValidationRSQLVisitor(TestEntity.class, QueryDto.class, annotationUtil, true, true);
        new RSQLParser().parse("profile.city==London,profile.city==Paris").accept(visitor);
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
        @MapsTo("profile")
        private ProfileDto profile;

        @SuppressWarnings("unused")
        private String unfilterable;
    }

    private static class ProfileDto {
        @MapsTo("address")
        private AddressDto address;

        @RsqlFilterable({RsqlOperator.EQUAL})
        @MapsTo("address.city")
        private String city;
    }

    private static class AddressDto {
        @RsqlFilterable({RsqlOperator.EQUAL})
        @MapsTo("city")
        private String city;
    }

    private static class InvalidMappingDto {
        @RsqlFilterable({RsqlOperator.EQUAL})
        @MapsTo("doesNotExist")
        private String city;
    }

    private static class AbsoluteDto {
        private NestedDto nested;
    }

    private static class NestedDto {
        @RsqlFilterable({RsqlOperator.EQUAL})
        @MapsTo(value = "profile.address.city", absolute = true)
        private String city;
    }
}
