package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized grouped particle that requires an explicit generated content-list shape. */
public record SchemaIrGroup(
    String modelKind, SchemaCardinality cardinality, List<SchemaIrParticle> particles)
    implements SchemaIrParticle {
  public SchemaIrGroup {
    modelKind = modelKind == null || modelKind.isBlank() ? "group" : modelKind;
    Objects.requireNonNull(cardinality, "cardinality");
    particles = List.copyOf(particles);
  }

  @Override
  public String toText(String indent) {
    String line = indent + modelKind + "Group cardinality=" + cardinality.toText();
    if (particles.isEmpty()) {
      return line;
    }
    return line
        + "\n"
        + particles.stream()
            .map(particle -> particle.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
  }
}
