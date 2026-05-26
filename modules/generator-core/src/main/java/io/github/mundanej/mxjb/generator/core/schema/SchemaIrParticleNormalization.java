package io.github.mundanej.mxjb.generator.core.schema;

import java.util.ArrayList;
import java.util.List;

/** Package-private particle normalization helpers used by {@link SchemaIrBuilder}. */
final class SchemaIrParticleNormalization {
  private SchemaIrParticleNormalization() {}

  static boolean containsGroupReference(XsdSyntaxNode node) {
    return node.children().stream().anyMatch(child -> child.kind() == XsdSyntaxKind.GROUP);
  }

  static void addFlattenedNestedSequence(
      List<SchemaIrParticle> particles, SchemaIrSequence nested) {
    if (nested.cardinality().minOccurs() == 1 && "1".equals(nested.cardinality().maxOccurs())) {
      particles.addAll(nested.particles());
      return;
    }
    if (nested.particles().size() == 1) {
      particles.add(withCardinality(nested.particles().getFirst(), nested.cardinality()));
      return;
    }
    particles.add(new SchemaIrGroup("sequence", nested.cardinality(), nested.particles()));
  }

  static SchemaIrParticle withCardinality(
      SchemaIrParticle particle, SchemaCardinality cardinality) {
    if (particle instanceof SchemaIrElement element) {
      return new SchemaIrElement(
          element.name(),
          element.type(),
          SchemaIrNormalizationPolicy.composeCardinality(cardinality, element.cardinality()),
          element.inlineComplexType(),
          element.semantics(),
          element.substitutionGroup(),
          element.abstractElement(),
          element.blockControls(),
          element.identityConstraints(),
          element.reference());
    }
    if (particle instanceof SchemaIrChoice choice) {
      return new SchemaIrChoice(
          SchemaIrNormalizationPolicy.composeCardinality(cardinality, choice.cardinality()),
          choice.branches());
    }
    if (particle instanceof SchemaIrWildcard wildcard) {
      return new SchemaIrWildcard(
          SchemaIrNormalizationPolicy.composeCardinality(cardinality, wildcard.cardinality()),
          wildcard.namespaceConstraint(),
          wildcard.processContents());
    }
    if (particle instanceof SchemaIrAll all) {
      return new SchemaIrAll(
          SchemaIrNormalizationPolicy.composeCardinality(cardinality, all.cardinality()),
          all.elements());
    }
    if (particle instanceof SchemaIrGroup group) {
      return new SchemaIrGroup(
          group.modelKind(),
          SchemaIrNormalizationPolicy.composeCardinality(cardinality, group.cardinality()),
          group.particles());
    }
    return particle;
  }

  static WildcardAmbiguityInputs wildcardAmbiguityInputs(List<SchemaIrParticle> particles) {
    List<SchemaQName> elementNames = new ArrayList<>();
    List<SchemaIrWildcard> wildcards = new ArrayList<>();
    collectWildcardAmbiguityInputs(particles, elementNames, wildcards);
    return new WildcardAmbiguityInputs(elementNames, wildcards);
  }

  private static void collectWildcardAmbiguityInputs(
      List<SchemaIrParticle> particles,
      List<SchemaQName> elementNames,
      List<SchemaIrWildcard> wildcards) {
    for (SchemaIrParticle particle : particles) {
      if (particle instanceof SchemaIrElement element) {
        elementNames.add(element.name());
      } else if (particle instanceof SchemaIrAll all) {
        for (SchemaIrElement element : all.elements()) {
          elementNames.add(element.name());
        }
      } else if (particle instanceof SchemaIrChoice choice) {
        for (SchemaIrElement branch : choice.elementBranches()) {
          elementNames.add(branch.name());
        }
        wildcards.addAll(choice.wildcardBranches());
      } else if (particle instanceof SchemaIrGroup group) {
        collectWildcardAmbiguityInputs(group.particles(), elementNames, wildcards);
      } else if (particle instanceof SchemaIrWildcard wildcard) {
        wildcards.add(wildcard);
      }
    }
  }

  record WildcardAmbiguityInputs(List<SchemaQName> elementNames, List<SchemaIrWildcard> wildcards) {
    WildcardAmbiguityInputs {
      elementNames = List.copyOf(elementNames);
      wildcards = List.copyOf(wildcards);
    }
  }
}
