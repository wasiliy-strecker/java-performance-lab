package io.github.wasiliystrecker.performance.workload;

/** Stable, content-free reason categories for rejected order-event rows. */
public enum RejectionReason {
  /** The row reference is {@code null}. */
  NULL_LINE,

  /** The row does not contain exactly five fields. */
  FIELD_COUNT,

  /** The event identifier is missing, zero, or malformed. */
  INVALID_EVENT_ID,

  /** The customer identifier violates the documented format. */
  INVALID_CUSTOMER_ID,

  /** The currency is not supported. */
  INVALID_CURRENCY,

  /** The amount is missing or malformed. */
  INVALID_AMOUNT,

  /** The status is not a supported order-event status. */
  INVALID_STATUS,

  /** A numeric field is larger than a signed {@code long}. */
  NUMERIC_OVERFLOW,

  /** Adding a valid row would overflow a batch aggregate. */
  AGGREGATE_OVERFLOW
}
