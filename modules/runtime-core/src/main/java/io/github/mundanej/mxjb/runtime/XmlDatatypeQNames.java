package io.github.mundanej.mxjb.runtime;

import java.util.Objects;

final class XmlDatatypeQNames {
  private XmlDatatypeQNames() {}

  static XmlQName parseQName(String value, XmlEventReader input) {
    String lexical = XmlDatatypeLexical.collapseWhitespace(value);
    int separator = lexical.indexOf(':');
    if (separator < 0) {
      XmlDatatypeLexical.requirePattern("QName", lexical, XmlDatatypeLexical.NC_NAME);
      return new XmlQName("", lexical, lexical);
    }
    String prefix = lexical.substring(0, separator);
    String localName = lexical.substring(separator + 1);
    XmlDatatypeLexical.requirePattern("QName prefix", prefix, XmlDatatypeLexical.NC_NAME);
    XmlDatatypeLexical.requirePattern("QName local name", localName, XmlDatatypeLexical.NC_NAME);
    String namespace = input == null ? null : input.namespaceUriForPrefix(prefix);
    if (namespace == null || namespace.isEmpty()) {
      throw new IllegalArgumentException("Unresolved QName prefix " + prefix + ".");
    }
    return new XmlQName(namespace, localName, lexical);
  }

  static void requireQNameValue(String namespaceUri, String localName, String lexicalName) {
    Objects.requireNonNull(namespaceUri, "namespaceUri");
    Objects.requireNonNull(localName, "localName");
    Objects.requireNonNull(lexicalName, "lexicalName");
    XmlDatatypeLexical.requirePattern("QName local name", localName, XmlDatatypeLexical.NC_NAME);
    int separator = lexicalName.indexOf(':');
    if (separator < 0) {
      XmlDatatypeLexical.requirePattern(
          "QName lexical name", lexicalName, XmlDatatypeLexical.NC_NAME);
      return;
    }
    if (separator != lexicalName.lastIndexOf(':')) {
      throw new IllegalArgumentException("Invalid QName lexical name value.");
    }
    XmlDatatypeLexical.requirePattern(
        "QName prefix", lexicalName.substring(0, separator), XmlDatatypeLexical.NC_NAME);
    XmlDatatypeLexical.requirePattern(
        "QName local name", lexicalName.substring(separator + 1), XmlDatatypeLexical.NC_NAME);
  }
}
