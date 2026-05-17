package io.github.mundanej.mxjb.generator.core.schema;

/** Raw XSD syntax constructs accepted by the phase-one frontend. */
public enum XsdSyntaxKind {
  ELEMENT("element"),
  COMPLEX_TYPE("complexType"),
  SIMPLE_TYPE("simpleType"),
  RESTRICTION("restriction"),
  ENUMERATION("enumeration"),
  LENGTH("length"),
  MIN_LENGTH("minLength"),
  MAX_LENGTH("maxLength"),
  MIN_INCLUSIVE("minInclusive"),
  MAX_INCLUSIVE("maxInclusive"),
  PATTERN("pattern"),
  LIST("list"),
  UNION("union"),
  ATTRIBUTE("attribute"),
  GROUP("group"),
  ATTRIBUTE_GROUP("attributeGroup"),
  SEQUENCE("sequence"),
  CHOICE("choice");

  private final String manifestName;

  XsdSyntaxKind(String manifestName) {
    this.manifestName = manifestName;
  }

  public String manifestName() {
    return manifestName;
  }
}
