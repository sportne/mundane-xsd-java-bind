package io.github.xsdbind.generator.core.schema;

import io.github.xsdbind.generator.core.diagnostics.DiagnosticCode;
import io.github.xsdbind.generator.core.diagnostics.SchemaDiagnostic;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the supported component graph and normalized IR from frontend syntax. */
public final class SchemaIrBuilder {
  public SchemaIrResult build(XsdSyntaxResult syntaxResult) {
    if (!syntaxResult.diagnostics().isEmpty()) {
      return SchemaIrResult.empty(syntaxResult.diagnostics());
    }

    BuildState state = new BuildState();
    indexGlobalComponents(syntaxResult.model(), state);
    if (!state.diagnostics.isEmpty()) {
      return SchemaIrResult.empty(sortedDiagnostics(state.diagnostics));
    }

    SchemaIrModel model = normalizeModel(syntaxResult.model(), state);
    if (!state.diagnostics.isEmpty()) {
      return SchemaIrResult.empty(sortedDiagnostics(state.diagnostics));
    }

    return new SchemaIrResult(
        new SchemaComponentGraph(state.components), model, sortedDiagnostics(state.diagnostics));
  }

  private void indexGlobalComponents(XsdSyntaxModel model, BuildState state) {
    for (XsdSyntaxDocument document : model.documents()) {
      for (XsdSyntaxNode child : document.children()) {
        SchemaComponentKind kind = componentKind(child.kind());
        if (kind == null) {
          continue;
        }
        String name = child.attributes().get("name");
        if (name == null || name.isBlank()) {
          diagnostic(
              state,
              DiagnosticCode.SCHEMA_IR_MISSING_NAME,
              document.resourceId(),
              kind.manifestName() + " declaration is missing a name.");
          continue;
        }
        SchemaComponentKey key =
            new SchemaComponentKey(kind, new SchemaQName(document.targetNamespace(), name));
        SchemaComponent previous =
            state.components.putIfAbsent(
                key, new SchemaComponent(key, document.resourceId(), child));
        if (previous != null) {
          diagnostic(
              state,
              DiagnosticCode.SCHEMA_IR_DUPLICATE_COMPONENT,
              document.resourceId(),
              "Duplicate " + key.toText() + " also declared in " + previous.resourceId() + ".");
        }
      }
    }
  }

  private SchemaIrModel normalizeModel(XsdSyntaxModel model, BuildState state) {
    List<SchemaIrElement> elements = new ArrayList<>();
    List<SchemaIrComplexType> complexTypes = new ArrayList<>();
    List<SchemaIrSimpleType> simpleTypes = new ArrayList<>();
    List<SchemaIrAttribute> attributes = new ArrayList<>();
    for (XsdSyntaxDocument document : model.documents()) {
      for (XsdSyntaxNode child : document.children()) {
        switch (child.kind()) {
          case ELEMENT -> addIfPresent(elements, normalizeElement(document, child, state));
          case COMPLEX_TYPE ->
              addIfPresent(complexTypes, normalizeComplexType(document, child, false, state));
          case SIMPLE_TYPE ->
              addIfPresent(simpleTypes, normalizeSimpleType(document, child, state));
          case ATTRIBUTE -> addIfPresent(attributes, normalizeAttribute(document, child, state));
          case SEQUENCE ->
              diagnostic(
                  state,
                  DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
                  document.resourceId(),
                  "Global xs:sequence is not valid in profile XP-DATA-10.");
        }
      }
    }
    return new SchemaIrModel(elements, complexTypes, simpleTypes, attributes);
  }

  private SchemaIrElement normalizeElement(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    SchemaCardinality cardinality = cardinality(document, node, state);
    if (cardinality == null) {
      return null;
    }

    String ref = node.attributes().get("ref");
    if (ref != null) {
      SchemaQName refName = resolveQName(document, ref, state);
      if (refName == null) {
        return null;
      }
      requireComponent(
          state,
          document.resourceId(),
          new SchemaComponentKey(SchemaComponentKind.ELEMENT, refName));
      return new SchemaIrElement(
          refName, SchemaIrTypeReference.named(refName), cardinality, null, true);
    }

    String name = node.attributes().get("name");
    if (name == null || name.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_MISSING_NAME,
          document.resourceId(),
          "element declaration is missing a name.");
      return null;
    }

    SchemaIrComplexType inlineComplexType = inlineComplexType(document, node, state);
    SchemaIrTypeReference type = typeReference(document, node, inlineComplexType, state);
    if (type == null) {
      return null;
    }
    return new SchemaIrElement(
        new SchemaQName(document.targetNamespace(), name),
        type,
        cardinality,
        inlineComplexType,
        false);
  }

  private SchemaIrTypeReference typeReference(
      XsdSyntaxDocument document,
      XsdSyntaxNode node,
      SchemaIrComplexType inlineComplexType,
      BuildState state) {
    String type = node.attributes().get("type");
    if (type != null && inlineComplexType != null) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "element "
              + node.attributes().get("name")
              + " cannot have both type and inline complexType.");
      return null;
    }
    if (inlineComplexType != null) {
      return SchemaIrTypeReference.anonymousType();
    }
    if (type == null || type.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "element " + node.attributes().get("name") + " is missing a type or inline complexType.");
      return null;
    }
    return resolveTypeReference(document, type, state);
  }

  private SchemaIrComplexType inlineComplexType(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    SchemaIrComplexType inline = null;
    for (XsdSyntaxNode child : node.children()) {
      if (child.kind() == XsdSyntaxKind.COMPLEX_TYPE) {
        if (inline != null) {
          diagnostic(
              state,
              DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
              document.resourceId(),
              "element "
                  + node.attributes().get("name")
                  + " has multiple inline complexType children.");
          return null;
        }
        inline = normalizeComplexType(document, child, true, state);
      }
    }
    return inline;
  }

  private SchemaIrComplexType normalizeComplexType(
      XsdSyntaxDocument document, XsdSyntaxNode node, boolean anonymous, BuildState state) {
    SchemaQName name = null;
    if (!anonymous) {
      String localName = node.attributes().get("name");
      if (localName == null || localName.isBlank()) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_MISSING_NAME,
            document.resourceId(),
            "complexType declaration is missing a name.");
        return null;
      }
      name = new SchemaQName(document.targetNamespace(), localName);
    }

    List<SchemaIrAttribute> attributes = new ArrayList<>();
    List<SchemaIrSequence> sequences = new ArrayList<>();
    for (XsdSyntaxNode child : node.children()) {
      if (child.kind() == XsdSyntaxKind.ATTRIBUTE) {
        addIfPresent(attributes, normalizeAttribute(document, child, state));
      } else if (child.kind() == XsdSyntaxKind.SEQUENCE) {
        addIfPresent(sequences, normalizeSequence(document, child, state));
      } else {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Unsupported child "
                + child.kind().manifestName()
                + " inside complexType for normalized IR.");
      }
    }
    return new SchemaIrComplexType(name, attributes, sequences, anonymous);
  }

  private SchemaIrSequence normalizeSequence(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    SchemaCardinality cardinality = cardinality(document, node, state);
    if (cardinality == null) {
      return null;
    }
    List<SchemaIrElement> elements = new ArrayList<>();
    for (XsdSyntaxNode child : node.children()) {
      if (child.kind() == XsdSyntaxKind.ELEMENT) {
        addIfPresent(elements, normalizeElement(document, child, state));
      } else {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Only xs:element is supported inside xs:sequence for normalized IR.");
      }
    }
    return new SchemaIrSequence(cardinality, elements);
  }

  private SchemaIrSimpleType normalizeSimpleType(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    String localName = node.attributes().get("name");
    if (localName == null || localName.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_MISSING_NAME,
          document.resourceId(),
          "simpleType declaration is missing a name.");
      return null;
    }
    return new SchemaIrSimpleType(new SchemaQName(document.targetNamespace(), localName));
  }

  private SchemaIrAttribute normalizeAttribute(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    String ref = node.attributes().get("ref");
    if (ref != null) {
      SchemaQName refName = resolveQName(document, ref, state);
      if (refName == null) {
        return null;
      }
      requireComponent(
          state,
          document.resourceId(),
          new SchemaComponentKey(SchemaComponentKind.ATTRIBUTE, refName));
      return new SchemaIrAttribute(
          refName, SchemaIrTypeReference.named(refName), node.attributes().get("use"), true);
    }

    String localName = node.attributes().get("name");
    if (localName == null || localName.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_MISSING_NAME,
          document.resourceId(),
          "attribute declaration is missing a name.");
      return null;
    }
    String type = node.attributes().get("type");
    if (type == null || type.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "attribute " + localName + " is missing a type.");
      return null;
    }
    SchemaIrTypeReference typeReference = resolveTypeReference(document, type, state);
    if (typeReference == null) {
      return null;
    }
    return new SchemaIrAttribute(
        new SchemaQName(document.targetNamespace(), localName),
        typeReference,
        node.attributes().get("use"),
        false);
  }

  private SchemaIrTypeReference resolveTypeReference(
      XsdSyntaxDocument document, String lexicalQName, BuildState state) {
    SchemaQName name = resolveQName(document, lexicalQName, state);
    if (name == null) {
      return null;
    }
    if (name.isXmlSchemaBuiltIn()) {
      return SchemaIrTypeReference.named(name);
    }
    boolean known =
        state.components.containsKey(new SchemaComponentKey(SchemaComponentKind.COMPLEX_TYPE, name))
            || state.components.containsKey(
                new SchemaComponentKey(SchemaComponentKind.SIMPLE_TYPE, name));
    if (!known) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE,
          document.resourceId(),
          "Unresolved type reference " + name.toText() + ".");
      return null;
    }
    return SchemaIrTypeReference.named(name);
  }

  private SchemaQName resolveQName(
      XsdSyntaxDocument document, String lexicalQName, BuildState state) {
    String trimmed = lexicalQName == null ? "" : lexicalQName.trim();
    if (trimmed.isEmpty()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE,
          document.resourceId(),
          "QName reference is empty.");
      return null;
    }
    int colon = trimmed.indexOf(':');
    if (colon < 0) {
      return new SchemaQName("", trimmed);
    }
    String prefix = trimmed.substring(0, colon);
    String localName = trimmed.substring(colon + 1);
    String namespace = document.namespaceDeclarations().get("xmlns:" + prefix);
    if (namespace == null || localName.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_NAMESPACE_CONFLICT,
          document.resourceId(),
          "Cannot resolve namespace prefix in QName " + trimmed + ".");
      return null;
    }
    return new SchemaQName(namespace, localName);
  }

  private void requireComponent(
      BuildState state, String resourceId, SchemaComponentKey componentKey) {
    if (!state.components.containsKey(componentKey)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE,
          resourceId,
          "Unresolved " + componentKey.toText() + ".");
    }
  }

  private SchemaCardinality cardinality(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    String minText = node.attributes().getOrDefault("minOccurs", "1");
    String maxText = node.attributes().getOrDefault("maxOccurs", "1");
    int minOccurs;
    try {
      minOccurs = Integer.parseInt(minText);
    } catch (NumberFormatException exception) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY,
          document.resourceId(),
          "Invalid minOccurs value " + minText + ".");
      return null;
    }
    if (minOccurs < 0) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY,
          document.resourceId(),
          "minOccurs must be non-negative.");
      return null;
    }
    if (!"unbounded".equals(maxText)) {
      try {
        int maxOccurs = Integer.parseInt(maxText);
        if (maxOccurs < minOccurs) {
          diagnostic(
              state,
              DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY,
              document.resourceId(),
              "maxOccurs must be greater than or equal to minOccurs.");
          return null;
        }
      } catch (NumberFormatException exception) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY,
            document.resourceId(),
            "Invalid maxOccurs value " + maxText + ".");
        return null;
      }
    }
    return new SchemaCardinality(minOccurs, maxText);
  }

  private SchemaComponentKind componentKind(XsdSyntaxKind syntaxKind) {
    return switch (syntaxKind) {
      case ELEMENT -> SchemaComponentKind.ELEMENT;
      case COMPLEX_TYPE -> SchemaComponentKind.COMPLEX_TYPE;
      case SIMPLE_TYPE -> SchemaComponentKind.SIMPLE_TYPE;
      case ATTRIBUTE -> SchemaComponentKind.ATTRIBUTE;
      case SEQUENCE -> null;
    };
  }

  private <T> void addIfPresent(List<T> values, T value) {
    if (value != null) {
      values.add(value);
    }
  }

  private void diagnostic(
      BuildState state, DiagnosticCode code, String resourceId, String message) {
    state.diagnostics.add(new SchemaDiagnostic(code, resourceId, message));
  }

  private List<SchemaDiagnostic> sortedDiagnostics(List<SchemaDiagnostic> diagnostics) {
    return diagnostics.stream()
        .sorted(
            Comparator.comparing((SchemaDiagnostic diagnostic) -> diagnostic.resource())
                .thenComparing(diagnostic -> diagnostic.code().name())
                .thenComparing(SchemaDiagnostic::message))
        .toList();
  }

  private static final class BuildState {
    private final Map<SchemaComponentKey, SchemaComponent> components = new LinkedHashMap<>();
    private final List<SchemaDiagnostic> diagnostics = new ArrayList<>();
  }
}
