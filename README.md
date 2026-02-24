# Spring Web Query

[![License](https://img.shields.io/github/license/abansal755/spring-web-query?style=flat&label=License&color=blue
)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue?style=flat&color=orange)](https://www.oracle.com/java/technologies/javase-jdk21-downloads.html)
[![Spring Boot 4.0.2+](https://img.shields.io/badge/Spring_Boot-4.0.2%2B-orange?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)

[![Maven Central (spring-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fin%2Fco%2Fakshitbansal%2Fspring-web-query-core%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Maven%20Central%20(Core)&color=brightgreen&logo=apachemaven)](https://central.sonatype.com/artifact/in.co.akshitbansal/spring-web-query-core)
[![Maven Central (spring-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fin%2Fco%2Fakshitbansal%2Fspring-web-query-core%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Snapshot%20(Core)&color=yellow&logo=apachemaven)](https://central.sonatype.com/repository/maven-snapshots/in/co/akshitbansal/spring-web-query-core/maven-metadata.xml)

[![Maven Central (spring-boot-starter-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fin%2Fco%2Fakshitbansal%2Fspring-boot-starter-web-query%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Maven%20Central%20(Starter)&color=brightgreen&logo=apachemaven)](https://central.sonatype.com/artifact/in.co.akshitbansal/spring-boot-starter-web-query)
[![Maven Central (spring-boot-starter-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fin%2Fco%2Fakshitbansal%2Fspring-boot-starter-web-query%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Snapshot%20(Starter)&color=yellow&logo=apachemaven)](https://central.sonatype.com/repository/maven-snapshots/in/co/akshitbansal/spring-boot-starter-web-query/maven-metadata.xml)

`spring-web-query` brings **safe, declarative filtering + sorting** to Spring Web APIs using RSQL and Spring Data JPA Specifications.

## Table of Contents

- [Why this library exists](#why-this-library-exists)
- [What is RSQL](#what-is-rsql)
- [Project modules](#project-modules)
- [Features](#features)
- [Installation](#installation)
- [Quick start](#quick-start)
- [RSQL operator reference (default operators)](#rsql-operator-reference-default-operators)
- [Advanced configuration](#advanced-configuration)
- [Error handling](#error-handling)
- [Compatibility](#compatibility)
- [Release workflow](#release-workflow)
- [Contributing](#contributing)
- [License](#license)

## Why this library exists

### The common API problem

Most APIs eventually need dynamic querying:

- `GET /users?status=ACTIVE&city=London&createdAfter=...`
- then more requirements: `OR`, ranges, includes/excludes, nested fields, custom comparisons
- then pagination and sorting restrictions

This typically leads to either:

- too many ad-hoc query parameters and controller if/else logic, or
- exposing unrestricted query capabilities that are unsafe

### The declarative approach used here

`spring-web-query` turns this into metadata-driven configuration:

- you **declare** filterable fields and allowed operators using `@RsqlFilterable`
- you **declare** sortable fields using `@Sortable`
- you **declare** API aliases using `@FieldMapping`
- you **declare** method-level query context using `@WebQuery`

At runtime, the library validates incoming RSQL against those declarations before building a `Specification<T>`.

Result: expressive queries for clients, controlled surface area for server code.

## What is RSQL

RSQL (RESTful Service Query Language) is a URL-friendly query language for filtering resources.

Core syntax concepts:

- Comparison: `field==value`, `field=gt=10`, `field=in=(A,B)`
- Logical AND: `;` (semicolon)
- Logical OR: `,` (comma)
- Grouping: parentheses

Examples:

- `status==ACTIVE`
- `status==ACTIVE;createdAt=ge=2025-01-01T00:00:00Z`
- `(status==ACTIVE,status==PENDING);age=gt=18`

In this library, RSQL is parsed, validated against your entity annotations, and translated into Spring Data JPA `Specification`s.

## Project modules

This repo contains two artifacts:

- `spring-web-query-core`
  - Annotations (`@RsqlSpec`, `@RestrictedPageable`, `@WebQuery`, ...)
  - Argument resolvers
  - Validation and reflection utilities
  - Custom operator extension points
- `spring-boot-starter-web-query`
  - Auto-configuration for Spring Boot applications
  - Resolver registration
  - Pagination max page size customization

## Features

- Secure filtering with field/operator whitelisting
- Restricted sorting with explicit `@Sortable` declaration
- Alias mapping (`@FieldMapping`) for API-facing field names
- Nested path resolution (including inheritance, arrays, collections)
- Custom RSQL operators via `RsqlCustomOperator`
- Strict equality semantics for `==` (wildcards are not treated as implicit pattern matching)
- Configurable max page size (`api.pagination.max-page-size`, default `100`)
- ISO-8601 support for timestamp parsing via built-in converter

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

## Quick start

### 1. Annotate your entity

```java
@Entity
public class User {

    @RsqlFilterable(operators = {RsqlOperator.EQUAL, RsqlOperator.IN})
    private String status;

    @RsqlFilterable(operators = {RsqlOperator.GREATER_THAN, RsqlOperator.LESS_THAN})
    private Instant createdAt;

    @Sortable
    @RsqlFilterable(operators = {RsqlOperator.EQUAL})
    private String username;

    @OneToOne
    private Profile profile;
}

@Entity
public class Profile {

    @RsqlFilterable(operators = {RsqlOperator.EQUAL})
    private String city;
}
```

### 2. Use resolver annotations in controller

```java
@GetMapping("/users")
@WebQuery(entityClass = User.class)
public Page<User> search(
    @RsqlSpec Specification<User> spec,
    @RestrictedPageable Pageable pageable
) {
    return userRepository.findAll(spec, pageable);
}
```

`@WebQuery` is required on methods that use `@RsqlSpec` and/or `@RestrictedPageable`.

### 3. Example requests

- Filter: `/users?filter=status==ACTIVE`
- Complex logic: `/users?filter=(status==ACTIVE,status==PENDING);createdAt=gt=2025-01-01T00:00:00Z`
- Nested filter: `/users?filter=profile.city==London`
- Restricted sorting: `/users?sort=username,asc`

## RSQL operator reference (default operators)

The following default operators are available through `RsqlOperator`.

### Equality and inequality

- `EQUAL`
  - Symbols: `==`
  - Meaning: exact equality
  - Example: `name==John`
  - Note: in this library, strict equality is enabled, so `name==John*` matches literal `John*`
- `NOT_EQUAL`
  - Symbols: `!=`
  - Meaning: not equal
  - Example: `status!=DELETED`

### Ordering comparisons

- `GREATER_THAN`
  - Symbols: `>`, `=gt=`
  - Meaning: greater than
  - Example: `age>18`
- `GREATER_THAN_OR_EQUAL`
  - Symbols: `>=`, `=ge=`
  - Meaning: greater than or equal
  - Example: `score=ge=90`
- `LESS_THAN`
  - Symbols: `<`, `=lt=`
  - Meaning: less than
  - Example: `price<100`
- `LESS_THAN_OR_EQUAL`
  - Symbols: `<=`, `=le=`
  - Meaning: less than or equal
  - Example: `price=le=100`

### Set membership

- `IN`
  - Symbols: `=in=`
  - Meaning: value belongs to a set
  - Example: `role=in=(ADMIN,USER)`
- `NOT_IN`
  - Symbols: `=out=`
  - Meaning: value does not belong to a set
  - Example: `region=out=(APAC,EMEA)`

### Null checks

- `IS_NULL`
  - Symbols: `=null=`, `=isnull=`, `=na=`
  - Meaning: field is null
  - Examples: `middleName=null=`, `middleName=isnull=true`
- `NOT_NULL`
  - Symbols: `=notnull=`, `=isnotnull=`, `=nn=`
  - Meaning: field is not null
  - Examples: `email=notnull=`, `email=nn=true`

### Pattern matching

- `LIKE`
  - Symbols: `=like=`, `=ke=`
  - Meaning: SQL LIKE match
  - Example: `description=like=spring`
- `NOT_LIKE`
  - Symbols: `=notlike=`, `=nk=`
  - Meaning: SQL NOT LIKE match
  - Example: `title=notlike=Draft`

### Case-insensitive variants

- `IGNORE_CASE`
  - Symbols: `=icase=`, `=ic=`
  - Meaning: case-insensitive equality
  - Example: `city=icase=london`
- `IGNORE_CASE_LIKE`
  - Symbols: `=ilike=`, `=ik=`
  - Meaning: case-insensitive LIKE
  - Example: `username=ilike=admin`
- `IGNORE_CASE_NOT_LIKE`
  - Symbols: `=inotlike=`, `=ni=`
  - Meaning: case-insensitive NOT LIKE
  - Example: `tag=inotlike=test`

### Range comparisons

- `BETWEEN`
  - Symbols: `=between=`, `=bt=`
  - Meaning: value within inclusive range
  - Example: `createdAt=between=(2025-01-01,2025-12-31)`
- `NOT_BETWEEN`
  - Symbols: `=notbetween=`, `=nb=`
  - Meaning: value outside inclusive range
  - Example: `age=notbetween=(18,65)`

## Advanced configuration

### Field mapping (API aliases)

```java
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

- `name`: alias used by clients
- `field`: real entity field/path
- `allowOriginalFieldName`
  - `false` (default): only alias is allowed
  - `true`: both alias and original field are allowed

Examples:

- Filter with alias: `/users?filter=joined=gt=2025-01-01T00:00:00Z`
- Sort with alias: `/users?sort=joined,desc`

### Custom RSQL operators

1. Implement `RsqlCustomOperator`

```java
public class IsMondayOperator implements RsqlCustomOperator<Long> {
    @Override
    public ComparisonOperator getComparisonOperator() {
        return new ComparisonOperator("=monday=", Arity.nary(0));
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

2. Register operators via `RsqlCustomOperatorsConfigurer`

```java
@Configuration
public class RsqlConfig {
    @Bean
    public RsqlCustomOperatorsConfigurer customOperators() {
        return () -> Set.of(new IsMondayOperator());
    }
}
```

3. Explicitly whitelist custom operator on entity field

```java
@Entity
public class User {
    @RsqlFilterable(
        operators = {RsqlOperator.EQUAL},
        customOperators = {IsMondayOperator.class}
    )
    private LocalDateTime createdAt;
}
```

Request example:

- `/users?filter=createdAt=monday=`

### Pagination max-page-size

Configure in `application.properties`:

```properties
api.pagination.max-page-size=500
```

Default is `100`.

## Error handling

Exception hierarchy:

- `QueryException` (base type)
- `QueryValidationException`
  - invalid RSQL syntax
  - filtering/sorting not allowed for requested fields
  - operator not allowed for a field
- `QueryConfigurationException`
  - missing `@WebQuery`
  - duplicate/invalid field mappings
  - unregistered custom operators referenced by annotations

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
- Spring Data JPA / Spring Web MVC

## Release workflow

- `main` keeps `-SNAPSHOT` versions
- pushes to `main` publish snapshots
- releases are produced from `release/**` branches with GitHub Actions
- PR and non-main branches run CI build/tests only

## Contributing

Issues and PRs are welcome.

Recommended local flow:

1. Create a feature/fix branch
2. Run tests in both modules
3. Open a PR with behavior changes and usage examples when relevant

Build/test commands:

```bash
cd spring-web-query-core
mvn -B -ntp clean install

cd ../spring-boot-starter-web-query
mvn -B -ntp clean package
```

## License

Licensed under the Apache License, Version 2.0.

https://www.apache.org/licenses/LICENSE-2.0
