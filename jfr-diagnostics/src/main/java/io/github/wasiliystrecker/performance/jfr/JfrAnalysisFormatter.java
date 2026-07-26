package io.github.wasiliystrecker.performance.jfr;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/** Formats a compact, deterministic command-line report from a {@link JfrAnalysis}. */
public final class JfrAnalysisFormatter {
  private JfrAnalysisFormatter() {}

  /**
   * Creates a human-readable report without environment-specific paths.
   *
   * @param analysis analyzed recording
   * @return multi-line report
   */
  public static String format(JfrAnalysis analysis) {
    StringBuilder report = new StringBuilder();
    report.append("JFR analysis\n");
    report.append("Batch events: ").append(analysis.batchEvents()).append('\n');
    report
        .append("Rows: ")
        .append(analysis.totalRows())
        .append(" (accepted ")
        .append(analysis.acceptedRows())
        .append(", rejected ")
        .append(analysis.rejectedRows())
        .append(")\n");
    report
        .append("Batch latency p50/p95/p99: ")
        .append(formatDuration(analysis.p50BatchDuration()))
        .append(" / ")
        .append(formatDuration(analysis.p95BatchDuration()))
        .append(" / ")
        .append(formatDuration(analysis.p99BatchDuration()))
        .append('\n');
    report.append("Garbage collections: ").append(analysis.garbageCollections()).append('\n');
    report.append("Allocation samples: ").append(analysis.allocationSamples()).append('\n');
    report.append("Rejections:\n");
    if (analysis.rejectionsByReason().isEmpty()) {
      report.append("  none\n");
    } else {
      analysis.rejectionsByReason().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(
              entry ->
                  report
                      .append("  ")
                      .append(entry.getKey())
                      .append(": ")
                      .append(entry.getValue())
                      .append('\n'));
    }
    return report.toString();
  }

  private static String formatDuration(Duration duration) {
    return String.format(Locale.ROOT, "%.3f ms", duration.toNanos() / 1_000_000.0);
  }
}
