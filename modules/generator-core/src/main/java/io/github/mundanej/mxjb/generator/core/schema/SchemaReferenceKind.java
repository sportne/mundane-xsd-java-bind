package io.github.mundanej.mxjb.generator.core.schema;

/** Supported schema resource edge kinds. */
public enum SchemaReferenceKind {
  INCLUDE("include"),
  IMPORT("import");

  private final String manifestName;

  SchemaReferenceKind(String manifestName) {
    this.manifestName = manifestName;
  }

  public String manifestName() {
    return manifestName;
  }
}
