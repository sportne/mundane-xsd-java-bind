package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.stream.Collectors;

/** Deterministic raw syntax model for resolver-approved XSD documents. */
public record XsdSyntaxModel(List<XsdSyntaxDocument> documents) {
  public XsdSyntaxModel {
    documents = List.copyOf(documents);
  }

  public String toText() {
    if (documents.isEmpty()) {
      return "";
    }
    return documents.stream()
        .map(XsdSyntaxDocument::toText)
        .collect(Collectors.joining("\n\n", "", "\n"));
  }
}
