package io.github.mundanej.mxjb.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SchemaIrIdentityNormalizationTest {
  @Test
  void normalizesAcceptedSelectorAndFieldPathSubset() {
    List<String> diagnostics = new ArrayList<>();

    List<SchemaIrIdentityPath> selectorPaths =
        SchemaIrIdentityNormalization.paths(
            ".//tns:item|tns:line/*", false, this::resolveName, diagnostics::add);
    List<SchemaIrIdentityPath> fieldPaths =
        SchemaIrIdentityNormalization.paths(
            "@code|tns:id", true, this::resolveName, diagnostics::add);

    assertEquals(List.of(".//{urn:orders}item", "{urn:orders}line/*"), texts(selectorPaths));
    assertEquals(List.of("@{urn:orders}code", "{urn:orders}id"), texts(fieldPaths));
    assertEquals(List.of(), diagnostics);
  }

  @Test
  void preservesDotSelectorAndMissingXpathDiagnostics() {
    List<String> diagnostics = new ArrayList<>();

    List<SchemaIrIdentityPath> selectorPaths =
        SchemaIrIdentityNormalization.paths(".", false, this::resolveName, diagnostics::add);
    List<SchemaIrIdentityPath> missing =
        SchemaIrIdentityNormalization.paths(" ", true, this::resolveName, diagnostics::add);

    assertEquals(List.of("."), texts(selectorPaths));
    assertNull(missing);
    assertEquals(List.of("identity constraint XPath is missing."), diagnostics);
  }

  @Test
  void rejectsUnsupportedXpathAndNonTerminalAttributeSteps() {
    List<String> diagnostics = new ArrayList<>();

    assertNull(
        SchemaIrIdentityNormalization.paths(
            "tns:line[@code]", false, this::resolveName, diagnostics::add));
    assertNull(
        SchemaIrIdentityNormalization.paths(
            "@code/tns:value", true, this::resolveName, diagnostics::add));

    assertEquals(
        List.of(
            "Unsupported identity constraint XPath tns:line[@code].",
            "Attribute steps are supported only as terminal identity fields."),
        diagnostics);
  }

  private List<String> texts(List<SchemaIrIdentityPath> paths) {
    return paths.stream().map(SchemaIrIdentityPath::toText).toList();
  }

  private SchemaQName resolveName(String lexicalName) {
    String localName =
        lexicalName.contains(":")
            ? lexicalName.substring(lexicalName.indexOf(':') + 1)
            : lexicalName;
    return new SchemaQName("urn:orders", localName);
  }
}
