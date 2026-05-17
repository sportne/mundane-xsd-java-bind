package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;

/** Accepted named simple-type union metadata in normalized IR. */
public record SchemaIrSimpleUnion(List<SchemaQName> memberTypes) {
  public SchemaIrSimpleUnion {
    memberTypes = List.copyOf(memberTypes);
  }

  public String toText() {
    return "memberTypes="
        + memberTypes.stream()
            .map(SchemaQName::toText)
            .collect(java.util.stream.Collectors.joining(","));
  }
}
