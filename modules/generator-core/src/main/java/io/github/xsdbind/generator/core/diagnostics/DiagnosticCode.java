package io.github.xsdbind.generator.core.diagnostics;

/** Stable diagnostic identifiers for schema compiler failures. */
public enum DiagnosticCode {
  SCHEMA_RESOURCE_NETWORK_DENIED,
  SCHEMA_RESOURCE_OUTSIDE_LOCAL_ROOT,
  SCHEMA_RESOURCE_NOT_FOUND,
  SCHEMA_RESOURCE_IO_ERROR,
  SCHEMA_RESOURCE_XML_ERROR,
  SCHEMA_RESOURCE_CYCLE,
  SCHEMA_RESOURCE_MISSING_LOCATION
}
