package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized element declaration or reference. */
public record SchemaIrElement(
    SchemaQName name,
    SchemaIrTypeReference type,
    SchemaCardinality cardinality,
    SchemaIrComplexType inlineComplexType,
    SchemaIrValueSemantics semantics,
    SchemaQName substitutionGroup,
    boolean abstractElement,
    List<SchemaIrIdentityConstraint> identityConstraints,
    boolean reference)
    implements SchemaIrParticle {
  public SchemaIrElement(
      SchemaQName name,
      SchemaIrTypeReference type,
      SchemaCardinality cardinality,
      SchemaIrComplexType inlineComplexType,
      boolean reference) {
    this(
        name,
        type,
        cardinality,
        inlineComplexType,
        SchemaIrValueSemantics.NONE,
        null,
        false,
        List.of(),
        reference);
  }

  public SchemaIrElement {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(cardinality, "cardinality");
    semantics = semantics == null ? SchemaIrValueSemantics.NONE : semantics;
    identityConstraints =
        identityConstraints == null ? List.of() : List.copyOf(identityConstraints);
  }

  @Override
  public String toText(String indent) {
    String prefix = reference ? "elementRef " : "element ";
    String line =
        indent
            + prefix
            + name.toText()
            + " type="
            + type.toText()
            + " cardinality="
            + cardinality.toText()
            + (substitutionGroup == null ? "" : " substitutionGroup=" + substitutionGroup.toText())
            + (abstractElement ? " abstract=true" : "")
            + semantics.toText();
    String identityText =
        identityConstraints.stream()
            .map(identity -> identity.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
    String inlineText = inlineComplexType == null ? "" : inlineComplexType.toText(indent + "  ");
    return java.util.stream.Stream.of(line, identityText, inlineText)
        .filter(value -> !value.isEmpty())
        .collect(Collectors.joining("\n"));
  }

  static String elementsText(List<SchemaIrElement> elements, String indent) {
    return elements.stream()
        .map(element -> element.toText(indent))
        .collect(Collectors.joining("\n"));
  }
}
