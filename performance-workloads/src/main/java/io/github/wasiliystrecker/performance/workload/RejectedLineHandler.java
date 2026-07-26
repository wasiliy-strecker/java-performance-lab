package io.github.wasiliystrecker.performance.workload;

/** Receives metadata about a rejected row without exposing its potentially sensitive content. */
@FunctionalInterface
public interface RejectedLineHandler {
  /** Shared no-op handler for callers that do not need rejection metadata. */
  RejectedLineHandler IGNORING = (lineIndex, reason, lineLength) -> {};

  /**
   * Reports a rejected row.
   *
   * @param lineIndex zero-based index within the batch
   * @param reason stable rejection category
   * @param lineLength input length, or zero for a {@code null} row
   */
  void rejected(int lineIndex, RejectionReason reason, int lineLength);

  /**
   * Returns a singleton handler that discards notifications.
   *
   * @return no-op rejection handler
   */
  static RejectedLineHandler ignoring() {
    return IGNORING;
  }
}
