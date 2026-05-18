package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized direct substitution group with deterministic branch order. */
public record SchemaIrSubstitutionGroup(SchemaQName head, List<SchemaIrElement> branches) {
  public SchemaIrSubstitutionGroup {
    Objects.requireNonNull(head, "head");
    branches = List.copyOf(branches);
  }

  public String toText(String indent) {
    String line = indent + "substitutionGroup head=" + head.toText();
    if (branches.isEmpty()) {
      return line;
    }
    return line
        + "\n"
        + branches.stream()
            .map(branch -> branch.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
  }
}
