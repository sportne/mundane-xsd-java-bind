package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized complex type with attributes and ordered sequences. */
public record SchemaIrComplexType(
    SchemaQName name,
    SchemaIrSimpleContent simpleContent,
    List<SchemaIrAttribute> attributes,
    SchemaIrAnyAttribute anyAttribute,
    List<SchemaIrSequence> sequences,
    boolean mixed,
    boolean anonymous) {
  public SchemaIrComplexType(
      SchemaQName name,
      List<SchemaIrAttribute> attributes,
      List<SchemaIrSequence> sequences,
      boolean mixed,
      boolean anonymous) {
    this(name, null, attributes, null, sequences, mixed, anonymous);
  }

  public SchemaIrComplexType(
      SchemaQName name,
      List<SchemaIrAttribute> attributes,
      SchemaIrAnyAttribute anyAttribute,
      List<SchemaIrSequence> sequences,
      boolean mixed,
      boolean anonymous) {
    this(name, null, attributes, anyAttribute, sequences, mixed, anonymous);
  }

  public SchemaIrComplexType {
    if (!anonymous) {
      Objects.requireNonNull(name, "name");
    }
    attributes = List.copyOf(attributes);
    sequences = List.copyOf(sequences);
  }

  public String toText(String indent) {
    String line =
        indent
            + "complexType "
            + (anonymous ? "anonymous" : name.toText())
            + (mixed ? " mixed=true" : "");
    String sequenceText =
        sequences.stream()
            .map(sequence -> sequence.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
    String simpleContentText = simpleContent == null ? "" : simpleContent.toText(indent + "  ");
    String attributeText =
        attributes.stream()
            .map(attribute -> attribute.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
    String anyAttributeText = anyAttribute == null ? "" : anyAttribute.toText(indent + "  ");
    return java.util.stream.Stream.of(
            line, simpleContentText, sequenceText, attributeText, anyAttributeText)
        .filter(value -> !value.isEmpty())
        .collect(Collectors.joining("\n"));
  }
}
