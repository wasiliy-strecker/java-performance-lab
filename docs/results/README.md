# Raw measurement data

These files are machine-readable JMH 1.37 output from the environment documented
in [../reference-results.md](../reference-results.md):

- `order-event-ingestion-jdk21.json`
- `executor-workloads-jdk21.json`

All primary scores, secondary GC metrics, confidence data, percentiles, and raw
iterations are retained. The local JVM launcher path is the only normalized
field; it is stored as `$JAVA_HOME/bin/java`.
