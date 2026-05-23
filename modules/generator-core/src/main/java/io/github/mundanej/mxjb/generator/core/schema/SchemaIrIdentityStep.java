package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** One normalized step in an accepted identity-constraint XPath subset path. */
public record SchemaIrIdentityStep(SchemaQName name, boolean wildcard, boolean attribute) {
  public SchemaIrIdentityStep {
    if (!wildcard) {
      Objects.requireNonNull(name, "name");
    }
  }

  public String toText() {
    if (wildcard) {
      return attribute ? "@*" : "*";
    }
    return attribute ? "@" + name.toText() : name.toText();
  }
}
