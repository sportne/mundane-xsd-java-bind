package io.github.mundanej.mxjb.generator.core.bind;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** One cardinality-bearing group inside a generated content-list model. */
public record BindingContentGroup(
    String modelKind, BindingCardinality cardinality, List<BindingContentBranch> branches) {
  public BindingContentGroup {
    modelKind = modelKind == null || modelKind.isBlank() ? "sequence" : modelKind;
    Objects.requireNonNull(cardinality, "cardinality");
    branches = List.copyOf(branches);
  }

  public String toText(String indent) {
    return indent
        + "group "
        + modelKind
        + " "
        + cardinality.toText()
        + " branches="
        + branches.stream().map(BindingContentBranch::javaName).collect(Collectors.joining(","));
  }
}
