package io.github.mundanej.mxjb.generator.core.bind;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Validator metadata for a restricted scalar alias. */
public record BindingSimpleRestriction(
    String baseScalar,
    List<String> enumerations,
    Integer length,
    Integer minLength,
    Integer maxLength,
    String minInclusive,
    String maxInclusive,
    List<String> patterns) {
  public BindingSimpleRestriction {
    Objects.requireNonNull(baseScalar, "baseScalar");
    enumerations = List.copyOf(enumerations);
    patterns = List.copyOf(patterns);
  }

  public boolean hasRules() {
    return !enumerations.isEmpty()
        || length != null
        || minLength != null
        || maxLength != null
        || minInclusive != null
        || maxInclusive != null
        || !patterns.isEmpty();
  }

  public String toText() {
    if (!hasRules()) {
      return "";
    }
    return java.util.stream.Stream.of(
            listText("enumeration", enumerations),
            valueText("length", length),
            valueText("minLength", minLength),
            valueText("maxLength", maxLength),
            valueText("minInclusive", minInclusive),
            valueText("maxInclusive", maxInclusive),
            listText("pattern", patterns))
        .filter(value -> !value.isEmpty())
        .collect(Collectors.joining(","));
  }

  private static String listText(String name, List<String> values) {
    return values.isEmpty() ? "" : name + "=" + String.join("|", values);
  }

  private static String valueText(String name, Object value) {
    return value == null ? "" : name + "=" + value;
  }
}
