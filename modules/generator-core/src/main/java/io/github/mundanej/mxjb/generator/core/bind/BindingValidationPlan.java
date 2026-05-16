package io.github.mundanej.mxjb.generator.core.bind;

import java.util.List;
import java.util.stream.Collectors;

/** Validation metadata captured before validation source generation exists. */
public record BindingValidationPlan(List<String> rules) {
  public BindingValidationPlan {
    rules = List.copyOf(rules);
  }

  public String toText(String indent) {
    if (rules.isEmpty()) {
      return "";
    }
    return indent
        + "validation\n"
        + rules.stream().map(rule -> indent + "  rule " + rule).collect(Collectors.joining("\n"));
  }
}
