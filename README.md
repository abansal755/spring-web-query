# Spring Web Query

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21+](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/technologies/javase-jdk21-downloads.html)
[![Spring Boot 4.0.2+](https://img.shields.io/badge/Spring%20Boot-4.0.2+-brightgreen.svg)](https://spring.io/projects/spring-boot)

`spring-web-query` is split into two artifacts:

- `spring-web-query-core`: Core annotations, argument resolvers, validation, and query utilities.
- `spring-boot-starter-web-query`: Spring Boot auto-configuration on top of `core` for zero-config setup.

---

## Key Features

- Secure filtering: Whitelist filterable fields and specific operators using `@RsqlFilterable`.
- Restricted sorting: Allow sorting only on fields explicitly marked with `@Sortable`.
- Deep path resolution: Support for nested properties (for example `user.address.city`), collections, and arrays.
- API aliasing: Use `@FieldMapping` to expose clean API field names without leaking internal entity structures.
- Zero-config (starter): Auto-configures argument resolvers for `@RsqlSpec` and `@RestrictedPageable`.
- DoS protection: Built-in maximum page size enforcement.
- ISO-8601 ready: Handling of date/time formats in query strings.

---

## Installation

Use one of the following depending on your setup.

### Option 1: Spring Boot starter (recommended)

```xml
<dependency>
    <groupId>in.co.akshitbansal</groupId>
    <artifactId>spring-boot-starter-web-query</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

This includes `spring-web-query-core` transitively and auto-registers required configuration.

### Option 2: Core only (manual wiring)

```xml
<dependency>
    <groupId>in.co.akshitbansal</groupId>
    <artifactId>spring-web-query-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Use this when you do not want Boot starter auto-configuration and prefer manual resolver setup.

The project targets Spring Boot `4.0.2+` and Java `21+`.

---

## Usage

### 1. Annotate your Entity

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

### 2. Use in Controller

```java
@GetMapping("/users")
public Page<User> search(
    @RsqlSpec(entityClass = User.class) Specification<User> spec,
    @RestrictedPageable(entityClass = User.class) Pageable pageable
) {
    return userRepository.findAll(spec, pageable);
}
```

### 3. Example Queries

| Feature | Query |
| :--- | :--- |
| Simple Filter | `/users?filter=status==ACTIVE` |
| Complex Logical | `/users?filter=status==ACTIVE;username==john*` |
| Date Range | `/users?filter=createdAt=gt=2024-01-01T00:00:00Z` |
| Nested Paths | `/users?filter=profile.city==NewYork` |
| Secure Sorting | `/users?sort=username,asc` |

---

## Advanced Configuration

### Field Mapping (Aliases)

```java
@RsqlSpec(
    entityClass = User.class,
    fieldMappings = {
        @FieldMapping(name = "joined", field = "createdAt")
    }
) Specification<User> spec
```

Query: `/users?filter=joined=gt=2024-01-01T00:00:00Z`

### Enforced Pagination Defaults

Configure maximum allowed page size in `application.properties` (default `100`):

```properties
api.pagination.max-page-size=500
```

---

## Error Handling

The library throws a `QueryException` when security or syntax rules are violated:

- Filtering on a non-`@RsqlFilterable` field.
- Using a restricted operator on a field.
- Sorting on a non-`@Sortable` field.
- Invalid RSQL syntax.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(QueryException.class)
    public ResponseEntity<String> handleQueryException(QueryException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
```

---

## How It Works

1. Parsing: The RSQL string is parsed into an AST.
2. Validation: A custom `RSQLVisitor` traverses the AST and checks every node against the `@RsqlFilterable` configuration on the target entity.
3. Reflection: `ReflectionUtil` resolves dot-notation paths, handling JPA associations and collection types.
4. Specification: Once validated, it is converted into a `Specification<T>` compatible with Spring Data JPA `findAll(Specification, Pageable)`.

---

## License

Distributed under the MIT License.
