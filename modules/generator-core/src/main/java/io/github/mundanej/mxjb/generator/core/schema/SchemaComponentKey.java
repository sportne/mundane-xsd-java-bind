package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** Stable key for a global schema component. */
public record SchemaComponentKey(SchemaComponentKind kind, SchemaQName name)
    implements Comparable<SchemaComponentKey> {
  public SchemaComponentKey {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
  }

  public String toText() {
    return kind.manifestName() + " " + name.toText();
  }

  @Override
  public int compareTo(SchemaComponentKey other) {
    int kindComparison = kind.compareTo(other.kind);
    if (kindComparison != 0) {
      return kindComparison;
    }
    return name.compareTo(other.name);
  }
}
