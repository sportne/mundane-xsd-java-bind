package io.github.mundanej.mxjb.generator.core.bind;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** One cardinality-bearing automata position in a generated content-list group. */
public record BindingContentPosition(
    BindingCardinality cardinality, List<BindingContentBranch> branches) {
  public BindingContentPosition {
    Objects.requireNonNull(cardinality, "cardinality");
    branches = List.copyOf(branches);
  }

  public String toText() {
    return cardinality.toText()
        + ":"
        + branches.stream().map(BindingContentBranch::javaName).collect(Collectors.joining("|"));
  }
}
