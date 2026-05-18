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
    BindingValueSemantics semantics,
    BindingChoice choice,
    BindingWildcard wildcard,
    BindingContent content) {
  public BindingField(
      String kind,
      SchemaQName xmlName,
      String javaName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order,
      boolean required) {
    this(kind, xmlName, javaName, type, cardinality, order, required, BindingValueSemantics.NONE);
  }

  public BindingField(
      String kind,
      SchemaQName xmlName,
      String javaName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order,
      boolean required,
      BindingValueSemantics semantics) {
    this(kind, xmlName, javaName, type, cardinality, order, required, semantics, null, null, null);
  }

  public BindingField(
      String kind,
      SchemaQName xmlName,
      String javaName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order,
      boolean required,
      BindingChoice choice) {
    this(
        kind,
        xmlName,
        javaName,
        type,
        cardinality,
        order,
        required,
        BindingValueSemantics.NONE,
        choice,
        null,
        null);
  }

  public BindingField(
      String kind,
      SchemaQName xmlName,
      String javaName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order,
      boolean required,
      BindingWildcard wildcard) {
    this(
        kind,
        xmlName,
        javaName,
        type,
        cardinality,
        order,
        required,
        BindingValueSemantics.NONE,
        null,
        wildcard,
        null);
  }

  public BindingField(
      String kind,
      SchemaQName xmlName,
      String javaName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order,
      boolean required,
      BindingContent content) {
    this(
        kind,
        xmlName,
        javaName,
        type,
        cardinality,
        order,
        required,
        BindingValueSemantics.NONE,
        null,
        null,
        content);
  }

  public BindingField {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(xmlName, "xmlName");
    Objects.requireNonNull(javaName, "javaName");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(cardinality, "cardinality");
    semantics = semantics == null ? BindingValueSemantics.NONE : semantics;
  }

  public String toText(String indent) {
    String base =
        indent
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
            + semantics.toText()
            + (choice == null ? "" : "\n" + choice.toText(indent + "  "));
    return base
        + (wildcard == null ? "" : "\n" + wildcard.toText(indent + "  "))
        + (content == null ? "" : "\n" + content.toText(indent + "  "));
  }
}
