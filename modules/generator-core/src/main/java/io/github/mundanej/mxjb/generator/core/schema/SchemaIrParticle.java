package io.github.mundanej.mxjb.generator.core.schema;

/** Normalized content-model particle inside an ordered sequence. */
public sealed interface SchemaIrParticle permits SchemaIrElement, SchemaIrChoice, SchemaIrWildcard {
  String toText(String indent);
}
