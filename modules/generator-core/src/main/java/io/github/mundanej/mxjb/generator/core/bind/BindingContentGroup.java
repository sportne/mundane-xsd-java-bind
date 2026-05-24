package io.github.mundanej.mxjb.generator.core.bind;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** One cardinality-bearing group inside a generated content-list model. */
public record BindingContentGroup(
    String modelKind,
    BindingCardinality cardinality,
    List<BindingContentBranch> branches,
    List<BindingContentPosition> positions) {
  public BindingContentGroup(
      String modelKind, BindingCardinality cardinality, List<BindingContentBranch> branches) {
    this(
        modelKind,
        cardinality,
        branches,
        branches.stream()
            .map(branch -> new BindingContentPosition(branch.cardinality(), List.of(branch)))
            .collect(Collectors.toList()));
  }

  public BindingContentGroup {
    modelKind = modelKind == null || modelKind.isBlank() ? "sequence" : modelKind;
    Objects.requireNonNull(cardinality, "cardinality");
    Objects.requireNonNull(branches, "branches");
    Objects.requireNonNull(positions, "positions");
    branches = List.copyOf(branches);
    positions = List.copyOf(positions);
  }

  public String toText(String indent) {
    return indent
        + "group "
        + modelKind
        + " "
        + cardinality.toText()
        + " branches="
        + branches.stream().map(BindingContentBranch::javaName).collect(Collectors.joining(","))
        + " positions="
        + positions.stream().map(BindingContentPosition::toText).collect(Collectors.joining(","));
  }

  @Override
  public List<BindingContentPosition> positions() {
    return List.copyOf(positions);
  }
}
