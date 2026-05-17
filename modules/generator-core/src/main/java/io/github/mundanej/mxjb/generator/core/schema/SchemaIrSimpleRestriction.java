package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Accepted named simple-type restriction metadata in normalized IR. */
public record SchemaIrSimpleRestriction(
    SchemaQName base,
    List<String> enumerations,
    Integer length,
    Integer minLength,
    Integer maxLength,
    String minInclusive,
    String maxInclusive,
    List<String> patterns) {
  public SchemaIrSimpleRestriction {
    Objects.requireNonNull(base, "base");
    enumerations = List.copyOf(enumerations);
    patterns = List.copyOf(patterns);
  }

  public String toText() {
    return java.util.stream.Stream.of(
            "base=" + base.toText(),
            listText("enumeration", enumerations),
            valueText("length", length),
            valueText("minLength", minLength),
            valueText("maxLength", maxLength),
            valueText("minInclusive", minInclusive),
            valueText("maxInclusive", maxInclusive),
            listText("pattern", patterns))
        .filter(value -> !value.isEmpty())
        .collect(Collectors.joining(" "));
  }

  private static String listText(String name, List<String> values) {
    return values.isEmpty() ? "" : name + "=" + String.join(",", values);
  }

  private static String valueText(String name, Object value) {
    return value == null ? "" : name + "=" + value;
  }
}
