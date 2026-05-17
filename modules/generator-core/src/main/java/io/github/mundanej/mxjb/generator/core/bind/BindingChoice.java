package io.github.mundanej.mxjb.generator.core.bind;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Generated sealed choice type and branch records planned from a schema choice particle. */
public record BindingChoice(BindingJavaName javaName, List<BindingChoiceBranch> branches) {
  public BindingChoice {
    Objects.requireNonNull(javaName, "javaName");
    branches = List.copyOf(branches);
  }

  public String toText(String indent) {
    String line = indent + "choiceType " + javaName.qualifiedName();
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
