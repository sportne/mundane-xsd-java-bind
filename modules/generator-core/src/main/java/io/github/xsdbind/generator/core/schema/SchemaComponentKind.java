package io.github.xsdbind.generator.core.schema;

/** XSD component symbol spaces supported by the first normalized IR slice. */
public enum SchemaComponentKind {
  ELEMENT("element"),
  COMPLEX_TYPE("complexType"),
  SIMPLE_TYPE("simpleType"),
  ATTRIBUTE("attribute");

  private final String manifestName;

  SchemaComponentKind(String manifestName) {
    this.manifestName = manifestName;
  }

  public String manifestName() {
    return manifestName;
  }
}
