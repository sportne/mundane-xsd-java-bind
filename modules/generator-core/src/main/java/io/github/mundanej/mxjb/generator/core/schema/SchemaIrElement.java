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
    boolean reference)
    implements SchemaIrParticle {
  public SchemaIrElement(
      SchemaQName name,
      SchemaIrTypeReference type,
      SchemaCardinality cardinality,
      SchemaIrComplexType inlineComplexType,
      boolean reference) {
    this(name, type, cardinality, inlineComplexType, SchemaIrValueSemantics.NONE, reference);
  }

  public SchemaIrElement {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(cardinality, "cardinality");
    semantics = semantics == null ? SchemaIrValueSemantics.NONE : semantics;
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
            + semantics.toText();
    if (inlineComplexType == null) {
      return line;
    }
    return line + "\n" + inlineComplexType.toText(indent + "  ");
  }

  static String elementsText(List<SchemaIrElement> elements, String indent) {
    return elements.stream()
        .map(element -> element.toText(indent))
        .collect(Collectors.joining("\n"));
  }
}
