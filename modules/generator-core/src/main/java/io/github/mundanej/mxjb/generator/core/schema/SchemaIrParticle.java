package io.github.mundanej.mxjb.generator.core.schema;

/** Normalized content-model particle inside a complex type content model. */
public sealed interface SchemaIrParticle
    permits SchemaIrElement, SchemaIrChoice, SchemaIrWildcard, SchemaIrAll {
  String toText(String indent);
}
