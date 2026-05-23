package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.List;
import java.util.Objects;

/** Binding metadata for a generated wildcard/open-content field. */
public record BindingWildcard(
    SchemaIrWildcardNamespace namespaceConstraint,
    String processContents,
    List<SchemaQName> excludedNames) {
  public BindingWildcard(SchemaIrWildcardNamespace namespaceConstraint) {
    this(namespaceConstraint, "strict", List.of());
  }

  public BindingWildcard(SchemaIrWildcardNamespace namespaceConstraint, String processContents) {
    this(namespaceConstraint, processContents, List.of());
  }

  public BindingWildcard {
    Objects.requireNonNull(namespaceConstraint, "namespaceConstraint");
    processContents =
        processContents == null || processContents.isBlank() ? "strict" : processContents;
    excludedNames = List.copyOf(excludedNames == null ? List.of() : excludedNames);
  }

  public String toText(String indent) {
    return indent
        + "wildcard namespace="
        + namespaceConstraint.toText()
        + " processContents="
        + processContents;
  }
}
