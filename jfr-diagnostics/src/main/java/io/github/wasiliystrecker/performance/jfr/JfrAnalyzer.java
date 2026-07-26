package io.github.wasiliystrecker.performance.jfr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/** Streams JFR events into a small, stable analysis model without loading the recording at once. */
public final class JfrAnalyzer {
  private static final String GARBAGE_COLLECTION_EVENT = "jdk.GarbageCollection";
  private static final String ALLOCATION_SAMPLE_EVENT = "jdk.ObjectAllocationSample";

  /**
   * Analyzes custom workload events and selected JDK runtime events.
   *
   * @param input readable JFR recording
   * @return immutable analysis summary
   * @throws IOException if the recording cannot be read
   */
  public JfrAnalysis analyze(Path input) throws IOException {
    Objects.requireNonNull(input, "input");
    Path normalizedInput = input.toAbsolutePath().normalize();
    if (!Files.isRegularFile(normalizedInput)) {
      throw new IOException("recording does not exist: " + normalizedInput);
    }

    MutableAnalysis analysis = new MutableAnalysis();
    try (RecordingFile recording = new RecordingFile(normalizedInput)) {
      while (recording.hasMoreEvents()) {
        analysis.accept(recording.readEvent());
      }
    }
    return analysis.finish();
  }

  private static final class MutableAnalysis {
    private final List<Duration> batchDurations = new ArrayList<>();
    private final Map<String, Long> rejectionsByReason = new TreeMap<>();
    private long acceptedRows;
    private long rejectedRows;
    private long garbageCollections;
    private long allocationSamples;

    void accept(RecordedEvent event) {
      switch (event.getEventType().getName()) {
        case OrderBatchEvent.NAME -> acceptBatch(event);
        case RejectedLineEvent.NAME ->
            rejectionsByReason.merge(event.getString("reason"), 1L, Math::addExact);
        case GARBAGE_COLLECTION_EVENT -> garbageCollections = Math.addExact(garbageCollections, 1L);
        case ALLOCATION_SAMPLE_EVENT -> allocationSamples = Math.addExact(allocationSamples, 1L);
        default -> {
          // The analyzer intentionally ignores unrelated JVM events.
        }
      }
    }

    private void acceptBatch(RecordedEvent event) {
      acceptedRows = Math.addExact(acceptedRows, event.getInt("acceptedRows"));
      rejectedRows = Math.addExact(rejectedRows, event.getInt("rejectedRows"));
      batchDurations.add(event.getDuration());
    }

    JfrAnalysis finish() {
      batchDurations.sort(Comparator.naturalOrder());
      Duration totalDuration =
          batchDurations.stream().reduce(Duration.ZERO, JfrAnalyzer::addDurationExact);
      return new JfrAnalysis(
          batchDurations.size(),
          acceptedRows,
          rejectedRows,
          totalDuration,
          percentile(batchDurations, 50),
          percentile(batchDurations, 95),
          percentile(batchDurations, 99),
          garbageCollections,
          allocationSamples,
          rejectionsByReason);
    }
  }

  private static Duration percentile(List<Duration> sortedDurations, int percentile) {
    if (sortedDurations.isEmpty()) {
      return Duration.ZERO;
    }
    int rank = (int) Math.ceil(percentile / 100.0 * sortedDurations.size());
    return sortedDurations.get(Math.max(0, rank - 1));
  }

  private static Duration addDurationExact(Duration left, Duration right) {
    return left.plus(right);
  }
}
