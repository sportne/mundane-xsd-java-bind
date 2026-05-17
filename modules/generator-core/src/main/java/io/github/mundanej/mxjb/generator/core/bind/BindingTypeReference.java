package io.github.mundanej.mxjb.generator.core.bind;

import java.util.Objects;

/** Scalar or generated-model type reference in the binding model. */
public record BindingTypeReference(String kind, String name, BindingSimpleRestriction restriction) {
  public BindingTypeReference(String kind, String name) {
    this(kind, name, null);
  }

  public BindingTypeReference {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
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

  public String toText() {
    return kind
        + ":"
        + name
        + (restriction == null || !restriction.hasRules()
            ? ""
            : " facets[" + restriction.toText() + "]");
  }
}
