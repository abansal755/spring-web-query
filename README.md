# Spring Web Query

[![License](https://img.shields.io/github/license/abansal755/spring-web-query?style=flat&label=License&color=blue
)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue?style=flat&color=orange)](https://www.oracle.com/java/technologies/javase-jdk21-downloads.html)
[![Spring Boot 4.0.2+](https://img.shields.io/badge/Spring_Boot-4.0.2%2B-orange?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)

[![Maven Central (spring-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fin%2Fco%2Fakshitbansal%2Fspring-web-query-core%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Maven%20Central%20(Core)&color=brightgreen&logo=apachemaven)](https://central.sonatype.com/artifact/in.co.akshitbansal/spring-web-query-core)
[![Maven Central (spring-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fin%2Fco%2Fakshitbansal%2Fspring-web-query-core%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Snapshot%20(Core)&color=yellow&logo=apachemaven)](https://central.sonatype.com/repository/maven-snapshots/in/co/akshitbansal/spring-web-query-core/maven-metadata.xml)

[![Maven Central (spring-boot-starter-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fin%2Fco%2Fakshitbansal%2Fspring-boot-starter-web-query%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Maven%20Central%20(Starter)&color=brightgreen&logo=apachemaven)](https://central.sonatype.com/artifact/in.co.akshitbansal/spring-boot-starter-web-query)
[![Maven Central (spring-boot-starter-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fin%2Fco%2Fakshitbansal%2Fspring-boot-starter-web-query%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Snapshot%20(Starter)&color=yellow&logo=apachemaven)](https://central.sonatype.com/repository/maven-snapshots/in/co/akshitbansal/spring-boot-starter-web-query/maven-metadata.xml)


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
    <version>X.X.X</version>
</dependency>
```

This includes `spring-web-query-core` transitively and auto-registers required configuration.

### Option 2: Core only (manual wiring)

```xml
<dependency>
    <groupId>in.co.akshitbansal</groupId>
    <artifactId>spring-web-query-core</artifactId>
    <version>X.X.X</version>
</dependency>
```

Use this when you do not want Boot starter auto-configuration and prefer manual resolver setup.

The project targets Spring Boot `4.0.2+` and Java `21+`.

---

## Branching And Release Workflow

- `main` always contains `-SNAPSHOT` versions.
- Every commit to `main` publishes a snapshot version to Maven Central.
- Releases are created from `release/**` branches:
  versions are changed to non-snapshot values, and release publishing is triggered manually through a GitHub Action.
- For all non-`main` branch commits and pull requests, CI only verifies build and test success.

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
        @FieldMapping(name = "joined", field = "createdAt", allowOriginalFieldName = false)
    }
) Specification<User> spec
```

- `name`: The alias to be used in the query.
- `field`: The actual entity field path.
- `allowOriginalFieldName`: If `true`, both the alias and original field name can be used. If `false` (default), only the alias is allowed.

Query: `/users?filter=joined=gt=2024-01-01T00:00:00Z`

---

## Custom RSQL Operators

You can define custom operators to extend filtering capabilities.

### 1. Implement `RsqlCustomOperator`

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
        // MySQL example: DAYOFWEEK() returns 1 for Sunday, 2 for Monday...
        return cb.equal(
            cb.function("DAYOFWEEK", Long.class, input.getPath()),
            2
        );
    }
}
```

### 2. Register via `RsqlCustomOperatorsConfigurer`

Register your custom operators as a Spring Bean. You can register multiple `RsqlCustomOperatorsConfigurer` beans, and the library will automatically combine all custom operators from all registered configurers.

```java
@Configuration
public class RsqlConfig {
    @Bean
    public RsqlCustomOperatorsConfigurer customOperators() {
        return () -> Set.of(new IsMondayOperator());
    }
}
```

### 3. Enable in Entity

Whitelisting is required for custom operators just like default ones.

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

Query: `/users?filter=createdAt=monday=`

---

### Enforced Pagination Defaults

Configure maximum allowed page size in `application.properties` (default `100`):

```properties
api.pagination.max-page-size=500
```

---

## Error Handling

The library provides a hierarchy of exceptions to distinguish between client-side validation errors and developer-side configuration issues. All exceptions extend the base `QueryException`.

### Exception Hierarchy

- **`QueryValidationException`**: Thrown when an API consumer provides invalid input. These should typically be returned as a `400 Bad Request`.
  - Filtering on a non-`@RsqlFilterable` field.
  - Using a disallowed operator on a field.
  - Sorting on a non-`@Sortable` field.
  - Using an original field name when a mapping alias is required (`allowOriginalFieldName = false`).
  - Malformed RSQL syntax.
- **`QueryConfigurationException`**: Thrown when the library or entity mapping is misconfigured by the developer. These should typically be treated as a `500 Internal Server Error`.
  - Custom operators referenced in `@RsqlFilterable` that are not registered.
  - Field mappings pointing to non-existent fields on the entity.

### Handling Exceptions

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QueryValidationException.class)
    public ResponseEntity<String> handleValidationException(QueryValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(QueryConfigurationException.class)
    public ResponseEntity<String> handleConfigurationException(QueryConfigurationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal configuration error");
    }

    // Alternatively, catch the base exception for unified handling
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

Licensed under the Apache License, Version 2.0.

You may obtain a copy of the License at:

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
