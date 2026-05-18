package io.github.mundanej.mxjb.generator.core.bind;

import java.util.List;
import java.util.Objects;

/** Scalar or generated-model type reference in the binding model. */
public record BindingTypeReference(
    String kind,
    String name,
    BindingSimpleRestriction restriction,
    BindingTypeReference itemType,
    List<BindingTypeReference> unionMembers) {
  public BindingTypeReference(String kind, String name) {
    this(kind, name, null);
  }

  public BindingTypeReference(String kind, String name, BindingSimpleRestriction restriction) {
    this(kind, name, restriction, null, List.of());
  }

  public BindingTypeReference {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
    unionMembers = List.copyOf(unionMembers);
  }

  static BindingTypeReference scalar(String name) {
    return new BindingTypeReference("scalar", name);
  }

  static BindingTypeReference scalar(String name, BindingSimpleRestriction restriction) {
    return new BindingTypeReference("scalar", name, restriction);
  }

  static BindingTypeReference model(BindingJavaName name) {
    return new BindingTypeReference("model", name.qualifiedName());
  }

  static BindingTypeReference choice(BindingJavaName name) {
    return new BindingTypeReference("choice", name.qualifiedName());
  }

  static BindingTypeReference list(BindingTypeReference itemType) {
    return new BindingTypeReference("list", itemType.name(), null, itemType, List.of());
  }

  static BindingTypeReference union(List<BindingTypeReference> members) {
    return new BindingTypeReference("union", "string", null, null, members);
  }

  static BindingTypeReference fragment() {
    return new BindingTypeReference("fragment", "io.github.mundanej.mxjb.runtime.XmlFragment");
  }

  public String toText() {
    if ("list".equals(kind)) {
      return "list:" + itemType.toText();
    }
    if ("union".equals(kind)) {
      return "union:"
          + unionMembers.stream()
              .map(BindingTypeReference::toText)
              .collect(java.util.stream.Collectors.joining("|"));
    }
    return kind
        + ":"
        + name
        + (restriction == null || !restriction.hasRules()
            ? ""
            : " facets[" + restriction.toText() + "]");
  }
}
