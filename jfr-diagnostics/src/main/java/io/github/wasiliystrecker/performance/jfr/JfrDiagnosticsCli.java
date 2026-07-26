package io.github.wasiliystrecker.performance.jfr;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

/** Command-line entry point for recording and analyzing the performance case study. */
public final class JfrDiagnosticsCli {
  private static final String USAGE =
      """
      Usage:
        java -jar jfr-diagnostics.jar record <readable|allocation-aware> <output.jfr>
        java -jar jfr-diagnostics.jar analyze <input.jfr>
      """;

  private JfrDiagnosticsCli() {}

  /**
   * Runs the diagnostics command.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    int status = run(args, System.out, System.err);
    if (status != 0) {
      System.exit(status);
    }
  }

  static int run(String[] args, PrintStream out, PrintStream err) {
    try {
      if (args.length == 3 && "record".equals(args[0])) {
        ParserVariant variant = ParserVariant.fromCliName(args[1]);
        Path output = Path.of(args[2]);
        new DiagnosticRecorder().record(variant, output);
        out.println("Recording written: " + output.toAbsolutePath().normalize());
        return 0;
      }
      if (args.length == 2 && "analyze".equals(args[0])) {
        JfrAnalysis analysis = new JfrAnalyzer().analyze(Path.of(args[1]));
        out.print(JfrAnalysisFormatter.format(analysis));
        return 0;
      }
      err.print(USAGE);
      return 2;
    } catch (IllegalArgumentException | IOException exception) {
      err.println("Error: " + exception.getMessage());
      return 1;
    }
  }
}
