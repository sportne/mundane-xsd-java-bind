package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.Objects;

/** Schema-known global attribute declaration that may satisfy a retained attribute wildcard. */
public record BindingWildcardAttribute(SchemaQName xmlName, BindingTypeReference type) {
  public BindingWildcardAttribute {
    Objects.requireNonNull(xmlName, "xmlName");
    Objects.requireNonNull(type, "type");
  }
}
