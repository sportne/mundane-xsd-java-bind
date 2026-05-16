package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.stream.Collectors;

/** Deterministic normalized schema IR for the supported phase-one subset. */
public record SchemaIrModel(
    List<SchemaIrElement> elements,
    List<SchemaIrComplexType> complexTypes,
    List<SchemaIrSimpleType> simpleTypes,
    List<SchemaIrAttribute> attributes) {
  public SchemaIrModel {
    elements = List.copyOf(elements);
    complexTypes = List.copyOf(complexTypes);
    simpleTypes = List.copyOf(simpleTypes);
    attributes = List.copyOf(attributes);
  }

  public static SchemaIrModel empty() {
    return new SchemaIrModel(List.of(), List.of(), List.of(), List.of());
  }

  public String toText() {
    String elementText =
        elements.stream().map(element -> element.toText("  ")).collect(Collectors.joining("\n"));
    String complexTypeText =
        complexTypes.stream()
            .map(complexType -> complexType.toText("  "))
            .collect(Collectors.joining("\n"));
    String simpleTypeText =
        simpleTypes.stream()
            .map(simpleType -> simpleType.toText("  "))
            .collect(Collectors.joining("\n"));
    String attributeText =
        attributes.stream()
            .map(attribute -> attribute.toText("  "))
            .collect(Collectors.joining("\n"));
    String body =
        java.util.stream.Stream.of(elementText, complexTypeText, simpleTypeText, attributeText)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.joining("\n"));
    return body.isEmpty() ? "" : "schema-ir\n" + body + "\n";
  }
}
