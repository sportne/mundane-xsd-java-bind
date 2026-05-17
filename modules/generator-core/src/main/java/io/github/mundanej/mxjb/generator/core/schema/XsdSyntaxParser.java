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
    if ("choice".equals(localName) && profile != GeneratorProfile.XP_DATA_10_CHOICE) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
              resourceId,
              "xs:choice requires profile XP-DATA-10-CHOICE."));
      skipSubtree(reader);
      return null;
    }

    XsdSyntaxKind kind = kindFor(localName);
    if (kind == null) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_CONSTRUCT,
              resourceId,
              "Unsupported XSD construct xs:" + localName + " for profile XP-DATA-10."));
      skipSubtree(reader);
      return null;
    }

    Map<String, String> attributes = attributes(kind, reader);
    List<XsdSyntaxNode> children = readChildren(resourceId, reader, diagnostics, profile);
    return new XsdSyntaxNode(kind, attributes, children);
  }

  private XsdSyntaxKind kindFor(String localName) {
    return switch (localName) {
      case "element" -> XsdSyntaxKind.ELEMENT;
      case "complexType" -> XsdSyntaxKind.COMPLEX_TYPE;
      case "simpleType" -> XsdSyntaxKind.SIMPLE_TYPE;
      case "attribute" -> XsdSyntaxKind.ATTRIBUTE;
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
        addCardinality(attributes, reader);
      }
      case COMPLEX_TYPE, SIMPLE_TYPE ->
          addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
      case ATTRIBUTE -> {
        addIfPresent(attributes, "name", reader.getAttributeValue(null, "name"));
        addIfPresent(attributes, "ref", reader.getAttributeValue(null, "ref"));
        addIfPresent(attributes, "type", reader.getAttributeValue(null, "type"));
        addIfPresent(attributes, "use", reader.getAttributeValue(null, "use"));
      }
      case SEQUENCE, CHOICE -> addCardinality(attributes, reader);
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
