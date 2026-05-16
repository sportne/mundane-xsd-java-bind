package io.github.xsdbind.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.xsdbind.generator.core.diagnostics.DiagnosticCode;
import io.github.xsdbind.generator.core.diagnostics.SchemaDiagnostic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class XsdSyntaxModelEdgeTest {
  @TempDir private Path tempDirectory;

  @Test
  void emptySyntaxModelHasEmptyText() {
    XsdSyntaxResult result = new XsdSyntaxResult(new XsdSyntaxModel(List.of()), List.of());

    assertEquals("", result.model().toText());
    assertEquals(false, result.hasErrors());
  }

  @Test
  void syntaxResultReportsErrors() {
    XsdSyntaxResult result =
        new XsdSyntaxResult(
            new XsdSyntaxModel(List.of()),
            List.of(
                new SchemaDiagnostic(
                    DiagnosticCode.SCHEMA_FRONTEND_EXPECTED_SCHEMA_ROOT,
                    "broken.xsd",
                    "Expected xs:schema root but found no document element.")));

    assertEquals(true, result.hasErrors());
  }

  @Test
  void documentTextOmitsEmptyNamespaceAndChildSections() {
    XsdSyntaxDocument document =
        new XsdSyntaxDocument("empty.xsd", null, java.util.Map.of(), List.of());

    assertEquals("document empty.xsd namespace=", document.toText());
  }

  @Test
  void frontendXmlErrorsProduceNoPartialModel() throws IOException {
    Path schema = tempDirectory.resolve("broken.xsd");
    Files.writeString(schema, "<xs:schema");
    ResolvedSchemaManifest manifest =
        new ResolvedSchemaManifest(
            List.of(new ResolvedSchema("broken.xsd", schema, "urn:test", List.of())));

    XsdSyntaxResult result = new XsdSyntaxParser().parse(manifest);

    assertEquals(
        List.of(DiagnosticCode.SCHEMA_FRONTEND_XML_ERROR),
        result.diagnostics().stream().map(SchemaDiagnostic::code).toList());
    assertEquals("", result.model().toText());
  }
}
