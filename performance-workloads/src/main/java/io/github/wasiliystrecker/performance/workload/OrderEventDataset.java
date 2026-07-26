package io.github.wasiliystrecker.performance.workload;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/** Creates deterministic datasets outside measured benchmark regions. */
public final class OrderEventDataset {
  private static final String[] CURRENCIES = {"EUR", "USD", "GBP"};

  private OrderEventDataset() {}

  /**
   * Generates order-event rows with a deterministic mixture of valid and malformed data.
   *
   * @param size number of rows
   * @param invalidPercent approximate malformed-row percentage from 0 to 100
   * @param seed deterministic random seed
   * @return immutable dataset
   */
  public static List<String> generate(int size, int invalidPercent, long seed) {
    if (size < 0) {
      throw new IllegalArgumentException("size must not be negative");
    }
    if (invalidPercent < 0 || invalidPercent > 100) {
      throw new IllegalArgumentException("invalidPercent must be between 0 and 100");
    }

    SplittableRandom random = new SplittableRandom(seed);
    List<String> lines = new ArrayList<>(size);
    for (int index = 0; index < size; index++) {
      long eventId = index + 1L;
      String customerId = "customer-" + (1_000 + random.nextInt(9_000));
      String currency = CURRENCIES[random.nextInt(CURRENCIES.length)];
      long amountMinor = 1L + random.nextLong(1_000_000L);
      String status = random.nextInt(10) == 0 ? "REFUNDED" : "CAPTURED";
      String valid = eventId + "|" + customerId + "|" + currency + "|" + amountMinor + "|" + status;
      lines.add(
          random.nextInt(100) < invalidPercent ? corrupt(valid, index, random.nextInt(8)) : valid);
    }
    return List.copyOf(lines);
  }

  private static String corrupt(String valid, int index, int variant) {
    String[] fields = valid.split("\\|", -1);
    return switch (variant) {
      case 0 -> String.join("|", fields[0], fields[1], fields[2], fields[3]);
      case 1 ->
          "event-" + index + "|" + fields[1] + "|" + fields[2] + "|" + fields[3] + "|" + fields[4];
      case 2 -> fields[0] + "||" + fields[2] + "|" + fields[3] + "|" + fields[4];
      case 3 -> fields[0] + "|" + fields[1] + "|CHF|" + fields[3] + "|" + fields[4];
      case 4 -> fields[0] + "|" + fields[1] + "|" + fields[2] + "|-1|" + fields[4];
      case 5 -> fields[0] + "|" + fields[1] + "|" + fields[2] + "|" + fields[3] + "|UNKNOWN";
      case 6 ->
          "9223372036854775808|" + fields[1] + "|" + fields[2] + "|" + fields[3] + "|" + fields[4];
      case 7 -> valid + "|unexpected";
      default -> throw new IllegalArgumentException("unsupported corruption variant");
    };
  }
}
