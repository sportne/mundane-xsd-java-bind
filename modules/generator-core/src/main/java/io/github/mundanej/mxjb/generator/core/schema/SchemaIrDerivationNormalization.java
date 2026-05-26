package io.github.mundanej.mxjb.generator.core.schema;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Package-private derivation checks for already-normalized complex type content. */
final class SchemaIrDerivationNormalization {
  private SchemaIrDerivationNormalization() {}

  static String finalControlDiagnostic(SchemaIrComplexType baseType, String derivationKind) {
    if (!baseType.finalControls().contains(derivationKind)) {
      return null;
    }
    return "Derivation by "
        + derivationKind
        + " is final for base type "
        + baseType.name().toText()
        + ".";
  }

  static List<String> restrictionDiagnostics(
      SchemaIrComplexType baseType,
      List<SchemaIrAttribute> restrictedAttributes,
      SchemaIrAnyAttribute restrictedAnyAttribute,
      List<SchemaIrSequence> restrictedSequences) {
    List<String> diagnostics = new ArrayList<>();
    Set<SchemaQName> baseElements = new LinkedHashSet<>();
    for (SchemaIrSequence sequence : baseType.sequences()) {
      for (SchemaIrElement element : sequence.elements()) {
        baseElements.add(element.name());
      }
    }
    for (SchemaIrSequence sequence : restrictedSequences) {
      for (SchemaIrElement element : sequence.elements()) {
        if (!baseElements.contains(element.name())) {
          diagnostics.add(
              "Restricted element " + element.name().toText() + " is not present in base type.");
        }
      }
    }
    Set<SchemaQName> baseAttributes = new LinkedHashSet<>();
    for (SchemaIrAttribute attribute : baseType.attributes()) {
      baseAttributes.add(attribute.name());
    }
    for (SchemaIrAttribute attribute : restrictedAttributes) {
      if (!baseAttributes.contains(attribute.name()) && !"prohibited".equals(attribute.use())) {
        diagnostics.add(
            "Restricted attribute " + attribute.name().toText() + " is not present in base type.");
      }
    }
    addAnyAttributeRestrictionDiagnostics(baseType, restrictedAnyAttribute, diagnostics);
    return List.copyOf(diagnostics);
  }

  private static void addAnyAttributeRestrictionDiagnostics(
      SchemaIrComplexType baseType,
      SchemaIrAnyAttribute restrictedAnyAttribute,
      List<String> diagnostics) {
    if (restrictedAnyAttribute == null) {
      return;
    }
    if (baseType.anyAttribute() == null) {
      diagnostics.add("Restricted anyAttribute is not present in base type.");
      return;
    }
    if (!SchemaIrWildcardNormalization.namespaceSubset(
        restrictedAnyAttribute.namespaceConstraint(),
        baseType.anyAttribute().namespaceConstraint())) {
      diagnostics.add("Restricted anyAttribute namespace is not a subset of the base wildcard.");
      return;
    }
    if (!SchemaIrWildcardNormalization.processContentsAllowsRestriction(
        baseType.anyAttribute().processContents(), restrictedAnyAttribute.processContents())) {
      diagnostics.add("Restricted anyAttribute processContents cannot weaken the base wildcard.");
    }
  }
}
