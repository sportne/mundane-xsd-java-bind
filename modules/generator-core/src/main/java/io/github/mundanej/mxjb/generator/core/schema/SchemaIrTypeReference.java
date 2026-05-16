package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** A normalized reference to a schema type or inline anonymous type. */
public record SchemaIrTypeReference(SchemaQName name, boolean anonymous) {
  public SchemaIrTypeReference {
    if (!anonymous) {
      Objects.requireNonNull(name, "name");
    }
  }

  public static SchemaIrTypeReference named(SchemaQName name) {
    return new SchemaIrTypeReference(name, false);
  }

  public static SchemaIrTypeReference anonymousType() {
    return new SchemaIrTypeReference(null, true);
  }

  public String toText() {
    return anonymous ? "anonymous" : name.toText();
  }
}
