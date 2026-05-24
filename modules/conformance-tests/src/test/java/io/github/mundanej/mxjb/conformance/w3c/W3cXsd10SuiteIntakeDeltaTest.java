package io.github.mundanej.mxjb.conformance.w3c;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class W3cXsd10SuiteIntakeDeltaTest {
  @TempDir private Path tempDirectory;

  @Test
  void rejectsDuplicateStableIdsAcrossLocalMetadata() throws IOException {
    Path suiteRoot =
        writeSuite(
            List.of(
                spec(
                    "duplicate-a",
                    "duplicate-group",
                    "duplicate",
                    "../data/duplicate.xsd",
                    "valid",
                    "accepted",
                    "ordinary validation fixture",
                    true),
                spec(
                    "duplicate-b",
                    "duplicate-group",
                    "duplicate",
                    "../data/duplicate.xsd",
                    "valid",
                    "accepted",
                    "ordinary validation fixture",
                    true)));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new W3cXsd10SuiteIntake().parse(suiteRoot));

    assertTrue(exception.getMessage().contains("Duplicate W3C suite fixture id"));
  }

  @Test
  void rejectsMissingReferencedFiles() throws IOException {
    Path suiteRoot =
        writeSuite(
            List.of(
                spec(
                    "missing-file",
                    "missing-file-group",
                    "missing-file",
                    "../data/missing-file.xsd",
                    "valid",
                    "accepted",
                    "ordinary validation fixture",
                    false)));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new W3cXsd10SuiteIntake().parse(suiteRoot));

    assertTrue(exception.getMessage().contains("Missing W3C test document"));
    assertTrue(exception.getMessage().contains("missing-file.xsd"));
  }

  @Test
  void rejectsUnknownCurrentStatus() throws IOException {
    Path suiteRoot =
        writeSuite(
            List.of(
                spec(
                    "unknown-status",
                    "unknown-status-group",
                    "unknown-status",
                    "../data/unknown-status.xsd",
                    "valid",
                    "withdrawn",
                    "ordinary validation fixture",
                    true)));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new W3cXsd10SuiteIntake().parse(suiteRoot));

    assertTrue(exception.getMessage().contains("Unknown W3C current status 'withdrawn'"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"xsd 1.1", "xsd1.1", "xml 1.1", "xml1.1"})
  void rejectsXsd11AndXml11FixtureMarkers(String marker) throws IOException {
    Path suiteRoot =
        writeSuite(
            List.of(
                spec(
                    "version-marker-" + marker.replace(" ", "-").replace(".", "-"),
                    "version-marker-group",
                    "version-marker",
                    "../data/version-marker.xsd",
                    "valid",
                    "accepted",
                    "fixture mentions " + marker,
                    true)));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new W3cXsd10SuiteIntake().parse(suiteRoot));

    assertTrue(exception.getMessage().contains("Unexpected XSD 1.1 or XML 1.1 fixture"));
  }

  @Test
  void summaryIncludesBindingSupportedZeroForMinimalLocalMetadata() throws IOException {
    Path suiteRoot = writeSuite(List.of());
    Path reportDirectory = tempDirectory.resolve("summary-report");

    W3cXsd10SuiteIntake.Report report = new W3cXsd10SuiteIntake().run(suiteRoot, reportDirectory);

    assertEquals(15, report.total());
    assertEquals(0, report.categoryCounts().get(W3cXsd10SuiteIntake.Category.BINDING_SUPPORTED));
    String summary = Files.readString(reportDirectory.resolve("summary.txt"));
    assertTrue(summary.contains("binding-supported=0"));
    assertTrue(summary.contains("category.binding-supported=0"));
  }

  @Test
  void reportRowsAreDeterministic() throws IOException {
    Path suiteRoot =
        writeSuite(
            List.of(
                spec(
                    "z-local",
                    "z-group",
                    "z-test",
                    "../data/z-test.xsd",
                    "valid",
                    "accepted",
                    "ordinary validation fixture",
                    true),
                spec(
                    "a-local",
                    "a-group",
                    "a-test",
                    "../data/a-test.xsd",
                    "valid",
                    "accepted",
                    "ordinary validation fixture",
                    true)));
    Path firstReportDirectory = tempDirectory.resolve("first-report");
    Path secondReportDirectory = tempDirectory.resolve("second-report");

    new W3cXsd10SuiteIntake().run(suiteRoot, firstReportDirectory);
    new W3cXsd10SuiteIntake().run(suiteRoot, secondReportDirectory);

    List<String> firstRows = reportRows(firstReportDirectory.resolve("fixtures.tsv"));
    List<String> secondRows = reportRows(secondReportDirectory.resolve("fixtures.tsv"));
    List<String> sortedRows = firstRows.stream().sorted(Comparator.naturalOrder()).toList();
    assertIterableEquals(sortedRows, firstRows);
    assertEquals(firstRows, secondRows);
  }

  private Path writeSuite(List<TestSpec> overrides) throws IOException {
    Path root = tempDirectory.resolve(W3cXsd10SuiteIntake.EXPECTED_ROOT_NAME);
    Files.createDirectories(root);
    List<TestSpec> specs = new ArrayList<>(overrides);
    for (int index = specs.size(); index < W3cXsd10SuiteIntake.EXPECTED_TEST_SET_COUNT; index++) {
      specs.add(
          spec(
              "fixture-" + index,
              "group-" + index,
              "test-" + index,
              "../data/test-" + index + ".xsd",
              "valid",
              "accepted",
              "ordinary validation fixture",
              true));
    }
    for (int index = 0; index < specs.size(); index++) {
      TestSpec spec = specs.get(index);
      Path testSet = root.resolve("meta/" + index + "-" + spec.fileStem() + ".testSet");
      write(
          testSet,
          testSet(
              spec.groupName(),
              spec.testName(),
              spec.href(),
              spec.validity(),
              spec.status(),
              spec.description()));
      if (spec.writeDocument()) {
        write(parent(testSet).resolve(spec.href()).normalize(), minimalSchema(spec.testName()));
      }
    }
    return root;
  }

  private static List<String> reportRows(Path report) throws IOException {
    List<String> lines = Files.readAllLines(report);
    return lines.subList(1, lines.size());
  }

  private static TestSpec spec(
      String fileStem,
      String groupName,
      String testName,
      String href,
      String validity,
      String status,
      String description,
      boolean writeDocument) {
    return new TestSpec(
        fileStem, groupName, testName, href, validity, status, description, writeDocument);
  }

  private static String testSet(
      String groupName,
      String testName,
      String href,
      String validity,
      String status,
      String description) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<testSet name=\"AttrDecl\" contributor=\"LOCAL\""
        + " xmlns=\"http://www.w3.org/XML/2004/xml-schema-test-suite/\""
        + " xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n"
        + "  <testGroup name=\""
        + groupName
        + "\">\n"
        + "    <annotation><documentation><Description>"
        + description
        + "</Description></documentation></annotation>\n"
        + "    <schemaTest name=\""
        + testName
        + "\">\n"
        + "      <schemaDocument xlink:href=\""
        + href
        + "\"/>\n"
        + "      <expected validity=\""
        + validity
        + "\"/>\n"
        + "      <current status=\""
        + status
        + "\"/>\n"
        + "    </schemaTest>\n"
        + "  </testGroup>\n"
        + "</testSet>\n";
  }

  private static String minimalSchema(String name) {
    return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
        + "  <xs:element name=\"root\" type=\"xs:string\"/>\n"
        + "  <xs:annotation><xs:documentation>"
        + name
        + "</xs:documentation></xs:annotation>\n"
        + "</xs:schema>\n";
  }

  private static void write(Path path, String content) throws IOException {
    Files.createDirectories(parent(path));
    Files.writeString(path, content);
  }

  private static Path parent(Path path) {
    return Objects.requireNonNull(path.getParent(), "Path has no parent: " + path);
  }

  private record TestSpec(
      String fileStem,
      String groupName,
      String testName,
      String href,
      String validity,
      String status,
      String description,
      boolean writeDocument) {}
}
