package io.github.wasiliystrecker.performance.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/** JFR event for one complete order-event batch. */
@Name(OrderBatchEvent.NAME)
@Label("Order event batch")
@Description("Processing outcome for a deterministic order-event batch")
@Category({"Java Performance Lab", "Order ingestion"})
@StackTrace(false)
final class OrderBatchEvent extends Event {
  static final String NAME = "io.github.wasiliystrecker.performance.OrderBatch";

  @Label("Parser")
  String parser;

  @Label("Input rows")
  int inputRows;

  @Label("Accepted rows")
  int acceptedRows;

  @Label("Rejected rows")
  int rejectedRows;

  @Label("Event ID checksum")
  long eventIdChecksum;
}
