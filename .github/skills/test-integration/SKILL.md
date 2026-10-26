---
name: test-integration
description: "Generate backend integration & E2E tests from a spec for Spring Boot (Java/Kotlin, JUnit 5) or ASP.NET Core (.NET, xUnit). Produces controller-slice, repository, service, and full-context tests with acceptance-scenario traceability. Backend counterpart to the browser E2E plugins (/test/playwright, /test/cypress)."
argument-hint: "<SPEC-ID> [-svc <service> | -app <app>]"
---

# /test/integration

Generate backend integration/E2E tests from spec: **$ARGUMENTS**

This is the backend counterpart to the browser E2E plugins (`/test/playwright` in `playwright-core`, `/test/cypress` in `cypress-core`). Use it for services with **no UI** — REST/gRPC/message-driven Spring Boot (Java/Kotlin + JUnit 5) or ASP.NET Core (.NET + xUnit). It never drives a browser; it exercises the application through its own boundaries (HTTP, persistence, message broker) using the project's existing test stack.

## Workflow

### Step 1: Read the Spec

Read `$MODULE_DIR/.specify/specs/$SPEC_ID/spec.md`, resolved per the
target-resolution contract above. Extract:
- **Technical Approach** — backend layers touched (controller, service, repository, messaging)
- **Acceptance scenarios** — map each scenario to one or more backend tests

If the file doesn't exist, list available specs and ask which one to use.

### Step 2: Detect Stack & Read Project Context

Determine the backend stack from the build file, then read the project's test conventions.

| Stack | Detect via | Test framework | Runner |
|-------|-----------|----------------|--------|
| Kotlin / Spring Boot | `build.gradle(.kts)` with `kotlin(...)` | JUnit 5 + MockK / springmockk | `./gradlew test` |
| Java / Spring Boot | `pom.xml` / `build.gradle` | JUnit 5 + Mockito | `./mvnw verify` / `./gradlew test` |
| C# / ASP.NET Core | `*.csproj`, `*.sln` | xUnit + FluentAssertions + Moq | `dotnet test` |

Read to reuse existing patterns before writing anything new:
- `AGENTS.md` / `CLAUDE.md` — project structure, test conventions, run commands
- Existing tests under `src/test/**` (JVM) or `*.Tests/**` (.NET)
- Shared test infrastructure: base test classes, test data factories, custom assertions/extensions
- `.specify/memory/test-conventions.md` if present (authoritative)

Do NOT invent a new test style if the project already has one — mirror it.

### Step 3: Map Acceptance Criteria to Test Layers

For each AC, choose the **lightest layer** that proves the behavior. Prefer the test pyramid — most coverage as unit/slice tests, integration only where boundaries are crossed, full-context E2E only for critical flows.

| Behavior under test | Preferred layer |
|---------------------|-----------------|
| Pure business logic, mapping, validation | Unit test (no Spring context) |
| HTTP routing, request validation, serialization, auth headers | Controller slice — `@WebMvcTest` (JVM) / `WebApplicationFactory` (.NET) |
| JPA/EF mappings, queries, constraints | Repository test — `@DataJpaTest` (JVM) / EF Core + Testcontainers (.NET) |
| Multiple beans + real DB, persisted state | Integration — `@SpringBootTest` + Testcontainers |
| Async message flow (NATS/Kafka/queue) | E2E — publish real event, poll for the effect |

For each scenario capture: **type** (positive / negative / edge / boundary), **preconditions**, **action**, **assertions** (status, body, persisted state, published message).

### Step 4: Write Test Files

Follow the detected stack. Mirror the project's existing base classes and factories.

**Kotlin / Spring Boot — controller slice / integration (JUnit 5):**

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class <Feature>ResourceTest(
    private val mockMvc: MockMvc,
    private val <feature>Repository: <Feature>Repository,
) : BaseTest() {

    @BeforeEach
    @AfterEach
    fun cleanUp() = <feature>Repository.deleteAll()

    @Test
    fun `AC1 - creates <feature> and returns 201`() {
        val response = mockMvc
            .postAuthenticated(BASE_PATH, content = createRequest(), options = AUTH_OPT)
            .expectNormalRequest(expectedStatus = HttpStatus.CREATED)
            .returnValue<CreateResponseDTO>()

        assertThat(<feature>Repository.findAll()).hasSize(1)
    }
}
```

**Kotlin — pure unit test (JUnit 5 + MockK):**

```kotlin
class <Feature>ServiceTest {
    private val repository: <Feature>Repository = mockk()
    private val service = <Feature>Service(repository)

    @Test
    fun `AC2 - rejects blank name`() {
        assertThrows<ValidationException> { service.create(request(name = "")) }
        verify(exactly = 0) { repository.save(any()) }
    }
}
```

**Java / Spring Boot — controller slice (JUnit 5 + Mockito):**

```java
@WebMvcTest(FeatureController.class)
class FeatureControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean FeatureService service;

    @Test
    void ac1_createReturns201() throws Exception {
        mockMvc.perform(post("/api/v1/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"f1\"}"))
            .andExpect(status().isCreated());
    }
}
```

**Java — integration with Testcontainers (JUnit 5):**

```java
@SpringBootTest
@Testcontainers
class FeatureIntegrationTest {
    @Container
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
    }

    @Autowired FeatureRepository repository;

    @Test
    void ac3_persistsFeature() {
        Feature saved = repository.save(new Feature("f1"));
        assertThat(repository.findById(saved.getId())).isPresent();
    }
}
```

**C# / ASP.NET Core — integration (xUnit + WebApplicationFactory + Testcontainers):**

```csharp
public class FeatureApiTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient _client;
    public FeatureApiTests(WebApplicationFactory<Program> factory) =>
        _client = factory.CreateClient();

    [Fact]
    public async Task Ac1_Create_Returns201()
    {
        var response = await _client.PostAsJsonAsync("/api/v1/features", new { name = "f1" });
        response.StatusCode.Should().Be(HttpStatusCode.Created);
    }
}
```

### Step 5: Async / Message-Driven Flows (if applicable)

For NATS/Kafka/queue-driven behavior, add an E2E test that publishes a real event and polls the datastore for the effect. Guard broker-dependent tests so they are skipped in CI without the broker:

```kotlin
@DisabledIfSystemProperty(named = "spring.profiles.active", matches = ".*ci.*")
class <Feature>E2ETest(...) : BaseTest() {
    @Test
    fun `AC4 - consumer persists on published event`() {
        natsCommunicator.publishToStream(SUBJECT, event.toByteArray())
        await untilAsserted { assertThat(repository.findAll()).isNotEmpty() }
    }
}
```

## Hard Constraints

- Mirror the project's existing base test class, test data factory, and custom assertions — do NOT introduce a parallel convention
- Use **JUnit 5** for JVM (never JUnit 4); **xUnit** for .NET unless the repo already standardises on NUnit/MSTest
- Kotlin: mock with **MockK / springmockk**, never Mockito on final classes
- Prefer real dependencies via **Testcontainers** over mocks at persistence/message boundaries; never mock what you can run
- Clean shared state before AND after each test (`@BeforeEach`/`@AfterEach` deleteAll, or a transactional rollback)
- One primary behavior per test; name tests after the AC they prove
- **No browser, no Playwright or Cypress** — this command is for headless backend verification only

## Output Structure

```
JVM (Gradle/Maven):
src/test/kotlin|java/<pkg>/
├── <feature>/
│   ├── <Feature>ServiceTest.kt|java        # unit
│   ├── <Feature>ResourceTest.kt|java       # controller slice / REST
│   ├── <Feature>RepositoryTest.kt|java     # @DataJpaTest
│   └── <Feature>IntegrationTest.kt|java    # @SpringBootTest + Testcontainers
└── utils/<Feature>TestFactory.kt|java      # shared test data builders

.NET:
tests/<Project>.Tests/
├── Unit/<Feature>ServiceTests.cs
└── Integration/<Feature>ApiTests.cs
```

## Step 6: Record Test Traceability

After writing all test files, record the acceptance-scenario-to-test mapping in the spec or change task record. Update any external work item only when the project workflow requires it.

Comment format:
```markdown
## Backend Test Cases Generated

| Layer | Count |
|-------|-------|
| Unit | N |
| Controller slice | N |
| Repository | N |
| Integration / E2E | N |
| Total | N |

### AC Traceability
| AC | Tests |
|----|-------|
| AC1 | <Feature>ResourceTest.ac1_* |

### Artifacts
- Tests: `src/test/**` (or `tests/**` for .NET)
- Traceability: spec or change task record
```

## Step 7: Verify

Run the suite and paste real output — never claim "tests pass" without evidence (see `/ai-sdlc/verify`).

```bash
# Kotlin / Java (Gradle)
./gradlew test --tests "*<Feature>*"
# Java (Maven)
./mvnw verify
# .NET
dotnet test --filter FullyQualifiedName~<Feature>
```

## Checklist Before Finishing

- [ ] Every AC from the spec maps to at least one backend test
- [ ] Correct layer chosen per AC (unit → slice → integration → E2E)
- [ ] Existing base class / test factory reused (no parallel convention)
- [ ] JUnit 5 (JVM) / xUnit (.NET); Kotlin uses MockK / springmockk
- [ ] Testcontainers used at DB/message boundaries where the repo supports it
- [ ] Shared state cleaned before and after each test
- [ ] Acceptance-scenario-to-test mapping recorded
- [ ] Suite run with output pasted as evidence

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
