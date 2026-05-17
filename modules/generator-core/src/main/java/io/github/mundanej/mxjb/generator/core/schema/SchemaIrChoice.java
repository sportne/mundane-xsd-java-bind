package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized singleton choice particle with supported element branches. */
public record SchemaIrChoice(SchemaCardinality cardinality, List<SchemaIrElement> branches)
    implements SchemaIrParticle {
  public SchemaIrChoice {
    Objects.requireNonNull(cardinality, "cardinality");
    branches = List.copyOf(branches);
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
