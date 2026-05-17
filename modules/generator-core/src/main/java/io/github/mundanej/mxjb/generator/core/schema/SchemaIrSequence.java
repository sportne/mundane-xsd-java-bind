package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;

/** Normalized ordered sequence particle. */
public record SchemaIrSequence(SchemaCardinality cardinality, List<SchemaIrParticle> particles) {
  public SchemaIrSequence {
    Objects.requireNonNull(cardinality, "cardinality");
    particles = List.copyOf(particles);
  }

  public List<SchemaIrElement> elements() {
    return particles.stream()
        .filter(SchemaIrElement.class::isInstance)
        .map(SchemaIrElement.class::cast)
        .toList();
  }

  public List<SchemaIrChoice> choices() {
    return particles.stream()
        .filter(SchemaIrChoice.class::isInstance)
        .map(SchemaIrChoice.class::cast)
        .toList();
  }

  public String toText(String indent) {
    String line = indent + "sequence cardinality=" + cardinality.toText();
    if (particles.isEmpty()) {
      return line;
    }
    return line
        + "\n"
        + particles.stream()
            .map(particle -> particle.toText(indent + "  "))
            .collect(java.util.stream.Collectors.joining("\n"));
  }
}
