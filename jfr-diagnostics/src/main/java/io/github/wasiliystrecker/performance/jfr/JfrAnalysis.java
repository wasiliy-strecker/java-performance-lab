package io.github.wasiliystrecker.performance.jfr;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable summary extracted from one Flight Recorder file.
 *
 * @param batchEvents number of custom batch events
 * @param acceptedRows total accepted rows across batches
 * @param rejectedRows total rejected rows across batches
 * @param totalBatchDuration sum of custom batch-event durations
 * @param p50BatchDuration nearest-rank median batch duration
 * @param p95BatchDuration nearest-rank p95 batch duration
 * @param p99BatchDuration nearest-rank p99 batch duration
 * @param garbageCollections number of JDK garbage-collection events
 * @param allocationSamples number of JDK sampled-allocation events
 * @param rejectionsByReason content-free rejection counts
 */
public record JfrAnalysis(
    int batchEvents,
    long acceptedRows,
    long rejectedRows,
    Duration totalBatchDuration,
    Duration p50BatchDuration,
    Duration p95BatchDuration,
    Duration p99BatchDuration,
    long garbageCollections,
    long allocationSamples,
    Map<String, Long> rejectionsByReason) {

  /** Validates counts and defensively copies the rejection map. */
  public JfrAnalysis {
    if (batchEvents < 0
        || acceptedRows < 0
        || rejectedRows < 0
        || garbageCollections < 0
        || allocationSamples < 0) {
      throw new IllegalArgumentException("analysis counts must not be negative");
    }
    Objects.requireNonNull(totalBatchDuration, "totalBatchDuration");
    Objects.requireNonNull(p50BatchDuration, "p50BatchDuration");
    Objects.requireNonNull(p95BatchDuration, "p95BatchDuration");
    Objects.requireNonNull(p99BatchDuration, "p99BatchDuration");
    rejectionsByReason = Map.copyOf(rejectionsByReason);
  }

  /**
   * Returns accepted plus rejected rows.
   *
   * @return all rows represented by custom batch events
   */
  public long totalRows() {
    return Math.addExact(acceptedRows, rejectedRows);
  }
}
