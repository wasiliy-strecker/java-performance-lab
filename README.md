# Java Performance Lab

[![Verify](https://github.com/wasiliy-strecker/java-performance-lab/actions/workflows/verify.yml/badge.svg)](https://github.com/wasiliy-strecker/java-performance-lab/actions/workflows/verify.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)

Reproducible Java 21 performance experiments with JMH, JFR, allocation profiling,
GC analysis, and evidence-based optimization.

This repository is being built as a production-minded performance investigation:
correctness comes first, measurements include their environment, and CI never
pretends that a shared runner can enforce stable performance thresholds.

## Planned evidence

| Module | Responsibility |
|---|---|
| `performance-workloads` | Equivalent readable and allocation-aware workloads with differential tests |
| `jmh-benchmarks` | JMH pitfalls, ingestion measurements, and executor comparisons |
| `jfr-diagnostics` | Custom events plus an executable recorder and analyzer |

## Build

Requirements: a full JDK 21 or newer.

```bash
./mvnw clean verify
```

The public API and benchmark evidence will be added in independently verified
milestones. Benchmark scores will be documented only together with machine, JVM,
fork, warm-up, and measurement metadata.

Licensed under the [Apache License 2.0](LICENSE).
