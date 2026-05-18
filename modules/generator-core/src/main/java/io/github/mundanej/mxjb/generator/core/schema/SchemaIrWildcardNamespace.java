package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized namespace constraint for an accepted xs:any particle. */
public record SchemaIrWildcardNamespace(String kind, List<String> namespaces) {
  public SchemaIrWildcardNamespace {
    Objects.requireNonNull(kind, "kind");
    namespaces = List.copyOf(namespaces == null ? List.of() : namespaces);
  }

  public String toText() {
    if (namespaces.isEmpty()) {
      return kind;
    }
    return kind + ":" + namespaces.stream().collect(Collectors.joining(","));
  }
}
