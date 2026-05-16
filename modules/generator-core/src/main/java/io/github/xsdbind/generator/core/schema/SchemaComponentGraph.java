package io.github.xsdbind.generator.core.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Deterministic component graph for supported global XSD declarations. */
public record SchemaComponentGraph(Map<SchemaComponentKey, SchemaComponent> components) {
  public SchemaComponentGraph {
    components = Collections.unmodifiableMap(new LinkedHashMap<>(components));
  }

  public static SchemaComponentGraph empty() {
    return new SchemaComponentGraph(Map.of());
  }

  public String toText() {
    if (components.isEmpty()) {
      return "";
    }
    return components.values().stream()
        .map(SchemaComponent::toText)
        .collect(Collectors.joining("\n", "", "\n"));
  }
}
