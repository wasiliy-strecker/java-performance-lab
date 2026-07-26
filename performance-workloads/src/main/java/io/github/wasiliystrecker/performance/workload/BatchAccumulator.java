package io.github.wasiliystrecker.performance.workload;

final class BatchAccumulator {
  private final RejectedLineHandler rejectedLineHandler;
  private int acceptedRows;
  private int rejectedRows;
  private long eurNetMinorUnits;
  private long usdNetMinorUnits;
  private long gbpNetMinorUnits;
  private long eventIdChecksum;

  BatchAccumulator(RejectedLineHandler rejectedLineHandler) {
    this.rejectedLineHandler = rejectedLineHandler;
  }

  void accept(
      int lineIndex, int lineLength, long eventId, int currency, long amount, int direction) {
    long signedAmount = direction > 0 ? amount : -amount;
    try {
      long newChecksum = Math.addExact(eventIdChecksum, eventId);
      long newCurrencyTotal =
          switch (currency) {
            case 0 -> Math.addExact(eurNetMinorUnits, signedAmount);
            case 1 -> Math.addExact(usdNetMinorUnits, signedAmount);
            case 2 -> Math.addExact(gbpNetMinorUnits, signedAmount);
            default -> throw new IllegalArgumentException("unknown currency index");
          };

      eventIdChecksum = newChecksum;
      switch (currency) {
        case 0 -> eurNetMinorUnits = newCurrencyTotal;
        case 1 -> usdNetMinorUnits = newCurrencyTotal;
        case 2 -> gbpNetMinorUnits = newCurrencyTotal;
        default -> throw new IllegalArgumentException("unknown currency index");
      }
      acceptedRows++;
    } catch (ArithmeticException overflow) {
      reject(lineIndex, RejectionReason.AGGREGATE_OVERFLOW, lineLength);
    }
  }

  void reject(int lineIndex, RejectionReason reason, int lineLength) {
    rejectedRows++;
    rejectedLineHandler.rejected(lineIndex, reason, lineLength);
  }

  BatchSummary summary() {
    return new BatchSummary(
        acceptedRows,
        rejectedRows,
        eurNetMinorUnits,
        usdNetMinorUnits,
        gbpNetMinorUnits,
        eventIdChecksum);
  }
}
