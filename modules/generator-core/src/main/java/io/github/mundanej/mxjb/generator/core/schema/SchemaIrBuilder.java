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
            new SchemaComponentKey(kind, new SchemaQName(schemaNamespace(document), name));
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
    List<SchemaIrSubstitutionGroup> substitutionGroups;

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
          case ELEMENT -> addIfPresent(elements, normalizeElement(document, child, state, true));
          case COMPLEX_TYPE ->
              addIfPresent(complexTypes, normalizeNamedComplexType(document, child, state));
          case SIMPLE_TYPE -> {
            // Normalized in a first pass so list/union aliases can resolve named restricted
            // scalar aliases deterministically.
          }
          case ATTRIBUTE -> addIfPresent(attributes, normalizeAttribute(document, child, state));
          case ANNOTATION, APPINFO, DOCUMENTATION, INCLUDE, IMPORT, NOTATION -> {
            // Metadata and notation declarations are indexed/tolerated at the component layer.
          }
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
              MIN_EXCLUSIVE,
              MAX_EXCLUSIVE,
              TOTAL_DIGITS,
              FRACTION_DIGITS,
              WHITE_SPACE,
              PATTERN,
              LIST,
              UNION,
              REDEFINE,
              ALL,
              ANY ->
              diagnostic(
                  state,
                  DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
                  document.resourceId(),
                  "Global xs:" + child.kind().manifestName() + " is not valid.");
          case ANY_ATTRIBUTE, UNIQUE, KEY, KEYREF, SELECTOR, FIELD ->
              diagnostic(
                  state,
                  DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
                  document.resourceId(),
                  "xs:" + child.kind().manifestName() + " is not supported before binding.");
        }
      }
    }
    substitutionGroups = normalizeSubstitutionGroups(elements, state);
    return new SchemaIrModel(
        elements,
        complexTypes,
        simpleTypes,
        attributes,
        modelGroups,
        attributeGroups,
        substitutionGroups);
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
        || (node.children().getFirst().kind() != XsdSyntaxKind.SEQUENCE
            && node.children().getFirst().kind() != XsdSyntaxKind.ALL)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:group declarations support exactly one xs:sequence or legal xs:all child.");
      return null;
    }
    XsdSyntaxNode child = node.children().getFirst();
    SchemaIrSequence sequence =
        child.kind() == XsdSyntaxKind.ALL
            ? allAsSequence(document, child, state)
            : normalizeSequence(document, child, state, false);
    if (sequence == null || !requireSingletonGroupCardinality(document, node, state)) {
      return null;
    }
    if (!requireSingletonGroupCardinality(document, node.children().getFirst(), state)) {
      return null;
    }
    SchemaIrModelGroup group =
        new SchemaIrModelGroup(new SchemaQName(schemaNamespace(document), localName), sequence);
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
            new SchemaQName(schemaNamespace(document), localName), attributes);
    state.attributeGroups.put(group.name(), group);
    return group;
  }

  private List<SchemaIrSubstitutionGroup> normalizeSubstitutionGroups(
      List<SchemaIrElement> elements, BuildState state) {
    Map<SchemaQName, SchemaIrElement> elementsByName = new LinkedHashMap<>();
    for (SchemaIrElement element : elements) {
      elementsByName.put(element.name(), element);
    }
    Map<SchemaQName, List<SchemaIrElement>> membersByHead = new LinkedHashMap<>();
    for (SchemaIrElement element : elements) {
      SchemaQName head = element.substitutionGroup();
      if (head == null) {
        continue;
      }
      if (element.cardinality().minOccurs() != 1
          || !"1".equals(element.cardinality().maxOccurs())) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            "schema-ir",
            "Substitution group member " + element.name().toText() + " must be singleton.");
        continue;
      }
      if (element.name().equals(head)) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            "schema-ir",
            "Substitution group member " + element.name().toText() + " cannot substitute itself.");
        continue;
      }
      membersByHead.computeIfAbsent(head, ignored -> new ArrayList<>()).add(element);
    }
    List<SchemaIrSubstitutionGroup> groups = new ArrayList<>();
    for (Map.Entry<SchemaQName, List<SchemaIrElement>> entry : membersByHead.entrySet()) {
      SchemaQName headName = entry.getKey();
      SchemaIrElement head = elementsByName.get(headName);
      if (head == null) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE,
            "schema-ir",
            "Substitution group head " + headName.toText() + " is not declared.");
        continue;
      }
      if (head.substitutionGroup() != null) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            "schema-ir",
            "Nested substitution group head " + headName.toText() + " is not supported.");
        continue;
      }
      if (head.cardinality().minOccurs() != 1 || !"1".equals(head.cardinality().maxOccurs())) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            "schema-ir",
            "Substitution group head " + headName.toText() + " must be singleton.");
        continue;
      }
      Set<SchemaQName> branchNames = new LinkedHashSet<>();
      List<SchemaIrElement> branches = new ArrayList<>();
      branches.add(head);
      branchNames.add(head.name());
      for (SchemaIrElement member : entry.getValue()) {
        if (!branchNames.add(member.name())) {
          diagnostic(
              state,
              DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
              "schema-ir",
              "Duplicate substitution group branch " + member.name().toText() + ".");
          continue;
        }
        branches.add(member);
      }
      groups.add(new SchemaIrSubstitutionGroup(headName, branches));
    }
    return groups;
  }

  private SchemaIrElement normalizeElement(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state, boolean global) {
    SchemaCardinality cardinality = cardinality(document, node, state);
    if (cardinality == null) {
      return null;
    }
    if (node.attributes().containsKey("block") || node.attributes().containsKey("final")) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:element block/final substitution controls are not supported in profile XP-XSD10-SEMANTIC.");
      return null;
    }
    if ("true".equals(node.attributes().get("abstract"))) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "abstract xs:element declarations are not supported in profile XP-XSD10-SEMANTIC.");
      return null;
    }

    String ref = node.attributes().get("ref");
    if (ref != null) {
      if (semanticAttributes(node).hasAny()) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:element ref uses cannot override nillable/default/fixed semantics.");
        return null;
      }
      if (node.attributes().containsKey("substitutionGroup")
          || node.attributes().containsKey("abstract")) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:element ref uses cannot declare substitution group metadata.");
        return null;
      }
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
    if (node.attributes().containsKey("substitutionGroup") && !global) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:element substitutionGroup is supported only on global element declarations.");
      return null;
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
    SchemaQName substitutionGroup = null;
    String substitutionGroupText = node.attributes().get("substitutionGroup");
    if (substitutionGroupText != null) {
      substitutionGroup = resolveQName(document, substitutionGroupText, state);
      if (substitutionGroup == null) {
        return null;
      }
    }
    if (hasUnsupportedBindingChildren(document, node, state)) {
      return null;
    }

    SchemaIrComplexType inlineComplexType = inlineComplexType(document, node, state);
    SchemaIrTypeReference type = typeReference(document, node, inlineComplexType, state);
    if (type == null) {
      return null;
    }
    SchemaIrValueSemantics semantics = semanticAttributes(node);
    return new SchemaIrElement(
        new SchemaQName(schemaNamespace(document), name),
        type,
        cardinality,
        inlineComplexType,
        semantics,
        substitutionGroup,
        false);
  }

  private boolean hasUnsupportedBindingChildren(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    boolean unsupported = false;
    for (XsdSyntaxNode child : node.children()) {
      if (child.kind() == XsdSyntaxKind.UNIQUE
          || child.kind() == XsdSyntaxKind.KEY
          || child.kind() == XsdSyntaxKind.KEYREF) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:" + child.kind().manifestName() + " is not supported before binding.");
        unsupported = true;
      }
    }
    return unsupported;
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
      name = new SchemaQName(schemaNamespace(document), localName);
    }
    if ("true".equals(node.attributes().get("abstract"))) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "abstract complexType is not supported in profile XP-XSD10-COMPOSED.");
      return null;
    }
    if (node.attributes().containsKey("block") || node.attributes().containsKey("final")) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "complexType block/final derivation controls are not supported in profile XP-XSD10-FULL.");
      return null;
    }
    boolean mixed = "true".equals(node.attributes().get("mixed"));
    List<XsdSyntaxNode> complexContentChildren =
        node.children().stream()
            .filter(child -> child.kind() == XsdSyntaxKind.COMPLEX_CONTENT)
            .toList();
    if (mixed && !complexContentChildren.isEmpty()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "mixed complexType with xs:complexContent is not supported in profile XP-XSD10-DOCUMENT.");
      return null;
    }
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

    ComplexTypeContent content =
        normalizeComplexTypeContent(document, node.children(), state, mixed);
    return new SchemaIrComplexType(
        name, content.attributes(), content.sequences(), mixed, anonymous);
  }

  private ComplexTypeContent normalizeComplexTypeContent(
      XsdSyntaxDocument document, List<XsdSyntaxNode> children, BuildState state, boolean mixed) {
    List<SchemaIrAttribute> attributes = new ArrayList<>();
    List<SchemaIrSequence> sequences = new ArrayList<>();
    int contentParticleCount = 0;
    boolean directChoiceSeen = false;
    boolean directGroupSeen = false;
    boolean sequenceGroupSeen = false;
    boolean allSeen = false;
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
      } else if (child.kind() == XsdSyntaxKind.ALL) {
        contentParticleCount++;
        allSeen = true;
        addIfPresent(sequences, allAsSequence(document, child, state));
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
    if (allSeen && contentParticleCount > 1) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:all is supported only as the sole complexType content particle.");
    }
    if (mixed) {
      if (directChoiceSeen) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "mixed content supports xs:sequence content only; direct xs:choice is not supported.");
      }
      if (directGroupSeen) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "mixed content supports xs:sequence content only; direct xs:group ref is not supported.");
      }
      if (allSeen) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "mixed content does not support xs:all.");
      }
      if (contentParticleCount != sequences.size()) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "mixed content supports only direct xs:sequence content and attributes.");
      }
      for (SchemaIrSequence sequence : sequences) {
        if (!sequence.choices().isEmpty()) {
          diagnostic(
              state,
              DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
              document.resourceId(),
              "mixed xs:choice content is not supported in profile XP-XSD10-DOCUMENT.");
        }
      }
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
        normalizeComplexTypeContent(document, node.children(), state, false);
    if (baseType == null) {
      return null;
    }
    List<SchemaIrAttribute> attributes = new ArrayList<>(baseType.attributes());
    attributes.addAll(extensionContent.attributes());
    List<SchemaIrSequence> sequences = new ArrayList<>(baseType.sequences());
    sequences.addAll(extensionContent.sequences());
    validateDuplicateElementNames(document, sequences, state);
    validateDuplicateAttributeNames(document, attributes, state);
    return new SchemaIrComplexType(typeName, attributes, sequences, false, false);
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
    SchemaQName name = new SchemaQName(schemaNamespace(document), localName);
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
        addIfPresent(particles, normalizeElement(document, child, state, false));
      } else if (child.kind() == XsdSyntaxKind.CHOICE) {
        addIfPresent(particles, normalizeChoice(document, child, state));
      } else if (child.kind() == XsdSyntaxKind.SEQUENCE) {
        SchemaIrSequence nested = normalizeSequence(document, child, state, allowGroupReferences);
        if (nested != null) {
          addFlattenedNestedSequence(document, particles, nested, state);
        }
      } else if (child.kind() == XsdSyntaxKind.ANY) {
        addIfPresent(particles, normalizeWildcard(document, child, state));
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
            "Only xs:element, accepted xs:sequence, accepted xs:choice, accepted xs:any, and accepted xs:group refs "
                + "are supported inside xs:sequence for normalized IR.");
      }
    }
    validateWildcardElementAmbiguity(document, particles, state);
    return new SchemaIrSequence(cardinality, particles);
  }

  private SchemaIrSequence allAsSequence(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    SchemaIrAll all = normalizeAll(document, node, state);
    if (all == null) {
      return null;
    }
    return new SchemaIrSequence(SchemaCardinality.ONE, List.of(all));
  }

  private SchemaIrAll normalizeAll(
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
          "xs:all supports only minOccurs 0 or 1 and maxOccurs 1.");
      return null;
    }
    List<SchemaIrElement> elements = new ArrayList<>();
    for (XsdSyntaxNode child : node.children()) {
      if (child.kind() != XsdSyntaxKind.ELEMENT) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:all supports only xs:element children.");
        continue;
      }
      String branchMin = child.attributes().getOrDefault("minOccurs", "1");
      String branchMax = child.attributes().getOrDefault("maxOccurs", "1");
      if (!("0".equals(branchMin) || "1".equals(branchMin)) || !"1".equals(branchMax)) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:all element children support only minOccurs 0 or 1 and maxOccurs 1.");
        continue;
      }
      addIfPresent(elements, normalizeElement(document, child, state, false));
    }
    if (elements.isEmpty()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:all must contain at least one supported element.");
      return null;
    }
    if (cardinality.minOccurs() == 0
        && elements.stream().anyMatch(element -> element.cardinality().minOccurs() > 0)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:all minOccurs=0 with required children requires grouped content-model state.");
      return null;
    }
    validateDuplicateAllElementNames(document, elements, state);
    return new SchemaIrAll(cardinality, elements);
  }

  private void validateDuplicateAllElementNames(
      XsdSyntaxDocument document, List<SchemaIrElement> elements, BuildState state) {
    Set<SchemaQName> names = new HashSet<>();
    for (SchemaIrElement element : elements) {
      addUniqueElementName(document, names, element.name(), state);
    }
  }

  private void addFlattenedNestedSequence(
      XsdSyntaxDocument document,
      List<SchemaIrParticle> particles,
      SchemaIrSequence nested,
      BuildState state) {
    if (nested.cardinality().minOccurs() == 1 && "1".equals(nested.cardinality().maxOccurs())) {
      particles.addAll(nested.particles());
      return;
    }
    if (nested.particles().size() == 1) {
      particles.add(withCardinality(nested.particles().getFirst(), nested.cardinality()));
      return;
    }
    diagnostic(
        state,
        DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
        document.resourceId(),
        "Repeated or optional nested xs:sequence with multiple particles requires grouped content-list binding.");
  }

  private SchemaIrParticle withCardinality(
      SchemaIrParticle particle, SchemaCardinality cardinality) {
    if (particle instanceof SchemaIrElement element) {
      return new SchemaIrElement(
          element.name(),
          element.type(),
          composeCardinality(cardinality, element.cardinality()),
          element.inlineComplexType(),
          element.semantics(),
          element.substitutionGroup(),
          element.reference());
    }
    if (particle instanceof SchemaIrChoice choice) {
      return new SchemaIrChoice(
          composeCardinality(cardinality, choice.cardinality()), choice.branches());
    }
    if (particle instanceof SchemaIrWildcard wildcard) {
      return new SchemaIrWildcard(
          composeCardinality(cardinality, wildcard.cardinality()), wildcard.namespaceConstraint());
    }
    if (particle instanceof SchemaIrAll all) {
      return new SchemaIrAll(composeCardinality(cardinality, all.cardinality()), all.elements());
    }
    return particle;
  }

  private SchemaCardinality composeCardinality(SchemaCardinality outer, SchemaCardinality inner) {
    return new SchemaCardinality(
        outer.minOccurs() * inner.minOccurs(), multiplyMax(outer.maxOccurs(), inner.maxOccurs()));
  }

  private String multiplyMax(String left, String right) {
    if ("unbounded".equals(left) || "unbounded".equals(right)) {
      return "unbounded";
    }
    return Integer.toString(Integer.parseInt(left) * Integer.parseInt(right));
  }

  private void validateWildcardElementAmbiguity(
      XsdSyntaxDocument document, List<SchemaIrParticle> particles, BuildState state) {
    List<SchemaQName> elementNames = new ArrayList<>();
    for (SchemaIrParticle particle : particles) {
      if (particle instanceof SchemaIrElement element) {
        elementNames.add(element.name());
      } else if (particle instanceof SchemaIrAll all) {
        for (SchemaIrElement element : all.elements()) {
          elementNames.add(element.name());
        }
      } else if (particle instanceof SchemaIrChoice choice) {
        for (SchemaIrElement branch : choice.branches()) {
          elementNames.add(branch.name());
        }
      }
    }
    for (SchemaIrParticle particle : particles) {
      if (particle instanceof SchemaIrWildcard wildcard) {
        for (SchemaQName elementName : elementNames) {
          if (wildcardMatches(elementName, wildcard.namespaceConstraint())) {
            diagnostic(
                state,
                DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
                document.resourceId(),
                "xs:any namespace constraint overlaps XML element "
                    + elementName.toText()
                    + " in the same sequence.");
          }
        }
      }
    }
  }

  private boolean wildcardMatches(SchemaQName elementName, SchemaIrWildcardNamespace namespace) {
    return switch (namespace.kind()) {
      case "any" -> true;
      case "other" -> !namespace.namespaces().contains(elementName.namespace());
      default -> namespace.namespaces().contains(elementName.namespace());
    };
  }

  private SchemaIrWildcard normalizeWildcard(
      XsdSyntaxDocument document, XsdSyntaxNode node, BuildState state) {
    SchemaCardinality cardinality = cardinality(document, node, state);
    if (cardinality == null) {
      return null;
    }
    String processContents = node.attributes().get("processContents");
    if (!"skip".equals(processContents)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:any supports only explicit processContents=\"skip\" in profile XP-XSD10-DOCUMENT.");
      return null;
    }
    SchemaIrWildcardNamespace namespace =
        wildcardNamespace(document, node.attributes().getOrDefault("namespace", "##any"), state);
    if (namespace == null) {
      return null;
    }
    return new SchemaIrWildcard(cardinality, namespace);
  }

  private SchemaIrWildcardNamespace wildcardNamespace(
      XsdSyntaxDocument document, String value, BuildState state) {
    String normalized = value == null || value.isBlank() ? "##any" : value.trim();
    List<String> tokens = List.of(normalized.split("\\s+"));
    Set<String> unique = new LinkedHashSet<>(tokens);
    if (unique.size() != tokens.size()) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:any namespace constraint contains duplicate tokens.");
      return null;
    }
    if (tokens.contains("##any")) {
      if (tokens.size() == 1) {
        return new SchemaIrWildcardNamespace("any", List.of());
      }
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:any namespace ##any cannot be combined with other namespace tokens.");
      return null;
    }
    if (tokens.size() == 1) {
      String token = tokens.get(0);
      if ("##other".equals(token)) {
        return new SchemaIrWildcardNamespace("other", List.of(schemaNamespace(document)));
      }
      if ("##local".equals(token)) {
        return new SchemaIrWildcardNamespace("explicit", List.of(""));
      }
      if ("##targetNamespace".equals(token)) {
        return new SchemaIrWildcardNamespace("explicit", List.of(schemaNamespace(document)));
      }
    }
    List<String> explicitNamespaces = new ArrayList<>();
    for (String token : tokens) {
      if ("##other".equals(token) || "##local".equals(token) || "##targetNamespace".equals(token)) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:any namespace special tokens cannot be combined with explicit namespace tokens.");
        return null;
      }
      if (token.startsWith("##")) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "Unsupported xs:any namespace token " + token + ".");
        return null;
      }
      explicitNamespaces.add(token);
    }
    return new SchemaIrWildcardNamespace("explicit", explicitNamespaces);
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
    SchemaCardinality cardinality = cardinality(document, node, state);
    if (cardinality == null) {
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
    if (cardinality.minOccurs() == 1 && "1".equals(cardinality.maxOccurs())) {
      return new SchemaIrSequence(SchemaCardinality.ONE, group.sequence().particles());
    }
    if (group.sequence().particles().size() == 1) {
      return new SchemaIrSequence(
          SchemaCardinality.ONE,
          List.of(withCardinality(group.sequence().particles().getFirst(), cardinality)));
    }
    diagnostic(
        state,
        DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
        document.resourceId(),
        "Repeated or optional xs:group ref with multiple particles requires grouped content-list binding.");
    return null;
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
        } else if (particle instanceof SchemaIrAll all) {
          for (SchemaIrElement element : all.elements()) {
            addUniqueElementName(document, names, element.name(), state);
          }
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
      addIfPresent(branches, normalizeElement(document, child, state, false));
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
        new SchemaQName(schemaNamespace(document), localName), restriction, list, union);
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
    if (restrictionState.totalDigits != null
        && restrictionState.fractionDigits != null
        && restrictionState.fractionDigits > restrictionState.totalDigits) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "xs:fractionDigits must be less than or equal to xs:totalDigits.");
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
    String minExclusive =
        derived.minExclusive() == null ? base.minExclusive() : derived.minExclusive();
    String maxExclusive =
        derived.maxExclusive() == null ? base.maxExclusive() : derived.maxExclusive();
    Integer totalDigits = mergeMinimum(base.totalDigits(), derived.totalDigits());
    Integer fractionDigits = mergeMinimum(base.fractionDigits(), derived.fractionDigits());
    String whiteSpace = derived.whiteSpace() == null ? base.whiteSpace() : derived.whiteSpace();
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
        minExclusive,
        maxExclusive,
        totalDigits,
        fractionDigits,
        whiteSpace,
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

  private Integer mergeMinimum(Integer base, Integer derived) {
    if (base == null) {
      return derived;
    }
    if (derived == null) {
      return base;
    }
    return Math.min(base, derived);
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
    SchemaQName name = new SchemaQName(schemaNamespace(document), localName);
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
          restriction.length = singleLengthFacet(document, node, restriction.length, state);
      case MIN_LENGTH ->
          restriction.minLength = singleLengthFacet(document, node, restriction.minLength, state);
      case MAX_LENGTH ->
          restriction.maxLength = singleLengthFacet(document, node, restriction.maxLength, state);
      case MIN_INCLUSIVE ->
          restriction.minInclusive =
              singleNumericFacet(document, node, restriction.minInclusive, restriction.base, state);
      case MAX_INCLUSIVE ->
          restriction.maxInclusive =
              singleNumericFacet(document, node, restriction.maxInclusive, restriction.base, state);
      case MIN_EXCLUSIVE ->
          restriction.minExclusive =
              singleNumericFacet(document, node, restriction.minExclusive, restriction.base, state);
      case MAX_EXCLUSIVE ->
          restriction.maxExclusive =
              singleNumericFacet(document, node, restriction.maxExclusive, restriction.base, state);
      case TOTAL_DIGITS ->
          restriction.totalDigits =
              singleDigitsFacet(document, node, restriction.totalDigits, state);
      case FRACTION_DIGITS ->
          restriction.fractionDigits =
              singleDigitsFacet(document, node, restriction.fractionDigits, state);
      case WHITE_SPACE ->
          restriction.whiteSpace =
              singleWhiteSpaceFacet(document, node, restriction.whiteSpace, state);
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
      XsdSyntaxDocument document, XsdSyntaxNode node, Integer previous, BuildState state) {
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

  private Integer singleDigitsFacet(
      XsdSyntaxDocument document, XsdSyntaxNode node, Integer previous, BuildState state) {
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
    try {
      int parsed = Integer.parseInt(value);
      if (parsed <= 0) {
        throw new NumberFormatException("non-positive");
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

  private String singleWhiteSpaceFacet(
      XsdSyntaxDocument document, XsdSyntaxNode node, String previous, BuildState state) {
    String value = facetValue(document, node, state);
    if (value == null) {
      return previous;
    }
    if (previous != null) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Duplicate xs:whiteSpace facet.");
      return previous;
    }
    if (!Set.of("preserve", "replace", "collapse").contains(value)) {
      diagnostic(
          state,
          DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
          document.resourceId(),
          "Invalid xs:whiteSpace value " + value + ".");
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
        && Set.of(
                "string",
                "normalizedString",
                "token",
                "language",
                "Name",
                "NCName",
                "ID",
                "IDREF",
                "IDREFS",
                "ENTITY",
                "ENTITIES",
                "NMTOKEN",
                "NMTOKENS",
                "boolean",
                "decimal",
                "float",
                "double",
                "duration",
                "dateTime",
                "time",
                "date",
                "gYearMonth",
                "gYear",
                "gMonthDay",
                "gDay",
                "gMonth",
                "hexBinary",
                "base64Binary",
                "anyURI",
                "QName",
                "NOTATION",
                "integer",
                "nonPositiveInteger",
                "negativeInteger",
                "long",
                "int",
                "short",
                "byte",
                "nonNegativeInteger",
                "unsignedLong",
                "unsignedInt",
                "unsignedShort",
                "unsignedByte",
                "positiveInteger")
            .contains(base.localName());
  }

  private boolean isNumericBase(SchemaQName base) {
    return switch (base.localName()) {
      case "int",
          "integer",
          "long",
          "decimal",
          "short",
          "byte",
          "float",
          "double",
          "nonPositiveInteger",
          "negativeInteger",
          "nonNegativeInteger",
          "unsignedLong",
          "unsignedInt",
          "unsignedShort",
          "unsignedByte",
          "positiveInteger" ->
          true;
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
        case "short" -> Short.parseShort(value);
        case "byte" -> Byte.parseByte(value);
        case "integer" -> new BigInteger(value);
        case "nonPositiveInteger",
            "negativeInteger",
            "nonNegativeInteger",
            "unsignedLong",
            "positiveInteger" ->
            new BigInteger(value);
        case "long" -> Long.parseLong(value);
        case "unsignedInt" -> Long.parseLong(value);
        case "unsignedShort" -> Integer.parseInt(value);
        case "unsignedByte" -> Short.parseShort(value);
        case "decimal" -> new BigDecimal(value);
        case "float" -> Float.parseFloat(value);
        case "double" -> Double.parseDouble(value);
        default -> {
          return isSupportedRestrictionBase(
              new SchemaQName("http://www.w3.org/2001/XMLSchema", base));
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
      if (semanticAttributes(node).hasAny()) {
        diagnostic(
            state,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            document.resourceId(),
            "xs:attribute ref uses cannot override default/fixed semantics.");
        return null;
      }
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
    SchemaIrValueSemantics semantics = semanticAttributes(node);
    return new SchemaIrAttribute(
        new SchemaQName(schemaNamespace(document), localName),
        typeReference,
        node.attributes().get("use"),
        semantics,
        false);
  }

  private SchemaIrValueSemantics semanticAttributes(XsdSyntaxNode node) {
    return new SchemaIrValueSemantics(
        "true".equals(node.attributes().get("nillable")),
        node.attributes().get("default"),
        node.attributes().get("fixed"));
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
      case NOTATION -> SchemaComponentKind.NOTATION;
      case ANNOTATION,
          APPINFO,
          DOCUMENTATION,
          INCLUDE,
          IMPORT,
          REDEFINE,
          RESTRICTION,
          COMPLEX_CONTENT,
          EXTENSION,
          SIMPLE_CONTENT,
          ENUMERATION,
          LENGTH,
          MIN_LENGTH,
          MAX_LENGTH,
          MIN_INCLUSIVE,
          MAX_INCLUSIVE,
          MIN_EXCLUSIVE,
          MAX_EXCLUSIVE,
          TOTAL_DIGITS,
          FRACTION_DIGITS,
          WHITE_SPACE,
          PATTERN,
          LIST,
          UNION,
          SEQUENCE,
          ALL,
          CHOICE,
          ANY,
          ANY_ATTRIBUTE,
          UNIQUE,
          KEY,
          KEYREF,
          SELECTOR,
          FIELD ->
          null;
    };
  }

  private String schemaNamespace(XsdSyntaxDocument document) {
    return document.effectiveTargetNamespace();
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
    private String minExclusive;
    private String maxExclusive;
    private Integer totalDigits;
    private Integer fractionDigits;
    private String whiteSpace;

    private SimpleRestrictionState(SchemaQName base) {
      this.base = base;
    }

    private SchemaIrSimpleRestriction toRestriction() {
      return new SchemaIrSimpleRestriction(
          base,
          enumerations,
          length,
          minLength,
          maxLength,
          minInclusive,
          maxInclusive,
          minExclusive,
          maxExclusive,
          totalDigits,
          fractionDigits,
          whiteSpace,
          patterns);
    }
  }
}
