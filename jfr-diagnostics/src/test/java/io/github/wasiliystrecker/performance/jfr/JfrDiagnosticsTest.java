package io.github.wasiliystrecker.performance.jfr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JfrDiagnosticsTest {
  @TempDir Path temporaryDirectory;

  @Test
  void recordsAndAnalyzesBothParserVariants() throws Exception {
    DiagnosticRecorder recorder = new DiagnosticRecorder(3, 500, 10, 42L);
    JfrAnalyzer analyzer = new JfrAnalyzer();

    for (ParserVariant variant : ParserVariant.values()) {
      Path recording = temporaryDirectory.resolve(variant.cliName() + ".jfr");

      recorder.record(variant, recording);
      JfrAnalysis analysis = analyzer.analyze(recording);

      assertThat(recording).isRegularFile();
      assertThat(analysis.batchEvents()).isEqualTo(3);
      assertThat(analysis.totalRows()).isEqualTo(1_500);
      assertThat(analysis.acceptedRows()).isPositive();
      assertThat(analysis.rejectedRows()).isPositive();
      assertThat(analysis.rejectionsByReason().values())
          .allMatch(count -> count > 0)
          .hasSizeGreaterThan(1);
      assertThat(analysis.p50BatchDuration()).isGreaterThanOrEqualTo(java.time.Duration.ZERO);
      assertThat(analysis.p95BatchDuration()).isGreaterThanOrEqualTo(analysis.p50BatchDuration());
      assertThat(analysis.p99BatchDuration()).isGreaterThanOrEqualTo(analysis.p95BatchDuration());
    }
  }

  @Test
  void rejectedLineEventsExposeOnlyContentFreeMetadata() throws Exception {
    Path recording = temporaryDirectory.resolve("privacy.jfr");
    new DiagnosticRecorder(1, 500, 20, 99L).record(ParserVariant.ALLOCATION_AWARE, recording);

    Set<String> customFields = new HashSet<>();
    try (RecordingFile recordingFile = new RecordingFile(recording)) {
      while (recordingFile.hasMoreEvents()) {
        RecordedEvent event = recordingFile.readEvent();
        if (RejectedLineEvent.NAME.equals(event.getEventType().getName())) {
          event.getEventType().getFields().stream()
              .map(ValueDescriptor::getName)
              .forEach(customFields::add);
        }
      }
    }

    assertThat(customFields).contains("lineIndex", "reason", "lineLength");
    assertThat(customFields)
        .noneMatch(
            field ->
                field.toLowerCase().contains("customer")
                    || field.toLowerCase().contains("content")
                    || field.toLowerCase().contains("payload")
                    || field.toLowerCase().contains("raw"));
  }

  @Test
  void cliAnalyzesARecordingAndRejectsInvalidUsage() throws Exception {
    Path recording = temporaryDirectory.resolve("cli.jfr");
    new DiagnosticRecorder(2, 200, 10, 7L).record(ParserVariant.READABLE, recording);
    ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();

    int analyzeStatus =
        JfrDiagnosticsCli.run(
            new String[] {"analyze", recording.toString()},
            new PrintStream(standardOutput, true, StandardCharsets.UTF_8),
            new PrintStream(errorOutput, true, StandardCharsets.UTF_8));
    int usageStatus =
        JfrDiagnosticsCli.run(
            new String[] {"unknown"},
            new PrintStream(standardOutput, true, StandardCharsets.UTF_8),
            new PrintStream(errorOutput, true, StandardCharsets.UTF_8));

    assertThat(analyzeStatus).isZero();
    assertThat(standardOutput.toString(StandardCharsets.UTF_8))
        .contains("JFR analysis", "Batch events: 2", "Rows: 400");
    assertThat(usageStatus).isEqualTo(2);
    assertThat(errorOutput.toString(StandardCharsets.UTF_8)).contains("Usage:");
  }

  @Test
  void analyzerRejectsMissingFiles() {
    assertThatIOException()
        .isThrownBy(() -> new JfrAnalyzer().analyze(temporaryDirectory.resolve("missing.jfr")))
        .withMessageContaining("recording does not exist");
  }
}
