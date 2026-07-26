# JFR diagnostic guide

The `jfr-diagnostics` module records the same deterministic workload used by the
benchmarks and analyzes it through Java's built-in Flight Recorder APIs. It has
no external observability framework dependency.

## Build

```bash
./mvnw -pl jfr-diagnostics -am package
```

## Record

Choose either semantically equivalent parser:

```bash
java -jar jfr-diagnostics/target/jfr-diagnostics.jar \
  record readable target/readable.jfr

java -jar jfr-diagnostics/target/jfr-diagnostics.jar \
  record allocation-aware target/allocation-aware.jfr
```

The default run records 20 batches of 10,000 rows with a deterministic 5%
malformed-input distribution. Dataset construction finishes before recording
starts.

## Analyze

```bash
java -jar jfr-diagnostics/target/jfr-diagnostics.jar \
  analyze target/allocation-aware.jfr
```

Example report shape:

```text
JFR analysis
Batch events: 20
Rows: 200000 (accepted ..., rejected ...)
Batch latency p50/p95/p99: ... ms / ... ms / ... ms
Garbage collections: ...
Allocation samples: ...
Rejections:
  FIELD_COUNT: ...
```

The analyzer streams events with `RecordingFile`, aggregates custom events, and
uses nearest-rank batch-duration percentiles.

## Event schema

### `io.github.wasiliystrecker.performance.OrderBatch`

| Field | Meaning |
|---|---|
| inherited duration | End-to-end parser time for one batch |
| `parser` | `readable` or `allocation-aware` |
| `inputRows` | Rows presented to the processor |
| `acceptedRows` | Rows included in aggregates |
| `rejectedRows` | Rows rejected |
| `eventIdChecksum` | Observable deterministic result |

### `io.github.wasiliystrecker.performance.RejectedLine`

| Field | Meaning |
|---|---|
| `lineIndex` | Zero-based position |
| `reason` | Stable rejection enum name |
| `lineLength` | Input length, or zero for `null` |

Stack traces are disabled for both custom event types. Rejected events never
contain customer IDs, row contents, payloads, or exception messages.

## Enabled JDK events

- `jdk.GarbageCollection` for collection counts
- `jdk.ObjectAllocationSample` for sampled allocation pressure

Allocation samples are statistical and must not be interpreted as exact object
counts. Use JMH's normalized allocation metric for controlled A/B measurements
and JFR to understand application-level behavior.

## External inspection

The generated recording is compatible with JDK tools and JDK Mission Control.
For a quick schema check with a full JDK:

```bash
jfr summary target/allocation-aware.jfr
jfr print --events \
  io.github.wasiliystrecker.performance.OrderBatch \
  target/allocation-aware.jfr
```

Recordings may contain runtime metadata even though the custom events avoid raw
business data. `.jfr` files are ignored by Git and should be handled as
diagnostic artifacts, not committed fixtures.
