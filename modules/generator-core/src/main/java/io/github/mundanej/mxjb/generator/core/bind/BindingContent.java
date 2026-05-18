package io.github.mundanej.mxjb.generator.core.bind;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Generated sealed content-list type planned from accepted mixed content. */
public record BindingContent(BindingJavaName javaName, List<BindingContentBranch> branches) {
  public BindingContent {
    Objects.requireNonNull(javaName, "javaName");
    branches = List.copyOf(branches);
  }

  public String toText(String indent) {
    String line = indent + "contentType " + javaName.qualifiedName();
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
