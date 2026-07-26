package io.github.wasiliystrecker.performance.workload;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class OrderEventProcessorDifferentialTest {
  private final OrderEventBatchProcessor readable = new ReadableOrderEventBatchProcessor();
  private final OrderEventBatchProcessor allocationAware =
      new AllocationAwareOrderEventBatchProcessor();

  @Test
  void implementationsRemainEquivalentAcrossThirtyThousandRows() {
    for (int invalidPercent : List.of(0, 5, 35)) {
      for (long seed = 0; seed < 10; seed++) {
        List<String> dataset = OrderEventDataset.generate(1_000, invalidPercent, seed);
        Observed readableResult = process(readable, dataset);
        Observed allocationAwareResult = process(allocationAware, dataset);

        assertThat(allocationAwareResult)
            .as("invalidPercent=%s seed=%s", invalidPercent, seed)
            .isEqualTo(readableResult);
      }
    }
  }

  @Test
  void datasetIsDeterministicAndImmutable() {
    List<String> first = OrderEventDataset.generate(20, 10, 42);
    List<String> second = OrderEventDataset.generate(20, 10, 42);

    assertThat(first).isEqualTo(second);
    org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> first.add("unexpected"));
  }

  @Test
  void datasetRejectsInvalidBounds() {
    org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> OrderEventDataset.generate(-1, 0, 1));
    org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> OrderEventDataset.generate(1, -1, 1));
    org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> OrderEventDataset.generate(1, 101, 1));
  }

  private static Observed process(OrderEventBatchProcessor processor, List<String> lines) {
    List<Rejected> rejected = new ArrayList<>();
    BatchSummary summary =
        processor.process(
            lines, (index, reason, length) -> rejected.add(new Rejected(index, reason, length)));
    return new Observed(summary, List.copyOf(rejected));
  }

  private record Observed(BatchSummary summary, List<Rejected> rejected) {}

  private record Rejected(int index, RejectionReason reason, int length) {}
}
