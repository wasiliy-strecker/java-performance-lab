package io.github.wasiliystrecker.performance.workload;

import static io.github.wasiliystrecker.performance.workload.OrderEventParsing.INVALID_NUMBER;
import static io.github.wasiliystrecker.performance.workload.OrderEventParsing.NUMBER_OVERFLOW;

import java.util.List;
import java.util.Objects;

/**
 * Readability-first implementation that tokenizes each row with {@link String#split(String, int)}.
 */
public final class ReadableOrderEventBatchProcessor implements OrderEventBatchProcessor {

  /** Creates a readability-first processor. */
  public ReadableOrderEventBatchProcessor() {}

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

      String[] fields = line.split("\\|", -1);
      if (fields.length != 5) {
        accumulator.reject(index, RejectionReason.FIELD_COUNT, line.length());
        continue;
      }

      long eventId = OrderEventParsing.unsignedLong(fields[0], 0, fields[0].length());
      if (eventId == INVALID_NUMBER || eventId == 0L) {
        accumulator.reject(index, RejectionReason.INVALID_EVENT_ID, line.length());
        continue;
      }
      if (eventId == NUMBER_OVERFLOW) {
        accumulator.reject(index, RejectionReason.NUMERIC_OVERFLOW, line.length());
        continue;
      }
      if (!OrderEventParsing.validCustomer(fields[1], 0, fields[1].length())) {
        accumulator.reject(index, RejectionReason.INVALID_CUSTOMER_ID, line.length());
        continue;
      }

      int currency = OrderEventParsing.currency(fields[2], 0, fields[2].length());
      if (currency < 0) {
        accumulator.reject(index, RejectionReason.INVALID_CURRENCY, line.length());
        continue;
      }

      long amount = OrderEventParsing.unsignedLong(fields[3], 0, fields[3].length());
      if (amount == INVALID_NUMBER) {
        accumulator.reject(index, RejectionReason.INVALID_AMOUNT, line.length());
        continue;
      }
      if (amount == NUMBER_OVERFLOW) {
        accumulator.reject(index, RejectionReason.NUMERIC_OVERFLOW, line.length());
        continue;
      }

      int direction = OrderEventParsing.direction(fields[4], 0, fields[4].length());
      if (direction == 0) {
        accumulator.reject(index, RejectionReason.INVALID_STATUS, line.length());
        continue;
      }
      accumulator.accept(index, line.length(), eventId, currency, amount, direction);
    }
    return accumulator.summary();
  }
}
