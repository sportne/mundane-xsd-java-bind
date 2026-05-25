package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingContentBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import java.util.List;

record GeneratedWriterContentTraversalPlan(
    BindingField field, List<BindingContentBranch> branches) {
  GeneratedWriterContentTraversalPlan {
    branches = List.copyOf(branches);
  }

  static GeneratedWriterContentTraversalPlan from(BindingField field) {
    if (!"content".equals(field.kind()) || field.content() == null) {
      return new GeneratedWriterContentTraversalPlan(field, List.of());
    }
    return new GeneratedWriterContentTraversalPlan(field, field.content().branches());
  }
}
