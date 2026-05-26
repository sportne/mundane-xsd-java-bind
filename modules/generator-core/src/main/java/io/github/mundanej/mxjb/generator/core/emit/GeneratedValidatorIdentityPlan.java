package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityConstraint;
import java.util.List;

record GeneratedValidatorIdentityPlan(
    BindingRootElement root, BindingType rootType, List<SchemaIrIdentityConstraint> constraints) {
  GeneratedValidatorIdentityPlan {
    constraints = List.copyOf(constraints);
  }

  static GeneratedValidatorIdentityPlan from(BindingRootElement root, BindingType rootType) {
    return new GeneratedValidatorIdentityPlan(root, rootType, root.identityConstraints());
  }

  boolean hasConstraints() {
    return !constraints.isEmpty();
  }
}
