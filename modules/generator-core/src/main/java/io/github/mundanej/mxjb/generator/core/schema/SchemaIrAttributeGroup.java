package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized named attribute group declaration for the composed profile subset. */
public record SchemaIrAttributeGroup(
    SchemaQName name, List<SchemaIrAttribute> attributes, SchemaIrAnyAttribute anyAttribute) {
  public SchemaIrAttributeGroup(SchemaQName name, List<SchemaIrAttribute> attributes) {
    this(name, attributes, null);
  }

  public SchemaIrAttributeGroup {
    Objects.requireNonNull(name, "name");
    attributes = List.copyOf(attributes);
  }

  public String toText(String indent) {
    String line = indent + "attributeGroup " + name.toText();
    String attributeText =
        attributes.stream()
            .map(attribute -> attribute.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
    String anyAttributeText = anyAttribute == null ? "" : anyAttribute.toText(indent + "  ");
    return java.util.stream.Stream.of(line, attributeText, anyAttributeText)
        .filter(value -> !value.isEmpty())
        .collect(Collectors.joining("\n"));
  }
}
