package io.github.wasiliystrecker.performance.jfr;

import io.github.wasiliystrecker.performance.workload.BatchSummary;
import io.github.wasiliystrecker.performance.workload.OrderEventBatchProcessor;
import io.github.wasiliystrecker.performance.workload.OrderEventDataset;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import jdk.jfr.Recording;

/** Records deterministic parser workloads with custom and selected JDK Flight Recorder events. */
public final class DiagnosticRecorder {
  private static final int DEFAULT_BATCH_COUNT = 20;
  private static final int DEFAULT_BATCH_SIZE = 10_000;
  private static final int DEFAULT_INVALID_PERCENT = 5;
  private static final long DEFAULT_SEED = 0xD1A6_0057L;

  private final int batchCount;
  private final int batchSize;
  private final int invalidPercent;
  private final long seed;

  /** Creates a recorder with a representative, deterministic workload. */
  public DiagnosticRecorder() {
    this(DEFAULT_BATCH_COUNT, DEFAULT_BATCH_SIZE, DEFAULT_INVALID_PERCENT, DEFAULT_SEED);
  }

  /**
   * Creates a recorder with explicit workload dimensions.
   *
   * @param batchCount number of repeated batch events
   * @param batchSize rows per batch
   * @param invalidPercent approximate percentage of malformed rows
   * @param seed deterministic dataset seed
   */
  public DiagnosticRecorder(int batchCount, int batchSize, int invalidPercent, long seed) {
    if (batchCount <= 0) {
      throw new IllegalArgumentException("batchCount must be positive");
    }
    if (batchSize <= 0) {
      throw new IllegalArgumentException("batchSize must be positive");
    }
    if (invalidPercent < 0 || invalidPercent > 100) {
      throw new IllegalArgumentException("invalidPercent must be between 0 and 100");
    }
    this.batchCount = batchCount;
    this.batchSize = batchSize;
    this.invalidPercent = invalidPercent;
    this.seed = seed;
  }

  /**
   * Records the selected parser and writes a self-contained JFR file.
   *
   * <p>Dataset construction happens before recording so events describe parser behavior rather than
   * fixture generation. Existing output files are replaced.
   *
   * @param variant parser implementation
   * @param output destination ending in {@code .jfr}
   * @throws IOException if the destination cannot be written
   */
  public void record(ParserVariant variant, Path output) throws IOException {
    Objects.requireNonNull(variant, "variant");
    Objects.requireNonNull(output, "output");
    Path normalizedOutput = output.toAbsolutePath().normalize();
    createParentDirectories(normalizedOutput);

    List<String> dataset = OrderEventDataset.generate(batchSize, invalidPercent, seed);
    OrderEventBatchProcessor processor = variant.createProcessor();

    try (Recording recording = new Recording()) {
      recording.setName("java-performance-lab-" + variant.cliName());
      recording.setToDisk(true);
      recording.enable(OrderBatchEvent.class).withoutThreshold().withoutStackTrace();
      recording.enable(RejectedLineEvent.class).withoutThreshold().withoutStackTrace();
      recording.enable("jdk.GarbageCollection").withoutThreshold().withoutStackTrace();
      recording.enable("jdk.ObjectAllocationSample").withoutStackTrace();
      recording.start();

      for (int batchIndex = 0; batchIndex < batchCount; batchIndex++) {
        recordBatch(variant, processor, dataset);
      }

      recording.stop();
      recording.dump(normalizedOutput);
    }
  }

  private static void recordBatch(
      ParserVariant variant, OrderEventBatchProcessor processor, List<String> dataset) {
    OrderBatchEvent batchEvent = new OrderBatchEvent();
    batchEvent.parser = variant.cliName();
    batchEvent.inputRows = dataset.size();
    batchEvent.begin();

    BatchSummary summary =
        processor.process(
            dataset,
            (lineIndex, reason, lineLength) -> {
              RejectedLineEvent rejectedEvent = new RejectedLineEvent();
              rejectedEvent.lineIndex = lineIndex;
              rejectedEvent.reason = reason.name();
              rejectedEvent.lineLength = lineLength;
              rejectedEvent.commit();
            });

    batchEvent.acceptedRows = summary.acceptedRows();
    batchEvent.rejectedRows = summary.rejectedRows();
    batchEvent.eventIdChecksum = summary.eventIdChecksum();
    batchEvent.end();
    batchEvent.commit();
  }

  private static void createParentDirectories(Path output) throws IOException {
    Path parent = output.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }
}
