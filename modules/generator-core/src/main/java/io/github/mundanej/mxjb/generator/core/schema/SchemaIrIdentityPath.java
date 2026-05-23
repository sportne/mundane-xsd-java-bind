package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.stream.Collectors;

/** One selector or field XPath alternative accepted for identity constraints. */
public record SchemaIrIdentityPath(
    boolean descendant, boolean self, List<SchemaIrIdentityStep> steps) {
  public SchemaIrIdentityPath {
    steps = List.copyOf(steps);
  }

  public String toText() {
    if (self) {
      return ".";
    }
    String joined =
        steps.stream().map(SchemaIrIdentityStep::toText).collect(Collectors.joining("/"));
    return descendant ? ".//" + joined : joined;
  }
}
