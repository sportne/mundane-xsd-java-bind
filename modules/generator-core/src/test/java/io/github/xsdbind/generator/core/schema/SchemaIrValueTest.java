package io.github.xsdbind.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.xsdbind.generator.core.diagnostics.DiagnosticCode;
import io.github.xsdbind.generator.core.diagnostics.SchemaDiagnostic;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class SchemaIrValueTest {
  @Test
  void qNamesRenderBuiltInEmptyAndQualifiedNames() {
    assertEquals("xs:string", new SchemaQName(SchemaQName.XSD_NAMESPACE, "string").toText());
    assertEquals("local", new SchemaQName("", "local").toText());
    assertEquals("{urn:test}local", new SchemaQName("urn:test", "local").toText());
    assertTrue(new SchemaQName("a", "b").compareTo(new SchemaQName("a", "c")) < 0);
  }

  @Test
  void componentKeysRenderAndSortDeterministically() {
    SchemaComponentKey attribute =
        new SchemaComponentKey(SchemaComponentKind.ATTRIBUTE, new SchemaQName("urn:test", "id"));
    SchemaComponentKey element =
        new SchemaComponentKey(SchemaComponentKind.ELEMENT, new SchemaQName("urn:test", "id"));

    assertEquals("attribute {urn:test}id", attribute.toText());
    assertTrue(attribute.compareTo(element) > 0);
  }

  @Test
  void emptyGraphsAndResultsHaveStableText() {
    SchemaIrResult result = SchemaIrResult.empty(List.of());

    assertEquals("", result.graph().toText());
    assertEquals("", result.model().toText());
    assertEquals(false, result.hasErrors());
  }

  @Test
  void resultReportsDiagnostics() {
    SchemaIrResult result =
        SchemaIrResult.empty(
            List.of(
                new SchemaDiagnostic(
                    DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT, "main.xsd", "invalid component")));

    assertTrue(result.hasErrors());
  }

  @Test
  void valueTextCoversEmptyChildrenAndDefaultBranches() {
    SchemaIrSequence sequence = new SchemaIrSequence(SchemaCardinality.ONE, List.of());
    SchemaIrAttribute attribute =
        new SchemaIrAttribute(
            new SchemaQName("urn:test", "id"),
            SchemaIrTypeReference.named(new SchemaQName(SchemaQName.XSD_NAMESPACE, "string")),
            "",
            false);
    XsdSyntaxNode syntaxNode = new XsdSyntaxNode(XsdSyntaxKind.ELEMENT, Map.of(), List.of());
    SchemaComponent component =
        new SchemaComponent(
            new SchemaComponentKey(SchemaComponentKind.ELEMENT, new SchemaQName("urn:test", "id")),
            "main.xsd",
            syntaxNode);
    SchemaCardinality defaulted = new SchemaCardinality(1, "");

    assertEquals("  sequence cardinality=1..1", sequence.toText("  "));
    assertEquals("  attribute {urn:test}id type=xs:string use=optional", attribute.toText("  "));
    assertEquals("element {urn:test}id @ main.xsd", component.toText());
    assertEquals("1..1", defaulted.toText());
  }
}
