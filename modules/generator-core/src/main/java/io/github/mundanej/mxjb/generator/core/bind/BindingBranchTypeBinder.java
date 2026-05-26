package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaIrElement;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrTypeReference;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;

@FunctionalInterface
interface BindingBranchTypeBinder {
  BindingTypeReference bind(
      SchemaIrTypeReference type, SchemaIrElement declaration, SchemaQName contextName);
}
