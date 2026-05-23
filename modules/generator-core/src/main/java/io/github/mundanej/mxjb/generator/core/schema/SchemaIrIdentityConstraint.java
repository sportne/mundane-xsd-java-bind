package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized XSD 1.0 unique/key/keyref metadata. */
public record SchemaIrIdentityConstraint(
    String kind,
    SchemaQName name,
    SchemaQName refer,
    List<SchemaIrIdentityPath> selectors,
    List<SchemaIrIdentityField> fields) {
  public SchemaIrIdentityConstraint {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
    selectors = List.copyOf(selectors);
    fields = List.copyOf(fields);
  }

  public String toText(String indent) {
    String selectorText =
        selectors.stream().map(SchemaIrIdentityPath::toText).collect(Collectors.joining("|"));
    String fieldText =
        fields.stream().map(SchemaIrIdentityField::toText).collect(Collectors.joining(" "));
    return indent
        + kind
        + " "
        + name.toText()
        + (refer == null ? "" : " refer=" + refer.toText())
        + " selector="
        + selectorText
        + " fields="
        + fieldText;
  }
}
