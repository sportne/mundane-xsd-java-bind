package io.github.xsdbind.generator.core.schema;

import java.util.List;
import java.util.stream.Collectors;

/** Stable text view of resolved schema traversal. */
public record ResolvedSchemaManifest(List<ResolvedSchema> schemas) {
  public ResolvedSchemaManifest {
    schemas = List.copyOf(schemas);
  }

  public String toText() {
    if (schemas.isEmpty()) {
      return "";
    }
    return schemas.stream()
        .map(ResolvedSchema::toManifestLine)
        .collect(Collectors.joining("\n", "", "\n"));
  }
}
