package io.github.wasiliystrecker.performance.workload;

import java.util.List;

/** Processes a complete batch without retaining input or mutable state between calls. */
public interface OrderEventBatchProcessor {

  /**
   * Processes rows while ignoring individual rejection notifications.
   *
   * @param lines order-event rows
   * @return deterministic batch summary
   */
  default BatchSummary process(List<String> lines) {
    return process(lines, RejectedLineHandler.ignoring());
  }

  /**
   * Processes rows and reports each rejected row in encounter order.
   *
   * <p>The handler must be fast and must not throw. A handler exception is propagated and aborts
   * processing.
   *
   * @param lines order-event rows
   * @param rejectedLineHandler rejection observer
   * @return deterministic batch summary
   */
  BatchSummary process(List<String> lines, RejectedLineHandler rejectedLineHandler);
}
