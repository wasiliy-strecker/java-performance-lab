package io.github.wasiliystrecker.performance.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Contrasts platform and virtual threads for blocking and CPU-bound task batches.
 *
 * <p>The experiment is intentionally workload-sensitive: it is not designed to produce a universal
 * "virtual threads are faster" claim.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Threads(1)
@State(Scope.Benchmark)
public class ExecutorWorkloadBenchmark {

  /** Number of tasks submitted by each invocation. */
  @Param({"32", "128"})
  public int taskCount;

  /** Kind of work executed by every task in a trial. */
  @Param({"BLOCKING", "CPU_BOUND"})
  public Workload workload;

  private ExecutorService platformExecutor;
  private ExecutorService virtualExecutor;
  private List<Callable<Long>> tasks;

  /** Creates both executors and immutable task descriptions before measurement begins. */
  @Setup(Level.Trial)
  public void setUp() {
    int availableProcessors = Runtime.getRuntime().availableProcessors();
    int platformThreads = Math.min(taskCount, Math.max(2, availableProcessors));
    platformExecutor = Executors.newFixedThreadPool(platformThreads);
    virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    List<Callable<Long>> configuredTasks = new ArrayList<>(taskCount);
    for (int taskIndex = 0; taskIndex < taskCount; taskIndex++) {
      long seed = taskIndex + 1L;
      configuredTasks.add(createTask(workload, seed));
    }
    tasks = List.copyOf(configuredTasks);
  }

  /** Stops trial-scoped executors even when a benchmark fork is interrupted. */
  @TearDown(Level.Trial)
  public void tearDown() {
    shutdown(platformExecutor);
    shutdown(virtualExecutor);
  }

  /**
   * Executes the task batch on a bounded platform-thread pool.
   *
   * @return checksum of all task results
   * @throws InterruptedException if the harness interrupts the invocation
   * @throws ExecutionException if a worker fails
   */
  @Benchmark
  public long platformThreads() throws InterruptedException, ExecutionException {
    return execute(platformExecutor);
  }

  /**
   * Executes the task batch with one virtual thread per task.
   *
   * @return checksum of all task results
   * @throws InterruptedException if the harness interrupts the invocation
   * @throws ExecutionException if a worker fails
   */
  @Benchmark
  public long virtualThreads() throws InterruptedException, ExecutionException {
    return execute(virtualExecutor);
  }

  private long execute(ExecutorService executor) throws InterruptedException, ExecutionException {
    long checksum = 0L;
    for (Future<Long> future : executor.invokeAll(tasks)) {
      checksum += future.get();
    }
    return checksum;
  }

  private static Callable<Long> createTask(Workload workload, long seed) {
    return switch (workload) {
      case BLOCKING ->
          () -> {
            TimeUnit.MILLISECONDS.sleep(2);
            return mix(seed);
          };
      case CPU_BOUND -> () -> burnCpu(seed);
    };
  }

  private static long burnCpu(long seed) {
    long value = seed;
    for (int iteration = 0; iteration < 20_000; iteration++) {
      value = mix(value + iteration);
    }
    return value;
  }

  private static long mix(long value) {
    value ^= value >>> 30;
    value *= 0xBF58_476D_1CE4_E5B9L;
    value ^= value >>> 27;
    value *= 0x94D0_49BB_1331_11EBL;
    return value ^ (value >>> 31);
  }

  private static void shutdown(ExecutorService executor) {
    if (executor == null) {
      return;
    }
    executor.shutdownNow();
  }

  /** Workload shapes with intentionally different scheduling characteristics. */
  public enum Workload {
    /** Simulated blocking I/O using a short sleep. */
    BLOCKING,

    /** Deterministic computation without blocking calls. */
    CPU_BOUND
  }
}
