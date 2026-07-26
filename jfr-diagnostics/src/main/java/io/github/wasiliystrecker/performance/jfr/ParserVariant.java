package io.github.wasiliystrecker.performance.jfr;

import io.github.wasiliystrecker.performance.workload.AllocationAwareOrderEventBatchProcessor;
import io.github.wasiliystrecker.performance.workload.OrderEventBatchProcessor;
import io.github.wasiliystrecker.performance.workload.ReadableOrderEventBatchProcessor;
import java.util.Locale;

/** Selects one of the semantically equivalent parser implementations. */
public enum ParserVariant {
  /** Readability-first implementation based on {@link String#split(String, int)}. */
  READABLE("readable"),

  /** Delimiter-scanning implementation designed to reduce temporary allocations. */
  ALLOCATION_AWARE("allocation-aware");

  private final String cliName;

  ParserVariant(String cliName) {
    this.cliName = cliName;
  }

  /**
   * Returns the stable command-line representation.
   *
   * @return lower-case CLI name
   */
  public String cliName() {
    return cliName;
  }

  /**
   * Parses a command-line representation.
   *
   * @param value parser name
   * @return matching variant
   * @throws IllegalArgumentException if the value is unsupported
   */
  public static ParserVariant fromCliName(String value) {
    if (value == null) {
      throw new IllegalArgumentException("parser must not be null");
    }
    String normalized = value.toLowerCase(Locale.ROOT);
    for (ParserVariant candidate : values()) {
      if (candidate.cliName.equals(normalized)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException(
        "unsupported parser '" + value + "'; expected readable or allocation-aware");
  }

  OrderEventBatchProcessor createProcessor() {
    return switch (this) {
      case READABLE -> new ReadableOrderEventBatchProcessor();
      case ALLOCATION_AWARE -> new AllocationAwareOrderEventBatchProcessor();
    };
  }
}
