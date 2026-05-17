package io.github.mundanej.mxjb.generator.core.schema;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
      state.documentsByResourceId.put(document.resourceId(), document);
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
    List<SchemaIrModelGroup> modelGroups = new ArrayList<>();
    List<SchemaIrAttributeGroup> attributeGroups = new ArrayList<>();

    for (XsdSyntaxDocument document : model.documents()) {
      for (XsdSyntaxNode child : document.children()) {
        if (child.kind() == XsdSyntaxKind.GROUP) {
          addIfPresent(modelGroups, normalizeModelGroup(document, child, state));
        } else if (child.kind() == XsdSyntaxKind.ATTRIBUTE_GROUP) {
          addIfPresent(attributeGroups, normalizeAttributeGroup(document, child, state));
        } else if (child.kind() == XsdSyntaxKind.SIMPLE_TYPE) {
          SchemaIrSimpleType simpleType = normalizeNamedSimpleType(document, child, state);
          addIfPresent(simpleTypes, simpleType);
        }
      }
    }

    for (XsdSyntaxDocument document : model.documents()) {
      for (XsdSyntaxNode child : document.children()) {
        switch (child.kind()) {
          case ELEMENT -> addIfPresent(elements, normalizeElement(document, child, state));
          case COMPLEX_TYPE ->
              addIfPresent(complexTypes, normalizeNamedComplexType(document, child, state));
          case SIMPLE_TYPE -> {
            // Normalized in a first pass so list/union aliases can resolve named restricted
            // scalar aliases deterministically.
          }
          case ATTRIBUTE -> addIfPresent(attributes, normalizeAttribute(document, child, state));
          case GROUP, ATTRIBUTE_GROUP -> {
            // Normalized in a first pass so complex type references can be flattened
            // deterministically.
          }
          case SEQUENCE ->
              diagnostic(
                  state,
                  DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
                  document.resourceId(),
                  "Global xs:sequence is not valid in profile XP-DATA-10.");
          case CHOICE ->
              diagnostic(
                  state,
                  DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
                  document.resourceId(),
                  "Global xs:choice is not valid in profile XP-DATA-10-CHOICE.");
          case RESTRICTION,
              COMPLEX_CONTENT,
              EXTENSION,
              SIMPLE_CONTENT,
              ENUMERATION,
              LENGTH,
              MIN_LENGTH,
              MAX_LENGTH,
              MIN_INCLUSIVE,
              MAX_INCLUSIVE,
              PATTERN,
              LIST,
              UNION ->
              diagnostic(
                  state,
                  DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
                  document.resourceId(),
                  "Global xs:" + child.kind().manifestName() + " is not valid.");
        }
      }
    }
    return new SchemaIrModel(
        elements, complexTypes, simpleTypes, attributes, modelGroups, attributeGroups);
  }

  private SchemaIrModelGroup normalizeModelGroup(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    String ref = node.attributes().get("ref");
    if (ref != null) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Global xs:group declarations must use name, not ref.");
      return null;
    }
    String localName = node.attributes().get("name");
    if (localName == null || localName.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_MISSING_NAME,
          document.resourceId(),
          "group declaration is missing a name.");
      return null;
    }
    if (node.children().size() != 1
        || node.children().getFirst().kind() != XsdSyntaxKind.SEQUENCE) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:group declarations support exactly one xs:sequence child in profile XP-XSD10-COMPOSED.");
      return null;
    }
    SchemaIrSequence sequence =
        normalizeSequence(document, node.children().getFirst(), state, false);
    if (sequence == null || !requireSingletonGroupCardinality(document, node, state)) {
      return null;
    }
    if (!requireSingletonGroupCardinality(document, node.children().getFirst(), state)) {
      return null;
    }
    SchemaIrModelGroup group =
        new SchemaIrModelGroup(new SchemaQName(document.targetNamespace(), localName), sequence);
    state.modelGroups.put(group.name(), group);
    return group;
  }

  private SchemaIrAttributeGroup normalizeAttributeGroup(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    String ref = node.attributes().get("ref");
    if (ref != null) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Global xs:attributeGroup declarations must use name, not ref.");
      return null;
    }
    String localName = node.attributes().get("name");
    if (localName == null || localName.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_MISSING_NAME,
          document.resourceId(),
          "attributeGroup declaration is missing a name.");
      return null;
    }
    List<SchemaIrAttribute> attributes = new ArrayList<>();
    for (XsdSyntaxNode child : node.children()) {
      if (child.kind() == XsdSyntaxKind.ATTRIBUTE) {
        addIfPresent(attributes, normalizeAttribute(document, child, state));
      } else {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Only xs:attribute children are supported inside xs:attributeGroup in profile XP-XSD10-COMPOSED.");
      }
    }
    SchemaIrAttributeGroup group =
        new SchemaIrAttributeGroup(
            new SchemaQName(document.targetNamespace(), localName), attributes);
    state.attributeGroups.put(group.name(), group);
    return group;
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
    if ("true".equals(node.attributes().get("abstract"))) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "abstract complexType is not supported in profile XP-XSD10-COMPOSED.");
      return null;
    }
    if ("true".equals(node.attributes().get("mixed"))) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "mixed complexType is not supported in profile XP-XSD10-COMPOSED.");
      return null;
    }

    List<XsdSyntaxNode> complexContentChildren =
        node.children().stream()
            .filter(child -> child.kind() == XsdSyntaxKind.COMPLEX_CONTENT)
            .toList();
    if (!complexContentChildren.isEmpty()) {
      if (anonymous) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:complexContent/xs:extension is supported only for named complexType declarations.");
        return null;
      }
      if (node.children().size() != 1 || complexContentChildren.size() != 1) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:complexContent must be the only child of complexType in profile XP-XSD10-COMPOSED.");
        return null;
      }
      return normalizeComplexContent(document, complexContentChildren.getFirst(), name, state);
    }

    ComplexTypeContent content = normalizeComplexTypeContent(document, node.children(), state);
    return new SchemaIrComplexType(name, content.attributes(), content.sequences(), anonymous);
  }

  private ComplexTypeContent normalizeComplexTypeContent(
      XsdSyntaxDocument document, List<XsdSyntaxNode> children, BuildState state) {
    List<SchemaIrAttribute> attributes = new ArrayList<>();
    List<SchemaIrSequence> sequences = new ArrayList<>();
    int contentParticleCount = 0;
    boolean directChoiceSeen = false;
    boolean directGroupSeen = false;
    boolean sequenceGroupSeen = false;
    boolean attributeGroupSeen = false;
    for (XsdSyntaxNode child : children) {
      if (child.kind() == XsdSyntaxKind.ATTRIBUTE) {
        addIfPresent(attributes, normalizeAttribute(document, child, state));
      } else if (child.kind() == XsdSyntaxKind.ATTRIBUTE_GROUP) {
        attributeGroupSeen = true;
        addAttributeGroupReference(document, child, attributes, state);
      } else if (child.kind() == XsdSyntaxKind.SEQUENCE) {
        contentParticleCount++;
        sequenceGroupSeen = sequenceGroupSeen || containsGroupReference(child);
        addIfPresent(sequences, normalizeSequence(document, child, state));
      } else if (child.kind() == XsdSyntaxKind.CHOICE) {
        contentParticleCount++;
        directChoiceSeen = true;
        SchemaIrChoice choice = normalizeChoice(document, child, state);
        if (choice != null) {
          sequences.add(
              new SchemaIrSequence(SchemaCardinality.ONE, List.<SchemaIrParticle>of(choice)));
        }
      } else if (child.kind() == XsdSyntaxKind.GROUP) {
        contentParticleCount++;
        directGroupSeen = true;
        addIfPresent(sequences, normalizeGroupReferenceAsSequence(document, child, state));
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
    if (directChoiceSeen && contentParticleCount > 1) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Direct xs:choice is supported only as the sole complexType content particle or inside xs:sequence.");
    }
    if (directGroupSeen && contentParticleCount > 1) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Direct xs:group ref is supported only as the sole complexType content particle or inside xs:sequence.");
    }
    if (directGroupSeen || sequenceGroupSeen) {
      validateDuplicateElementNames(document, sequences, state);
    }
    if (attributeGroupSeen) {
      validateDuplicateAttributeNames(document, attributes, state);
    }
    return new ComplexTypeContent(attributes, sequences);
  }

  private SchemaIrComplexType normalizeComplexContent(
      XsdSyntaxDocument document, XsdSyntaxNode node, SchemaQName typeName, BuildState state) {
    if ("true".equals(node.attributes().get("mixed"))) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "mixed xs:complexContent is not supported in profile XP-XSD10-COMPOSED.");
      return null;
    }
    if (node.children().size() != 1) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:complexContent supports exactly one xs:extension child in profile XP-XSD10-COMPOSED.");
      return null;
    }
    XsdSyntaxNode child = node.children().getFirst();
    if (child.kind() == XsdSyntaxKind.RESTRICTION) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:complexContent/xs:restriction is not supported in profile XP-XSD10-COMPOSED.");
      return null;
    }
    if (child.kind() != XsdSyntaxKind.EXTENSION) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:complexContent supports only xs:extension in profile XP-XSD10-COMPOSED.");
      return null;
    }
    return normalizeComplexExtension(document, child, typeName, state);
  }

  private SchemaIrComplexType normalizeComplexExtension(
      XsdSyntaxDocument document, XsdSyntaxNode node, SchemaQName typeName, BuildState state) {
    String baseText = node.attributes().get("base");
    if (baseText == null || baseText.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:extension is missing a base.");
      return null;
    }
    SchemaQName baseName = resolveQName(document, baseText, state);
    if (baseName == null) {
      return null;
    }
    SchemaComponentKey baseKey = new SchemaComponentKey(SchemaComponentKind.COMPLEX_TYPE, baseName);
    requireComponent(state, document.resourceId(), baseKey);
    if (!state.components.containsKey(baseKey)) {
      return null;
    }
    SchemaIrComplexType baseType = normalizeReferencedComplexType(document, baseName, state);
    ComplexTypeContent extensionContent =
        normalizeComplexTypeContent(document, node.children(), state);
    if (baseType == null) {
      return null;
    }
    List<SchemaIrAttribute> attributes = new ArrayList<>(baseType.attributes());
    attributes.addAll(extensionContent.attributes());
    List<SchemaIrSequence> sequences = new ArrayList<>(baseType.sequences());
    sequences.addAll(extensionContent.sequences());
    validateDuplicateElementNames(document, sequences, state);
    validateDuplicateAttributeNames(document, attributes, state);
    return new SchemaIrComplexType(typeName, attributes, sequences, false);
  }

  private SchemaIrComplexType normalizeNamedComplexType(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    String localName = node.attributes().get("name");
    if (localName == null || localName.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_MISSING_NAME,
          document.resourceId(),
          "complexType declaration is missing a name.");
      return null;
    }
    SchemaQName name = new SchemaQName(document.targetNamespace(), localName);
    SchemaIrComplexType cached = state.complexTypes.get(name);
    if (cached != null) {
      return cached;
    }
    if (state.complexTypeStack.contains(name)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Recursive complexType extension involving " + name.toText() + ".");
      return null;
    }
    state.complexTypeStack.add(name);
    SchemaIrComplexType normalized = normalizeComplexType(document, node, false, state);
    state.complexTypeStack.remove(name);
    if (normalized != null) {
      state.complexTypes.put(name, normalized);
    }
    return normalized;
  }

  private SchemaIrComplexType normalizeReferencedComplexType(
      XsdSyntaxDocument currentDocument, SchemaQName name, BuildState state) {
    SchemaIrComplexType cached = state.complexTypes.get(name);
    if (cached != null) {
      return cached;
    }
    SchemaComponent component =
        state.components.get(new SchemaComponentKey(SchemaComponentKind.COMPLEX_TYPE, name));
    if (component == null) {
      return null;
    }
    XsdSyntaxDocument document = state.documentsByResourceId.get(component.resourceId());
    if (document == null) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE,
          currentDocument.resourceId(),
          "Cannot locate schema document for complexType " + name.toText() + ".");
      return null;
    }
    return normalizeNamedComplexType(document, component.syntaxNode(), state);
  }

  private boolean containsGroupReference(XsdSyntaxNode node) {
    return node.children().stream().anyMatch(child -> child.kind() == XsdSyntaxKind.GROUP);
  }

  private SchemaIrSequence normalizeSequence(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    return normalizeSequence(document, node, state, true);
  }

  private SchemaIrSequence normalizeSequence(
      XsdSyntaxDocument document,
      XsdSyntaxNode node,
      BuildState state,
      boolean allowGroupReferences) {
    SchemaCardinality cardinality = cardinality(document, node, state);
    if (cardinality == null) {
      return null;
    }
    List<SchemaIrParticle> particles = new ArrayList<>();
    for (XsdSyntaxNode child : node.children()) {
      if (child.kind() == XsdSyntaxKind.ELEMENT) {
        addIfPresent(particles, normalizeElement(document, child, state));
      } else if (child.kind() == XsdSyntaxKind.CHOICE) {
        addIfPresent(particles, normalizeChoice(document, child, state));
      } else if (child.kind() == XsdSyntaxKind.GROUP && allowGroupReferences) {
        SchemaIrSequence groupSequence = normalizeGroupReferenceAsSequence(document, child, state);
        if (groupSequence != null) {
          particles.addAll(groupSequence.particles());
        }
      } else {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Only xs:element, accepted xs:choice, and accepted xs:group refs are supported "
                + "inside xs:sequence for normalized IR.");
      }
    }
    return new SchemaIrSequence(cardinality, particles);
  }

  private SchemaIrSequence normalizeGroupReferenceAsSequence(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    if (node.attributes().containsKey("name")) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Local xs:group use must reference a global group with ref.");
      return null;
    }
    if (!requireSingletonGroupCardinality(document, node, state)) {
      return null;
    }
    String ref = node.attributes().get("ref");
    if (ref == null || ref.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE,
          document.resourceId(),
          "xs:group ref is required.");
      return null;
    }
    SchemaQName refName = resolveQName(document, ref, state);
    if (refName == null) {
      return null;
    }
    requireComponent(
        state,
        document.resourceId(),
        new SchemaComponentKey(SchemaComponentKind.MODEL_GROUP, refName));
    SchemaIrModelGroup group = state.modelGroups.get(refName);
    if (group == null) {
      return null;
    }
    return new SchemaIrSequence(SchemaCardinality.ONE, group.sequence().particles());
  }

  private void addAttributeGroupReference(
      XsdSyntaxDocument document,
      XsdSyntaxNode node,
      List<SchemaIrAttribute> attributes,
      BuildState state) {
    if (node.attributes().containsKey("name")) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Local xs:attributeGroup use must reference a global attributeGroup with ref.");
      return;
    }
    String ref = node.attributes().get("ref");
    if (ref == null || ref.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE,
          document.resourceId(),
          "xs:attributeGroup ref is required.");
      return;
    }
    SchemaQName refName = resolveQName(document, ref, state);
    if (refName == null) {
      return;
    }
    requireComponent(
        state,
        document.resourceId(),
        new SchemaComponentKey(SchemaComponentKind.ATTRIBUTE_GROUP, refName));
    SchemaIrAttributeGroup group = state.attributeGroups.get(refName);
    if (group != null) {
      attributes.addAll(group.attributes());
    }
  }

  private boolean requireSingletonGroupCardinality(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    SchemaCardinality cardinality = cardinality(document, node, state);
    if (cardinality == null) {
      return false;
    }
    if (cardinality.minOccurs() != 1 || !"1".equals(cardinality.maxOccurs())) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:group supports only minOccurs=1 and maxOccurs=1 in profile XP-XSD10-COMPOSED.");
      return false;
    }
    return true;
  }

  private void validateDuplicateElementNames(
      XsdSyntaxDocument document, List<SchemaIrSequence> sequences, BuildState state) {
    Set<SchemaQName> names = new HashSet<>();
    for (SchemaIrSequence sequence : sequences) {
      for (SchemaIrParticle particle : sequence.particles()) {
        if (particle instanceof SchemaIrElement element) {
          addUniqueElementName(document, names, element.name(), state);
        } else if (particle instanceof SchemaIrChoice choice) {
          for (SchemaIrElement branch : choice.branches()) {
            addUniqueElementName(document, names, branch.name(), state);
          }
        }
      }
    }
  }

  private void addUniqueElementName(
      XsdSyntaxDocument document, Set<SchemaQName> names, SchemaQName name, BuildState state) {
    if (!names.add(name)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Duplicate flattened XML element " + name.toText() + " in complexType.");
    }
  }

  private void validateDuplicateAttributeNames(
      XsdSyntaxDocument document, List<SchemaIrAttribute> attributes, BuildState state) {
    Set<SchemaQName> names = new HashSet<>();
    for (SchemaIrAttribute attribute : attributes) {
      if (!names.add(attribute.name())) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Duplicate flattened XML attribute " + attribute.name().toText() + " in complexType.");
      }
    }
  }

  private SchemaIrChoice normalizeChoice(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    SchemaCardinality cardinality = cardinality(document, node, state);
    if (cardinality == null) {
      return null;
    }
    if ((cardinality.minOccurs() != 0 && cardinality.minOccurs() != 1)
        || !"1".equals(cardinality.maxOccurs())) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:choice supports only minOccurs 0 or 1 and maxOccurs 1 in profile XP-DATA-10-CHOICE.");
      return null;
    }
    List<SchemaIrElement> branches = new ArrayList<>();
    for (XsdSyntaxNode child : node.children()) {
      if (child.kind() != XsdSyntaxKind.ELEMENT) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Only singleton xs:element branches are supported inside xs:choice.");
        continue;
      }
      String branchMin = child.attributes().getOrDefault("minOccurs", "1");
      String branchMax = child.attributes().getOrDefault("maxOccurs", "1");
      if (!"1".equals(branchMin) || !"1".equals(branchMax)) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:choice branches must use singleton cardinality in profile XP-DATA-10-CHOICE.");
        continue;
      }
      boolean hasInlineComplexType =
          child.children().stream()
              .anyMatch(grandchild -> grandchild.kind() == XsdSyntaxKind.COMPLEX_TYPE);
      if (hasInlineComplexType) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:choice branches do not support anonymous complexType declarations.");
        continue;
      }
      addIfPresent(branches, normalizeElement(document, child, state));
    }
    if (branches.isEmpty()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:choice must contain at least one supported branch.");
      return null;
    }
    return new SchemaIrChoice(cardinality, branches);
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
    SchemaIrSimpleRestriction restriction = null;
    SchemaIrSimpleList list = null;
    SchemaIrSimpleUnion union = null;
    for (XsdSyntaxNode child : node.children()) {
      if (restriction != null || list != null || union != null) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "simpleType " + localName + " has multiple simple type definition children.");
        return null;
      }
      if (child.kind() == XsdSyntaxKind.RESTRICTION) {
        restriction = normalizeSimpleRestriction(document, child, state);
      } else if (child.kind() == XsdSyntaxKind.LIST) {
        list = normalizeSimpleList(document, child, state);
      } else if (child.kind() == XsdSyntaxKind.UNION) {
        union = normalizeSimpleUnion(document, child, state);
      } else {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Unsupported child "
                + child.kind().manifestName()
                + " inside simpleType for normalized IR.");
      }
    }
    return new SchemaIrSimpleType(
        new SchemaQName(document.targetNamespace(), localName), restriction, list, union);
  }

  private SchemaIrSimpleList normalizeSimpleList(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    if (!node.children().isEmpty()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:list supports only itemType references in profile XP-XSD10-COMPOSED.");
      return null;
    }
    String itemTypeText = node.attributes().get("itemType");
    if (itemTypeText == null || itemTypeText.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:list is missing itemType.");
      return null;
    }
    SchemaQName itemType = resolveQName(document, itemTypeText, state);
    if (itemType == null) {
      return null;
    }
    if (!isAcceptedSimpleCompositionMember(itemType, state)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Unsupported xs:list itemType " + itemType.toText() + ".");
      return null;
    }
    return new SchemaIrSimpleList(itemType);
  }

  private SchemaIrSimpleUnion normalizeSimpleUnion(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    if (!node.children().isEmpty()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:union supports only memberTypes references in profile XP-XSD10-COMPOSED.");
      return null;
    }
    String memberTypesText = node.attributes().get("memberTypes");
    if (memberTypesText == null || memberTypesText.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:union is missing memberTypes.");
      return null;
    }
    List<SchemaQName> memberTypes = new ArrayList<>();
    for (String memberTypeText :
        Pattern.compile("\\s+").splitAsStream(memberTypesText.trim()).toList()) {
      SchemaQName memberType = resolveQName(document, memberTypeText, state);
      if (memberType == null) {
        continue;
      }
      if (!isAcceptedSimpleCompositionMember(memberType, state)) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Unsupported xs:union memberType " + memberType.toText() + ".");
        continue;
      }
      memberTypes.add(memberType);
    }
    if (memberTypes.isEmpty()) {
      return null;
    }
    return new SchemaIrSimpleUnion(memberTypes);
  }

  private boolean isAcceptedSimpleCompositionMember(SchemaQName type, BuildState state) {
    if (isSupportedRestrictionBase(type)) {
      return true;
    }
    SchemaIrSimpleType simpleType = normalizeReferencedSimpleType(type, state);
    if (simpleType != null) {
      return simpleType.restriction() != null;
    }
    return false;
  }

  private SchemaIrSimpleRestriction normalizeSimpleRestriction(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    String baseText = node.attributes().get("base");
    if (baseText == null || baseText.isBlank()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "simpleType restriction is missing a base.");
      return null;
    }
    SchemaQName base = resolveQName(document, baseText, state);
    if (base == null) {
      return null;
    }
    SchemaIrSimpleRestriction baseRestriction = null;
    SchemaQName effectiveBase = base;
    if (!isSupportedRestrictionBase(base)) {
      SchemaIrSimpleType baseType = normalizeReferencedSimpleType(base, state);
      if (baseType != null && baseType.restriction() != null) {
        baseRestriction = baseType.restriction();
        effectiveBase = baseRestriction.base();
      } else {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Unsupported simpleType restriction base " + base.toText() + ".");
        return null;
      }
    }
    if (!isSupportedRestrictionBase(effectiveBase)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Unsupported simpleType restriction base " + effectiveBase.toText() + ".");
      return null;
    }
    SimpleRestrictionState restrictionState = new SimpleRestrictionState(effectiveBase);
    for (XsdSyntaxNode child : node.children()) {
      normalizeFacet(document, child, restrictionState, state);
    }
    SchemaIrSimpleRestriction localRestriction =
        validateRestriction(document, restrictionState, state);
    if (localRestriction == null) {
      return null;
    }
    if (baseRestriction == null) {
      return localRestriction;
    }
    return mergeRestrictionChain(document, baseRestriction, localRestriction, state);
  }

  private SchemaIrSimpleRestriction validateRestriction(
      XsdSyntaxDocument document, SimpleRestrictionState restrictionState, BuildState state) {
    if (restrictionState.length != null
        && (restrictionState.minLength != null || restrictionState.maxLength != null)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:length cannot be combined with xs:minLength or xs:maxLength.");
      return null;
    }
    if (restrictionState.minLength != null
        && restrictionState.maxLength != null
        && restrictionState.minLength > restrictionState.maxLength) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:minLength must be less than or equal to xs:maxLength.");
      return null;
    }
    if (restrictionState.minInclusive != null
        && restrictionState.maxInclusive != null
        && new BigDecimal(restrictionState.minInclusive)
                .compareTo(new BigDecimal(restrictionState.maxInclusive))
            > 0) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:minInclusive must be less than or equal to xs:maxInclusive.");
      return null;
    }
    return restrictionState.toRestriction();
  }

  private SchemaIrSimpleRestriction mergeRestrictionChain(
      XsdSyntaxDocument document,
      SchemaIrSimpleRestriction base,
      SchemaIrSimpleRestriction derived,
      BuildState state) {
    if (!base.base().equals(derived.base())) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Simple restriction derivation chain has incompatible scalar bases.");
      return null;
    }
    int diagnosticsBeforeMerge = state.diagnostics.size();
    List<String> enumerations = mergeEnumerations(document, base, derived, state);
    Integer length = mergeLength(document, base, derived, state);
    Integer minLength = mergeMinLength(base, derived);
    Integer maxLength = mergeMaxLength(base, derived);
    String minInclusive = mergeMinInclusive(base, derived);
    String maxInclusive = mergeMaxInclusive(base, derived);
    List<String> patterns = new ArrayList<>(base.patterns());
    patterns.addAll(derived.patterns());
    if (state.diagnostics.size() == diagnosticsBeforeMerge
        && length != null
        && ((minLength != null && length < minLength)
            || (maxLength != null && length > maxLength))) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Simple restriction derivation chain has incompatible length facets.");
      return null;
    }
    if (length != null) {
      minLength = null;
      maxLength = null;
    }
    if (minLength != null && maxLength != null && minLength > maxLength) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Simple restriction derivation chain has incompatible length facets.");
      return null;
    }
    if (minInclusive != null
        && maxInclusive != null
        && new BigDecimal(minInclusive).compareTo(new BigDecimal(maxInclusive)) > 0) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Simple restriction derivation chain has incompatible numeric range facets.");
      return null;
    }
    if (state.diagnostics.size() > diagnosticsBeforeMerge) {
      return null;
    }
    return new SchemaIrSimpleRestriction(
        base.base(),
        enumerations,
        length,
        minLength,
        maxLength,
        minInclusive,
        maxInclusive,
        patterns);
  }

  private List<String> mergeEnumerations(
      XsdSyntaxDocument document,
      SchemaIrSimpleRestriction base,
      SchemaIrSimpleRestriction derived,
      BuildState state) {
    if (base.enumerations().isEmpty()) {
      return derived.enumerations();
    }
    if (derived.enumerations().isEmpty()) {
      return base.enumerations();
    }
    List<String> merged = new ArrayList<>();
    for (String value : derived.enumerations()) {
      if (base.enumerations().contains(value)) {
        merged.add(value);
      } else {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Simple restriction enumeration " + value + " is not allowed by the base type.");
      }
    }
    return merged;
  }

  private Integer mergeLength(
      XsdSyntaxDocument document,
      SchemaIrSimpleRestriction base,
      SchemaIrSimpleRestriction derived,
      BuildState state) {
    if (base.length() != null
        && derived.length() != null
        && !base.length().equals(derived.length())) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Simple restriction derivation chain has incompatible length facets.");
      return null;
    }
    if (derived.length() != null) {
      return derived.length();
    }
    return base.length();
  }

  private Integer mergeMinLength(
      SchemaIrSimpleRestriction base, SchemaIrSimpleRestriction derived) {
    if (base.minLength() == null) {
      return derived.minLength();
    }
    if (derived.minLength() == null) {
      return base.minLength();
    }
    return Math.max(base.minLength(), derived.minLength());
  }

  private Integer mergeMaxLength(
      SchemaIrSimpleRestriction base, SchemaIrSimpleRestriction derived) {
    if (base.maxLength() == null) {
      return derived.maxLength();
    }
    if (derived.maxLength() == null) {
      return base.maxLength();
    }
    return Math.min(base.maxLength(), derived.maxLength());
  }

  private String mergeMinInclusive(
      SchemaIrSimpleRestriction base, SchemaIrSimpleRestriction derived) {
    if (base.minInclusive() == null) {
      return derived.minInclusive();
    }
    if (derived.minInclusive() == null) {
      return base.minInclusive();
    }
    return new BigDecimal(base.minInclusive()).compareTo(new BigDecimal(derived.minInclusive()))
            >= 0
        ? base.minInclusive()
        : derived.minInclusive();
  }

  private String mergeMaxInclusive(
      SchemaIrSimpleRestriction base, SchemaIrSimpleRestriction derived) {
    if (base.maxInclusive() == null) {
      return derived.maxInclusive();
    }
    if (derived.maxInclusive() == null) {
      return base.maxInclusive();
    }
    return new BigDecimal(base.maxInclusive()).compareTo(new BigDecimal(derived.maxInclusive()))
            <= 0
        ? base.maxInclusive()
        : derived.maxInclusive();
  }

  private SchemaIrSimpleType normalizeNamedSimpleType(
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
    SchemaQName name = new SchemaQName(document.targetNamespace(), localName);
    SchemaIrSimpleType cached = state.simpleTypes.get(name);
    if (cached != null) {
      return cached;
    }
    if (state.simpleTypeStack.contains(name)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Recursive simpleType restriction derivation involving " + name.toText() + ".");
      return null;
    }
    state.simpleTypeStack.add(name);
    SchemaIrSimpleType normalized = normalizeSimpleType(document, node, state);
    state.simpleTypeStack.remove(name);
    if (normalized != null) {
      state.simpleTypes.put(name, normalized);
    }
    return normalized;
  }

  private SchemaIrSimpleType normalizeReferencedSimpleType(SchemaQName name, BuildState state) {
    SchemaIrSimpleType cached = state.simpleTypes.get(name);
    if (cached != null) {
      return cached;
    }
    SchemaComponent component =
        state.components.get(new SchemaComponentKey(SchemaComponentKind.SIMPLE_TYPE, name));
    if (component == null) {
      return null;
    }
    XsdSyntaxDocument document = state.documentsByResourceId.get(component.resourceId());
    if (document == null) {
      return null;
    }
    return normalizeNamedSimpleType(document, component.syntaxNode(), state);
  }

  private void normalizeFacet(
      XsdSyntaxDocument document,
      XsdSyntaxNode node,
      SimpleRestrictionState restriction,
      BuildState state) {
    switch (node.kind()) {
      case ENUMERATION -> addEnumeration(document, node, restriction, state);
      case LENGTH ->
          restriction.length =
              singleLengthFacet(document, node, restriction.length, restriction.base, state);
      case MIN_LENGTH ->
          restriction.minLength =
              singleLengthFacet(document, node, restriction.minLength, restriction.base, state);
      case MAX_LENGTH ->
          restriction.maxLength =
              singleLengthFacet(document, node, restriction.maxLength, restriction.base, state);
      case MIN_INCLUSIVE ->
          restriction.minInclusive =
              singleNumericFacet(document, node, restriction.minInclusive, restriction.base, state);
      case MAX_INCLUSIVE ->
          restriction.maxInclusive =
              singleNumericFacet(document, node, restriction.maxInclusive, restriction.base, state);
      case PATTERN -> addPattern(document, node, restriction, state);
      default ->
          diagnostic(
              state,
              DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
              document.resourceId(),
              "Unsupported simpleType restriction facet xs:" + node.kind().manifestName() + ".");
    }
  }

  private void addEnumeration(
      XsdSyntaxDocument document,
      XsdSyntaxNode node,
      SimpleRestrictionState restriction,
      BuildState state) {
    String value = facetValue(document, node, state);
    if (value == null) {
      return;
    }
    if (!isValidScalarLexical(restriction.base.localName(), value)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Invalid xs:enumeration value " + value + " for base " + restriction.base.toText() + ".");
      return;
    }
    restriction.enumerations.add(value);
  }

  private Integer singleLengthFacet(
      XsdSyntaxDocument document,
      XsdSyntaxNode node,
      Integer previous,
      SchemaQName base,
      BuildState state) {
    String value = facetValue(document, node, state);
    if (value == null) {
      return previous;
    }
    if (!"string".equals(base.localName())) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:" + node.kind().manifestName() + " is supported only for xs:string.");
      return previous;
    }
    if (previous != null) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Duplicate xs:" + node.kind().manifestName() + " facet.");
      return previous;
    }
    try {
      int parsed = Integer.parseInt(value);
      if (parsed < 0) {
        throw new NumberFormatException("negative");
      }
      return parsed;
    } catch (NumberFormatException exception) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Invalid xs:" + node.kind().manifestName() + " value " + value + ".");
      return previous;
    }
  }

  private String singleNumericFacet(
      XsdSyntaxDocument document,
      XsdSyntaxNode node,
      String previous,
      SchemaQName base,
      BuildState state) {
    String value = facetValue(document, node, state);
    if (value == null) {
      return previous;
    }
    if (previous != null) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Duplicate xs:" + node.kind().manifestName() + " facet.");
      return previous;
    }
    if (!isNumericBase(base)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:" + node.kind().manifestName() + " is supported only for numeric bases.");
      return previous;
    }
    if (!isValidScalarLexical(base.localName(), value)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Invalid xs:" + node.kind().manifestName() + " value " + value + ".");
      return previous;
    }
    return value;
  }

  private void addPattern(
      XsdSyntaxDocument document,
      XsdSyntaxNode node,
      SimpleRestrictionState restriction,
      BuildState state) {
    String value = facetValue(document, node, state);
    if (value == null) {
      return;
    }
    if (!"string".equals(restriction.base.localName())) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:pattern is supported only for xs:string.");
      return;
    }
    try {
      Pattern.compile(value);
    } catch (PatternSyntaxException exception) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Invalid xs:pattern value " + value + ".");
      return;
    }
    restriction.patterns.add(value);
  }

  private String facetValue(XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    String value = node.attributes().get("value");
    if (value == null) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:" + node.kind().manifestName() + " facet is missing a value.");
      return null;
    }
    return value;
  }

  private boolean isSupportedRestrictionBase(SchemaQName base) {
    return base.isXmlSchemaBuiltIn()
        && Set.of("string", "boolean", "int", "integer", "long", "decimal")
            .contains(base.localName());
  }

  private boolean isNumericBase(SchemaQName base) {
    return switch (base.localName()) {
      case "int", "integer", "long", "decimal" -> true;
      default -> false;
    };
  }

  private boolean isValidScalarLexical(String base, String value) {
    try {
      switch (base) {
        case "string" -> {
          return true;
        }
        case "boolean" -> {
          return "true".equals(value)
              || "false".equals(value)
              || "0".equals(value)
              || "1".equals(value);
        }
        case "int" -> Integer.parseInt(value);
        case "integer" -> new BigInteger(value);
        case "long" -> Long.parseLong(value);
        case "decimal" -> new BigDecimal(value);
        default -> {
          return false;
        }
      }
      return true;
    } catch (NumberFormatException exception) {
      return false;
    }
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
      case GROUP -> SchemaComponentKind.MODEL_GROUP;
      case ATTRIBUTE_GROUP -> SchemaComponentKind.ATTRIBUTE_GROUP;
      case RESTRICTION,
          COMPLEX_CONTENT,
          EXTENSION,
          SIMPLE_CONTENT,
          ENUMERATION,
          LENGTH,
          MIN_LENGTH,
          MAX_LENGTH,
          MIN_INCLUSIVE,
          MAX_INCLUSIVE,
          PATTERN,
          LIST,
          UNION,
          SEQUENCE,
          CHOICE ->
          null;
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
    private final Map<String, XsdSyntaxDocument> documentsByResourceId = new LinkedHashMap<>();
    private final Map<SchemaComponentKey, SchemaComponent> components = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrModelGroup> modelGroups = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrAttributeGroup> attributeGroups = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrSimpleType> simpleTypes = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrComplexType> complexTypes = new LinkedHashMap<>();
    private final Set<SchemaQName> simpleTypeStack = new LinkedHashSet<>();
    private final Set<SchemaQName> complexTypeStack = new LinkedHashSet<>();
    private final List<SchemaDiagnostic> diagnostics = new ArrayList<>();
  }

  private record ComplexTypeContent(
      List<SchemaIrAttribute> attributes, List<SchemaIrSequence> sequences) {
    private ComplexTypeContent {
      attributes = List.copyOf(attributes);
      sequences = List.copyOf(sequences);
    }
  }

  private static final class SimpleRestrictionState {
    private final SchemaQName base;
    private final List<String> enumerations = new ArrayList<>();
    private final List<String> patterns = new ArrayList<>();
    private Integer length;
    private Integer minLength;
    private Integer maxLength;
    private String minInclusive;
    private String maxInclusive;

    private SimpleRestrictionState(SchemaQName base) {
      this.base = base;
    }

    private SchemaIrSimpleRestriction toRestriction() {
      return new SchemaIrSimpleRestriction(
          base, enumerations, length, minLength, maxLength, minInclusive, maxInclusive, patterns);
    }
  }
}
