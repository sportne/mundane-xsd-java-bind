package io.github.mundanej.mxjb.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class SchemaIrNormalizationPolicyTest {
  @Test
  void normalizesDefaultAndExplicitCardinality() {
    assertEquals(
        SchemaCardinality.ONE,
        SchemaIrNormalizationPolicy.cardinality(Map.of(), "main.xsd").cardinality());
    assertEquals(
        new SchemaCardinality(0, "unbounded"),
        SchemaIrNormalizationPolicy.cardinality(
                Map.of("minOccurs", "0", "maxOccurs", "unbounded"), "main.xsd")
            .cardinality());
  }

  @Test
  void rejectsInvalidCardinalityDeterministically() {
    SchemaIrNormalizationPolicy.CardinalityResult result =
        SchemaIrNormalizationPolicy.cardinality(
            Map.of("minOccurs", "2", "maxOccurs", "1"), "main.xsd");

    assertNull(result.cardinality());
    assertEquals(
        List.of(
            "SCHEMA_IR_INVALID_CARDINALITY | main.xsd | maxOccurs must be greater than or equal to minOccurs."),
        result.diagnostics().stream().map(SchemaDiagnostic::toManifestLine).toList());
  }

  @Test
  void resolvesUnqualifiedAndPrefixedQNames() {
    assertEquals(
        new SchemaQName("", "Local"),
        SchemaIrNormalizationPolicy.resolveQName(Map.of(), "main.xsd", " Local ").name());
    assertEquals(
        new SchemaQName("urn:orders", "Order"),
        SchemaIrNormalizationPolicy.resolveQName(
                Map.of("xmlns:tns", "urn:orders"), "main.xsd", "tns:Order")
            .name());
  }

  @Test
  void reportsQNameDiagnosticsWithStableCodes() {
    SchemaIrNormalizationPolicy.QNameResult empty =
        SchemaIrNormalizationPolicy.resolveQName(Map.of(), "main.xsd", " ");
    SchemaIrNormalizationPolicy.QNameResult missingPrefix =
        SchemaIrNormalizationPolicy.resolveQName(Map.of(), "main.xsd", "missing:Thing");

    assertEquals(
        DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE, empty.diagnostics().getFirst().code());
    assertEquals(
        DiagnosticCode.SCHEMA_IR_NAMESPACE_CONFLICT, missingPrefix.diagnostics().getFirst().code());
  }

  @Test
  void composesCardinalityWithUnboundedMaximum() {
    assertEquals(
        new SchemaCardinality(0, "unbounded"),
        SchemaIrNormalizationPolicy.composeCardinality(
            new SchemaCardinality(0, "2"), new SchemaCardinality(1, "unbounded")));
  }

  @Test
  void sortsDiagnosticsByResourceCodeAndMessage() {
    List<SchemaDiagnostic> sorted =
        SchemaIrNormalizationPolicy.sortedDiagnostics(
            List.of(
                SchemaIrNormalizationPolicy.diagnostic(
                    DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE, "b.xsd", "B"),
                SchemaIrNormalizationPolicy.diagnostic(
                    DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT, "a.xsd", "Z"),
                SchemaIrNormalizationPolicy.diagnostic(
                    DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT, "a.xsd", "A")));

    assertTrue(sorted.get(0).message().endsWith("A"));
    assertTrue(sorted.get(1).message().endsWith("Z"));
    assertEquals("b.xsd", sorted.get(2).resource());
  }
}
