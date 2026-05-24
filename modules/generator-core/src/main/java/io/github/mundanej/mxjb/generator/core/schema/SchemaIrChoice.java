package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized singleton choice particle with supported element branches. */
public record SchemaIrChoice(SchemaCardinality cardinality, List<SchemaIrParticle> branches)
    implements SchemaIrParticle {
  public SchemaIrChoice {
    Objects.requireNonNull(cardinality, "cardinality");
    branches = List.copyOf(branches);
  }

  public List<SchemaIrElement> elementBranches() {
    return branches.stream()
        .filter(SchemaIrElement.class::isInstance)
        .map(SchemaIrElement.class::cast)
        .toList();
  }

  public List<SchemaIrWildcard> wildcardBranches() {
    return branches.stream()
        .filter(SchemaIrWildcard.class::isInstance)
        .map(SchemaIrWildcard.class::cast)
        .toList();
  }

  @Override
  public String toText(String indent) {
    String line = indent + "choice cardinality=" + cardinality.toText();
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
