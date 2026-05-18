package io.github.mundanej.mxjb.generator.core.bind;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Generated sealed choice type and branch records planned from a schema choice particle. */
public record BindingChoice(
    BindingJavaName javaName, List<BindingChoiceBranch> branches, String modelKind) {
  public BindingChoice(BindingJavaName javaName, List<BindingChoiceBranch> branches) {
    this(javaName, branches, "choice");
  }

  public BindingChoice {
    Objects.requireNonNull(javaName, "javaName");
    branches = List.copyOf(branches);
    modelKind = modelKind == null || modelKind.isBlank() ? "choice" : modelKind;
  }

  public String toText(String indent) {
    String line = indent + modelKind + "Type " + javaName.qualifiedName();
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
