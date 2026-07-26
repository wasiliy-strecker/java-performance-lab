# Reference results

These measurements were captured on 2026-07-26. They are evidence for the
specific implementations and environment below, not portable performance
guarantees or CI thresholds.

## Environment

| Component | Value |
|---|---|
| CPU | AMD Ryzen 9 5950X, 16 cores / 32 hardware threads |
| Memory | 62 GiB |
| Operating system | Linux 6.14.0-33-generic, x86_64 |
| JVM | Eclipse Temurin 21.0.11+10 LTS |
| Collector | G1 GC |
| Heap | Fixed 2 GiB (`-Xms2g -Xmx2g`) |
| Harness | JMH 1.37 |
| Benchmark source | Commit [`039b7a5`](https://github.com/wasiliy-strecker/java-performance-lab/commit/039b7a5) |
| Harness isolation | 2 forks, 1 benchmark thread |
| Warm-up | 3 iterations x 1 second per fork |
| Measurement | 5 iterations x 1 second per fork |
| Profiler | JMH `gc` profiler |

The `±` values below are JMH's reported score errors over ten measured
iterations. CPU cores were not pinned and background system services remained
enabled; the raw data is retained so later controlled runs can be compared
honestly.

## Case study 1: equivalent parsers

Each operation processes an immutable 10,000-row batch with 5% deterministic
malformed input. Dataset creation occurs outside the measured region. Contract
and differential tests establish equivalent output before either implementation
is measured.

| Implementation | Throughput | Allocation per batch |
|---|---:|---:|
| Delimiter-scanning | 1,488.7 ± 21.4 batches/s | 60.6 ± 0.1 B/op |
| `String.split` | 765.7 ± 15.9 batches/s | 3,440,585.1 ± 0.2 B/op |

On this workload, delimiter scanning delivered 1.94x the batch throughput and
reduced normalized allocation by 99.998%. That is a candidate optimization for
a proven ingestion hot path. It is not a recommendation to replace readable
parsing everywhere: the optimized implementation carries more code and should
remain protected by equivalence tests and production profiling.

The machine-readable results include the complete 100/10,000-row and 0%/5%
invalid-input matrix:

- [Order-event ingestion JSON](results/order-event-ingestion-jdk21.json)

## Case study 2: platform and virtual threads

Each operation submits either 32 or 128 immutable tasks. The platform executor
is bounded to the smaller of the task count and available processors; on this
machine it therefore uses 32 workers for the 128-task case. The virtual-thread
executor creates one virtual thread per task.

Results for 128 tasks:

| Workload | Executor | Average batch time | Allocation per batch |
|---|---|---:|---:|
| Blocking, 2 ms per task | Platform threads | 8.483 ± 0.020 ms/op | 13,715 ± 253 B/op |
| Blocking, 2 ms per task | Virtual threads | 2.142 ± 0.004 ms/op | 116,310 ± 5,275 B/op |
| CPU-bound | Platform threads | 0.624 ± 0.049 ms/op | 12,797 ± 69 B/op |
| CPU-bound | Virtual threads | 0.628 ± 0.045 ms/op | 49,920 ± 66 B/op |

Virtual threads reduced blocking batch latency by 74.75% when 128 tasks exceeded
the bounded platform pool. They also allocated 8.48x more bytes in that case.
For the CPU-bound batch, the 0.65% score difference is smaller than the reported
uncertainty and does not support a latency advantage. The conclusion is narrow:
virtual threads are useful here for concurrent blocking work, not as a generic
CPU-speed optimization.

- [Executor-workload JSON](results/executor-workloads-jdk21.json)

## Reproduce

Build the executable benchmark JAR:

```bash
export JAVA_HOME=/path/to/temurin-21
./mvnw -pl jmh-benchmarks -am clean package
```

Run the parser matrix:

```bash
"$JAVA_HOME/bin/java" -jar jmh-benchmarks/target/benchmarks.jar \
  '.*OrderEventIngestionBenchmark.*' \
  -f 2 -wi 3 -i 5 -w 1s -r 1s \
  -prof gc -jvm "$JAVA_HOME/bin/java" \
  -jvmArgs '-Xms2g -Xmx2g' \
  -rf json -rff target/order-event-ingestion.json -foe true
```

Run the executor matrix:

```bash
"$JAVA_HOME/bin/java" -jar jmh-benchmarks/target/benchmarks.jar \
  '.*ExecutorWorkloadBenchmark.*' \
  -f 2 -wi 3 -i 5 -w 1s -r 1s \
  -prof gc -jvm "$JAVA_HOME/bin/java" \
  -jvmArgs '-Xms2g -Xmx2g' \
  -rf json -rff target/executor-workloads.json -foe true
```

See [measurement-methodology.md](measurement-methodology.md) before comparing a
new run with these results.
