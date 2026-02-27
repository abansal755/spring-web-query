# Spring Web Query

[![License](https://img.shields.io/github/license/abansal755/spring-web-query?style=flat&label=License&color=blue
)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue?style=flat&color=orange)](https://www.oracle.com/java/technologies/javase-jdk21-downloads.html)
[![Spring Boot 4.0.2+](https://img.shields.io/badge/Spring_Boot-4.0.2%2B-orange?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)

[![Maven Central (spring-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fin%2Fco%2Fakshitbansal%2Fspring-web-query-core%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Maven%20Central%20(Core)&color=brightgreen&logo=apachemaven)](https://central.sonatype.com/artifact/in.co.akshitbansal/spring-web-query-core)
[![Maven Central (spring-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fin%2Fco%2Fakshitbansal%2Fspring-web-query-core%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Snapshot%20(Core)&color=yellow&logo=apachemaven)](https://central.sonatype.com/repository/maven-snapshots/in/co/akshitbansal/spring-web-query-core/maven-metadata.xml)

[![Maven Central (spring-boot-starter-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fin%2Fco%2Fakshitbansal%2Fspring-boot-starter-web-query%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Maven%20Central%20(Starter)&color=brightgreen&logo=apachemaven)](https://central.sonatype.com/artifact/in.co.akshitbansal/spring-boot-starter-web-query)
[![Maven Central (spring-boot-starter-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fin%2Fco%2Fakshitbansal%2Fspring-boot-starter-web-query%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Snapshot%20(Starter)&color=yellow&logo=apachemaven)](https://central.sonatype.com/repository/maven-snapshots/in/co/akshitbansal/spring-boot-starter-web-query/maven-metadata.xml)

`spring-web-query` adds safe, declarative filtering and sorting to Spring Web APIs using RSQL and Spring Data JPA `Specification`.

## Table of Contents

- [Project modules](#project-modules)
- [Why this library exists](#why-this-library-exists)
- [What is RSQL](#what-is-rsql)
- [Installation](#installation)
- [How resolution modes work](#how-resolution-modes-work)
- [Quick start (DTO-aware, recommended)](#quick-start-dto-aware-recommended)
- [Entity-aware setup (supported)](#entity-aware-setup-supported)
- [RestrictedPageable behavior](#restrictedpageable-behavior)
- [RSQL operator reference (default operators)](#rsql-operator-reference-default-operators)
- [Advanced configuration](#advanced-configuration)
- [Error handling](#error-handling)
- [Compatibility](#compatibility)
- [Contributing](#contributing)
- [License](#license)

## Project modules

This repository publishes two Maven artifacts:

- `in.co.akshitbansal:spring-web-query-core`
  - Core annotations (`@WebQuery`, `@RsqlSpec`, `@RestrictedPageable`, etc.)
  - Validation visitors
  - Argument resolvers
  - Reflection/annotation utilities
  - Custom operator extension contracts
- `in.co.akshitbansal:spring-boot-starter-web-query`
  - Spring Boot auto-configuration
  - Resolver registration
  - Pageable max-page-size customization (`api.pagination.max-page-size`)
  - ISO-8601 to `Timestamp` converter registration for RSQL parsing

For most applications, use the starter.

## Why this library exists

Most APIs eventually need dynamic querying:

- `GET /users?status=ACTIVE&city=London`
- OR expressions, ranges, IN/NOT IN, nested fields
- constrained sorting and pagination

Without a shared contract, this usually becomes either:

- controller-level if/else parsing that is hard to maintain, or
- unrestricted query surfaces that are unsafe

`spring-web-query` provides a declarative contract:

- `@RsqlFilterable`: which fields can be filtered and with which operators
- `@Sortable`: which fields can be sorted
- `@WebQuery`: method-level query context (`entityClass`, optional `dtoClass`, optional `fieldMappings`)
- `@RsqlSpec` / `@RestrictedPageable`: controller parameters resolved with validation

## What is RSQL

RSQL (RESTful Service Query Language) is a URL-friendly query language.

- Comparison: `field==value`, `field=gt=10`, `field=in=(A,B)`
- Logical AND: `;`
- Logical OR: `,`
- Grouping: `( ... )`

Examples:

- `status==ACTIVE`
- `status==ACTIVE;createdAt=ge=2025-01-01T00:00:00Z`
- `(status==ACTIVE,status==PENDING);age=gt=18`

## Installation

### Option 1: Spring Boot starter (recommended)

```xml
<dependency>
    <groupId>in.co.akshitbansal</groupId>
    <artifactId>spring-boot-starter-web-query</artifactId>
    <version>X.X.X</version>
</dependency>
```

### Option 2: Core only (manual wiring)

```xml
<dependency>
    <groupId>in.co.akshitbansal</groupId>
    <artifactId>spring-web-query-core</artifactId>
    <version>X.X.X</version>
</dependency>
```

If you use `core` directly, you must register the argument resolvers and supporting beans yourself.

## How resolution modes work

The library supports two resolution modes chosen by `@WebQuery`:

### DTO-aware mode (recommended)

Use `@WebQuery(dtoClass = ...)`.

- Filter selectors are validated against DTO fields annotated with `@RsqlFilterable`
- Sort selectors are validated against DTO fields annotated with `@Sortable`
- DTO selectors are mapped to entity paths via `@MapsTo`
- Mapped entity paths are validated against the entity class

Why this is recommended:

- clean API contract decoupled from persistence model
- safer evolution of entities without breaking API queries
- explicit external naming and path control

### Entity-aware mode

Use `@WebQuery` without `dtoClass` (default `void.class`).

- selectors are validated directly on entity fields
- optional aliasing supported with `@FieldMapping`

`@FieldMapping` is only used in entity-aware mode.

## Quick start (DTO-aware, recommended)

### 1. Define entity model

```java
@Entity
public class User {
    private String status;
    private Instant createdAt;
    private String username;

    @OneToOne
    private Profile profile;
}

@Entity
public class Profile {
    private String city;
}
```

### 2. Define DTO query contract

```java
public class UserQueryDto {

    @RsqlFilterable(operators = {RsqlOperator.EQUAL, RsqlOperator.IN})
    @Sortable
    private String status;

    @RsqlFilterable(operators = {RsqlOperator.GREATER_THAN, RsqlOperator.LESS_THAN})
    @Sortable
    @MapsTo(field = "createdAt")
    private Instant joinedAt;

    @MapsTo(field = "profile")
    private ProfileQueryDto profile;

    public static class ProfileQueryDto {
        @RsqlFilterable(operators = {RsqlOperator.EQUAL})
        @MapsTo(field = "city")
        private String city;
    }
}
```

### 3. Use in controller

```java
@GetMapping("/users")
@WebQuery(entityClass = User.class, dtoClass = UserQueryDto.class)
public Page<User> search(
    @RsqlSpec Specification<User> spec,
    @RestrictedPageable Pageable pageable
) {
    return userRepository.findAll(spec, pageable);
}
```

Notes:

- `@WebQuery` is required when using `@RsqlSpec` and/or `@RestrictedPageable`
- if the filter parameter is missing/blank, `@RsqlSpec` resolves to `Specification.unrestricted()`
- default RSQL query parameter name is `filter` (override with `@RsqlSpec(paramName = "q")`)

### 4. Example requests

- `/users?filter=status==ACTIVE`
- `/users?filter=joinedAt=gt=2025-01-01T00:00:00Z`
- `/users?filter=profile.city==London`
- `/users?sort=joinedAt,desc`

## Entity-aware setup (supported)

Use entity fields directly and optionally define aliases via `@FieldMapping`.

In this mode, `@RsqlFilterable` and `@Sortable` must be placed on entity fields because validation is performed on the entity model.


```java
@GetMapping("/users")
@WebQuery(
    entityClass = User.class,
    fieldMappings = {
        @FieldMapping(name = "joined", field = "createdAt", allowOriginalFieldName = false)
    }
)
public Page<User> search(
    @RsqlSpec Specification<User> spec,
    @RestrictedPageable Pageable pageable
) {
    return userRepository.findAll(spec, pageable);
}
```

Examples:

- filter with alias: `/users?filter=joined=gt=2025-01-01T00:00:00Z`
- sort with alias: `/users?sort=joined,desc`

## RestrictedPageable behavior

`@RestrictedPageable` does **not** replace Spring’s pageable parser.

It works as a validation/remapping layer on top of Spring Data Web:

1. delegates parsing of `page`, `size`, `sort` to `PageableHandlerMethodArgumentResolver`
2. validates each requested sort field against your query contract
3. rewrites sort properties when aliasing/mapping is configured
4. returns a validated `Pageable`

So pagination behavior remains Spring-standard; this library only adds constraints and safe mapping for sort fields.

## RSQL operator reference (default operators)

Default operators are exposed via `RsqlOperator`.

### Equality and inequality

- `EQUAL` (`==`)
- `NOT_EQUAL` (`!=`)

### Ordering comparisons

- `GREATER_THAN` (`>`, `=gt=`)
- `GREATER_THAN_OR_EQUAL` (`>=`, `=ge=`)
- `LESS_THAN` (`<`, `=lt=`)
- `LESS_THAN_OR_EQUAL` (`<=`, `=le=`)

### Set membership

- `IN` (`=in=`)
- `NOT_IN` (`=out=`)

### Null checks

- `IS_NULL` (`=null=`, `=isnull=`, `=na=`)
- `NOT_NULL` (`=notnull=`, `=isnotnull=`, `=nn=`)

### Pattern matching

- `LIKE` (`=like=`, `=ke=`)
- `NOT_LIKE` (`=notlike=`, `=nk=`)

### Case-insensitive variants

- `IGNORE_CASE` (`=icase=`, `=ic=`)
- `IGNORE_CASE_LIKE` (`=ilike=`, `=ik=`)
- `IGNORE_CASE_NOT_LIKE` (`=inotlike=`, `=ni=`)

### Range comparisons

- `BETWEEN` (`=between=`, `=bt=`)
- `NOT_BETWEEN` (`=notbetween=`, `=nb=`)

Strict equality is enabled internally for `==` conversion (`name==John*` is treated as literal equality, not wildcard prefix matching).

## Advanced configuration

### DTO path mapping with `@MapsTo`

- annotate DTO fields with `@MapsTo(field = "...")` to map each path segment
- use `@MapsTo(absolute = true)` when you need to reset accumulated parent segments and map from the entity root

### Custom operators

1. Implement `RsqlCustomOperator`

```java
public class IsMondayOperator implements RsqlCustomOperator<Long> {
    @Override
    public ComparisonOperator getComparisonOperator() {
        return new ComparisonOperator("=monday=");
    }

    @Override
    public Class<Long> getType() {
        return Long.class;
    }

    @Override
    public Predicate toPredicate(RSQLCustomPredicateInput input) {
        CriteriaBuilder cb = input.getCriteriaBuilder();
        return cb.equal(cb.function("DAYOFWEEK", Long.class, input.getPath()), 2);
    }
}
```

2. Register one or more `RsqlCustomOperatorsConfigurer` beans

```java
@Configuration
public class RsqlConfig {
    @Bean
    public RsqlCustomOperatorsConfigurer customOperators() {
        return () -> Set.of(new IsMondayOperator());
    }
}
```

3. Whitelist the custom operator on each filterable field using `@RsqlFilterable(customOperators = ...)`

```java
@RsqlFilterable(
    operators = {RsqlOperator.EQUAL},
    customOperators = {IsMondayOperator.class}
)
private LocalDateTime createdAt;
```

### Page size cap

```properties
api.pagination.max-page-size=500
```

Default is `100`.

### Timestamp parsing

The starter registers an ISO-8601 converter for `java.sql.Timestamp` values in RSQL expressions.

## Error handling

Exception hierarchy, semantics, and metadata:

- `QueryException`
  - generic base exception for all query-related failures
  - `QueryValidationException`
    - client-side validation failure (map to 4xx responses)
    - thrown for invalid RSQL syntax
    - `QueryFieldValidationException`
      - field-specific validation failure
      - thrown when a field is unknown in filter/sort
      - thrown when filtering/sorting is not allowed on a field
      - additional metadata: `fieldPath`
      - `QueryForbiddenOperatorException`
        - thrown when an operator is not allowed for a field
        - additional metadata: `fieldPath`, `operator` (used), `allowedOperators` (set of allowed operators)
  - `QueryConfigurationException`
    - server-side misconfiguration (map to 5xx responses)
    - thrown for missing `@WebQuery`
    - thrown for invalid or conflicting mappings
    - thrown for unregistered custom operators referenced in `@RsqlFilterable`
    - thrown for duplicate operator symbols across default/custom operators

Suggested controller advice:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QueryValidationException.class)
    public ResponseEntity<String> handleValidationException(QueryValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(QueryConfigurationException.class)
    public ResponseEntity<String> handleConfigurationException(QueryConfigurationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal configuration error");
    }
}
```

## Compatibility

- Java: `21+`
- Spring Boot: `4.0.2+`
- Spring Data JPA + Spring Web MVC

## Contributing

Issues and PRs are welcome.

## License

Licensed under the Apache License, Version 2.0.

https://www.apache.org/licenses/LICENSE-2.0
