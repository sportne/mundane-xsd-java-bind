package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import java.util.Objects;

record GeneratedEmitterFieldPlan(BindingJavaName ownerName, BindingField field) {
  GeneratedEmitterFieldPlan {
    Objects.requireNonNull(ownerName, "ownerName");
    Objects.requireNonNull(field, "field");
  }
}
