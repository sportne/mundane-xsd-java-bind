package io.github.mundanej.mxjb.generator.core.schema;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Package-private normalization policies shared by the IR builder. */
final class SchemaIrNormalizationPolicy {
  private SchemaIrNormalizationPolicy() {}

  static CardinalityResult cardinality(Map<String, String> attributes, String resourceId) {
    String minText = attributes.getOrDefault("minOccurs", "1");
    String maxText = attributes.getOrDefault("maxOccurs", "1");
    int minOccurs;
    try {
      minOccurs = Integer.parseInt(minText);
    } catch (NumberFormatException exception) {
      return CardinalityResult.invalid(
          diagnostic(
              DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY,
              resourceId,
              "Invalid minOccurs value " + minText + "."));
    }
    if (minOccurs < 0) {
      return CardinalityResult.invalid(
          diagnostic(
              DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY,
              resourceId,
              "minOccurs must be non-negative."));
    }
    if (!"unbounded".equals(maxText)) {
      try {
        int maxOccurs = Integer.parseInt(maxText);
        if (maxOccurs < minOccurs) {
          return CardinalityResult.invalid(
              diagnostic(
                  DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY,
                  resourceId,
                  "maxOccurs must be greater than or equal to minOccurs."));
        }
      } catch (NumberFormatException exception) {
        return CardinalityResult.invalid(
            diagnostic(
                DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY,
                resourceId,
                "Invalid maxOccurs value " + maxText + "."));
      }
    }
    return CardinalityResult.valid(new SchemaCardinality(minOccurs, maxText));
  }

  static QNameResult resolveQName(
      Map<String, String> namespaceDeclarations, String resourceId, String lexicalQName) {
    String trimmed = lexicalQName == null ? "" : lexicalQName.trim();
    if (trimmed.isEmpty()) {
      return QNameResult.invalid(
          diagnostic(
              DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE,
              resourceId,
              "QName reference is empty."));
    }
    int colon = trimmed.indexOf(':');
    if (colon < 0) {
      return QNameResult.valid(new SchemaQName("", trimmed));
    }
    String prefix = trimmed.substring(0, colon);
    String localName = trimmed.substring(colon + 1);
    String namespace = namespaceDeclarations.get("xmlns:" + prefix);
    if (namespace == null || localName.isBlank()) {
      return QNameResult.invalid(
          diagnostic(
              DiagnosticCode.SCHEMA_IR_NAMESPACE_CONFLICT,
              resourceId,
              "Cannot resolve namespace prefix in QName " + trimmed + "."));
    }
    return QNameResult.valid(new SchemaQName(namespace, localName));
  }

  static SchemaCardinality composeCardinality(SchemaCardinality outer, SchemaCardinality inner) {
    return new SchemaCardinality(
        outer.minOccurs() * inner.minOccurs(), multiplyMax(outer.maxOccurs(), inner.maxOccurs()));
  }

  static SchemaDiagnostic diagnostic(DiagnosticCode code, String resourceId, String message) {
    return new SchemaDiagnostic(code, resourceId, message);
  }

  static List<SchemaDiagnostic> sortedDiagnostics(List<SchemaDiagnostic> diagnostics) {
    return diagnostics.stream()
        .sorted(
            Comparator.comparing((SchemaDiagnostic diagnostic) -> diagnostic.resource())
                .thenComparing(diagnostic -> diagnostic.code().name())
                .thenComparing(SchemaDiagnostic::message))
        .toList();
  }

  private static String multiplyMax(String left, String right) {
    if ("unbounded".equals(left) || "unbounded".equals(right)) {
      return "unbounded";
    }
    return Integer.toString(Integer.parseInt(left) * Integer.parseInt(right));
  }

  record CardinalityResult(SchemaCardinality cardinality, List<SchemaDiagnostic> diagnostics) {
    CardinalityResult {
      diagnostics = List.copyOf(diagnostics);
    }

    static CardinalityResult valid(SchemaCardinality cardinality) {
      return new CardinalityResult(cardinality, List.of());
    }

    static CardinalityResult invalid(SchemaDiagnostic diagnostic) {
      return new CardinalityResult(null, List.of(diagnostic));
    }
  }

  record QNameResult(SchemaQName name, List<SchemaDiagnostic> diagnostics) {
    QNameResult {
      diagnostics = List.copyOf(diagnostics);
    }

    static QNameResult valid(SchemaQName name) {
      return new QNameResult(name, List.of());
    }

    static QNameResult invalid(SchemaDiagnostic diagnostic) {
      return new QNameResult(null, List.of(diagnostic));
    }
  }
}
