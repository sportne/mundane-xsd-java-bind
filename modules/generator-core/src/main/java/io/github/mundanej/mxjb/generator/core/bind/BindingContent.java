package io.github.mundanej.mxjb.generator.core.bind;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Generated sealed content-list type planned from accepted ordered content. */
public record BindingContent(
    BindingJavaName javaName,
    List<BindingContentBranch> branches,
    String modelKind,
    List<BindingContentGroup> groups) {
  public BindingContent(BindingJavaName javaName, List<BindingContentBranch> branches) {
    this(javaName, branches, "mixed content");
  }

  public BindingContent(
      BindingJavaName javaName, List<BindingContentBranch> branches, String modelKind) {
    this(javaName, branches, modelKind, List.of());
  }

  public BindingContent {
    Objects.requireNonNull(javaName, "javaName");
    branches = List.copyOf(branches);
    modelKind = modelKind == null || modelKind.isBlank() ? "content" : modelKind;
    groups = List.copyOf(groups);
  }

  public String toText(String indent) {
    String line = indent + modelKind + "Type " + javaName.qualifiedName();
    if (branches.isEmpty()) {
      return line;
    }
    return line
        + "\n"
        + java.util.stream.Stream.concat(
                groups.stream().map(group -> group.toText(indent + "  ")),
                branches.stream().map(branch -> branch.toText(indent + "  ")))
            .collect(Collectors.joining("\n"));
  }
}
