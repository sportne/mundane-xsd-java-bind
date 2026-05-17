package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.Objects;

/** A Java field planned from a supported XML element or attribute. */
public record BindingField(
    String kind,
    SchemaQName xmlName,
    String javaName,
    BindingTypeReference type,
    BindingCardinality cardinality,
    int order,
    boolean required,
    BindingChoice choice) {
  public BindingField(
      String kind,
      SchemaQName xmlName,
      String javaName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order,
      boolean required) {
    this(kind, xmlName, javaName, type, cardinality, order, required, null);
  }

  public BindingField {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(xmlName, "xmlName");
    Objects.requireNonNull(javaName, "javaName");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(cardinality, "cardinality");
  }

  public String toText(String indent) {
    return indent
        + kind
        + " "
        + javaName
        + " xml="
        + xmlName.toText()
        + " type="
        + type.toText()
        + " cardinality="
        + cardinality.toText()
        + " order="
        + order
        + " required="
        + required
        + (choice == null ? "" : "\n" + choice.toText(indent + "  "));
  }
}
