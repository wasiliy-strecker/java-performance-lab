package io.github.wasiliystrecker.performance.workload;

final class OrderEventParsing {
  static final long INVALID_NUMBER = -1L;
  static final long NUMBER_OVERFLOW = -2L;

  private OrderEventParsing() {}

  static long unsignedLong(CharSequence value, int start, int end) {
    if (start >= end) {
      return INVALID_NUMBER;
    }
    long result = 0L;
    for (int index = start; index < end; index++) {
      char character = value.charAt(index);
      if (character < '0' || character > '9') {
        return INVALID_NUMBER;
      }
      int digit = character - '0';
      if (result > (Long.MAX_VALUE - digit) / 10L) {
        return NUMBER_OVERFLOW;
      }
      result = result * 10L + digit;
    }
    return result;
  }

  static boolean validCustomer(CharSequence value, int start, int end) {
    int length = end - start;
    if (length < 1 || length > 32) {
      return false;
    }
    for (int index = start; index < end; index++) {
      char character = value.charAt(index);
      boolean alphaNumeric =
          character >= 'a' && character <= 'z'
              || character >= 'A' && character <= 'Z'
              || character >= '0' && character <= '9';
      if (!alphaNumeric && character != '-') {
        return false;
      }
    }
    return true;
  }

  static int currency(CharSequence value, int start, int end) {
    if (end - start != 3) {
      return -1;
    }
    char first = value.charAt(start);
    char second = value.charAt(start + 1);
    char third = value.charAt(start + 2);
    if (first == 'E' && second == 'U' && third == 'R') {
      return 0;
    }
    if (first == 'U' && second == 'S' && third == 'D') {
      return 1;
    }
    if (first == 'G' && second == 'B' && third == 'P') {
      return 2;
    }
    return -1;
  }

  static int direction(CharSequence value, int start, int end) {
    if (matches(value, start, end, "CAPTURED")) {
      return 1;
    }
    if (matches(value, start, end, "REFUNDED")) {
      return -1;
    }
    return 0;
  }

  private static boolean matches(CharSequence value, int start, int end, String expected) {
    int length = end - start;
    if (length != expected.length()) {
      return false;
    }
    for (int index = 0; index < length; index++) {
      if (value.charAt(start + index) != expected.charAt(index)) {
        return false;
      }
    }
    return true;
  }
}
