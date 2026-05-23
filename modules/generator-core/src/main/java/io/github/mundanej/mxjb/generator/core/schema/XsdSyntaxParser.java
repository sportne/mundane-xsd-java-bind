package io.github.mundanej.mxjb.generator.core.schema;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Parses resolver-approved XSD documents into a raw syntax model. */
public final class XsdSyntaxParser {
  private static final String XSD_NAMESPACE = XMLConstants.W3C_XML_SCHEMA_NS_URI;

  public XsdSyntaxResult parse(ResolvedSchemaManifest manifest) {
    return parse(manifest, GeneratorProfile.XP_DATA_10);
  }

  public XsdSyntaxResult parse(ResolvedSchemaManifest manifest, GeneratorProfile profile) {
    List<SchemaDiagnostic> diagnostics = new ArrayList<>();
    List<XsdSyntaxDocument> documents = new ArrayList<>();
    GeneratorProfile effectiveProfile = profile == null ? GeneratorProfile.XP_DATA_10 : profile;
    Map<String, String> effectiveNamespaces = effectiveNamespaces(manifest, diagnostics);
    for (ResolvedSchema schema : manifest.schemas()) {
      XsdSyntaxDocument document =
          parseDocument(
              schema, diagnostics, effectiveProfile, effectiveNamespaces.get(schema.resourceId()));
      if (document != null) {
        documents.add(document);
      }
    }
    return new XsdSyntaxResult(new XsdSyntaxModel(documents), diagnostics);
  }

  private XsdSyntaxDocument parseDocument(
      ResolvedSchema schema,
      List<SchemaDiagnostic> diagnostics,
      GeneratorProfile profile,
      String effectiveTargetNamespace) {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    try (InputStream input = java.nio.file.Files.newInputStream(schema.sourcePath())) {
      XMLStreamReader reader = factory.createXMLStreamReader(input);
      try {
        return readDocument(schema, reader, diagnostics, profile, effectiveTargetNamespace);
      } finally {
        reader.close();
      }
    } catch (IOException | XMLStreamException exception) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_XML_ERROR,
              schema.resourceId(),
              exception.getMessage()));
      return null;
    }
  }

  private XsdSyntaxDocument readDocument(
      ResolvedSchema schema,
      XMLStreamReader reader,
      List<SchemaDiagnostic> diagnostics,
      GeneratorProfile profile,
      String effectiveTargetNamespace)
      throws XMLStreamException {
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        if (isXsdElement(reader, "schema")) {
          return readSchema(schema, reader, diagnostics, profile, effectiveTargetNamespace);
        }
        diagnostics.add(
            new SchemaDiagnostic(
                DiagnosticCode.SCHEMA_FRONTEND_EXPECTED_SCHEMA_ROOT,
                schema.resourceId(),
                "Expected xs:schema root but found " + elementName(reader) + "."));
        skipSubtree(reader);
        return null;
      }
    }
    diagnostics.add(
        new SchemaDiagnostic(
            DiagnosticCode.SCHEMA_FRONTEND_EXPECTED_SCHEMA_ROOT,
            schema.resourceId(),
            "Expected xs:schema root but found no document element."));
    return null;
  }

  private XsdSyntaxDocument readSchema(
      ResolvedSchema schema,
      XMLStreamReader reader,
      List<SchemaDiagnostic> diagnostics,
      GeneratorProfile profile,
      String effectiveTargetNamespace)
      throws XMLStreamException {
    String targetNamespace = valueOrEmpty(reader.getAttributeValue(null, "targetNamespace"));
    Map<String, String> schemaAttributes = schemaAttributes(reader);
    Map<String, String> namespaceDeclarations = namespaceDeclarations(reader);
    List<XsdSyntaxNode> children = readChildren(schema.resourceId(), reader, diagnostics, profile);
    return new XsdSyntaxDocument(
        schema.resourceId(),
        targetNamespace,
        valueOrDefault(effectiveTargetNamespace, targetNamespace),
        schemaAttributes,
        namespaceDeclarations,
        children);
  }

  private List<XsdSyntaxNode> readChildren(
      String resourceId,
      XMLStreamReader reader,
      List<SchemaDiagnostic> diagnostics,
      GeneratorProfile profile)
      throws XMLStreamException {
    List<XsdSyntaxNode> children = new ArrayList<>();
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        XsdSyntaxNode child = readChild(resourceId, reader, diagnostics, profile);
        if (child != null) {
          children.add(child);
        }
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        return children;
      }
    }
    return children;
  }

  private XsdSyntaxNode readChild(
      String resourceId,
      XMLStreamReader reader,
      List<SchemaDiagnostic> diagnostics,
      GeneratorProfile profile)
      throws XMLStreamException {
    if (!XSD_NAMESPACE.equals(reader.getNamespaceURI())) {
      skipSubtree(reader);
      return null;
    }

    String localName = reader.getLocalName();
    if ("choice".equals(localName) && !supportsChoice(profile)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
              resourceId,
              "xs:choice requires profile XP-DATA-10-CHOICE."));
      skipSubtree(reader);
      return null;
    }
    if (isSimpleRestrictionConstruct(localName) && !supportsSimpleRestrictions(profile)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
              resourceId,
              "xs:" + localName + " requires profile XP-VALIDATION-10-BASIC."));
      skipSubtree(reader);
      return null;
    }
    if (isComposedSchemaConstruct(localName) && !supportsComposedSchema(profile)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
              resourceId,
              "xs:" + localName + " requires profile XP-XSD10-COMPOSED."));
      skipSubtree(reader);
      return null;
    }
    if ("any".equals(localName) && !supportsDocumentSchema(profile)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
              resourceId,
              "xs:any requires profile XP-XSD10-DOCUMENT."));
      skipSubtree(reader);
      return null;
    }

    XsdSyntaxKind kind = kindFor(localName);
    if (kind == null) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_CONSTRUCT,
              resourceId,
              "Unsupported XSD construct xs:"
                  + localName
                  + " for profile "
                  + profile.cliToken()
                  + "."));
      skipSubtree(reader);
      return null;
    }

    Map<String, String> attributes = attributes(kind, reader);
    if (hasSemanticAttributes(kind, attributes) && !supportsSemanticSchema(profile)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
              resourceId,
              "xs:"
                  + localName
                  + " nillable/default/fixed semantics require profile XP-XSD10-SEMANTIC."));
      skipSubtree(reader);
      return null;
    }
    if (hasSubstitutionAttributes(kind, attributes) && !supportsSemanticSchema(profile)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
              resourceId,
              "xs:element substitutionGroup requires profile XP-XSD10-SEMANTIC."));
      skipSubtree(reader);
      return null;
    }
    if (hasMixedAttribute(kind, attributes) && !supportsDocumentSchema(profile)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
              resourceId,
              "xs:" + localName + " mixed content requires profile XP-XSD10-DOCUMENT."));
      skipSubtree(reader);
      return null;
    }
    if (kind == XsdSyntaxKind.RESTRICTION
        && !supportsComposedSchema(profile)
        && !isXmlSchemaBuiltInBase(attributes.get("base"), reader)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
              resourceId,
              "xs:restriction derivation chains require profile XP-XSD10-COMPOSED."));
      skipSubtree(reader);
      return null;
    }
    List<XsdSyntaxNode> children = readChildren(resourceId, reader, diagnostics, profile);
    return new XsdSyntaxNode(kind, attributes, children);
  }

  private boolean isSimpleRestrictionConstruct(String localName) {
    return switch (localName) {
      case "restriction",
          "enumeration",
          "length",
          "minLength",
          "maxLength",
          "minInclusive",
          "maxInclusive",
          "minExclusive",
          "maxExclusive",
          "totalDigits",
          "fractionDigits",
          "whiteSpace",
          "pattern" ->
          true;
      default -> false;
    };
  }

  private boolean isComposedSchemaConstruct(String localName) {
    return "group".equals(localName)
        || "attributeGroup".equals(localName)
        || "list".equals(localName)
        || "union".equals(localName)
        || "complexContent".equals(localName)
        || "extension".equals(localName)
        || "simpleContent".equals(localName);
  }

  private boolean supportsChoice(GeneratorProfile profile) {
    return profile == GeneratorProfile.XP_DATA_10_CHOICE
        || profile == GeneratorProfile.XP_XSD10_COMPOSED
        || profile == GeneratorProfile.XP_XSD10_SEMANTIC
        || profile == GeneratorProfile.XP_XSD10_DOCUMENT
        || profile == GeneratorProfile.XP_XSD10_FULL;
  }

  private boolean supportsSimpleRestrictions(GeneratorProfile profile) {
    return profile == GeneratorProfile.XP_VALIDATION_10_BASIC
        || profile == GeneratorProfile.XP_XSD10_COMPOSED
        || profile == GeneratorProfile.XP_XSD10_SEMANTIC
        || profile == GeneratorProfile.XP_XSD10_DOCUMENT
        || profile == GeneratorProfile.XP_XSD10_FULL;
  }

  private boolean supportsComposedSchema(GeneratorProfile profile) {
    return profile == GeneratorProfile.XP_XSD10_COMPOSED
        || profile == GeneratorProfile.XP_XSD10_SEMANTIC
        || profile == GeneratorProfile.XP_XSD10_DOCUMENT
        || profile == GeneratorProfile.XP_XSD10_FULL;
  }

  private boolean supportsSemanticSchema(GeneratorProfile profile) {
    return profile == GeneratorProfile.XP_XSD10_SEMANTIC
        || profile == GeneratorProfile.XP_XSD10_DOCUMENT
        || profile == GeneratorProfile.XP_XSD10_FULL;
  }

  private boolean supportsDocumentSchema(GeneratorProfile profile) {
    return profile == GeneratorProfile.XP_XSD10_DOCUMENT
        || profile == GeneratorProfile.XP_XSD10_FULL;
  }

  private boolean hasMixedAttribute(XsdSyntaxKind kind, Map<String, String> attributes) {
    return (kind == XsdSyntaxKind.COMPLEX_TYPE || kind == XsdSyntaxKind.COMPLEX_CONTENT)
        && "true".equals(attributes.get("mixed"));
  }

  private boolean hasSemanticAttributes(XsdSyntaxKind kind, Map<String, String> attributes) {
    return switch (kind) {
      case ELEMENT ->
          attributes.containsKey("nillable")
              || attributes.containsKey("default")
              || attributes.containsKey("fixed");
      case ATTRIBUTE -> attributes.containsKey("default") || attributes.containsKey("fixed");
      default -> false;
    };
  }

  private boolean hasSubstitutionAttributes(XsdSyntaxKind kind, Map<String, String> attributes) {
    return kind == XsdSyntaxKind.ELEMENT && attributes.containsKey("substitutionGroup");
  }

  private XsdSyntaxKind kindFor(String localName) {
    return switch (localName) {
      case "element" -> XsdSyntaxKind.ELEMENT;
      case "annotation" -> XsdSyntaxKind.ANNOTATION;
      case "appinfo" -> XsdSyntaxKind.APPINFO;
      case "documentation" -> XsdSyntaxKind.DOCUMENTATION;
      case "include" -> XsdSyntaxKind.INCLUDE;
      case "import" -> XsdSyntaxKind.IMPORT;
      case "redefine" -> XsdSyntaxKind.REDEFINE;
      case "complexType" -> XsdSyntaxKind.COMPLEX_TYPE;
      case "complexContent" -> XsdSyntaxKind.COMPLEX_CONTENT;
      case "extension" -> XsdSyntaxKind.EXTENSION;
      case "simpleContent" -> XsdSyntaxKind.SIMPLE_CONTENT;
      case "simpleType" -> XsdSyntaxKind.SIMPLE_TYPE;
      case "restriction" -> XsdSyntaxKind.RESTRICTION;
      case "enumeration" -> XsdSyntaxKind.ENUMERATION;
      case "length" -> XsdSyntaxKind.LENGTH;
      case "minLength" -> XsdSyntaxKind.MIN_LENGTH;
      case "maxLength" -> XsdSyntaxKind.MAX_LENGTH;
      case "minInclusive" -> XsdSyntaxKind.MIN_INCLUSIVE;
      case "maxInclusive" -> XsdSyntaxKind.MAX_INCLUSIVE;
      case "minExclusive" -> XsdSyntaxKind.MIN_EXCLUSIVE;
      case "maxExclusive" -> XsdSyntaxKind.MAX_EXCLUSIVE;
      case "totalDigits" -> XsdSyntaxKind.TOTAL_DIGITS;
      case "fractionDigits" -> XsdSyntaxKind.FRACTION_DIGITS;
      case "whiteSpace" -> XsdSyntaxKind.WHITE_SPACE;
      case "pattern" -> XsdSyntaxKind.PATTERN;
      case "list" -> XsdSyntaxKind.LIST;
      case "union" -> XsdSyntaxKind.UNION;
      case "attribute" -> XsdSyntaxKind.ATTRIBUTE;
      case "group" -> XsdSyntaxKind.GROUP;
      case "attributeGroup" -> XsdSyntaxKind.ATTRIBUTE_GROUP;
      case "notation" -> XsdSyntaxKind.NOTATION;
      case "sequence" -> XsdSyntaxKind.SEQUENCE;
      case "all" -> XsdSyntaxKind.ALL;
      case "choice" -> XsdSyntaxKind.CHOICE;
      case "any" -> XsdSyntaxKind.ANY;
      case "anyAttribute" -> XsdSyntaxKind.ANY_ATTRIBUTE;
      case "unique" -> XsdSyntaxKind.UNIQUE;
      case "key" -> XsdSyntaxKind.KEY;
      case "keyref" -> XsdSyntaxKind.KEYREF;
      case "selector" -> XsdSyntaxKind.SELECTOR;
      case "field" -> XsdSyntaxKind.FIELD;
      default -> null;
    };
  }

  private Map<String, String> attributes(XsdSyntaxKind kind, XMLStreamReader reader) {
    Map<String, String> attributes = new LinkedHashMap<>();
    switch (kind) {
      case ANNOTATION -> {
        addIfPresent(attributes, "id", reader.getAttributeValue(null, "id"));
      }
      case APPINFO, DOCUMENTATION -> {
        addIfPresent(attributes, "source", reader.getAttributeValue(null, "source"));
        addIfPresent(
            attributes, "xml:lang", reader.getAttributeValue(XMLConstants.XML_NS_URI, "lang"));
      }
      case INCLUDE, REDEFINE ->
          addIfPresent(
              attributes, "schemaLocation", reader.getAttributeValue(null, "schemaLocation"));
      case IMPORT -> {
        addIfPresent(attributes, "namespace", reader.getAttributeValue(null, "namespace"));
        addIfPresent(
            attributes, "schemaLocation", reader.getAttributeValue(null, "schemaLocation"));
      }
      case ELEMENT -> {
        addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
        addIfPresent(attributes, "ref", reader.getAttributeValue(null, "ref"));
        addIfPresent(attributes, "type", reader.getAttributeValue(null, "type"));
        addIfPresent(
            attributes, "substitutionGroup", reader.getAttributeValue(null, "substitutionGroup"));
        addIfPresent(attributes, "abstract", reader.getAttributeValue(null, "abstract"));
        addIfPresent(attributes, "block", reader.getAttributeValue(null, "block"));
        addIfPresent(attributes, "final", reader.getAttributeValue(null, "final"));
        addIfPresent(attributes, "nillable", reader.getAttributeValue(null, "nillable"));
        addIfPresent(attributes, "default", reader.getAttributeValue(null, "default"));
        addIfPresent(attributes, "fixed", reader.getAttributeValue(null, "fixed"));
        addCardinality(attributes, reader);
      }
      case COMPLEX_TYPE -> {
        addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
        addIfPresent(attributes, "mixed", reader.getAttributeValue(null, "mixed"));
        addIfPresent(attributes, "abstract", reader.getAttributeValue(null, "abstract"));
        addIfPresent(attributes, "block", reader.getAttributeValue(null, "block"));
        addIfPresent(attributes, "final", reader.getAttributeValue(null, "final"));
      }
      case SIMPLE_TYPE, UNIQUE, KEY ->
          addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
      case COMPLEX_CONTENT ->
          addIfPresent(attributes, "mixed", reader.getAttributeValue(null, "mixed"));
      case EXTENSION, RESTRICTION ->
          addIfPresent(attributes, "base", reader.getAttributeValue(null, "base"));
      case SIMPLE_CONTENT -> {
        // No accepted attributes for this out-of-scope construct.
      }
      case ENUMERATION,
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
          PATTERN ->
          addIfPresent(attributes, "value", reader.getAttributeValue(null, "value"));
      case ATTRIBUTE -> {
        addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
        addIfPresent(attributes, "ref", reader.getAttributeValue(null, "ref"));
        addIfPresent(attributes, "type", reader.getAttributeValue(null, "type"));
        addIfPresent(attributes, "use", reader.getAttributeValue(null, "use"));
        addIfPresent(attributes, "form", reader.getAttributeValue(null, "form"));
        addIfPresent(attributes, "default", reader.getAttributeValue(null, "default"));
        addIfPresent(attributes, "fixed", reader.getAttributeValue(null, "fixed"));
      }
      case NOTATION -> {
        addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
        addIfPresent(attributes, "public", reader.getAttributeValue(null, "public"));
        addIfPresent(attributes, "system", reader.getAttributeValue(null, "system"));
      }
      case GROUP -> {
        addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
        addIfPresent(attributes, "ref", reader.getAttributeValue(null, "ref"));
        addCardinality(attributes, reader);
      }
      case ATTRIBUTE_GROUP -> {
        addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
        addIfPresent(attributes, "ref", reader.getAttributeValue(null, "ref"));
      }
      case SEQUENCE, CHOICE, ALL -> addCardinality(attributes, reader);
      case ANY -> {
        addIfPresent(attributes, "namespace", reader.getAttributeValue(null, "namespace"));
        addIfPresent(
            attributes, "processContents", reader.getAttributeValue(null, "processContents"));
        addCardinality(attributes, reader);
      }
      case ANY_ATTRIBUTE -> {
        addIfPresent(attributes, "namespace", reader.getAttributeValue(null, "namespace"));
        addIfPresent(
            attributes, "processContents", reader.getAttributeValue(null, "processContents"));
      }
      case KEYREF -> {
        addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
        addIfPresent(attributes, "refer", reader.getAttributeValue(null, "refer"));
      }
      case SELECTOR, FIELD ->
          addIfPresent(attributes, "xpath", reader.getAttributeValue(null, "xpath"));
      case LIST, UNION -> {
        addIfPresent(attributes, "itemType", reader.getAttributeValue(null, "itemType"));
        addIfPresent(attributes, "memberTypes", reader.getAttributeValue(null, "memberTypes"));
      }
    }
    return attributes;
  }

  private void addCardinality(Map<String, String> attributes, XMLStreamReader reader) {
    attributes.put("minOccurs", valueOrDefault(reader.getAttributeValue(null, "minOccurs"), "1"));
    attributes.put("maxOccurs", valueOrDefault(reader.getAttributeValue(null, "maxOccurs"), "1"));
  }

  private void addIfPresent(Map<String, String> attributes, String name, String value) {
    if (value != null && !value.isBlank()) {
      attributes.put(name, value);
    }
  }

  private Map<String, String> schemaAttributes(XMLStreamReader reader) {
    Map<String, String> attributes = new LinkedHashMap<>();
    addIfPresent(
        attributes, "elementFormDefault", reader.getAttributeValue(null, "elementFormDefault"));
    addIfPresent(
        attributes, "attributeFormDefault", reader.getAttributeValue(null, "attributeFormDefault"));
    addIfPresent(attributes, "blockDefault", reader.getAttributeValue(null, "blockDefault"));
    addIfPresent(attributes, "finalDefault", reader.getAttributeValue(null, "finalDefault"));
    return attributes;
  }

  private boolean isXmlSchemaBuiltInBase(String base, XMLStreamReader reader) {
    if (base == null || base.isBlank()) {
      return true;
    }
    int colon = base.indexOf(':');
    if (colon < 0) {
      return false;
    }
    String prefix = base.substring(0, colon);
    return XSD_NAMESPACE.equals(reader.getNamespaceContext().getNamespaceURI(prefix));
  }

  private Map<String, String> namespaceDeclarations(XMLStreamReader reader) {
    List<Map.Entry<String, String>> declarations = new ArrayList<>();
    for (int index = 0; index < reader.getNamespaceCount(); index++) {
      String prefix = reader.getNamespacePrefix(index);
      String key = prefix == null || prefix.isBlank() ? "xmlns" : "xmlns:" + prefix;
      declarations.add(Map.entry(key, valueOrEmpty(reader.getNamespaceURI(index))));
    }
    declarations.sort(Comparator.comparing(Map.Entry::getKey));
    Map<String, String> sorted = new LinkedHashMap<>();
    for (Map.Entry<String, String> declaration : declarations) {
      sorted.put(declaration.getKey(), declaration.getValue());
    }
    return sorted;
  }

  private void skipSubtree(XMLStreamReader reader) throws XMLStreamException {
    int depth = 1;
    while (depth > 0 && reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        depth++;
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        depth--;
      }
    }
  }

  private boolean isXsdElement(XMLStreamReader reader, String localName) {
    return XSD_NAMESPACE.equals(reader.getNamespaceURI())
        && localName.equals(reader.getLocalName());
  }

  private String elementName(XMLStreamReader reader) {
    String namespace = reader.getNamespaceURI();
    if (namespace == null || namespace.isBlank()) {
      return reader.getLocalName();
    }
    if (XSD_NAMESPACE.equals(namespace)) {
      return "xs:" + reader.getLocalName();
    }
    return "{" + namespace + "}" + reader.getLocalName();
  }

  private String valueOrDefault(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  private Map<String, String> effectiveNamespaces(
      ResolvedSchemaManifest manifest, List<SchemaDiagnostic> diagnostics) {
    Map<String, ResolvedSchema> schemasByResource = new LinkedHashMap<>();
    for (ResolvedSchema schema : manifest.schemas()) {
      schemasByResource.put(schema.resourceId(), schema);
    }

    Map<String, Set<String>> adoptedNamespaces = new LinkedHashMap<>();
    for (ResolvedSchema schema : manifest.schemas()) {
      adoptedNamespaces.put(schema.resourceId(), new LinkedHashSet<>());
      if (!schema.targetNamespace().isBlank()) {
        adoptedNamespaces.get(schema.resourceId()).add(schema.targetNamespace());
      }
    }

    boolean changed;
    do {
      changed = false;
      for (ResolvedSchema schema : manifest.schemas()) {
        Set<String> sourceNamespaces = adoptedNamespaces.get(schema.resourceId());
        if (sourceNamespaces.isEmpty()) {
          continue;
        }
        for (SchemaReference reference : schema.references()) {
          if (reference.kind() == SchemaReferenceKind.INCLUDE) {
            for (String targetResource :
                matchingResources(
                    schema.resourceId(), reference.target(), schemasByResource.keySet())) {
              ResolvedSchema targetSchema = schemasByResource.get(targetResource);
              if (targetSchema.targetNamespace().isBlank()
                  && adoptedNamespaces.get(targetResource).addAll(sourceNamespaces)) {
                changed = true;
              }
            }
          }
        }
      }
    } while (changed);

    Map<String, Set<String>> includedNamespacesByResource = new LinkedHashMap<>();
    Set<String> namespaceConflictKeys = new LinkedHashSet<>();
    for (ResolvedSchema schema : manifest.schemas()) {
      for (SchemaReference reference : schema.references()) {
        if (reference.kind() == SchemaReferenceKind.INCLUDE) {
          for (String targetResource :
              matchingResources(
                  schema.resourceId(), reference.target(), schemasByResource.keySet())) {
            Set<String> sourceNamespaces = adoptedNamespaces.get(schema.resourceId());
            ResolvedSchema targetSchema = schemasByResource.get(targetResource);
            if (!targetSchema.targetNamespace().isBlank()
                && sourceNamespaces.stream()
                    .anyMatch(namespace -> !namespace.equals(targetSchema.targetNamespace()))) {
              addNamespaceConflictDiagnostic(
                  diagnostics,
                  namespaceConflictKeys,
                  targetResource,
                  "Chameleon include target namespace conflicts.");
            } else {
              includedNamespacesByResource
                  .computeIfAbsent(targetResource, ignored -> new LinkedHashSet<>())
                  .addAll(sourceNamespaces);
            }
          }
        }
      }
    }

    Map<String, String> effectiveNamespaces = new LinkedHashMap<>();
    for (ResolvedSchema schema : manifest.schemas()) {
      if (!schema.targetNamespace().isBlank()) {
        effectiveNamespaces.put(schema.resourceId(), schema.targetNamespace());
        continue;
      }
      Set<String> namespaces =
          includedNamespaces(schema.resourceId(), includedNamespacesByResource);
      if (namespaces.size() == 1) {
        effectiveNamespaces.put(schema.resourceId(), namespaces.iterator().next());
      } else if (namespaces.size() > 1) {
        diagnostics.add(
            new SchemaDiagnostic(
                DiagnosticCode.SCHEMA_IR_NAMESPACE_CONFLICT,
                schema.resourceId(),
                "Chameleon include is referenced from multiple target namespaces."));
        effectiveNamespaces.put(schema.resourceId(), "");
      } else {
        effectiveNamespaces.put(schema.resourceId(), "");
      }
    }
    return effectiveNamespaces;
  }

  private void addNamespaceConflictDiagnostic(
      List<SchemaDiagnostic> diagnostics,
      Set<String> namespaceConflictKeys,
      String resourceId,
      String message) {
    if (namespaceConflictKeys.add(resourceId + ":" + message)) {
      diagnostics.add(
          new SchemaDiagnostic(DiagnosticCode.SCHEMA_IR_NAMESPACE_CONFLICT, resourceId, message));
    }
  }

  private Set<String> includedNamespaces(
      String resourceId, Map<String, Set<String>> includedNamespacesByResource) {
    return includedNamespacesByResource.getOrDefault(resourceId, Set.of());
  }

  private Set<String> matchingResources(
      String sourceResource, String target, Set<String> resources) {
    Set<String> matches = new LinkedHashSet<>();
    String normalizedTarget = target.replace('\\', '/');
    for (String resource : resources) {
      if (resource.equals(normalizedTarget)) {
        matches.add(resource);
      }
    }
    if (!matches.isEmpty()) {
      return matches;
    }
    String relativeTarget = relativeTarget(sourceResource, normalizedTarget);
    for (String resource : resources) {
      if (resource.equals(relativeTarget)) {
        matches.add(resource);
      }
    }
    if (!matches.isEmpty()) {
      return matches;
    }
    for (String resource : resources) {
      if (resource.endsWith("/" + normalizedTarget)) {
        matches.add(resource);
      }
    }
    return matches;
  }

  private String relativeTarget(String sourceResource, String target) {
    if (target.indexOf(':') > 0) {
      return target;
    }
    int slash = sourceResource.lastIndexOf('/');
    if (slash < 0) {
      return target;
    }
    return Path.of(sourceResource.substring(0, slash))
        .resolve(target)
        .normalize()
        .toString()
        .replace('\\', '/');
  }
}
