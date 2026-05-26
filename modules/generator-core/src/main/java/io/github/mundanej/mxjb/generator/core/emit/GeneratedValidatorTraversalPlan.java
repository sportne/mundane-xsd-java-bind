package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingChoiceBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import java.util.Comparator;
import java.util.List;

record GeneratedValidatorTraversalPlan(
    BindingJavaName ownerName,
    List<BindingField> fields,
    List<BindingField> elementFields,
    List<BindingField> choiceFields,
    List<BindingField> contentFields) {
  GeneratedValidatorTraversalPlan {
    fields = List.copyOf(fields);
    elementFields = List.copyOf(elementFields);
    choiceFields = List.copyOf(choiceFields);
    contentFields = List.copyOf(contentFields);
  }

  List<BindingChoiceBranch> choiceBranches(BindingField field) {
    if (!"choice".equals(field.kind()) || field.choice() == null) {
      return List.of();
    }
    return field.choice().branches();
  }

  List<BindingContentBranch> contentBranches(BindingField field) {
    if (!"content".equals(field.kind()) || field.content() == null) {
      return List.of();
    }
    return field.content().branches();
  }

  static GeneratedValidatorTraversalPlan from(BindingType type) {
    return new GeneratedValidatorTraversalPlan(
        type.javaName(),
        type.fields().stream().sorted(fieldComparator()).toList(),
        type.fields().stream()
            .filter(field -> "element".equals(field.kind()))
            .sorted(Comparator.comparingInt(BindingField::order))
            .toList(),
        type.fields().stream().filter(field -> "choice".equals(field.kind())).toList(),
        type.fields().stream().filter(field -> "content".equals(field.kind())).toList());
  }

  private static Comparator<BindingField> fieldComparator() {
    return Comparator.comparingInt((BindingField field) -> "attribute".equals(field.kind()) ? 0 : 1)
        .thenComparingInt(BindingField::order)
        .thenComparing(BindingField::javaName);
  }
}
