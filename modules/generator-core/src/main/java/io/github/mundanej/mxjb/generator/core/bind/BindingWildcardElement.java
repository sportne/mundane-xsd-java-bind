package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.Objects;

/** Schema-known global element declaration that may satisfy a retained wildcard. */
public record BindingWildcardElement(SchemaQName xmlName, BindingTypeReference type) {
  public BindingWildcardElement {
    Objects.requireNonNull(xmlName, "xmlName");
    Objects.requireNonNull(type, "type");
  }
}
