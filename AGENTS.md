# Repository Guidelines

## Purpose

This repository demonstrates evidence-based JVM performance work. Keep benchmark
claims reproducible, scoped to the recorded environment, and backed by correctness
tests. Never optimize by weakening domain semantics.

## Build and verification

- `./mvnw clean verify`: compile, test, lint, document, and package every module.
- `java -jar jmh-benchmarks/target/benchmarks.jar -l`: list generated benchmarks.
- Benchmark scores must never be used as CI pass/fail thresholds.

## Code and documentation

Use Java 21 language features without preview APIs. Format Java with Google Java
Format through Spotless. Document benchmark setup, forks, warm-up, measurement
iterations, JVM flags, and machine metadata with every committed result.

Generated benchmark output, JFR recordings, profiler data, build output, secrets,
hostnames, and local paths must not be committed. Curated reference JSON under
`docs/results/` is allowed only after it has been reviewed and sanitized.
