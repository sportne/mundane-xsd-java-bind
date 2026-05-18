package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace;
import java.util.Objects;

/** Binding metadata for a generated wildcard/open-content field. */
public record BindingWildcard(SchemaIrWildcardNamespace namespaceConstraint) {
  public BindingWildcard {
    Objects.requireNonNull(namespaceConstraint, "namespaceConstraint");
  }

  public String toText(String indent) {
    return indent + "wildcard namespace=" + namespaceConstraint.toText();
  }
}
