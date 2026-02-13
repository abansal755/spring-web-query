# Spring Web Query

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21+](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/technologies/javase-jdk21-downloads.html)
[![Spring Boot 4.0.2+](https://img.shields.io/badge/Spring%20Boot-4.0.2+-brightgreen.svg)](https://spring.io/projects/spring-boot)

`spring-web-query` is a powerful, **secure-by-default** library for Spring Boot that enables declarative, dynamic filtering and sorting for your REST APIs. It bridges the gap between RSQL query strings and Spring Data JPA Specifications while enforcing strict field-level security.

---

## 🚀 Key Features

*   **🔒 Secure Filtering**: Whitelist filterable fields and specific operators using `@RsqlFilterable`.
*   **↕️ Restricted Sorting**: Allow sorting only on fields explicitly marked with `@Sortable`.
*   **📂 Deep Path Resolution**: Native support for nested properties (e.g., `user.address.city`), collections, and arrays.
*   **🔗 API Aliasing**: Use `@FieldMapping` to expose clean API field names without leaking internal entity structures.
*   **⚡ Zero-Config**: Auto-configures argument resolvers for `@RsqlSpec` and `@RestrictedPageable`.
*   **🛡️ DoS Protection**: Built-in maximum page size enforcement.
*   **📅 ISO-8601 Ready**: Seamless handling of date/time formats in query strings.

---

## 📦 Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.abansal755</groupId>
    <artifactId>spring-web-query</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

The library is designed for **Spring Boot 4.x** and **Java 21+**.

---

## 🛠️ Usage

### 1. Annotate your Entity

Explicitly define which fields can be filtered or sorted.

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

    // Nested property example
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

Add the annotations to your controller method arguments.

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
| **Simple Filter** | `/users?filter=status==ACTIVE` |
| **Complex Logical** | `/users?filter=status==ACTIVE;username==john*` |
| **Date Range** | `/users?filter=createdAt=gt=2024-01-01T00:00:00Z` |
| **Nested Paths** | `/users?filter=profile.city==NewYork` |
| **Secure Sorting** | `/users?sort=username,asc` |

---

## ⚙️ Advanced Configuration

### Field Mapping (Aliases)

Hide your internal database structure from the API.

```java
@RsqlSpec(
    entityClass = User.class,
    fieldMappings = {
        @FieldMapping(name = "joined", field = "createdAt")
    }
) Specification<User> spec
```
*Query:* `/users?filter=joined=gt=2024-01-01T00:00:00Z`

### Enforced Pagination Defaults

Control the maximum allowed page size via `application.properties` (default is **100**):

```properties
api.pagination.max-page-size=500
```

---

## ❌ Error Handling

The library throws a `QueryException` when security or syntax rules are violated:
*   Attempting to filter on a non-`@RsqlFilterable` field.
*   Using a restricted operator on a field.
*   Sorting on a non-`@Sortable` field.
*   Invalid RSQL syntax.

**Recommended Global Exception Handler:**

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

## 🏗️ How It Works

1.  **Parsing**: The RSQL string is parsed into an AST.
2.  **Validation**: A custom `RSQLVisitor` traverses the AST and checks every node against the `@RsqlFilterable` configuration on the target Entity.
3.  **Reflection**: `ReflectionUtil` resolves dot-notation paths, handling JPA associations and collection types.
4.  **Specification**: Once validated, it is converted into a `Specification<T>` that is compatible with Spring Data JPA's `findAll(Specification, Pageable)`.

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.