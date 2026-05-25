package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;

/** Accepted named simple-type union metadata in normalized IR. */
public record SchemaIrSimpleUnion(
    List<SchemaQName> memberTypes, List<SchemaIrSimpleRestriction> anonymousMemberRestrictions) {
  public SchemaIrSimpleUnion(List<SchemaQName> memberTypes) {
    this(memberTypes, List.of());
  }

  public SchemaIrSimpleUnion {
    memberTypes = List.copyOf(memberTypes);
    anonymousMemberRestrictions = List.copyOf(anonymousMemberRestrictions);
  }

  public String toText() {
    return "memberTypes="
        + memberTypes.stream()
            .map(SchemaQName::toText)
            .collect(java.util.stream.Collectors.joining(","))
        + (anonymousMemberRestrictions.isEmpty()
            ? ""
            : " anonymousMembers="
                + anonymousMemberRestrictions.stream()
                    .map(SchemaIrSimpleRestriction::toText)
                    .collect(java.util.stream.Collectors.joining("|")));
  }
}
