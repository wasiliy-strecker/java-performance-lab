package io.github.wasiliystrecker.performance.benchmark;

import io.github.wasiliystrecker.performance.workload.AllocationAwareOrderEventBatchProcessor;
import io.github.wasiliystrecker.performance.workload.BatchSummary;
import io.github.wasiliystrecker.performance.workload.OrderEventBatchProcessor;
import io.github.wasiliystrecker.performance.workload.OrderEventDataset;
import io.github.wasiliystrecker.performance.workload.ReadableOrderEventBatchProcessor;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Compares equivalent parsers over deterministic order-event batches.
 *
 * <p>Dataset creation is intentionally outside the measured region. Returning the summary makes the
 * computed result observable to the harness.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(2)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Threads(1)
@State(Scope.Benchmark)
public class OrderEventIngestionBenchmark {
  private static final long DATASET_SEED = 0x5EED_C0DEL;

  /** Number of rows processed by one benchmark invocation. */
  @Param({"100", "10000"})
  public int size;

  /** Percentage of deterministically corrupted rows. */
  @Param({"0", "5"})
  public int invalidPercent;

  private List<String> lines;
  private OrderEventBatchProcessor readable;
  private OrderEventBatchProcessor allocationAware;

  /** Creates trial-scoped inputs and processor instances outside measured invocations. */
  @Setup(Level.Trial)
  public void setUp() {
    lines = OrderEventDataset.generate(size, invalidPercent, DATASET_SEED);
    readable = new ReadableOrderEventBatchProcessor();
    allocationAware = new AllocationAwareOrderEventBatchProcessor();
  }

  /**
   * Processes a batch by tokenizing each row.
   *
   * @return observable batch summary
   */
  @Benchmark
  public BatchSummary readableSplitParser() {
    return readable.process(lines);
  }

  /**
   * Processes a batch by scanning delimiter ranges.
   *
   * @return observable batch summary
   */
  @Benchmark
  public BatchSummary allocationAwareParser() {
    return allocationAware.process(lines);
  }
}
