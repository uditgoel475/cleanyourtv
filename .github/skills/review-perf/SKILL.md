---
name: review-perf
description: "Performance optimization with measure-first discipline. Use when performance SLAs exist, Core Web Vitals fail, profiling reveals bottlenecks, or N+1 patterns are suspected."
argument-hint: "<file, endpoint, or feature> [-svc <service> | -app <app>]"
---

# /review/perf

Optimize performance for: **$ARGUMENTS**

## Core Principle

**Measure before optimizing.** Performance work without measurement is guessing. Profile first, identify the actual bottleneck, fix it, measure again.

## When to Use

- Performance requirements exist in the spec (load time budgets, response time SLAs)
- Users or monitoring report slow behavior
- Core Web Vitals scores are below thresholds
- You suspect a change introduced a regression
- Building features that handle large datasets or high traffic (bulk APIs, batch jobs)

**When NOT to use:** Don't optimize before you have evidence of a problem.

## The Optimization Workflow

```
1. MEASURE  → Establish baseline with real data
2. IDENTIFY → Find the actual bottleneck (not assumed)
3. FIX      → Address the specific bottleneck
4. VERIFY   → Measure again, confirm improvement
5. GUARD    → Add monitoring or tests to prevent regression
```

## Core Web Vitals Targets (SWE)

| Metric | Good | Needs Improvement | Poor |
|--------|------|-------------------|------|
| **LCP** | <= 2.5s | <= 4.0s | > 4.0s |
| **INP** | <= 200ms | <= 500ms | > 500ms |
| **CLS** | <= 0.1 | <= 0.25 | > 0.25 |

## Common Anti-Patterns by Domain

### N+1 Queries (SWE Backend)

```python
# BAD: N+1 — one query per record
for order in orders:
    order.customer = db.query(Customer).filter_by(id=order.customer_id).first()

# GOOD: Single query with join
orders = db.query(Order).options(joinedload(Order.customer)).all()
```

### Row-by-row writes (SWE Backend)

The write-side version of N+1: a loop computes a value per element and issues one UPDATE (or
INSERT/DELETE) per row. Cost grows with N round trips, N statements, and N lock acquisitions. A
single statement that joins the staged values to the target table does the same work at once.

```kotlin
// BAD: one UPDATE per element
items.forEach { item ->
    val score = computeScore(item)
    repository.updateScore(item.id, score)      // N statements
}
```

```sql
-- GOOD (PostgreSQL): compute in the loop, stage, then one joined UPDATE per chunk.
UPDATE device d
SET    score = v.score, updated_at = now()
FROM   unnest(CAST(:ids AS uuid[]), CAST(:scores AS int[])) AS v(id, score)   -- or (VALUES (...), (...)) v(id, score)
WHERE  d.id = v.id
  AND  d.tenant_id = :tenantId                                -- keep tenant/distribution key in the join
  AND  d.score IS DISTINCT FROM v.score;                      -- skip no-op writes
```

Pick the staging form by volume:

| Rows per operation | Staging |
|---|---|
| up to a few thousand | `unnest(array params)` or a `VALUES` list: two bind parameters total with `unnest` |
| tens of thousands or more | `CREATE TEMP TABLE ... ON COMMIT DROP`, fill with `COPY` / batch insert, `ANALYZE`, then `UPDATE ... FROM temp` |
| unbounded | chunk (e.g. 1–5k rows per statement) in a stable key order, so lock time and WAL stay bounded and concurrent writers don't deadlock |

Other engines: SQL Server `UPDATE t SET ... FROM t JOIN @tvp v ON ...` or `MERGE`; MySQL
`UPDATE t JOIN (SELECT ... UNION ALL ...) v ON ...`; EF Core 7+ `ExecuteUpdateAsync` when every row
gets the same expression.

ORM traps that still send one statement per row: JPA dirty checking inside a loop, `saveAll`,
EF Core `SaveChanges` on N tracked entities. JDBC/Hibernate batching only cuts round trips, not
statements. Bulk statements bypass the persistence context, so clear or refresh it afterwards
(`@Modifying(clearAutomatically = true, flushAutomatically = true)`). They also skip entity
listeners and `@Version` checks.

**Per-row writes are justified only when** each row must fire domain events or listeners, needs
optimistic-lock checks, needs per-row error isolation, or N is tiny and bounded (say ≤ 10). State
that reason in the plan.

### Unbounded Data Fetching

```python
# BAD: Fetching all records
records = db.query(Record).all()  # Loads entire table into memory

# GOOD: Paginate or aggregate
summary = db.query(Record.category, func.count()).group_by(Record.category).all()
```

### Unnecessary Re-renders (SWE Frontend)

```typescript
// BAD: Creates new object on every render
return <TaskFilters options={{ sortBy: 'date', order: 'desc' }} />;

// GOOD: Stable reference
const DEFAULT_OPTIONS = { sortBy: 'date', order: 'desc' } as const;
return <TaskFilters options={DEFAULT_OPTIONS} />;
```

## API SLA Targets

```
API response time: < 200ms (p95)
Time to Interactive: < 3.5s on 4G
Lighthouse Performance score: >= 90
```

## Performance Budget

```
JavaScript bundle: < 200KB gzipped (initial load)
API response time: < 200ms (p95)
```

## Verification

After any performance-related change:

- [ ] Before and after measurements exist (specific numbers)
- [ ] The specific bottleneck is identified and addressed
- [ ] Core Web Vitals are within "Good" thresholds (SWE frontend)
- [ ] No N+1 queries or actions in new data fetching code
- [ ] No per-row writes inside loops; collection writes are set-based or carry a stated reason
- [ ] Bundle size hasn't increased significantly (SWE frontend)
- [ ] Existing tests still pass (optimization didn't break behavior)

## Red Flags

- Optimization without profiling data to justify it
- N+1 query patterns in data fetching
- A save/update/delete call inside a loop over a collection
- List endpoints without pagination
- Loading entire tables into memory instead of paginating or aggregating
- Bundle size growing without review
- No performance monitoring in production

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
