package io.github.xsdbind.generator.core.schema;

/** Raw XSD syntax constructs accepted by the phase-one frontend. */
public enum XsdSyntaxKind {
  ELEMENT("element"),
  COMPLEX_TYPE("complexType"),
  SIMPLE_TYPE("simpleType"),
  ATTRIBUTE("attribute"),
  SEQUENCE("sequence");

  private final String manifestName;

  XsdSyntaxKind(String manifestName) {
    this.manifestName = manifestName;
  }

  public String manifestName() {
    return manifestName;
  }
}
