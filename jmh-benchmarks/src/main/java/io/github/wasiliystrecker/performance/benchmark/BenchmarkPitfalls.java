package io.github.wasiliystrecker.performance.benchmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Executable examples of two common microbenchmark mistakes.
 *
 * <p>Methods prefixed with {@code wrong} are teaching examples, not evidence for an optimization.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Threads(1)
@State(Scope.Thread)
public class BenchmarkPitfalls {

  /** Runtime-provided input that prevents compile-time constant folding. */
  @Param({"42.0"})
  public double input;

  /**
   * Incorrect: the operation has constant inputs and can be folded away.
   *
   * @return a value that does not rescue the constant input
   */
  @Benchmark
  public double wrongConstantFolding() {
    return Math.log(42.0);
  }

  /**
   * Correct: obtain inputs through JMH state.
   *
   * @return computed logarithm
   */
  @Benchmark
  public double correctRuntimeInput() {
    return Math.log(input);
  }

  /** Incorrect: the unused pure computation is eligible for dead-code elimination. */
  @Benchmark
  public void wrongDeadCodeElimination() {
    Math.log(input);
  }

  /**
   * Correct: make a result observable with a JMH black hole.
   *
   * @param blackhole harness-provided result consumer
   */
  @Benchmark
  public void correctConsumeResult(Blackhole blackhole) {
    blackhole.consume(Math.log(input));
  }
}
