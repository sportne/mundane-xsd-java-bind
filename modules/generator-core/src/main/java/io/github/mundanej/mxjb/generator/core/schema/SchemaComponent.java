package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** A global schema component tied back to its raw syntax node. */
public record SchemaComponent(SchemaComponentKey key, String resourceId, XsdSyntaxNode syntaxNode) {
  public SchemaComponent {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(resourceId, "resourceId");
    Objects.requireNonNull(syntaxNode, "syntaxNode");
  }

  public String toText() {
    return key.toText() + " @ " + resourceId;
  }
}
