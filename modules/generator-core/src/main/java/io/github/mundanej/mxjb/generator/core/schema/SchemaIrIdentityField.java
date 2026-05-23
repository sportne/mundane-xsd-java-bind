package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.stream.Collectors;

/** One XSD identity-constraint field with its accepted XPath alternatives. */
public record SchemaIrIdentityField(List<SchemaIrIdentityPath> alternatives) {
  public SchemaIrIdentityField {
    alternatives = List.copyOf(alternatives);
  }

  public String toText() {
    return alternatives.stream().map(SchemaIrIdentityPath::toText).collect(Collectors.joining("|"));
  }
}
