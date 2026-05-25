package io.github.mundanej.mxjb.generator.core.emit;

enum GeneratedEmitterKind {
  READER("read", "XmlReader"),
  WRITER("write", "XmlWriter"),
  VALIDATOR("validate", "XmlValidator");

  private final String helperPrefix;
  private final String sourceSuffix;

  GeneratedEmitterKind(String helperPrefix, String sourceSuffix) {
    this.helperPrefix = helperPrefix;
    this.sourceSuffix = sourceSuffix;
  }

  String helperPrefix() {
    return helperPrefix;
  }

  String sourceSuffix() {
    return sourceSuffix;
  }
}
