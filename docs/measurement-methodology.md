# Measurement methodology

Performance work in this repository follows a correctness-first evidence loop:

1. Define one observable behavioral contract.
2. Prove candidate implementations equivalent with focused and differential
   tests.
3. Isolate fixture generation from the measured region.
4. Run JMH in fresh JVM forks with explicit warm-up and measurement settings.
5. Measure allocation and GC behavior as well as time or throughput.
6. Use JFR to investigate causes and runtime side effects.
7. Record the environment, raw results, interpretation, and limitations.
8. Keep the optimization only when its benefit matters for the target workload.

## Benchmark guardrails

- Benchmark methods return their result or consume it through a JMH
  `Blackhole`, preventing dead-code elimination.
- Inputs come from JMH `@Param` state rather than compile-time constants.
- Trial-scoped setup creates deterministic datasets and executors before
  measurement.
- Forked JVMs prevent prior in-process compilation history from contaminating
  results.
- Fixed heap settings reduce one source of run-to-run variance.
- The `gc` profiler reports normalized allocation, collection counts, and
  collection time alongside the primary score.
- Error intervals and raw iteration data are retained; a single best run is
  never selected for presentation.

`BenchmarkPitfalls` intentionally contains methods prefixed with `wrong`.
They demonstrate constant folding and dead-code elimination and must not be
reported as optimization evidence.

## Comparing runs

Use the same JDK vendor and patch release, heap settings, CPU power policy,
operating-system load, JMH parameters, and benchmark commit. Run on an otherwise
idle machine. Compare confidence intervals and secondary metrics rather than
ranking rounded point estimates alone.

Do not compare local measurements directly with GitHub-hosted runner scores.
CI runs only short forked smoke tests because shared hardware is unsuitable for
stable performance gates.

## Limits

The datasets are deterministic and synthetic. They make regressions and
implementation trade-offs easier to study but do not model every distribution
found in production. JFR traces and application-level service objectives remain
necessary before applying an optimization to a real system.

The committed JMH JSON preserves all scores and raw iterations. Only the local
JVM launcher path was normalized to `$JAVA_HOME/bin/java` before publication;
measurement values were not changed.
