package io.github.mundanej.mxjb.generator.core.bind;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Internal binding configuration used before public generator configuration exists. */
public record BindingConfiguration(String defaultPackage, Map<String, String> namespacePackages) {
  public static final String DEFAULT_GENERATED_PACKAGE = "io.github.mundanej.mxjb.generated";

  public BindingConfiguration {
    Objects.requireNonNull(defaultPackage, "defaultPackage");
    namespacePackages = Map.copyOf(namespacePackages);
  }

  public static BindingConfiguration defaults() {
    return new BindingConfiguration(DEFAULT_GENERATED_PACKAGE, Map.of());
  }

  public static BindingConfiguration withNamespacePackages(Map<String, String> namespacePackages) {
    return new BindingConfiguration(
        DEFAULT_GENERATED_PACKAGE, new LinkedHashMap<>(namespacePackages));
  }
}
