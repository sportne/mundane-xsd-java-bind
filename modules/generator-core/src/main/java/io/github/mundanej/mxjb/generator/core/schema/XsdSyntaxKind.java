package io.github.mundanej.mxjb.generator.core.schema;

/** Raw XSD syntax constructs accepted by the phase-one frontend. */
public enum XsdSyntaxKind {
  ANNOTATION("annotation"),
  APPINFO("appinfo"),
  DOCUMENTATION("documentation"),
  INCLUDE("include"),
  IMPORT("import"),
  REDEFINE("redefine"),
  ELEMENT("element"),
  COMPLEX_TYPE("complexType"),
  COMPLEX_CONTENT("complexContent"),
  EXTENSION("extension"),
  SIMPLE_CONTENT("simpleContent"),
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
  NOTATION("notation"),
  SEQUENCE("sequence"),
  ALL("all"),
  CHOICE("choice"),
  ANY("any"),
  ANY_ATTRIBUTE("anyAttribute"),
  UNIQUE("unique"),
  KEY("key"),
  KEYREF("keyref"),
  SELECTOR("selector"),
  FIELD("field");

  private final String manifestName;

  XsdSyntaxKind(String manifestName) {
    this.manifestName = manifestName;
  }

  public String manifestName() {
    return manifestName;
  }
}
