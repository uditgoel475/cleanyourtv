---
name: swe-java
description: "Scaffold a Java/Spring Boot feature from spec — controllers, services, JPA entities, DTOs, validation, tests"
argument-hint: "spec ID (e.g. ONE-1991)"
---

# /swe/java

Scaffold a Java/Spring Boot feature from a spec using project conventions.

- **Build system:** Maven (default) — falls back to Gradle when `build.gradle` / `build.gradle.kts` exists at repo root.
- **Stack:** Java 21+, Spring Boot 3.3+, Spring Data JPA, Hibernate, Bean Validation (Jakarta), MapStruct 1.5+, JUnit 5, Testcontainers, AssertJ.
- **Layout:** Maven standard — `src/main/java`, `src/main/resources`, `src/test/java`.
- **Architecture:** layered — `api` (controller) → `service` → `repository` → `domain` (entity). DTO boundary at the controller, MapStruct between DTO and entity.
- **Errors:** RFC 7807 `ProblemDetail` via a single `@RestControllerAdvice`.
- **DB migrations:** Flyway under `src/main/resources/db/migration/V<n>__<desc>.sql`.
- **Logging/observability:** SLF4J + Spring Boot Actuator (`/actuator/health`, `/actuator/metrics`, Micrometer).

## When to run this skill

- After `/ai-sdlc/specify`, `/ai-sdlc/plan`, and `/ai-sdlc/tasks` have produced the spec.
- A feature is one bounded resource (e.g., `customer`, `order`, `formula`). Multiple controllers per feature is a smell — split the spec instead.

## Workflow

### Step 0 — Detect mode (greenfield vs brownfield)

Run these checks BEFORE writing any code:

| Check | Greenfield signal | Brownfield signal |
| --- | --- | --- |
| `pom.xml` / `build.gradle*` at repo root | absent | present |
| `src/main/java/**` populated | absent or empty | populated |
| `.specify/memory/memory-index.md` | absent | present (run `/ai-sdlc/memorize` first if missing) |

**Greenfield (no Java project exists yet):**

1. Read `$SPEC_DIR/spec.md` and `plan.md` (if present), where `$SPEC_DIR =
   $MODULE_DIR/.specify/specs/$SPEC_ID` — split `$ARGUMENTS` on its final `/` into
   `$MODULE_DIR` (default `.`) and `$SPEC_ID` so module-qualified ids like
   `services/<module>/ONE-1991` resolve to the module's specs.
2. Bootstrap the project — copy from the kit's `templates/swe/java/`:
   - `templates/swe/java/pom.xml`           → `./pom.xml`
   - `templates/swe/java/application.yml`   → `src/main/resources/application.yml`
   - `templates/swe/java/Application.java`  → `src/main/java/<group_path>/Application.java`
   - `templates/swe/java/V1__init.sql`      → `src/main/resources/db/migration/V1__init.sql` (only if absent; rename the table inside to match the first feature)
3. In each copied file, replace the example coordinates with the values from `spec.md` (group id, artifact id, package). Use Edit, not regex sed.
4. **Generate the Maven wrapper** so every developer (Linux / macOS / Windows) builds with the same Maven version. Run **one** of:
   - macOS / Linux: `mvn -N wrapper:wrapper -Dmaven=3.9.9`
   - Windows: `mvn.cmd -N wrapper:wrapper -Dmaven=3.9.9` (or `mvn` if the user has it on `PATH` already — the kit's `framework/cli/_platform.py:find_executable` resolves either name)

   This produces `./mvnw`, `.\mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties` — commit all three. Every subsequent build command in this skill assumes the wrapper exists and prefers it over a globally installed `mvn`.
5. Also create `.gitattributes` at repo root with the content from `templates/swe/.gitattributes` so CRLF/LF stays consistent across platforms.
6. Continue at step 1 of _Brownfield_ below.

**Brownfield (Java project already exists):**

1. Read `.specify/memory/memory-index.md` and `patterns.md` if they exist. They tell you the project's conventions — do NOT assume.
2. Read `pom.xml` / `build.gradle.kts` to confirm:
   - Java version
   - Spring Boot version
   - Existing dependencies (don't re-add what's already there)
   - Existing `<groupId>` and root package — match it, do not impose `com.teamviewer.*`.
3. Glob `src/main/java/**/*.java` to discover the package convention actually in use. Mirror it.
4. Read 1–2 existing controllers and services to see the project's idioms (Lombok or explicit ctors? records or classes? `@RestControllerAdvice` already present?).

### Step 1 — Plan the feature

5. From `spec.md`, extract:
   - **Feature name** (lowercase, single word, e.g. `formula`)
   - **Resource path** (e.g. `/api/v1/formulas`)
   - **Acceptance scenarios** — one Given/When/Then per `## Acceptance Scenarios` bullet.

### Step 2 — TDD per acceptance scenario

6. For each scenario:
   - **RED** — write a failing `@WebMvcTest` or integration test in `src/test/java/...` first.
   - **GREEN** — write the minimum controller/service/entity/mapper code to pass.
   - **REFACTOR** — extract value objects, tighten types. Re-run the build: `./mvnw -q verify` on macOS / Linux, `.\mvnw.cmd -q verify` on Windows. If the wrapper doesn't exist yet (very old project), fall back to `mvn` / `mvn.cmd` and recommend generating the wrapper.
7. Every directory in _Required Package Layout_ MUST have at least one file — never leave an empty package.

### Step 3 — Gate before handing back

8. Run the verification build. Use the wrapper, not the system `mvn`:
   - macOS / Linux: `./mvnw -q verify` (or `./gradlew check`)
   - Windows: `.\mvnw.cmd -q verify` (or `.\gradlew.bat check`)

   Confirm:
   - All tests pass
   - JaCoCo branch coverage ≥ 85% on new code (build fails otherwise — gate is in `pom.xml`)
   - `./mvnw spotless:check` (POSIX) / `.\mvnw.cmd spotless:check` (Windows) clean
9. Show the user a summary of files created / modified and stop. The user reviews before any PR.

## Required Package Layout

```
src/main/java/com/teamviewer/<context>/<feature>/
├── api/
│   └── <Feature>Controller.java       # REST endpoints. Thin — delegates to service.
├── service/
│   ├── <Feature>Service.java          # Interface
│   └── impl/
│       └── <Feature>ServiceImpl.java  # Transactions live here.
├── repository/
│   └── <Feature>Repository.java       # Spring Data JPA interface.
├── domain/
│   ├── <Feature>.java                 # JPA entity. Mutable but encapsulated.
│   └── <ValueObject>.java             # Optional: @Embeddable value objects.
├── dto/
│   ├── <Feature>Request.java          # Record. Bean Validation on inputs.
│   └── <Feature>Response.java         # Record. No entity exposed to clients.
├── mapper/
│   └── <Feature>Mapper.java           # MapStruct entity ↔ DTO.
└── exception/
    └── <Feature>NotFoundException.java
```

A `GlobalExceptionHandler` lives in `com.teamviewer.<context>.common.exception` (shared across features).

## Required Patterns

### 1. Controller — thin, validated, status-aware

```java
package com.teamviewer.<context>.<feature>.api;

import com.teamviewer.<context>.<feature>.dto.<Feature>Request;
import com.teamviewer.<context>.<feature>.dto.<Feature>Response;
import com.teamviewer.<context>.<feature>.service.<Feature>Service;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/<resource>")
public class <Feature>Controller {

    private final <Feature>Service service;

    // Constructor injection ONLY. No @Autowired on fields.
    public <Feature>Controller(<Feature>Service service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public <Feature>Response create(@Valid @RequestBody <Feature>Request request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public <Feature>Response findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
```

Rules:
- One controller per resource. One method per HTTP operation.
- Use `@ResponseStatus` for non-200 success codes; do **not** wrap with `ResponseEntity<T>` unless headers are needed.
- Controllers must not call repositories directly — only services.

### 2. Service — interface + impl, transactions, domain exceptions

```java
package com.teamviewer.<context>.<feature>.service;

import com.teamviewer.<context>.<feature>.dto.<Feature>Request;
import com.teamviewer.<context>.<feature>.dto.<Feature>Response;
import java.util.UUID;

public interface <Feature>Service {
    <Feature>Response create(<Feature>Request request);
    <Feature>Response findById(UUID id);
    void delete(UUID id);
}
```

```java
package com.teamviewer.<context>.<feature>.service.impl;

import com.teamviewer.<context>.<feature>.domain.<Feature>;
import com.teamviewer.<context>.<feature>.dto.*;
import com.teamviewer.<context>.<feature>.exception.<Feature>NotFoundException;
import com.teamviewer.<context>.<feature>.mapper.<Feature>Mapper;
import com.teamviewer.<context>.<feature>.repository.<Feature>Repository;
import com.teamviewer.<context>.<feature>.service.<Feature>Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional
public class <Feature>ServiceImpl implements <Feature>Service {

    private static final Logger log = LoggerFactory.getLogger(<Feature>ServiceImpl.class);

    private final <Feature>Repository repository;
    private final <Feature>Mapper mapper;

    public <Feature>ServiceImpl(<Feature>Repository repository, <Feature>Mapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public <Feature>Response create(<Feature>Request request) {
        var entity = mapper.toEntity(request);
        var saved = repository.save(entity);
        log.info("created <feature> id={}", saved.getId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public <Feature>Response findById(UUID id) {
        return mapper.toResponse(
            repository.findById(id).orElseThrow(() -> new <Feature>NotFoundException(id))
        );
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new <Feature>NotFoundException(id);
        }
        repository.deleteById(id);
    }
}
```

Rules:
- `@Transactional` at class level on the impl; override with `@Transactional(readOnly = true)` on read paths.
- Domain exceptions, never `RuntimeException` or `IllegalStateException`.
- Log at INFO for state changes, DEBUG for diagnostic data, ERROR only when re-throwing.

### 3. Entity — JPA, UUID id, encapsulated

```java
package com.teamviewer.<context>.<feature>.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "<feature_table>")
public class <Feature> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected <Feature>() { /* JPA */ }

    public <Feature>(UUID id, String name, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }

    public void rename(String newName) {
        this.name = Objects.requireNonNull(newName);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof <Feature> other)) return false;
        return id != null && id.equals(other.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
```

Rules:
- UUID primary key — generate in the service before save (don't rely on `@GeneratedValue` for UUIDs in Postgres unless you use `uuid-ossp`).
- No public no-arg constructor — use `protected` for JPA.
- `equals`/`hashCode` based on the ID only. Never on mutable fields.
- Expose mutation through named methods (`rename`, `markPaid`) — never expose setters.

### 4. DTO — records with Bean Validation

```java
package com.teamviewer.<context>.<feature>.dto;

import jakarta.validation.constraints.*;

public record <Feature>Request(
    @NotBlank @Size(max = 120) String name,
    @Email String contactEmail
) {}
```

```java
package com.teamviewer.<context>.<feature>.dto;

import java.time.Instant;
import java.util.UUID;

public record <Feature>Response(UUID id, String name, Instant createdAt) {}
```

Rules:
- Records only — they're immutable and concise.
- Validation annotations live on `<Feature>Request`. Responses never have validation.
- Never expose `<Feature>` entity to the client — always map through `<Feature>Response`.

### 5. Mapper — MapStruct, Spring component model

```java
package com.teamviewer.<context>.<feature>.mapper;

import com.teamviewer.<context>.<feature>.domain.<Feature>;
import com.teamviewer.<context>.<feature>.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class})
public interface <Feature>Mapper {

    <Feature>Response toResponse(<Feature> entity);

    @Mapping(target = "id",        expression = "java(UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    <Feature> toEntity(<Feature>Request request);
}
```

### 6. Repository — Spring Data JPA

```java
package com.teamviewer.<context>.<feature>.repository;

import com.teamviewer.<context>.<feature>.domain.<Feature>;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface <Feature>Repository extends JpaRepository<<Feature>, UUID> {
    boolean existsByName(String name);
}
```

Rules:
- Derived query methods only when names are short (`findByName`, `existsByEmail`). For complex queries use `@Query` with JPQL — never string concatenation.
- No `findAll()` without pagination (`Pageable`) in production code.
- **No per-row writes in a loop.** `repository.save(x)` in a loop, `saveAll`, and dirty-checking
  managed entities in a loop all send one UPDATE per row; Hibernate batching only cuts round trips.
  Use one set-based statement per chunk. Same value for every row: a `@Modifying(clearAutomatically
  = true, flushAutomatically = true)` JPQL `update ... where e.id in :ids`. A different value per
  row: compute the values in the loop, then join them in one statement:

```java
private static final String UPDATE_SCORES = """
    UPDATE device d SET score = v.score, updated_at = now()
    FROM unnest(?::uuid[], ?::int[]) AS v(id, score)
    WHERE d.id = v.id AND d.tenant_id = ? AND d.score IS DISTINCT FROM v.score
    """;

public int updateScores(UUID tenantId, List<ScoreUpdate> updates) {   // call per chunk of ~1000, sorted by id
    PreparedStatementCreator creator = con -> {
        PreparedStatement ps = con.prepareStatement(UPDATE_SCORES);
        ps.setArray(1, con.createArrayOf("uuid", updates.stream().map(ScoreUpdate::id).toArray()));
        ps.setArray(2, con.createArrayOf("int4", updates.stream().map(ScoreUpdate::score).toArray()));
        ps.setObject(3, tenantId);
        return ps;
    };
    Integer updated = jdbcTemplate.execute(creator, (PreparedStatementCallback<Integer>) PreparedStatement::executeUpdate);
    return updated == null ? 0 : updated;
}
```

  Bulk statements skip entity listeners and `@Version` checks. When those must run per row, keep
  the per-row write and state the reason in the plan.

### 7. Exception + handler — RFC 7807 ProblemDetail

```java
package com.teamviewer.<context>.<feature>.exception;

import java.util.UUID;

public class <Feature>NotFoundException extends RuntimeException {
    public <Feature>NotFoundException(UUID id) {
        super("<Feature> %s not found".formatted(id));
    }
}
```

```java
package com.teamviewer.<context>.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleNotFound(RuntimeException ex) {
        if (ex.getClass().getSimpleName().endsWith("NotFoundException")) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        throw ex; // re-throw — falls to handleGeneric below
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
            .toList());
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Constraint violation");
        pd.setDetail(ex.getMessage());
        return pd;
    }
}
```

## Testing

Three layers — every feature needs all three:

1. **Unit tests** (`<Feature>ServiceImplTest`) — JUnit 5 + Mockito + AssertJ. Mock the repository.
2. **Web slice** (`<Feature>ControllerTest`) — `@WebMvcTest` + MockMvc. Mock the service. One test per HTTP path.
3. **Integration test** (`<Feature>IntegrationTest`) — `@SpringBootTest` + Testcontainers PostgreSQL. **Never mock the database** — spin up a real container.

```java
// 1. Unit test
@ExtendWith(MockitoExtension.class)
class <Feature>ServiceImplTest {

    @Mock <Feature>Repository repository;
    @Mock <Feature>Mapper mapper;
    @InjectMocks <Feature>ServiceImpl service;

    @Test
    void findById_returns_response_when_found() {
        var id = UUID.randomUUID();
        var entity = new <Feature>(id, "demo", Instant.EPOCH);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(new <Feature>Response(id, "demo", Instant.EPOCH));

        var result = service.findById(id);

        assertThat(result.name()).isEqualTo("demo");
    }

    @Test
    void findById_throws_when_missing() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id))
            .isInstanceOf(<Feature>NotFoundException.class);
    }
}
```

```java
// 2. Web slice
@WebMvcTest(<Feature>Controller.class)
class <Feature>ControllerTest {

    @Autowired MockMvc mvc;
    @MockBean <Feature>Service service;

    @Test
    void create_returns_201() throws Exception {
        var id = UUID.randomUUID();
        when(service.create(any())).thenReturn(new <Feature>Response(id, "demo", Instant.EPOCH));

        mvc.perform(post("/api/v1/<resource>")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"demo","contactEmail":"a@b.com"}
                    """))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void create_returns_400_on_invalid_email() throws Exception {
        mvc.perform(post("/api/v1/<resource>")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"demo","contactEmail":"not-an-email"}
                    """))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.title").value("Validation failed"));
    }
}
```

```java
// 3. Integration test — Testcontainers
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class <Feature>IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;

    @Test
    void create_then_get_roundtrip() throws Exception {
        var location = mvc.perform(post("/api/v1/<resource>")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"acme","contactEmail":"ops@acme.io"}
                    """))
           .andExpect(status().isCreated())
           .andReturn().getResponse().getContentAsString();

        // assert round-trip
    }
}
```

## Quality gates (run before opening PR)

```bash
mvn -q verify              # tests + JaCoCo coverage + Surefire reports
mvn spotless:check         # formatting
mvn checkstyle:check       # style
mvn pmd:check              # static analysis (optional but recommended)
```

Or Gradle:

```bash
./gradlew check spotlessCheck jacocoTestReport
```

- **Coverage gate:** ≥ 85% branch on all new code. Configure in `pom.xml` JaCoCo plugin or `build.gradle.kts` `jacoco` block.
- **Actuator:** `/actuator/health`, `/actuator/info`, `/actuator/metrics` must be enabled in `application.yml`.

## Database migrations (Flyway)

Place SQL migrations at `src/main/resources/db/migration/V<n>__<description>.sql`:

```sql
-- V1__create_formula_table.sql
CREATE TABLE formula (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX formula_name_idx ON formula(name);
```

Rules:
- Never edit a merged migration — write a new one (`V<n+1>__<desc>.sql`).
- Migrations must be idempotent friendly (use `CREATE TABLE IF NOT EXISTS` for additive only when Flyway baseline is in play).

## What NOT to do

- ❌ `@Autowired` on fields — constructor injection only
- ❌ JPA entities returned as REST responses — always map through a DTO
- ❌ Mocking the database in integration tests — use Testcontainers
- ❌ Catching `Exception` broadly — catch specific domain exceptions
- ❌ Mutable static state in services (`static List<...> CACHE`)
- ❌ Business logic in controllers — push to the service layer
- ❌ `findAll()` without `Pageable` in production code
- ❌ String-concatenated JPQL or native SQL — use parameterised queries
- ❌ `System.out.println` or `printStackTrace` — use SLF4J
- ❌ Lombok in controllers — write the constructor explicitly (clarity > brevity at boundaries)

## Platform notes (macOS / Linux / Windows)

The Java toolchain runs the same on every platform — only the **executable name** and the **wrapper script** differ. The skill respects the user's OS automatically; you don't need to detect it manually unless you're generating new build commands for a runbook.

| Concern | macOS / Linux | Windows |
| --- | --- | --- |
| Maven CLI | `mvn` | `mvn.cmd` (sometimes also `mvn.bat`) |
| Maven wrapper | `./mvnw clean verify` | `.\mvnw.cmd clean verify` |
| Gradle wrapper | `./gradlew build` | `.\gradlew.bat build` |
| Docker (Testcontainers) | native Docker or Colima | Docker Desktop with WSL2 backend |
| Path separator in classpath | `:` (colon) | `;` (semicolon) — use `File.pathSeparator`, never hardcode |
| Line endings | LF | CRLF unless overridden — keep `.gitattributes * text=auto` |

**Rules the skill enforces regardless of platform:**

1. **Prefer the wrapper (`mvnw` / `gradlew`) over the globally installed binary.** It pins the Maven/Gradle version per repo so a Windows developer and a Linux developer build with the exact same toolchain. When you generate or update a runbook, write `./mvnw` (POSIX) **and** `.\mvnw.cmd` (Windows) side-by-side, never just one.
2. **Never hardcode `/` or `\` in Java source.** Use `Path.of(...)`, `Paths.get(...)`, or `File.separator`. Resources loaded from the classpath always use `/`.
3. **Use `System.lineSeparator()` for generated text files, not `"\n"`.**
4. **Docker Desktop must be running before `mvn verify`** — Testcontainers will fail-fast otherwise. On macOS/Linux it's `docker info`; on Windows it's the Docker Desktop tray icon plus `docker info` from any shell.
5. **CI runners.** The kit's GitHub Actions templates use `runs-on: ubuntu-latest`. If a project needs Windows-specific build verification, add a matrix entry rather than swapping out Linux.

When this skill writes shell commands into a generated spec, plan, or PR description, render them in **two side-by-side fenced blocks** (POSIX and PowerShell) so the team can copy-paste either one. Example:

```bash
# macOS / Linux
./mvnw clean verify
```

```powershell
# Windows
.\mvnw.cmd clean verify
```

## Reference templates

Starter `pom.xml`, `build.gradle.kts`, and `application.yml` live in `templates/swe/java/`. Copy them when bootstrapping a brand-new module.

$ARGUMENTS

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
