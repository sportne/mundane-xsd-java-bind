package io.github.mundanej.mxjb.generator.core.schema;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    for (ResolvedSchema schema : manifest.schemas()) {
      XsdSyntaxDocument document = parseDocument(schema, diagnostics, effectiveProfile);
      if (document != null) {
        documents.add(document);
      }
    }
    return new XsdSyntaxResult(new XsdSyntaxModel(documents), diagnostics);
  }

  private XsdSyntaxDocument parseDocument(
      ResolvedSchema schema, List<SchemaDiagnostic> diagnostics, GeneratorProfile profile) {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    try (InputStream input = java.nio.file.Files.newInputStream(schema.sourcePath())) {
      XMLStreamReader reader = factory.createXMLStreamReader(input);
      try {
        return readDocument(schema, reader, diagnostics, profile);
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
      GeneratorProfile profile)
      throws XMLStreamException {
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        if (isXsdElement(reader, "schema")) {
          return readSchema(schema, reader, diagnostics, profile);
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
      GeneratorProfile profile)
      throws XMLStreamException {
    String targetNamespace = valueOrEmpty(reader.getAttributeValue(null, "targetNamespace"));
    Map<String, String> namespaceDeclarations = namespaceDeclarations(reader);
    List<XsdSyntaxNode> children = readChildren(schema.resourceId(), reader, diagnostics, profile);
    return new XsdSyntaxDocument(
        schema.resourceId(), targetNamespace, namespaceDeclarations, children);
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
    if ("include".equals(localName) || "import".equals(localName)) {
      skipSubtree(reader);
      return null;
    }
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
        || profile == GeneratorProfile.XP_XSD10_SEMANTIC;
  }

  private boolean supportsSimpleRestrictions(GeneratorProfile profile) {
    return profile == GeneratorProfile.XP_VALIDATION_10_BASIC
        || profile == GeneratorProfile.XP_XSD10_COMPOSED
        || profile == GeneratorProfile.XP_XSD10_SEMANTIC;
  }

  private boolean supportsComposedSchema(GeneratorProfile profile) {
    return profile == GeneratorProfile.XP_XSD10_COMPOSED
        || profile == GeneratorProfile.XP_XSD10_SEMANTIC;
  }

  private boolean supportsSemanticSchema(GeneratorProfile profile) {
    return profile == GeneratorProfile.XP_XSD10_SEMANTIC;
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
      case "pattern" -> XsdSyntaxKind.PATTERN;
      case "list" -> XsdSyntaxKind.LIST;
      case "union" -> XsdSyntaxKind.UNION;
      case "attribute" -> XsdSyntaxKind.ATTRIBUTE;
      case "group" -> XsdSyntaxKind.GROUP;
      case "attributeGroup" -> XsdSyntaxKind.ATTRIBUTE_GROUP;
      case "sequence" -> XsdSyntaxKind.SEQUENCE;
      case "choice" -> XsdSyntaxKind.CHOICE;
      default -> null;
    };
  }

  private Map<String, String> attributes(XsdSyntaxKind kind, XMLStreamReader reader) {
    Map<String, String> attributes = new LinkedHashMap<>();
    switch (kind) {
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
      }
      case SIMPLE_TYPE -> addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
      case COMPLEX_CONTENT ->
          addIfPresent(attributes, "mixed", reader.getAttributeValue(null, "mixed"));
      case EXTENSION, RESTRICTION ->
          addIfPresent(attributes, "base", reader.getAttributeValue(null, "base"));
      case SIMPLE_CONTENT -> {
        // No accepted attributes for this out-of-scope construct.
      }
      case ENUMERATION, LENGTH, MIN_LENGTH, MAX_LENGTH, MIN_INCLUSIVE, MAX_INCLUSIVE, PATTERN ->
          addIfPresent(attributes, "value", reader.getAttributeValue(null, "value"));
      case ATTRIBUTE -> {
        addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
        addIfPresent(attributes, "ref", reader.getAttributeValue(null, "ref"));
        addIfPresent(attributes, "type", reader.getAttributeValue(null, "type"));
        addIfPresent(attributes, "use", reader.getAttributeValue(null, "use"));
        addIfPresent(attributes, "default", reader.getAttributeValue(null, "default"));
        addIfPresent(attributes, "fixed", reader.getAttributeValue(null, "fixed"));
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
      case SEQUENCE, CHOICE -> addCardinality(attributes, reader);
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
}
