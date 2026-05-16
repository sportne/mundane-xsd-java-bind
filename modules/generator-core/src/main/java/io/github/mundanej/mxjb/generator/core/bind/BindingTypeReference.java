package io.github.mundanej.mxjb.generator.core.bind;

import java.util.Objects;

/** Scalar or generated-model type reference in the binding model. */
public record BindingTypeReference(String kind, String name) {
  public BindingTypeReference {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
  }

  static BindingTypeReference scalar(String name) {
    return new BindingTypeReference("scalar", name);
  }

  static BindingTypeReference model(BindingJavaName name) {
    return new BindingTypeReference("model", name.qualifiedName());
  }

  public String toText() {
    return kind + ":" + name;
  }
}
