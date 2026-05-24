package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.Objects;

/** One generated branch record in a sealed choice model. */
public record BindingChoiceBranch(
    SchemaQName xmlName,
    String javaName,
    BindingTypeReference type,
    BindingJavaName branchJavaName,
    SchemaQName dynamicTypeName,
    boolean defaultDynamicType) {
  public BindingChoiceBranch(
      SchemaQName xmlName,
      String javaName,
      BindingTypeReference type,
      BindingJavaName branchJavaName) {
    this(xmlName, javaName, type, branchJavaName, null, false);
  }

  public BindingChoiceBranch {
    Objects.requireNonNull(xmlName, "xmlName");
    Objects.requireNonNull(javaName, "javaName");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(branchJavaName, "branchJavaName");
  }

  public String toText(String indent) {
    return indent
        + "branch "
        + javaName
        + " xml="
        + xmlName.toText()
        + " type="
        + type.toText()
        + " model="
        + branchJavaName.qualifiedName()
        + (dynamicTypeName == null ? "" : " dynamicType=" + dynamicTypeName.toText())
        + (defaultDynamicType ? " defaultDynamicType=true" : "");
  }
}
