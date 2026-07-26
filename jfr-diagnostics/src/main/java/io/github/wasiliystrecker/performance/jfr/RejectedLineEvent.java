package io.github.wasiliystrecker.performance.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/** Content-free JFR event emitted when a workload row is rejected. */
@Name(RejectedLineEvent.NAME)
@Label("Rejected order-event row")
@Description("Non-sensitive metadata about a rejected order-event row")
@Category({"Java Performance Lab", "Order ingestion"})
@StackTrace(false)
final class RejectedLineEvent extends Event {
  static final String NAME = "io.github.wasiliystrecker.performance.RejectedLine";

  @Label("Row index")
  int lineIndex;

  @Label("Reason")
  String reason;

  @Label("Input length")
  int lineLength;
}
