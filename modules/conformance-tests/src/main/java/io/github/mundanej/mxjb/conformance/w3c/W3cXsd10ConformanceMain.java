package io.github.mundanej.mxjb.conformance.w3c;

import java.io.IOException;
import java.nio.file.Path;

/** Entry point for the opt-in W3C XML Schema 1.0 conformance intake lane. */
public final class W3cXsd10ConformanceMain {
  private W3cXsd10ConformanceMain() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      throw new IllegalArgumentException(
          "Usage: W3cXsd10ConformanceMain <xmlschema2006-11-06-dir> <report-dir>");
    }
    W3cXsd10SuiteIntake.Report report =
        new W3cXsd10SuiteIntake().run(Path.of(args[0]), Path.of(args[1]));
    System.out.println(report.toSummaryLine());
    report.categoryCounts().entrySet().stream()
        .map(
            entry ->
                "w3c-xsd10-category category="
                    + entry.getKey().token()
                    + " count="
                    + entry.getValue())
        .forEach(System.out::println);
    report.featureCounts().entrySet().stream()
        .map(entry -> "w3c-xsd10-feature feature=" + entry.getKey() + " count=" + entry.getValue())
        .forEach(System.out::println);
  }
}
