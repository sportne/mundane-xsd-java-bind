package io.github.mundanej.mxjb.runtime;

import java.util.Objects;

/** Exact expanded QName value for XML Schema QName and NOTATION values. */
public record XmlQName(String namespaceUri, String localName, String lexicalName) {
  public XmlQName {
    Objects.requireNonNull(lexicalName, "lexicalName");
    XmlDatatypes.requireQNameValue(namespaceUri, localName, lexicalName);
    if (!localName.equals(lexicalLocalName(lexicalName))) {
      throw new IllegalArgumentException("QName lexical local name must match localName.");
    }
  }

  public XmlQName(String namespaceUri, String localName) {
    this(namespaceUri, localName, localName);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof XmlQName that
        && namespaceUri.equals(that.namespaceUri)
        && localName.equals(that.localName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespaceUri, localName);
  }

  private static String lexicalLocalName(String lexicalName) {
    int separator = lexicalName.indexOf(':');
    return separator < 0 ? lexicalName : lexicalName.substring(separator + 1);
  }
}
