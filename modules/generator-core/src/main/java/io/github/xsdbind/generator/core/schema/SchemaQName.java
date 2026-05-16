package io.github.xsdbind.generator.core.schema;

import java.util.Objects;
import javax.xml.XMLConstants;

/** A namespace-qualified schema name after lexical QName resolution. */
public record SchemaQName(String namespace, String localName) implements Comparable<SchemaQName> {
  public static final String XSD_NAMESPACE = XMLConstants.W3C_XML_SCHEMA_NS_URI;

  public SchemaQName {
    namespace = namespace == null ? "" : namespace;
    Objects.requireNonNull(localName, "localName");
  }

  public boolean isXmlSchemaBuiltIn() {
    return XSD_NAMESPACE.equals(namespace);
  }

  public String toText() {
    if (isXmlSchemaBuiltIn()) {
      return "xs:" + localName;
    }
    if (namespace.isEmpty()) {
      return localName;
    }
    return "{" + namespace + "}" + localName;
  }

  @Override
  public int compareTo(SchemaQName other) {
    int namespaceComparison = namespace.compareTo(other.namespace);
    if (namespaceComparison != 0) {
      return namespaceComparison;
    }
    return localName.compareTo(other.localName);
  }
}
