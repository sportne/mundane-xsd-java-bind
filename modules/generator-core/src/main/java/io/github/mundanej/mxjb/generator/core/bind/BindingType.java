package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Planned generated Java model type. */
public record BindingType(
    BindingJavaName javaName,
    SchemaQName schemaName,
    String shape,
    List<BindingField> fields,
    BindingValidationPlan validationPlan) {
  public BindingType {
    Objects.requireNonNull(javaName, "javaName");
    Objects.requireNonNull(shape, "shape");
    fields = List.copyOf(fields);
    Objects.requireNonNull(validationPlan, "validationPlan");
  }

  public String toText(String indent) {
    String line =
        indent
            + "type "
            + javaName.qualifiedName()
            + " shape="
            + shape
            + (schemaName == null ? "" : " schema=" + schemaName.toText());
    String fieldText =
        fields.stream().map(field -> field.toText(indent + "  ")).collect(Collectors.joining("\n"));
    String validationText = validationPlan.toText(indent + "  ");
    return java.util.stream.Stream.of(line, fieldText, validationText)
        .filter(value -> !value.isEmpty())
        .collect(Collectors.joining("\n"));
  }
}
