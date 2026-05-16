package io.github.mundanej.mxjb.generator.core.bind;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Deterministic internal binding model for source-emission planning. */
public record BindingModel(List<BindingRootElement> rootElements, List<BindingType> types) {
  public BindingModel {
    rootElements = List.copyOf(rootElements);
    types = List.copyOf(types);
  }

  public static BindingModel empty() {
    return new BindingModel(List.of(), List.of());
  }

  public String toText() {
    String rootText =
        rootElements.stream()
            .sorted(Comparator.comparing(root -> root.xmlName().toText()))
            .map(root -> root.toText("  "))
            .collect(Collectors.joining("\n"));
    String typeText =
        types.stream()
            .sorted(Comparator.comparing(type -> type.javaName().qualifiedName()))
            .map(type -> type.toText("  "))
            .collect(Collectors.joining("\n"));
    String body =
        java.util.stream.Stream.of(rootText, typeText)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.joining("\n"));
    return body.isEmpty() ? "" : "binding-model\n" + body + "\n";
  }
}
