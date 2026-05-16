package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** A resolved include or import edge discovered in an XSD document. */
public record SchemaReference(SchemaReferenceKind kind, String namespace, String target) {
  public SchemaReference {
    Objects.requireNonNull(kind, "kind");
    namespace = namespace == null ? "" : namespace;
    Objects.requireNonNull(target, "target");
  }

  public String toManifestToken() {
    if (namespace.isBlank()) {
      return kind.manifestName() + ":" + target;
    }
    return kind.manifestName() + ":" + namespace + "->" + target;
  }
}
