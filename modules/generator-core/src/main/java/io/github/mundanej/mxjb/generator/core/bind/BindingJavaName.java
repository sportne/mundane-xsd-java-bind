package io.github.mundanej.mxjb.generator.core.bind;

import java.util.Objects;

/** Fully qualified generated Java type name. */
public record BindingJavaName(String packageName, String simpleName) {
  public BindingJavaName {
    Objects.requireNonNull(packageName, "packageName");
    Objects.requireNonNull(simpleName, "simpleName");
  }

  public String qualifiedName() {
    return packageName + "." + simpleName;
  }
}
