package io.github.wasiliystrecker.performance.workload;

/**
 * Immutable outcome of one order-event batch.
 *
 * @param acceptedRows rows incorporated into the totals
 * @param rejectedRows rows rejected without changing the totals
 * @param eurNetMinorUnits captured minus refunded EUR minor units
 * @param usdNetMinorUnits captured minus refunded USD minor units
 * @param gbpNetMinorUnits captured minus refunded GBP minor units
 * @param eventIdChecksum sum of accepted event IDs
 */
public record BatchSummary(
    int acceptedRows,
    int rejectedRows,
    long eurNetMinorUnits,
    long usdNetMinorUnits,
    long gbpNetMinorUnits,
    long eventIdChecksum) {

  /** Validates row counts while allowing signed net totals. */
  public BatchSummary {
    if (acceptedRows < 0) {
      throw new IllegalArgumentException("acceptedRows must not be negative");
    }
    if (rejectedRows < 0) {
      throw new IllegalArgumentException("rejectedRows must not be negative");
    }
  }

  /**
   * Returns the number of input rows represented by this summary.
   *
   * @return accepted plus rejected rows
   */
  public int totalRows() {
    return Math.addExact(acceptedRows, rejectedRows);
  }
}
