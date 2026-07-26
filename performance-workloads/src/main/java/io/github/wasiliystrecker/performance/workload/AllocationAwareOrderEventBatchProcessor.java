package io.github.wasiliystrecker.performance.workload;

import static io.github.wasiliystrecker.performance.workload.OrderEventParsing.INVALID_NUMBER;
import static io.github.wasiliystrecker.performance.workload.OrderEventParsing.NUMBER_OVERFLOW;

import java.util.List;
import java.util.Objects;

/** Allocation-aware parser that scans delimiter ranges without creating token arrays. */
public final class AllocationAwareOrderEventBatchProcessor implements OrderEventBatchProcessor {

  /** Creates an allocation-aware processor. */
  public AllocationAwareOrderEventBatchProcessor() {}

  @Override
  public BatchSummary process(List<String> lines, RejectedLineHandler rejectedLineHandler) {
    Objects.requireNonNull(lines, "lines");
    Objects.requireNonNull(rejectedLineHandler, "rejectedLineHandler");
    BatchAccumulator accumulator = new BatchAccumulator(rejectedLineHandler);

    for (int index = 0; index < lines.size(); index++) {
      String line = lines.get(index);
      if (line == null) {
        accumulator.reject(index, RejectionReason.NULL_LINE, 0);
        continue;
      }

      int first = line.indexOf('|');
      int second = first < 0 ? -1 : line.indexOf('|', first + 1);
      int third = second < 0 ? -1 : line.indexOf('|', second + 1);
      int fourth = third < 0 ? -1 : line.indexOf('|', third + 1);
      if (first < 0
          || second < 0
          || third < 0
          || fourth < 0
          || line.indexOf('|', fourth + 1) >= 0) {
        accumulator.reject(index, RejectionReason.FIELD_COUNT, line.length());
        continue;
      }

      long eventId = OrderEventParsing.unsignedLong(line, 0, first);
      if (eventId == INVALID_NUMBER || eventId == 0L) {
        accumulator.reject(index, RejectionReason.INVALID_EVENT_ID, line.length());
        continue;
      }
      if (eventId == NUMBER_OVERFLOW) {
        accumulator.reject(index, RejectionReason.NUMERIC_OVERFLOW, line.length());
        continue;
      }
      if (!OrderEventParsing.validCustomer(line, first + 1, second)) {
        accumulator.reject(index, RejectionReason.INVALID_CUSTOMER_ID, line.length());
        continue;
      }

      int currency = OrderEventParsing.currency(line, second + 1, third);
      if (currency < 0) {
        accumulator.reject(index, RejectionReason.INVALID_CURRENCY, line.length());
        continue;
      }

      long amount = OrderEventParsing.unsignedLong(line, third + 1, fourth);
      if (amount == INVALID_NUMBER) {
        accumulator.reject(index, RejectionReason.INVALID_AMOUNT, line.length());
        continue;
      }
      if (amount == NUMBER_OVERFLOW) {
        accumulator.reject(index, RejectionReason.NUMERIC_OVERFLOW, line.length());
        continue;
      }

      int direction = OrderEventParsing.direction(line, fourth + 1, line.length());
      if (direction == 0) {
        accumulator.reject(index, RejectionReason.INVALID_STATUS, line.length());
        continue;
      }
      accumulator.accept(index, line.length(), eventId, currency, amount, direction);
    }
    return accumulator.summary();
  }
}
