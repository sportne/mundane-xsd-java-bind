package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** Normalized named model group declaration for the composed profile subset. */
public record SchemaIrModelGroup(SchemaQName name, SchemaIrSequence sequence) {
  public SchemaIrModelGroup {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(sequence, "sequence");
  }

  public String toText(String indent) {
    return indent + "modelGroup " + name.toText() + "\n" + sequence.toText(indent + "  ");
  }
}
