# Spring Web Query

[![License](https://img.shields.io/badge/License-Apache--2.0-blue?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java 17/21](https://img.shields.io/badge/Java-17%20%7C%2021-blue?style=flat&color=orange)](https://www.oracle.com/java/technologies/downloads/)
[![Spring Boot 3.5.12/4.0.2](https://img.shields.io/badge/Spring_Boot-3%20%7C%204-orange?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)

[![Maven Central (spring-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fin%2Fco%2Fakshitbansal%2Fspring-web-query-core%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Maven%20Central%20(Core)&color=brightgreen&logo=apachemaven)](https://central.sonatype.com/artifact/in.co.akshitbansal/spring-web-query-core)
[![Maven Central (spring-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fin%2Fco%2Fakshitbansal%2Fspring-web-query-core%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Snapshot%20(Core)&color=yellow&logo=apachemaven)](https://central.sonatype.com/repository/maven-snapshots/in/co/akshitbansal/spring-web-query-core/maven-metadata.xml)

[![Maven Central (spring-boot-starter-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fin%2Fco%2Fakshitbansal%2Fspring-boot-starter-web-query%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Maven%20Central%20(Starter)&color=brightgreen&logo=apachemaven)](https://central.sonatype.com/artifact/in.co.akshitbansal/spring-boot-starter-web-query)
[![Maven Central (spring-boot-starter-web-query)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fin%2Fco%2Fakshitbansal%2Fspring-boot-starter-web-query%2Fmaven-metadata.xml&strategy=highestVersion&style=flat&label=Snapshot%20(Starter)&color=yellow&logo=apachemaven)](https://central.sonatype.com/repository/maven-snapshots/in/co/akshitbansal/spring-boot-starter-web-query/maven-metadata.xml)

`spring-web-query` allows you to add **filtering**, **pagination**, and **sorting** to your Spring MVC APIs in a **declarative** manner.

It uses **RSQL** (a URL-friendly query language for filtering, e.g. `status==ACTIVE;age>18`) and resolves validated `Specification<T>` + `Pageable` automatically.

You declare what clients can query, and the library enforces that contract at the API boundary.

```java
@GetMapping("/users")
@WebQuery(entityClass = User.class, dtoClass = UserResponse.class)
public Page<UserResponse> search(Specification<User> spec, Pageable pageable) {
    return userRepository.findAll(spec, pageable).map(this::toResponse);
}
```

Example request:

```http
GET /users?filter=status==ACTIVE;joinedAt>=2025-01-01T00:00:00Z&sort=joinedAt,desc&page=0&size=20
```

It also supports nested JPA relationship paths (join-style queries), for example filtering by `profile.city`.

## Table of contents

- [Why this library exists](#why-this-library-exists)
- [What you get](#what-you-get)
- [How it works](#how-it-works)
- [Project modules](#project-modules)
- [Installation](#installation)
- [Quick start (DTO-aware, recommended)](#quick-start-dto-aware-recommended)
- [Tuple projections with `WebQueryRepository`](#tuple-projections-with-webqueryrepository)
- [Entity-aware mode](#entity-aware-mode)
- [RSQL guide](#rsql-guide)
- [Annotation reference](#annotation-reference)
- [Custom operators](#custom-operators)
- [Pageable behavior](#pageable-behavior)
- [Exception Handling](#exception-handling)
- [Best practices](#best-practices)
- [Documentation](#documentation)
- [Compatibility](#compatibility)
- [Contributing](#contributing)
- [License](#license)

## Why this library exists

Most production APIs eventually need dynamic querying.

Initial requirements look simple:

- `GET /users?status=ACTIVE`
- `GET /users?city=London`

Soon, you need:

- nested fields
- OR/AND logic
- range and set operations
- controlled sorting and pagination

Typical outcomes without a shared query layer:

- repeated parser/mapper logic in controllers/services
- inconsistent query behavior across endpoints
- accidental exposure of internal entity fields
- unsafe or expensive queries slipping into production

`spring-web-query` solves this by turning querying into a contract:

- `@RSQLFilterable`: which fields are filterable and which operators are allowed
- `@Sortable`: which fields can be sorted
- `@WebQuery`: endpoint-level query context (`entityClass`, optional `dtoClass`, aliases, optional filter param override)
- framework-provided argument resolvers for `Specification<T>` and `Pageable`

You keep endpoint code focused on business behavior, not query parsing.

## What you get

### Core functional value

- Declarative query contracts through annotations
- RSQL parsing and validation before query execution
- Automatic conversion to Spring Data JPA `Specification<T>`
- Sort validation and path mapping for `Pageable`
- Join-style filtering and sorting across JPA entity relationships via nested paths

### Safety and governance

- Whitelist-only filtering and sorting
- Operator-level permissions per field
- AST depth limits to prevent overly complex filter expressions
- Field-level exceptions with meaningful context
- Configuration-time guardrails for duplicate aliases/operator symbols

### API design flexibility

- DTO-aware mode to decouple API query names from entity model
- Entity-aware mode for simpler setups
- Alias mapping in entity-aware mode (`@FieldMapping`)
- DTO path translation in DTO-aware mode (`@MapsTo`)

### Spring Boot integration

- Auto-configuration for resolvers and utility beans
- Optional custom operator registration hook
- Configurable global filtering defaults and max page size via properties
- built-in ISO-8601 `Timestamp` converter for RSQL values

## How it works

At request time, `spring-web-query` follows this flow:

1. Read `@WebQuery` metadata from your controller method.
2. Parse filter expression from the configured request query param (starter default: `filter`).
3. Validate all selectors and operators against your annotations.
4. Translate request paths to entity paths (DTO-aware or alias-aware).
5. Build a JPA `Specification<T>`.
6. Parse `Pageable` using Spring’s standard resolver.
7. Validate and remap sort fields.
8. Return validated `Specification<T>` + `Pageable` to your controller method.

This means the query contract is consistently enforced at the web boundary.

Because nested paths are validated and mapped, you can expose relationship-based querying (joins) safely instead of hand-building join logic per endpoint.

## Project modules

This repository publishes two Maven artifacts.

### `spring-boot-starter-web-query` (recommended)

Use this for most projects.

Includes:

- core library
- auto-configured argument resolvers
- auto-configured beans (`AnnotationUtil`, operator sets)

### `spring-web-query-core`

Use this if you want full manual wiring of beans/resolvers.

Includes:

- annotations
- validation visitors
- resolver implementations
- utility layer
- custom operator contracts

## Installation

### Option 1: Starter (recommended)

```xml
<dependency>
    <groupId>in.co.akshitbansal</groupId>
    <artifactId>spring-boot-starter-web-query</artifactId>
    <version>X.X.X</version>
</dependency>
```

### Option 2: Core only

```xml
<dependency>
    <groupId>in.co.akshitbansal</groupId>
    <artifactId>spring-web-query-core</artifactId>
    <version>X.X.X</version>
</dependency>
```

If you use core directly, register resolver beans and MVC configuration manually. That includes enabling Spring Data Web support yourself when you want Spring MVC to resolve `Pageable` arguments.

## Quick start (DTO-aware, recommended)

DTO-aware mode is the preferred approach for external/public APIs.

Why:

- your public query contract is explicit and stable
- entity refactors are less likely to break clients
- you control naming and structure exposed to consumers

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

### 2. Define query DTO contract

```java
public class UserQuery {

    @RSQLFilterable({RSQLDefaultOperator.EQUAL, RSQLDefaultOperator.IN})
    @Sortable
    private String status;

    @RSQLFilterable({
        RSQLDefaultOperator.GREATER_THAN,
        RSQLDefaultOperator.GREATER_THAN_OR_EQUAL,
        RSQLDefaultOperator.LESS_THAN,
        RSQLDefaultOperator.LESS_THAN_OR_EQUAL
    })
    @Sortable
    @MapsTo("createdAt")
    private Instant joinedAt;

    @RSQLFilterableText
    @Sortable
    private String username;

    private ProfileQuery profile;

    public static class ProfileQuery {

        @RSQLFilterable({RSQLDefaultOperator.EQUAL, RSQLDefaultOperator.NOT_EQUAL})
        @MapsTo("city")
        private String city;
    }
}
```

### 3. Use in controller

```java
@GetMapping("/users")
@WebQuery(entityClass = User.class, dtoClass = UserQuery.class)
public Page<UserResponse> search(
        Specification<User> spec,
        Pageable pageable
) {
    return userRepository.findAll(spec, pageable)
            .map(this::toResponse);
}
```

If you need logical OR in filters, override the endpoint policy explicitly:

```java
@WebQuery(
    entityClass = User.class,
    dtoClass = UserQuery.class,
    allowOrOperator = WebQuery.OperatorPolicy.ALLOW
)
```

Your repository interface should extend `JpaSpecificationExecutor<User>` (typically alongside `JpaRepository`) so Spring Data can execute `findAll(spec, pageable)`.

```java
public interface UserRepository
        extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
}
```

### 4. Call it

```http
GET /users?filter=status==ACTIVE
GET /users?filter=joinedAt=ge=2025-01-01T00:00:00Z;joinedAt=lt=2026-01-01T00:00:00Z
GET /users?filter=(status==ACTIVE,status==PENDING);profile.city==London  // requires allowOrOperator = WebQuery.OperatorPolicy.ALLOW
GET /users?sort=joinedAt,desc&sort=username,asc&page=0&size=20
```

## Tuple projections with `WebQueryRepository`

When you want validated filtering and sorting from `Specification<T>` + `Pageable`, but do not want to load full entities, extend `WebQueryRepository<T>` in your Spring Data repository.

This repository fragment lets you define a custom select clause while still reusing the same query contract already enforced by `@WebQuery`.

```java
public interface UserRepository extends
        JpaRepository<User, Long>,
        JpaSpecificationExecutor<User>,
        WebQueryRepository<User> {
}
```

```java
Page<Tuple> page = userRepository.findAllPaged(
        spec,
        pageable,
        (root, query, cb) -> List.of(
                root.get("id").alias("id"),
                root.get("username").alias("username"),
                root.get("profile").get("city").alias("city")
        )
);
```

Use:

- `findAll(...)` when you want a plain `List<Tuple>`
- `findAllPaged(...)` when you want a `Page<Tuple>` and need paging metadata
- `findAll(..., dtoClass)` when you want a plain `List<DTO>` backed by constructor projection
- `findAllPaged(..., dtoClass)` when you want a `Page<DTO>` and need paging metadata

In both cases, the fragment:

- applies the incoming `Specification<T>` as the filter
- applies sort information from `Pageable`
- applies page size and offset when the request is paged
- leaves the selected columns under your control via the selection callback
- passes the live `CriteriaQuery<?>` into the selection callback so you can build correlated subqueries when needed

For paged queries, avoid mutating the outer query in ways that change row cardinality, such as enabling `distinct`
or adding `groupBy`, because total counts are computed from a separate count query.

### DTO projection from tuple selections

If you want the fragment to return DTOs instead of raw `Tuple` objects, pass the DTO class to the overloaded methods.

```java
public record UserSummary(Long id, String username, String city) {
}
```

```java
Page<UserSummary> page = userRepository.findAllPaged(
        spec,
        pageable,
        (root, query, cb) -> List.of(
                root.get("id"),
                root.get("username"),
                root.get("profile").get("city")
        ),
        UserSummary.class
);
```

The DTO conversion works as follows:

- the `SelectionsProvider` still defines a tuple projection first
- each tuple row is then converted into the requested DTO type
- constructor arguments are bound by position, not by tuple alias or selection name
- the number of selected expressions must match the number of constructor parameters
- each constructor parameter type must be runtime-compatible with the corresponding tuple element type

That means the order of your selections is part of the contract. For example, this constructor:

```java
public record UserSummary(Long id, String username, String city) {
}
```

must be matched by selections in exactly that order:

```java
List.of(
        root.get("id"),
        root.get("username"),
        root.get("profile").get("city")
)
```

### How constructor selection works

When converting tuple rows into DTO instances, the library inspects the DTO constructors and looks for a compatible match.

A constructor is considered compatible when:

- it has the same number of parameters as the tuple has selected elements
- each parameter type is assignable from the corresponding tuple element Java type
- primitive parameter types are compared using their wrapper equivalents

If multiple constructors are compatible, a constructor annotated with `@PersistenceCreator` is preferred.

```java
public class UserSummary {

    private final Long id;
    private final String username;
    private final String city;

    @PersistenceCreator
    public UserSummary(Long id, String username, String city) {
        this.id = id;
        this.username = username;
        this.city = city;
    }
}
```

Recommendations:

- define one clear constructor for projection DTOs whenever possible
- if you use `@PersistenceCreator`, keep at most one constructor annotated with it

If more than one constructor is annotated with `@PersistenceCreator`, or if multiple compatible constructors exist and no unique preference exists, behavior is unpredictable.

## Entity-aware mode

Entity-aware mode validates directly against entity fields.

Use this when:

- you control both API consumers and persistence model tightly
- you want minimal setup
- you do not need a separate query DTO contract

```java
@GetMapping("/users")
@WebQuery(
    entityClass = User.class,
    fieldMappings = {
        @FieldMapping(name = "joined", field = "createdAt", allowOriginalFieldName = false),
        @FieldMapping(name = "city", field = "profile.city", allowOriginalFieldName = true)
    }
)
public Page<User> search(
        Specification<User> spec,
        Pageable pageable
) {
    return userRepository.findAll(spec, pageable);
}
```

In entity-aware mode:

- put `@RSQLFilterable` (or composed variants) on entity fields
- put `@Sortable` on entity fields
- optionally expose aliases via `@FieldMapping`

Example requests:

```http
GET /users?filter=joined=gt=2025-01-01T00:00:00Z
GET /users?filter=city==London
GET /users?sort=joined,desc
```

## RSQL guide

RSQL is a compact, URL-friendly query language. In this library, RSQL powers the `filter` parameter and is validated against your `@RSQLFilterable` contract.

### Query shape

Each expression is:

`<selector><operator><argument>`

Examples:

- `status==ACTIVE`
- `age=gt=18`
- `profile.city==London`
- `createdAt=between=(2025-01-01T00:00:00Z,2026-01-01T00:00:00Z)`

### Logical composition

- AND: `;`
- OR: `,`
- Parentheses for precedence: `(status==ACTIVE,status==PENDING);age=ge=18`

With the starter defaults, AND is allowed, OR is disabled, and `maxASTDepth` defaults to `1`. You can override this per endpoint via `@WebQuery(allowAndOperator = ..., allowOrOperator = ..., maxASTDepth = ...)`, or globally with Spring properties.

### Operator reference

#### Equality

- `==` (equal)
- `!=` (not equal)

#### Ordering

- `=gt=` or `>` (greater than)
- `=ge=` or `>=` (greater than or equal)
- `=lt=` or `<` (less than)
- `=le=` or `<=` (less than or equal)

#### Membership

- `=in=`: `status=in=(ACTIVE,PENDING)`
- `=out=`: `status=out=(DELETED,BLOCKED)`

#### Null checks

- `=null=` / `=isnull=` / `=na=`
- `=notnull=` / `=isnotnull=` / `=nn=`

#### Text

- `=like=` / `=ke=`
- `=notlike=` / `=nk=`
- `=icase=` / `=ic=`
- `=ilike=` / `=ik=`
- `=inotlike=` / `=ni=`

#### Range

- `=between=` / `=bt=`
- `=notbetween=` / `=nb=`

### Common API patterns

#### Multi-value status filter with OR

```http
GET /users?filter=(status==ACTIVE,status==PENDING)
```

#### Date window filter (inclusive lower bound, exclusive upper bound)

```http
GET /users?filter=createdAt=ge=2025-01-01T00:00:00Z;createdAt=lt=2026-01-01T00:00:00Z
```

#### Nested field filter + sort + pagination

```http
GET /users?filter=profile.city==London;status==ACTIVE&sort=joinedAt,desc&page=0&size=20
```

Here, `profile.city` is a relationship path (join-style query across associated entities).

#### IN list + not-null constraint

```http
GET /users?filter=role=in=(ADMIN,MANAGER);email=notnull=
```

### URL and encoding notes

- RSQL is sent in the query string, so URL encoding may be required by clients/proxies.
- ISO timestamps with `+` offset must encode `+` as `%2B`.
- Example:

```http
GET /users?filter=createdAt=ge=2025-12-08T00:00:00%2B00:00
```

### Strict equality behavior

This library enables strict equality conversion for `==`.

`name==John*` is treated as a literal equality against `John*`, not wildcard prefix matching.

## Annotation reference

### `@WebQuery`

Applied on controller methods.

- `entityClass`: required
- `dtoClass`: optional, default `void.class`
- `fieldMappings`: optional aliases (entity-aware mode)
- `filterParamName`: optional, default `""` (uses the configured global filter param name)
- `allowAndOperator`: optional, default `WebQuery.OperatorPolicy.GLOBAL`
- `allowOrOperator`: optional, default `WebQuery.OperatorPolicy.GLOBAL`
- `maxASTDepth`: optional, default `-1` (delegates to the configured global default)

Example custom filter param name:

```java
@WebQuery(entityClass = User.class, dtoClass = UserQuery.class, filterParamName = "q")
```

Request:

```http
GET /users?q=status==ACTIVE
```

If `filterParamName` is left blank, the resolver uses the configured global default filter parameter name instead.

Example enabling OR:

```java
@WebQuery(
    entityClass = User.class,
    dtoClass = UserQuery.class,
    allowOrOperator = WebQuery.OperatorPolicy.ALLOW
)
```

Example allowing deeper nested filter groups:

```java
@WebQuery(
    entityClass = User.class,
    dtoClass = UserQuery.class,
    allowOrOperator = WebQuery.OperatorPolicy.ALLOW,
    maxASTDepth = 2
)
```

`maxASTDepth` is measured from the root AST node, which starts at depth `0`.

- `status==ACTIVE` has a single comparison node at depth `0`
- `status==ACTIVE;role==ADMIN` has an `AND` node at depth `0` and comparison children at depth `1`
- each nested parenthesized logical group adds another level

Example tree:

```text
status==ACTIVE;(role==ADMIN,role==SUPPORT)

depth 0: AND
depth 1: status==ACTIVE
depth 1: OR
depth 2: role==ADMIN
depth 2: role==SUPPORT
```

That query requires `maxASTDepth >= 2`.

Depth guide:

| `maxASTDepth` | What it allows | Allowed Example | Rejected Example |
| --- | --- | --- | --- |
| `0` | Only a single root comparison | `status==ACTIVE` | `status==ACTIVE;role==ADMIN` |
| `1` | A top-level logical group with comparison children | `status==ACTIVE;role==ADMIN` | `status==ACTIVE;(role==ADMIN,role==SUPPORT)` |
| `2` | One nested logical group under the top-level expression | `status==ACTIVE;(role==ADMIN,role==SUPPORT)` | `status==ACTIVE;((role==ADMIN,role==SUPPORT);country==IN)` |
| `3` | Two nested logical levels under the top-level expression | `status==ACTIVE;((role==ADMIN,role==SUPPORT);country==IN)` | deeper nesting than the example at left |

Notes:

- On `@WebQuery`, the default is `maxASTDepth = -1`, which means "use the global setting".
- With the starter, the global default is `spring-web-query.filtering.max-ast-depth=1`.
- OR examples require either `allowOrOperator = WebQuery.OperatorPolicy.ALLOW` on the endpoint or `spring-web-query.filtering.allow-or-operation=true` globally.
- The limit applies to the full filter tree, not to individual fields.

### Global starter properties

When you use `spring-boot-starter-web-query`, these global defaults are available:

```properties
spring-web-query.filtering.filter-param-name=filter
spring-web-query.filtering.allow-and-operation=true
spring-web-query.filtering.allow-or-operation=false
spring-web-query.filtering.max-ast-depth=1
```

`@WebQuery` can override the filtering defaults per endpoint:

- `filterParamName = "q"`
- `allowAndOperator = WebQuery.OperatorPolicy.ALLOW`
- `allowAndOperator = WebQuery.OperatorPolicy.DENY`
- `allowOrOperator = WebQuery.OperatorPolicy.ALLOW`
- `allowOrOperator = WebQuery.OperatorPolicy.DENY`
- `maxASTDepth = 2` (or any non-negative value)

### `@RSQLFilterable`

Marks a field as filterable and declares which default and/or custom operators are allowed.

```java
@RSQLFilterable({RSQLDefaultOperator.EQUAL, RSQLDefaultOperator.NOT_EQUAL})
private String status;

@RSQLFilterable(customOperators = {IsMondayOperator.class})
private Long createdAt;
```

`value` is optional, so custom-only declarations are supported when a field should expose only registered `customOperators`.

### `@Sortable`

Whitelists a field for `sort` query usage.

```java
@Sortable
private Instant createdAt;
```

### `@MapsTo`

DTO-aware path mapping from DTO field segment to entity field segment/path.

```java
@MapsTo("createdAt")
private Instant joinedAt;
```

Absolute mapping reset is supported:

```java
@MapsTo(value = "profile.address.city", absolute = true)
private String city;
```

### `@FieldMapping`

Entity-aware alias mapping.

```java
@FieldMapping(name = "joined", field = "createdAt", allowOriginalFieldName = false)
```

When `allowOriginalFieldName = false`, only the alias is accepted in requests.

### Composed filter annotations

Composed annotations are shortcuts for common filter behavior, so you do not have to repeat long `@RSQLFilterable(...)` lists on every field.

- `@RSQLFilterableEquality` enables equality checks (`EQUAL`, `NOT_EQUAL`) for exact match / mismatch scenarios.
- `@RSQLFilterableMembership` enables set checks (`IN`, `NOT_IN`) when clients pass a list of allowed or excluded values.
- `@RSQLFilterableNull` enables nullability checks (`IS_NULL`, `NOT_NULL`) for presence/absence filters.
- `@RSQLFilterableRange` enables range and comparison operations (`GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL`, `BETWEEN`, `NOT_BETWEEN`) for numeric/date bounds.
- `@RSQLFilterableText` enables text-oriented matching (`LIKE`, `NOT_LIKE`, `IGNORE_CASE`, `IGNORE_CASE_LIKE`, `IGNORE_CASE_NOT_LIKE`) for case-sensitive or case-insensitive text search patterns.

You can combine them and/or mix with explicit `@RSQLFilterable`.

## Custom operators

`spring-web-query` supports pluggable custom operators.

### 1. Implement `RSQLCustomOperator`

```java
public class IsMondayOperator implements RSQLCustomOperator<Long> {

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

### 2. Register configurer bean(s)

```java
@Configuration
public class QueryConfig {

    @Bean
    RSQLCustomOperatorsConfigurer customOperators() {
        return () -> Set.of(new IsMondayOperator());
    }
}
```

You can define multiple `RSQLCustomOperatorsConfigurer` beans; all operators are aggregated.

### 3. Whitelist operator at field level

```java
@RSQLFilterable(
    value = {RSQLDefaultOperator.EQUAL},
    customOperators = {IsMondayOperator.class}
)
private LocalDateTime createdAt;
```

Important:

- Custom operator symbols must be unique.
- They must not overlap with default operator symbols.
- Referencing an unregistered custom operator in `@RSQLFilterable` throws a configuration exception.

## Pageable behavior

This library does not replace Spring’s pageable parsing. It adds a validation and mapping layer on top of Spring’s default `Pageable` resolution.

Under the hood, the resolver in this library delegates argument resolution to Spring’s `PageableHandlerMethodArgumentResolver`, then applies query-contract checks and path translation.

Flow:

1. parse page/size/sort using Spring defaults
2. validate each sort selector against your contract
3. remap selector to entity path when aliasing/DTO mapping is configured
4. return validated `Pageable`

### Max page size

Max page size is configured by Spring Data Web. In a Spring Boot application, the standard property is:

```properties
spring.data.web.pageable.max-page-size=500
```

### Default page size

When the request does not provide a `size` parameter, Spring's pageable resolver
falls back to the configured default page size. In a Spring Boot application,
the standard property is:

```properties
spring.data.web.pageable.default-page-size=50
```

This value must be greater than `0` and should not exceed
`spring.data.web.pageable.max-page-size`.

### Serialization mode

Spring Data also supports configuring how `Page` responses are serialized:

```properties
spring.data.web.pageable.serialization-mode=via-dto
```

### Important note about `@EnableSpringDataWebSupport`

If you add `@EnableSpringDataWebSupport` manually, Spring Data Web support is
configured directly and Spring Boot's `spring.data.web.pageable.*` properties
will not be applied automatically.

In that setup, register a `PageableHandlerMethodArgumentResolverCustomizer`
bean yourself to customize the shared resolver:

```java
@Configuration
class PageableConfig {

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(500);
            resolver.setFallbackPageable(Pageable.ofSize(50));
        };
    }
}
```

## Exception Handling

`spring-web-query` separates request problems from configuration problems so API consumers get clear feedback and backend teams can quickly identify misconfiguration.

In practice:

- **Validation exceptions** indicate the client request is invalid for your declared query contract (typically return `4xx`).
- **Configuration exceptions** indicate your application setup is inconsistent or incomplete (typically return `5xx`).

This split is important for production APIs because it avoids treating user mistakes and server defects as the same class of error.

Exception hierarchy:

```text
QueryException
├── QueryValidationException                        (client-side request issues, map to 4xx)
│   └── QueryFieldValidationException              (field-specific validation issue)
│       └── QueryForbiddenOperatorException        (operator not allowed for a field)
└── QueryConfigurationException                    (server/developer config issues, map to 5xx)
```

How to read this hierarchy:

- `QueryException` is the common parent for all query-related failures.
- `QueryValidationException` is raised when the incoming filter/sort expression is invalid.
- `QueryFieldValidationException` narrows that to field-level issues (unknown field, non-filterable, non-sortable).
- `QueryForbiddenOperatorException` is the most specific validation case: field exists, but the operator is not permitted for that field.
- `QueryConfigurationException` indicates a developer-side setup issue (for example, conflicting mappings or missing operator registration).

### Typical validation failures

- unknown field in `filter` or `sort`
- filtering on a field that is not annotated with `@RSQLFilterable`
- sorting on a field that is not annotated with `@Sortable`
- using an operator that is not allowed for the target field
- exceeding the configured `@WebQuery(maxASTDepth = ...)` limit
- malformed RSQL expression syntax

### Typical configuration failures

- duplicate alias names in `@FieldMapping`
- multiple aliases mapped to the same entity field
- duplicate operator symbols across default and custom operators
- unregistered custom operator referenced in `@RSQLFilterable(customOperators = ...)`
- invalid DTO-to-entity mapping path via `@MapsTo`

### Recommended API response strategy

- Map `QueryValidationException` (and subclasses) to `400 Bad Request`.
- Return a concise error message so clients can correct the query quickly.
- Map `QueryConfigurationException` to `500 Internal Server Error`.
- Avoid leaking internal stack/config details in `5xx` responses.

### Controller advice template

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QueryValidationException.class)
    public ResponseEntity<String> handleValidation(QueryValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(QueryConfigurationException.class)
    public ResponseEntity<String> handleConfiguration(QueryConfigurationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal query configuration error");
    }
}
```

## Why teams adopt this in production

- It standardizes query semantics across endpoints.
- It lowers maintenance cost by removing repeated query parsing code.
- It protects your API surface with explicit whitelists.
- It provides a clean migration path from entity-coupled contracts to DTO contracts.
- It scales from basic filtering to advanced operators without custom DSL infrastructure.

In short, it gives you power for consumers and control for maintainers.

## Best practices

- Prefer DTO-aware mode for public/external APIs.
- Keep DTO query contracts focused and stable.
- Only mark fields `@Sortable` if they are truly supported and safe.
- Keep operator sets minimal per field (principle of least power).
- Set `maxASTDepth` deliberately for each endpoint based on the query complexity you actually want to support.
- Use aliases deliberately; avoid exposing persistence-only field names.
- Treat `QueryConfigurationException` as a deployment/configuration defect.

## Documentation

- Javadoc: [https://javadoc.akshitbansal.co.in](https://javadoc.akshitbansal.co.in)

## Compatibility

- Java `17` and `21`
- Spring Boot `3` and `4`
- Spring Web MVC
- Spring Data JPA

## Contributing

Issues and pull requests are welcome.

If you are contributing code changes, include tests covering resolver behavior and validation outcomes.

## License

Apache License 2.0: [https://www.apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0)
