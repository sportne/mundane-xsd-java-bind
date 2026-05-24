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
    return particles.stream().flatMap(SchemaIrSequence::elementStream).toList();
  }

  public List<SchemaIrChoice> choices() {
    return particles.stream()
        .filter(SchemaIrChoice.class::isInstance)
        .map(SchemaIrChoice.class::cast)
        .toList();
  }

  public List<SchemaIrWildcard> wildcards() {
    return particles.stream()
        .filter(SchemaIrWildcard.class::isInstance)
        .map(SchemaIrWildcard.class::cast)
        .toList();
  }

  public List<SchemaIrAll> allGroups() {
    return particles.stream()
        .filter(SchemaIrAll.class::isInstance)
        .map(SchemaIrAll.class::cast)
        .toList();
  }

  private static java.util.stream.Stream<SchemaIrElement> elementStream(SchemaIrParticle particle) {
    if (particle instanceof SchemaIrElement element) {
      return java.util.stream.Stream.of(element);
    }
    if (particle instanceof SchemaIrAll all) {
      return all.elements().stream();
    }
    if (particle instanceof SchemaIrGroup group) {
      return group.particles().stream().flatMap(SchemaIrSequence::elementStream);
    }
    if (particle instanceof SchemaIrChoice choice) {
      return choice.elementBranches().stream();
    }
    return java.util.stream.Stream.empty();
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
