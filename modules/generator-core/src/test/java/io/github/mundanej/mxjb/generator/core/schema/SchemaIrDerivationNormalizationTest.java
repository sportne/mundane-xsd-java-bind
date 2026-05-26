package io.github.mundanej.mxjb.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

final class SchemaIrDerivationNormalizationTest {
  @Test
  void reportsFinalControlDiagnosticOnlyWhenDerivationIsBlocked() {
    SchemaIrComplexType base =
        new SchemaIrComplexType(
            q("Base"),
            null,
            List.of(),
            null,
            List.of(),
            false,
            false,
            false,
            null,
            "",
            List.of(),
            List.of("extension"));

    assertEquals(
        "Derivation by extension is final for base type {urn:orders}Base.",
        SchemaIrDerivationNormalization.finalControlDiagnostic(base, "extension"));
    assertNull(SchemaIrDerivationNormalization.finalControlDiagnostic(base, "restriction"));
  }

  @Test
  void preservesRestrictionDiagnosticsForMissingMembersAndWeakenedAnyAttribute() {
    SchemaIrComplexType base =
        new SchemaIrComplexType(
            q("Base"),
            null,
            List.of(attribute("id")),
            new SchemaIrAnyAttribute(
                new SchemaIrWildcardNamespace("explicit", List.of("urn:allowed")), "strict"),
            List.of(new SchemaIrSequence(SchemaCardinality.ONE, List.of(element("known")))),
            false,
            false,
            false,
            null,
            "",
            List.of(),
            List.of());

    List<String> diagnostics =
        SchemaIrDerivationNormalization.restrictionDiagnostics(
            base,
            List.of(attribute("missing")),
            new SchemaIrAnyAttribute(
                new SchemaIrWildcardNamespace("explicit", List.of("urn:allowed")), "skip"),
            List.of(new SchemaIrSequence(SchemaCardinality.ONE, List.of(element("unknown")))));

    assertEquals(
        List.of(
            "Restricted element {urn:orders}unknown is not present in base type.",
            "Restricted attribute {urn:orders}missing is not present in base type.",
            "Restricted anyAttribute processContents cannot weaken the base wildcard."),
        diagnostics);
  }

  @Test
  void allowsProhibitedRestrictionAttributeAbsentFromBase() {
    SchemaIrComplexType base =
        new SchemaIrComplexType(
            q("Base"), null, List.of(), null, List.of(), false, false, false, null, "", List.of(),
            List.of());

    assertEquals(
        List.of(),
        SchemaIrDerivationNormalization.restrictionDiagnostics(
            base, List.of(attribute("removed", "prohibited")), null, List.of()));
  }

  private SchemaIrElement element(String localName) {
    return new SchemaIrElement(
        q(localName),
        SchemaIrTypeReference.named(new SchemaQName(SchemaQName.XSD_NAMESPACE, "string")),
        SchemaCardinality.ONE,
        null,
        false);
  }

  private SchemaIrAttribute attribute(String localName) {
    return attribute(localName, "optional");
  }

  private SchemaIrAttribute attribute(String localName, String use) {
    return new SchemaIrAttribute(
        q(localName),
        SchemaIrTypeReference.named(new SchemaQName(SchemaQName.XSD_NAMESPACE, "string")),
        use,
        false);
  }

  private SchemaQName q(String localName) {
    return new SchemaQName("urn:orders", localName);
  }
}
