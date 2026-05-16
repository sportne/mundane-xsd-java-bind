package io.github.xsdbind.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** A schema document after resolver policy has accepted its location. */
public record ResolvedSchema(
    String resourceId, String targetNamespace, List<SchemaReference> references) {
  public ResolvedSchema {
    Objects.requireNonNull(resourceId, "resourceId");
    targetNamespace = targetNamespace == null ? "" : targetNamespace;
    references = List.copyOf(references);
  }

  public String toManifestLine() {
    String referenceText =
        references.stream().map(SchemaReference::toManifestToken).collect(Collectors.joining(","));
    return resourceId + " | namespace=" + targetNamespace + " | references=[" + referenceText + "]";
  }
}
