package io.github.mundanej.mxjb.conformance.w3c;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class W3cXsd10SuiteIntakeTest {
  @TempDir private Path tempDirectory;

  @Test
  void classifiesPinnedSuiteMetadataAndWritesReports() throws IOException {
    Path suiteRoot = writeSuite();
    W3cXsd10SuiteIntake.Report report =
        new W3cXsd10SuiteIntake().run(suiteRoot, tempDirectory.resolve("reports"));

    assertEquals(29, report.total());
    assertEquals(22, report.categoryCounts().get(W3cXsd10SuiteIntake.Category.VALIDATION_ONLY));
    assertEquals(2, report.categoryCounts().get(W3cXsd10SuiteIntake.Category.TOLERATED_METADATA));
    assertEquals(1, report.categoryCounts().get(W3cXsd10SuiteIntake.Category.EXPECTED_DIAGNOSTIC));
    assertEquals(
        2, report.categoryCounts().get(W3cXsd10SuiteIntake.Category.PRODUCT_SCOPE_INCOMPATIBLE));
    assertEquals(2, report.categoryCounts().get(W3cXsd10SuiteIntake.Category.BLOCKED));
    assertTrue(Files.isRegularFile(tempDirectory.resolve("reports/fixtures.tsv")));
    assertTrue(
        Files.readString(tempDirectory.resolve("reports/summary.txt")).contains("archiveSha256="));
  }

  @Test
  void commandLineMainWritesReportForSuiteDirectory() throws IOException {
    Path suiteRoot = writeSuite();
    Path reportDirectory = tempDirectory.resolve("main-reports");

    W3cXsd10ConformanceMain.main(new String[] {suiteRoot.toString(), reportDirectory.toString()});

    assertTrue(Files.readString(reportDirectory.resolve("summary.txt")).contains("total=29"));
  }

  @Test
  void commandLineMainRejectsInvalidArguments() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> W3cXsd10ConformanceMain.main(new String[0]));

    assertTrue(exception.getMessage().contains("Usage: W3cXsd10ConformanceMain"));
  }

  @Test
  void rejectsUnexpectedSuiteRootName() throws IOException {
    Path wrongRoot = tempDirectory.resolve("not-the-w3c-root");
    Files.createDirectories(wrongRoot);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new W3cXsd10SuiteIntake().parse(wrongRoot));

    assertTrue(exception.getMessage().contains("xmlschema2006-11-06"));
  }

  @Test
  void rejectsXml11OrXsd11Metadata() throws IOException {
    Path suiteRoot = writeSuite();
    Files.writeString(
        suiteRoot.resolve("sunMeta/Schema.testSet"),
        testSet(
            "Schema",
            "SUN",
            "schema-xml11",
            "xml 1.1 is outside this suite",
            schemaTest("xml11", "../sunData/Schema/xml11.xsd", "valid", "accepted")));
    write(suiteRoot.resolve("sunData/Schema/xml11.xsd"), minimalSchema("xml11"));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new W3cXsd10SuiteIntake().parse(suiteRoot));

    assertTrue(exception.getMessage().contains("XSD 1.1 or XML 1.1"));
  }

  @Test
  void rejectsUnknownExpectedValidity() throws IOException {
    Path suiteRoot = writeSuite();
    Files.writeString(
        suiteRoot.resolve("sunMeta/AttrDecl.testSet"),
        testSet(
            "AttrDecl",
            "SUN",
            "bad-validity",
            "Bad validity",
            schemaTest("bad", "../sunData/AttrDecl/bad.xsd", "maybe", "accepted")));
    write(suiteRoot.resolve("sunData/AttrDecl/bad.xsd"), minimalSchema("bad"));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new W3cXsd10SuiteIntake().parse(suiteRoot));

    assertTrue(exception.getMessage().contains("Unknown W3C expected validity"));
  }

  private Path writeSuite() throws IOException {
    Path root = tempDirectory.resolve(W3cXsd10SuiteIntake.EXPECTED_ROOT_NAME);
    writeTestSet(root, "boeingMeta/BoeingXSDTestSet.testSet", "BoeingXSDTestCases", "BOEING");
    writeTestSet(root, "nistMeta/NISTXMLSchemaDatatypes.testSet", "NIST2004-01-14", "NIST");
    writeTestSet(root, "sunMeta/AGroupDef.testSet", "AGroupDef", "SUN");
    writeTestSet(root, "sunMeta/AttrDecl.testSet", "AttrDecl", "SUN");
    writeTestSet(root, "sunMeta/AttrUse.testSet", "AttrUse", "SUN");
    writeTestSet(root, "sunMeta/CType.testSet", "CType", "SUN");
    writeTestSet(root, "sunMeta/ElemDecl.testSet", "ElemDecl", "SUN", "annotation");
    writeTestSet(root, "sunMeta/IdConstrDefs.testSet", "IdConstrDefs", "SUN");
    writeTestSet(root, "sunMeta/MGroup.testSet", "MGroup", "SUN");
    writeTestSet(root, "sunMeta/MGroupDef.testSet", "MGroupDef", "SUN");
    writeTestSet(root, "sunMeta/Notation.testSet", "Notation", "SUN");
    writeTestSet(root, "sunMeta/SType.testSet", "SType", "SUN");
    writeTestSet(root, "sunMeta/Schema.testSet", "Schema", "SUN", "blocked");
    writeTestSet(root, "sunMeta/Wildcard.testSet", "Wildcard", "SUN");
    writeRedefineTestSet(root);
    return root;
  }

  private void writeTestSet(Path root, String metadataPath, String name, String contributor)
      throws IOException {
    writeTestSet(root, metadataPath, name, contributor, "normal");
  }

  private void writeTestSet(
      Path root, String metadataPath, String name, String contributor, String mode)
      throws IOException {
    String groupName = name + "-group";
    Path metadata = root.resolve(metadataPath);
    Path metadataParent = parent(metadata);
    String dataDirectory = fileName(metadataParent).replace("Meta", "Data");
    String schemaHref = "../" + dataDirectory + "/" + name + ".xsd";
    String xmlHref = "../" + dataDirectory + "/" + name + ".xml";
    String status = "blocked".equals(mode) ? "queried" : "accepted";
    String description =
        switch (mode) {
          case "annotation" -> "annotation documentation for tolerated metadata";
          case "blocked" -> "queried W3C metadata row";
          default -> "ordinary validation fixture";
        };
    write(
        metadata,
        testSet(
            name,
            contributor,
            groupName,
            description,
            schemaTest(name, schemaHref, "valid", status),
            instanceTest(name, xmlHref, "valid", status)));
    write(metadataParent.resolve(schemaHref).normalize(), minimalSchema(name));
    write(metadataParent.resolve(xmlHref).normalize(), "<root>" + name + "</root>");
  }

  private void writeRedefineTestSet(Path root) throws IOException {
    Path metadata = root.resolve("sunMeta/suntest.testSet");
    Path metadataParent = parent(metadata);
    String schemaHref = "../sunData/suntest/redefine.xsd";
    write(
        metadata,
        testSet(
            "suntest",
            "SUN",
            "redefine",
            "redefine remains an expected diagnostic",
            schemaTest("redefine", schemaHref, "invalid", "accepted")));
    write(metadataParent.resolve(schemaHref).normalize(), redefineSchema());
    write(metadataParent.resolve("../sunData/suntest/base.xsd").normalize(), minimalSchema("base"));
  }

  private static String testSet(
      String name, String contributor, String groupName, String description, String... tests) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<testSet name=\""
        + name
        + "\" contributor=\""
        + contributor
        + "\" xmlns=\"http://www.w3.org/XML/2004/xml-schema-test-suite/\""
        + " xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n"
        + "  <testGroup name=\""
        + groupName
        + "\">\n"
        + "    <annotation><documentation><Description>"
        + description
        + "</Description></documentation></annotation>\n"
        + String.join("\n", tests)
        + "\n  </testGroup>\n"
        + "</testSet>\n";
  }

  private static String schemaTest(String name, String href, String validity, String status) {
    return "    <schemaTest name=\""
        + name
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
        + "    </schemaTest>";
  }

  private static String instanceTest(String name, String href, String validity, String status) {
    return "    <instanceTest name=\""
        + name
        + "\">\n"
        + "      <instanceDocument xlink:href=\""
        + href
        + "\"/>\n"
        + "      <expected validity=\""
        + validity
        + "\"/>\n"
        + "      <current status=\""
        + status
        + "\"/>\n"
        + "    </instanceTest>";
  }

  private static String minimalSchema(String name) {
    return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
        + "  <xs:element name=\"root\" type=\"xs:string\"/>\n"
        + "  <xs:annotation><xs:documentation>"
        + name
        + "</xs:documentation></xs:annotation>\n"
        + "</xs:schema>\n";
  }

  private static String redefineSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
          <xs:redefine schemaLocation="base.xsd">
            <xs:element name="root" type="xs:string"/>
          </xs:redefine>
        </xs:schema>
        """;
  }

  private static void write(Path path, String content) throws IOException {
    Files.createDirectories(parent(path));
    Files.writeString(path, content);
  }

  private static Path parent(Path path) {
    return Objects.requireNonNull(path.getParent(), "Path has no parent: " + path);
  }

  private static String fileName(Path path) {
    return Objects.requireNonNull(path.getFileName(), "Path has no file name: " + path).toString();
  }
}
