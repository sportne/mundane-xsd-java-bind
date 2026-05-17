package io.github.mundanej.mxjb.generator.api;

/** Public schema-to-Java generator entry point. */
public interface Generator {
  GeneratorResult generate(GeneratorRequest request);
}
