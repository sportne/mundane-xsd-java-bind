package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.Objects;

/** One generated branch record in a mixed-content sealed model. */
public record BindingContentBranch(
    String kind,
    SchemaQName xmlName,
    String javaName,
    BindingTypeReference type,
    BindingJavaName branchJavaName,
    BindingCardinality cardinality,
    int order,
    BindingWildcard wildcard) {
  public BindingContentBranch {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(xmlName, "xmlName");
    Objects.requireNonNull(javaName, "javaName");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(branchJavaName, "branchJavaName");
    Objects.requireNonNull(cardinality, "cardinality");
  }

  public String toText(String indent) {
    return indent
        + "branch "
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
        + " model="
        + branchJavaName.qualifiedName()
        + (wildcard == null ? "" : "\n" + wildcard.toText(indent + "  "));
  }
}
