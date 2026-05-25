package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import java.util.Comparator;
import java.util.List;

record GeneratedWriterTraversalPlan(
    BindingJavaName ownerName,
    List<BindingField> attributeFields,
    List<BindingField> anyAttributeFields,
    List<BindingField> simpleContentFields,
    List<BindingField> contentFields) {
  GeneratedWriterTraversalPlan {
    attributeFields = List.copyOf(attributeFields);
    anyAttributeFields = List.copyOf(anyAttributeFields);
    simpleContentFields = List.copyOf(simpleContentFields);
    contentFields = List.copyOf(contentFields);
  }

  static GeneratedWriterTraversalPlan from(BindingType type) {
    return new GeneratedWriterTraversalPlan(
        type.javaName(),
        fields(type, "attribute"),
        fields(type, "anyAttribute"),
        fields(type, "simpleContent"),
        type.fields().stream()
            .filter(GeneratedWriterTraversalPlan::isContentField)
            .sorted(Comparator.comparingInt(BindingField::order))
            .toList());
  }

  private static List<BindingField> fields(BindingType type, String kind) {
    return type.fields().stream()
        .filter(field -> kind.equals(field.kind()))
        .sorted(Comparator.comparingInt(BindingField::order))
        .toList();
  }

  private static boolean isContentField(BindingField field) {
    return "element".equals(field.kind())
        || "choice".equals(field.kind())
        || "wildcard".equals(field.kind())
        || "content".equals(field.kind());
  }
}
