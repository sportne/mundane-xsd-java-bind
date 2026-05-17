package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized named attribute group declaration for the composed profile subset. */
public record SchemaIrAttributeGroup(SchemaQName name, List<SchemaIrAttribute> attributes) {
  public SchemaIrAttributeGroup {
    Objects.requireNonNull(name, "name");
    attributes = List.copyOf(attributes);
  }

  public String toText(String indent) {
    String line = indent + "attributeGroup " + name.toText();
    if (attributes.isEmpty()) {
      return line;
    }
    return line
        + "\n"
        + attributes.stream()
            .map(attribute -> attribute.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
  }
}
