# RSQL & Pageable API Support for Spring Boot

A Spring Boot **auto-configurable library** that adds **safe, declarative filtering and sorting** to REST APIs using:

- **RSQL** for dynamic filtering
- **Spring Data JPA Specifications**
- **Field-level security for filtering and sorting**
- **Zero configuration** for consuming projects

---

## Features

### 🔍 RSQL Filtering
- Parse RSQL queries from request parameters
- Convert RSQL queries to Spring Data JPA `Specification`
- Explicit **whitelisting of filterable fields**
- **Operator-level restrictions per field**
- Support for **nested properties** and **collection traversal**
- Optional **API field aliases** via mappings

### ↕️ Secure Sorting
- Sorting allowed **only** on explicitly annotated entity fields
- Prevents accidental or malicious sorting on internal or sensitive columns
- Fully compatible with Spring Data `Pageable`

### 📄 Pagination Controls
- Enforced **maximum page size**
- Configurable via application properties

### ⚙️ Auto-Configuration
- Activated automatically when present on the classpath
- No `@Enable`, `@Import`, or manual wiring required
- Designed for Spring Boot **3.x**

---

## Installation

Add the dependency to your Spring Boot project.

### Maven
```xml
<dependency>
  <groupId>io.github.abansal755</groupId>
  <artifactId>spring-web-query</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Gradle
```gradle
implementation "io.github.abansal755:spring-web-query:0.0.1-SNAPSHOT"
```

No additional configuration is required.

---

## Usage

### 1️⃣ Declare filterable and sortable fields

```java
@Entity
public class User {

	@RsqlFilterable(operators = {
		RsqlOperator.EQUAL,
		RsqlOperator.IN
	})
	private String status;

	@RsqlFilterable(operators = {
		RsqlOperator.GREATER_THAN,
		RsqlOperator.LESS_THAN
	})
	private Instant createdAt;

	@Sortable
	private String username;
}
```

---

### 2️⃣ Enable RSQL filtering and restricted sorting in controllers

```java
@GetMapping("/users")
public Page<User> search(
	@RsqlSpec(entityClass = User.class) Specification<User> spec,
	@RestrictedPageable(entityClass = User.class) Pageable pageable
) {
	return userRepository.findAll(spec, pageable);
}
```

---

### 3️⃣ Example request

```http
GET /users?
	filter=status==ACTIVE;createdAt>=2025-01-01T00:00:00Z&
	sort=username,asc&
	page=0&
	size=20
```

---

## Field Mapping (Aliases)

Field mappings allow exposing API-friendly field names without leaking internal entity structure.

```java
@GetMapping("/users")
public Page<User> search(
	@RsqlSpec(
		entityClass = User.class,
		fieldMappings = {
			@FieldMapping(name = "createdDate", field = "createdAt")
		}
	) Specification<User> spec,
	@RestrictedPageable(entityClass = User.class) Pageable pageable
) {
	return userRepository.findAll(spec, pageable);
}
```

### Example query
```http
GET /users?filter=createdDate>=2025-01-01T00:00:00Z
```

---

## Sorting Restrictions

Sorting is allowed **only** on fields annotated with `@Sortable`.

```java
@Sortable
private String username;
```

Requests attempting to sort on non-whitelisted fields will fail with a `400 Bad Request`.

---

## Pagination Configuration

A global maximum page size is enforced.

### Default
```properties
api.pagination.max-page-size=100
```

### Override
```properties
api.pagination.max-page-size=500
```

---

## Date / Timestamp Support

The library supports **ISO-8601 timestamps** in RSQL filters.

Supported formats:
```text
2025-12-08T00:00:00Z
2025-12-08T00:00:00+00:00
```

Example range query:
```http
filter=createdAt>=2025-12-08T00:00:00%2B00:00;
       createdAt<2025-12-09T00:00:00%2B00:00
```

(`%2B` is the URL-encoded form of `+`.)

---

## Error Handling

Invalid queries result in a `QueryException`, including:

- Unknown fields
- Fields not marked as filterable
- Disallowed RSQL operators
- Invalid RSQL syntax
- Unauthorized sort fields

Typical global exception mapping:

```java
@ControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(QueryException.class)
	public ResponseEntity<?> handle(QueryException ex) {
		return ResponseEntity.badRequest().body(ex.getMessage());
	}
}
```

---

## Auto-Configuration Details

The following components are auto-configured:

- RSQL parser with controlled operator set
- `@RsqlSpec` argument resolver
- `@RestrictedPageable` argument resolver
- Pageable max-size customization
- ISO-8601 `Timestamp` converters for RSQL

Auto-configuration is registered via:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## Requirements

- Java 17+
- Spring Boot 3.x
- Spring Web MVC
- Spring Data JPA

---

## Design Principles

- **Secure by default**
- **Explicit field whitelisting**
- **Operator-level validation**
- **No runtime data inspection**
- **Library-first, framework-native design**