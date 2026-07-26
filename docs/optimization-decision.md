# Optimization decision: delimiter-scanning parser

- **Status:** Accepted for the measured ingestion hot-path case study
- **Decision date:** 2026-07-26
- **Benchmark source:** [`039b7a5`](https://github.com/wasiliy-strecker/java-performance-lab/commit/039b7a5)
- **Evidence:** [Reference results](reference-results.md)

## Context

The readability-first implementation uses `String.split` for every order-event
row. That is easy to review, but it creates an array and field strings per row.
For large batches, profiling suggested temporary allocation as a plausible cost.

The candidate scans four delimiter positions, validates character ranges, and
parses numeric fields without token arrays. It is more complex, so speed alone
is not enough to accept it.

## Preconditions

The candidate had to preserve:

- validation order and rejection reason
- accepted and rejected counts
- signed currency totals
- event-ID checksum
- overflow behavior
- callback encounter order

The 25-case parameterized contract suite exercises both processors, and a
30,000-row differential suite compares their complete observations before
measurements run. The optimization does not change the public interface or
output model.

## Evidence

For a deterministic 10,000-row batch with 5% malformed rows:

| Metric | `String.split` | Delimiter scan | Difference |
|---|---:|---:|---:|
| Throughput | 765.7 ± 15.9 batches/s | 1,488.7 ± 21.4 batches/s | 1.94x |
| Allocation | 3,440,585.1 ± 0.2 B/op | 60.6 ± 0.1 B/op | -99.998% |

The complete parameter matrix, raw iterations, JVM, hardware, and limitations
are retained in [reference-results.md](reference-results.md).

## Decision

Keep both implementations:

- use the readable parser as the executable baseline and maintenance reference
- make the delimiter-scanning parser available where profiling confirms this
  exact ingestion path is material
- keep both behind `OrderEventBatchProcessor`
- run equivalence tests for every semantic change

Do not claim that manual parsing is globally preferable to `String.split`.
Small, infrequent inputs may not justify the additional code.

## Consequences

Benefits:

- materially lower temporary allocation for the measured workload
- higher batch throughput with unchanged output
- less GC pressure at sustained ingestion volume

Costs and risks:

- more branches and index arithmetic to review
- greater risk around malformed boundaries and numeric overflow
- format changes must be implemented twice until the baseline is retired
- reference scores remain machine- and workload-specific

## Production rollout criteria

Before selecting the candidate in a service:

1. capture a production-like JFR trace and confirm parsing/allocation is material
2. replay representative sanitized data through both implementations
3. compare business outputs and rejection categories
4. canary the candidate behind configuration
5. monitor throughput, allocation rate, GC pauses, errors, and fallback count
6. keep an immediate switch back to the readable implementation

If the hot path disappears or maintenance cost outweighs the observed benefit,
the readable implementation remains the correct choice.
