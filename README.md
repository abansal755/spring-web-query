<div align="center">
    <h1>Spring Web Query</h1>
    <div>
        <a href="https://www.apache.org/licenses/LICENSE-2.0">
            <img alt="GitHub License" src="https://img.shields.io/github/license/abansal755/spring-web-query?style=for-the-badge&label=License&color=blue">
        </a>
        <img alt="Static Badge" src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge">
        <img alt="Static Badge" src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge">
        <img alt="Static Badge" src="https://img.shields.io/badge/Spring_Boot-3-green?style=for-the-badge&logo=spring&logoColor=white">
        <img alt="Static Badge" src="https://img.shields.io/badge/Spring_Boot-4-green?style=for-the-badge&logo=spring&logoColor=white">
    </div>
    <div>
        <a href="https://central.sonatype.com/artifact/in.co.akshitbansal/spring-boot-starter-web-query">
            <img alt="Maven Central Version" src="https://img.shields.io/maven-central/v/in.co.akshitbansal/spring-boot-starter-web-query?strategy=highestVersion&style=for-the-badge&logo=apachemaven&label=Maven%20Central&color=green">
        </a>
        <img alt="Maven metadata URL" src="https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fin%2Fco%2Fakshitbansal%2Fspring-web-query-core%2Fmaven-metadata.xml&strategy=highestVersion&style=for-the-badge&logo=apachemaven&label=Maven%20Central%20(Snapshot)&color=yellow">
    </div>
</div>
<br>

`spring-web-query` is a Spring Boot library that provides a **declarative**, **secure**, and **high-performance** way to implement filtering, pagination, and sorting for your Spring MVC APIs.

It leverages **RSQL** (a URL-friendly query language) to provide powerful filtering capabilities while ensuring that only explicitly whitelisted fields and operators are exposed to clients.

## Key Features

- **Declarative Contracts**: Define your query API using annotations on DTOs.
- **RSQL Filtering**: Support for complex filters like `status==ACTIVE;joinedAt>=2025-01-01T00:00:00Z`.
- **Safe Path Mapping**: Decouple your API field names from your database schema using `@MapsTo`.
- **Tuple Projections**: Fetch only the data you need using constructor projections for maximum performance.
- **Strict Validation**: Built-in protection against unauthorized filtering, sorting, or overly complex queries.
- **Spring Data Integration**: Seamlessly integrates as a Spring Data repository fragment.

## Table of Contents

- [How it Works](#how-it-works)
- [Installation](#installation)
- [Testing Matrix](#testing-matrix)
- [Quick Start](#quick-start)
- [Repository Fragment Methods](#repository-fragment-methods)
- [Projecting Results to DTOs](#projecting-results-to-dtos)
- [RSQL Guide](#rsql-guide)
- [Annotation Reference](#annotation-reference)
- [Custom Operators](#custom-operators)
- [Global Configuration](#global-configuration)
- [Exception Handling](#exception-handling)
- [Performance](#performance)
- [License](#license)

---

## How it Works

`spring-web-query` operates as a **repository-first** library. Instead of hidden magic in controllers, it provides a `WebQueryRepository<E>` fragment that you extend in your standard Spring Data JPA repositories.

1.  **Define a DTO**: Annotate a DTO class to define which fields are filterable and sortable.
2.  **Extend Repository**: Have your repository interface extend `WebQueryRepository<EntityClass>`.
3.  **Call from Controller**: Accept the RSQL string and `Pageable` in your controller and pass them to the repository.

At runtime, the library:
- Parses the RSQL string into an AST.
- Validates the AST against your DTO's annotations.
- Translates DTO field paths to JPA Entity paths.
- Builds a `CriteriaQuery` with the validated filters and sort orders.
- Executes the query using efficient Tuple projections.
- Materializes the results into your DTO instances using constructor injection.

---

## Installation

Add the following dependency to your project. Replace `${version}` with the latest version from Maven Central.

### Maven
```xml
<dependency>
    <groupId>in.co.akshitbansal</groupId>
    <artifactId>spring-boot-starter-web-query</artifactId>
    <version>${version}</version>
</dependency>
```

### Gradle (Groovy)
```groovy
implementation 'in.co.akshitbansal:spring-boot-starter-web-query:${version}'
```

### Gradle (Kotlin)
```kotlin
implementation("in.co.akshitbansal:spring-boot-starter-web-query:${version}")
```

---

## Testing Matrix

The library is rigorously tested against the following combinations to ensure stability and compatibility:

| Java | Spring Boot |
| :--- | :--- |
| **17** | 3 |
| **17** | 4 |
| **21** | 3 |
| **21** | 4 |

---

## Quick Start

### 1. Define your Entity

```java
@Entity
public class UserEntity {

    @Id 
    private Long id;

    private String username;
    private String status;
    private Instant createdAt;

    @OneToOne
    private ProfileEntity profile;
}
```

### 2. Define your Query DTO

The DTO acts as the contract for your API. Use `@RSQLFilterable` and `@Sortable` to whitelist fields.

```java
public class UserRes {

    private Long id;
    
    @RSQLFilterableText
    @Sortable
    private String username;
    
    @RSQLFilterableEquality
    private String status;
    
    @RSQLFilterableRange
    @MapsTo("profile.city")
    private String city;
}
```

### 3. Extend the Repository

Extend `WebQueryRepository` to gain access to the `findAllPaged` and `findAll` methods.

```java
public interface UserRepository extends 
    JpaRepository<UserEntity, Long>, WebQueryRepository<UserEntity> {
}
```

### 4. Use in Service/Controller

```java
@GetMapping("/users")
public Page<UserRes> search(
    @RequestParam(name = "filter", required = false) String filter,
    Pageable pageable
) {
    return userRepository.findAllPaged(
        filter,
        pageable,
        (root, query, cb) -> List.of(
            root.get("id"),
            root.get("username"),
            root.get("status"),
            root.get("profile").get("city")
        ),
        UserRes.class
    );
}
```

---

## Repository Fragment Methods

By extending `WebQueryRepository<E>`, your repository gains several methods that handle the entire query lifecycle (parsing, validation, mapping, execution, and projection).

Each method comes in variants that either use the global [Global Configuration](#global-configuration) or allow you to override them for a specific call.

### `findAllPaged`
The primary method for most API endpoints. It returns a Spring Data `Page<D>`.
-   **Variants**:
    -   `findAllPaged(rsql, pageable, selections, dtoClass)`: Uses global validation settings.
    -   `findAllPaged(rsql, pageable, selections, customizer, dtoClass)`: Allows adding a `SpecificationCustomizer`.
    -   `findAllPaged(rsql, pageable, selections, customizer, dtoClass, allowAnd, allowOr, maxDepth)`: Full control over validation.
-   **Execution Strategy**: Executes a count query first. If the count is zero, it skips the content query and returns an empty page immediately.

### `findAll`
Returns a `List<D>` of results for the requested page window.
-   **Variants**:
    -   `findAll(rsql, pageable, selections, dtoClass)`: Uses global validation settings.
    -   `findAll(rsql, pageable, selections, customizer, dtoClass)`: Allows adding a `SpecificationCustomizer`.
    -   `findAll(rsql, pageable, selections, customizer, dtoClass, allowAnd, allowOr, maxDepth)`: Full control over validation.
-   **Behavior**: Applies the offset and limit from the `Pageable` but does not issue a count query.

### `count`
Returns a `long` representing the number of rows matching the filter.
-   **Variants**:
    -   `count(rsql, dtoClass)`
    -   `count(rsql, customizer, dtoClass)`
    -   `count(rsql, customizer, dtoClass, allowAnd, allowOr, maxDepth)`
-   **Behavior**: Reuses the exact same validation and path mapping logic as the result queries to ensure consistency.

---

## Projecting Results to DTOs

The library uses **Constructor Projection** to materialize DTOs from JPA Tuples.

- **Positional Matching**: The order of selections in your `SelectionsProvider` must match the order of parameters in your DTO constructor.
- **Type Safety**: The types in the constructor must be assignable from the types returned by the JPA selections.
- **Disambiguation**: If your DTO has multiple constructors, use `@PersistenceCreator` to mark the one intended for query projection.

---

## RSQL Guide

### What is RSQL?

**RSQL** (RESTful Service Query Language) is a URI-friendly syntax for expressing complex queries. It is a superset of **FIQL** (Feed Item Query Language) and is designed to be easily embeddable in HTTP query parameters.

An RSQL expression is composed of one or more comparisons, joined by logical operators:
-   **Comparisons**: `selector<operator>argument` (e.g., `age=gt=18`)
-   **Logical AND**: `;` (e.g., `status==ACTIVE;age=gt=18`)
-   **Logical OR**: `,` (e.g., `status==ACTIVE,status==PENDING`)
-   **Grouping**: `()` (e.g., `(status==ACTIVE,status==PENDING);age=gt=18`)

### Default Operators

`spring-web-query` supports a comprehensive set of built-in operators:

| Group | Operator(s) | Description | Example |
| :--- | :--- | :--- | :--- |
| **Equality** | `==` | Strict Equality | `name=="John Doe"` |
| | `!=` | Inequality | `status!="DELETED"` |
| **Ordering** | `> `, `=gt=` | Greater Than | `age>18` |
| | `>=`, `=ge=` | Greater Than or Equal | `price>=100.0` |
| | `< `, `=lt=` | Less Than | `score<50` |
| | `<=`, `=le=` | Less Than or Equal | `count<=10` |
| **Membership**| `=in=` | Set Membership | `role=in=(ADMIN,USER)` |
| | `=out=` | Set Non-membership | `dept=out=(HR,FIN)` |
| **Null Checks**| `=null=`, `=isnull=`, `=na=` | Is Null | `middleName=null=` |
| | `=notnull=`, `=isnotnull=`, `=nn=` | Is Not Null | `email=notnull=` |
| **Text** | `=like=`, `=ke=` | Substring Match | `desc=like="spring"` |
| | `=notlike=`, `=nk=` | Negative Substring Match | `title=notlike="Draft"` |
| | `=icase=`, `=ic=` | Case-insensitive Equality | `city=icase=london` |
| | `=ilike=`, `=ik=` | Case-insensitive Like | `user=ilike="admin"` |
| | `=inotlike=`, `=ni=` | Case-insensitive Not Like | `tag=inotlike="test"` |
| **Range** | `=between=`, `=bt=` | Range (inclusive) | `age=between=(18,65)` |
| | `=notbetween=`, `=nb=` | Outside Range | `score=notbetween=(1,10)` |

> **Note on Equality (`==`)**: By default, this library treats `==` as strict literal equality. Wildcards like `*` are treated as literal characters, not as pattern markers. For pattern matching, use the `=like=` or `=ilike=` operators.

---

## Annotation Reference

### Primary Annotations

#### `@RSQLFilterable`
The base annotation to whitelist a field for filtering.
- `value`: An array of `RSQLDefaultOperator` (e.g., `{RSQLDefaultOperator.EQUAL, RSQLDefaultOperator.IN}`).
- `customOperators`: An array of implementation classes for custom operators.

You can apply multiple `@RSQLFilterable` annotations to a single field (can also be used with convenience annotations mentioned below), the allowed operators will be the union of all declared operators.

#### `@Sortable`
Whitelists a field for use in the `sort` parameter. If a field is not annotated with `@Sortable`, any attempt to sort by it will trigger a `QueryFieldValidationException`.

#### `@MapsTo`
Maps a DTO field to a specific path in the underlying JPA Entity.
- `value`: The target entity path (e.g., `profile.address.city`).
- `absolute`: If `true`, the mapping ignores any parent DTO path prefixes and starts from the entity root.

---

### Convenience Annotations

These annotations are shortcuts for common sets of operators. They can be combined on the same field.

#### `@RSQLFilterableEquality`
Enables basic equality checks.
- **Operators**: `==`, `!=`

#### `@RSQLFilterableMembership`
Enables set-based inclusion and exclusion.
- **Operators**: `=in=`, `=out=`

#### `@RSQLFilterableNull`
Enables nullability checks.
- **Operators**: `=null=`, `=notnull=` (and their aliases)

#### `@RSQLFilterableRange`
Enables comparison and range-based filtering. Ideal for numbers and dates.
- **Operators**: `>`, `>=`, `<`, `<=`, `=between=`, `=notbetween=`

#### `@RSQLFilterableText`
Enables advanced text matching capabilities.
- **Operators**: `=like=`, `=notlike=`, `=icase=`, `=ilike=`, `=inotlike=`

---

## Custom Operators

Adding a custom operator is a two-step process:

### 1. Implement the Operator
Create a class implementing `RSQLCustomOperator<T>`.

```java
@Component // Register as a Spring Bean
public class IsMondayOperator implements RSQLCustomOperator<Instant> {

    private final ComparisonOperator operator = new ComparisonOperator("=monday=");

    @Override
    public ComparisonOperator getComparisonOperator() {
        return operator;
    }

    @Override
    public Class<Instant> getType() {
        return Instant.class;
    }

    @Override
    public Predicate toPredicate(RSQLCustomPredicateInput input) {
        CriteriaBuilder cb = input.getCriteriaBuilder();
        // Custom JPA Criteria logic: extract day of week and check if it's Monday
        return cb.equal(cb.function("DAYOFWEEK", Integer.class, input.getPath()), 2);
    }
}
```

### 2. Enable on DTO field
Reference your operator class in the `@RSQLFilterable` annotation.

```java
public class MyDTO {
    
    @RSQLFilterable(customOperators = IsMondayOperator.class)
    Instant createdAt;
}
```

> **Automatic Registration**: In a Spring Boot application using the starter, any bean implementing `RSQLCustomOperator` is automatically discovered and registered with the RSQL parser and validation engine.

---

## Global Configuration

Customize the library behavior through these `application.properties`:

### Filtering Configuration

| Property | Default | Description |
| :--- | :--- | :--- |
| `spring-web-query.filtering.allow-and-operation` | `true` | Whether to allow the logical AND operator (`;`) in RSQL expressions. |
| `spring-web-query.filtering.allow-or-operation` | `false` | Whether to allow the logical OR operator (`,`) in RSQL expressions. |
| `spring-web-query.filtering.max-ast-depth` | `1` | The maximum allowed depth of the parsed RSQL Abstract Syntax Tree (AST). |

#### Understanding `max-ast-depth`

The `max-ast-depth` property is a security feature that prevents clients from sending overly complex or recursive filter expressions that could cause performance issues during parsing or execution.

The depth is calculated starting from `0` for the root nodes.

*   **Depth 0**: Only allows a single comparison.
    *   *Example*: `status==ACTIVE`
*   **Depth 1**: Allows a single top-level logical group.
    *   *Example*: `status==ACTIVE;role==ADMIN` (The `AND` node is at depth 0, children are at depth 1)
*   **Depth 2**: Allows one level of nesting with parentheses.
    *   *Example*: `(status==ACTIVE,status==PENDING);role==ADMIN` (The root `AND` is depth 0, the `OR` group is depth 1, and the comparisons inside the `OR` are depth 2)

If a query exceeds the configured depth, the library throws a `QueryMaxASTDepthExceededException`.

### Performance & Caching

The library caches the reflective mapping between DTO fields and Entity paths to ensure minimal overhead per request. It also caches discovered constructors used for Tuple projections.

| Property | Default | Description |
| :--- | :--- | :--- |
| `spring-web-query.field-resolution.caching.enabled` | `true` | Enables or disables the path resolution cache. |
| `spring-web-query.field-resolution.caching.failed-resolutions-max-capacity` | `1000` | The maximum number of failed path resolutions to cache (prevents repeated failed reflective lookups for invalid fields). |
| `spring-web-query.field-resolution.caching.lock-stripe-count` | `32` | Number of stripes for the fine-grained locking used during cache population. |
| `spring-web-query.constructor-discovery.caching.enabled` | `true` | Enables or disables the global cache for DTO constructor discovery. |

---

## Exception Handling

`spring-web-query` provides a structured exception hierarchy to help you distinguish between client-side usage errors and server-side configuration issues.

### Hierarchy

```text
QueryException (Root)
├── QueryValidationException (Client-side errors)
│   ├── QueryFieldValidationException
│   │   └── QueryForbiddenOperatorException
│   ├── QueryForbiddenLogicalOperatorException
│   └── QueryMaxASTDepthExceededException
└── QueryConfigurationException (Server-side configuration errors)
```

### Error Types

- **Client-side Errors (`QueryValidationException`)**: These occur when the incoming request violates the query contract. This includes using disallowed fields, unauthorized operators, or exceeding the maximum AST depth. These should typically be mapped to a **400 Bad Request** response.
- **Server-side Errors (`QueryConfigurationException`)**: These indicate a development-time issue, such as an invalid `@MapsTo` path or a missing custom operator registration. These should typically be mapped to a **500 Internal Server Error**.

### Controller Advice Example

You can use a `@RestControllerAdvice` to provide consistent error responses across your API:

```java
@RestControllerAdvice
public class GlobalQueryExceptionHandler {

    @ExceptionHandler(QueryValidationException.class)
    public ResponseEntity<String> handleValidation(QueryValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(QueryConfigurationException.class)
    public ResponseEntity<String> handleConfiguration(QueryConfigurationException ex) {
        return ResponseEntity.internalServerError().body("Internal query configuration error");
    }
}
```

---

## Performance

`spring-web-query` is designed for high-traffic environments:
- **Cached Path Mapping**: DTO-to-Entity path resolution is cached to minimize reflection overhead.
- **Tuple Projections**: Uses JPA Tuples to avoid loading full entities, reducing memory pressure and DB IO.

---

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
