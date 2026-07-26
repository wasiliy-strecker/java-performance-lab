package io.github.wasiliystrecker.performance.workload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class OrderEventBatchProcessorContractTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("processors")
  void aggregatesCapturedAndRefundedAmounts(String name, OrderEventBatchProcessor processor) {
    BatchSummary result =
        processor.process(
            List.of(
                "1|customer-1|EUR|100|CAPTURED",
                "2|customer-2|EUR|40|REFUNDED",
                "3|customer-3|USD|250|CAPTURED",
                "4|customer-4|GBP|0|REFUNDED"));

    assertThat(result).isEqualTo(new BatchSummary(4, 0, 60, 250, 0, 10));
    assertThat(result.totalRows()).isEqualTo(4);
  }

  @ParameterizedTest(name = "{0}: {1}")
  @MethodSource("invalidRows")
  void classifiesInvalidRows(
      String processorName,
      String scenario,
      OrderEventBatchProcessor processor,
      String line,
      RejectionReason expectedReason) {
    List<Rejected> rejected = new ArrayList<>();

    BatchSummary result =
        processor.process(
            java.util.Collections.singletonList(line),
            (index, reason, length) -> rejected.add(new Rejected(index, reason, length)));

    assertThat(result).isEqualTo(new BatchSummary(0, 1, 0, 0, 0, 0));
    assertThat(rejected)
        .containsExactly(new Rejected(0, expectedReason, line == null ? 0 : line.length()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("processors")
  void rejectsTheCurrentRowWhenAggregateTotalsWouldOverflow(
      String name, OrderEventBatchProcessor processor) {
    List<Rejected> rejected = new ArrayList<>();

    BatchSummary result =
        processor.process(
            List.of(
                "9223372036854775806|customer-1|EUR|9223372036854775807|CAPTURED",
                "1|customer-2|EUR|1|CAPTURED"),
            (index, reason, length) -> rejected.add(new Rejected(index, reason, length)));

    assertThat(result).isEqualTo(new BatchSummary(1, 1, Long.MAX_VALUE, 0, 0, Long.MAX_VALUE - 1));
    assertThat(rejected)
        .extracting(Rejected::reason)
        .containsExactly(RejectionReason.AGGREGATE_OVERFLOW);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("processors")
  void requiresBatchAndHandler(String name, OrderEventBatchProcessor processor) {
    assertThatNullPointerException().isThrownBy(() -> processor.process(null));
    assertThatNullPointerException().isThrownBy(() -> processor.process(List.of(), null));
  }

  @Test
  void validatesSummaryCounts() {
    org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> new BatchSummary(-1, 0, 0, 0, 0, 0));
    org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> new BatchSummary(0, -1, 0, 0, 0, 0));
  }

  private static Stream<Arguments> processors() {
    return Stream.of(
        Arguments.of("readable", new ReadableOrderEventBatchProcessor()),
        Arguments.of("allocation-aware", new AllocationAwareOrderEventBatchProcessor()));
  }

  private static Stream<Arguments> invalidRows() {
    return processors()
        .flatMap(
            processor ->
                Stream.of(
                    Arguments.of(
                        processor.get()[0],
                        "null",
                        processor.get()[1],
                        null,
                        RejectionReason.NULL_LINE),
                    Arguments.of(
                        processor.get()[0],
                        "field count",
                        processor.get()[1],
                        "1|customer|EUR|10",
                        RejectionReason.FIELD_COUNT),
                    Arguments.of(
                        processor.get()[0],
                        "invalid event id",
                        processor.get()[1],
                        "event|customer|EUR|10|CAPTURED",
                        RejectionReason.INVALID_EVENT_ID),
                    Arguments.of(
                        processor.get()[0],
                        "event id overflow",
                        processor.get()[1],
                        "9223372036854775808|customer|EUR|10|CAPTURED",
                        RejectionReason.NUMERIC_OVERFLOW),
                    Arguments.of(
                        processor.get()[0],
                        "invalid customer",
                        processor.get()[1],
                        "1|customer_42|EUR|10|CAPTURED",
                        RejectionReason.INVALID_CUSTOMER_ID),
                    Arguments.of(
                        processor.get()[0],
                        "invalid currency",
                        processor.get()[1],
                        "1|customer|CHF|10|CAPTURED",
                        RejectionReason.INVALID_CURRENCY),
                    Arguments.of(
                        processor.get()[0],
                        "invalid amount",
                        processor.get()[1],
                        "1|customer|EUR|-1|CAPTURED",
                        RejectionReason.INVALID_AMOUNT),
                    Arguments.of(
                        processor.get()[0],
                        "amount overflow",
                        processor.get()[1],
                        "1|customer|EUR|9223372036854775808|CAPTURED",
                        RejectionReason.NUMERIC_OVERFLOW),
                    Arguments.of(
                        processor.get()[0],
                        "invalid status",
                        processor.get()[1],
                        "1|customer|EUR|10|PENDING",
                        RejectionReason.INVALID_STATUS)));
  }

  private record Rejected(int index, RejectionReason reason, int length) {}
}
