package io.github.mundanej.mxjb.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class SchemaIrParticleNormalizationTest {
  @Test
  void flattensRequiredNestedSequenceInPlace() {
    List<SchemaIrParticle> particles = new ArrayList<>();
    SchemaIrSequence nested =
        new SchemaIrSequence(SchemaCardinality.ONE, List.of(element("one"), element("two")));

    SchemaIrParticleNormalization.addFlattenedNestedSequence(particles, nested);

    assertEquals(List.of(element("one"), element("two")), particles);
  }

  @Test
  void composesSingletonNestedSequenceCardinality() {
    List<SchemaIrParticle> particles = new ArrayList<>();
    SchemaIrSequence nested =
        new SchemaIrSequence(new SchemaCardinality(0, "unbounded"), List.of(element("item")));

    SchemaIrParticleNormalization.addFlattenedNestedSequence(particles, nested);

    SchemaIrElement element = assertInstanceOf(SchemaIrElement.class, particles.getFirst());
    assertEquals(new SchemaCardinality(0, "unbounded"), element.cardinality());
  }

  @Test
  void wrapsRepeatedMultiParticleNestedSequenceAsGroup() {
    List<SchemaIrParticle> particles = new ArrayList<>();
    SchemaIrSequence nested =
        new SchemaIrSequence(
            new SchemaCardinality(0, "2"), List.of(element("one"), element("two")));

    SchemaIrParticleNormalization.addFlattenedNestedSequence(particles, nested);

    SchemaIrGroup group = assertInstanceOf(SchemaIrGroup.class, particles.getFirst());
    assertEquals("sequence", group.modelKind());
    assertEquals(new SchemaCardinality(0, "2"), group.cardinality());
    assertEquals(List.of(element("one"), element("two")), group.particles());
  }

  @Test
  void collectsWildcardAmbiguityInputsFromNestedParticles() {
    SchemaIrWildcard wildcard =
        new SchemaIrWildcard(
            SchemaCardinality.ONE, new SchemaIrWildcardNamespace("any", List.of()));
    SchemaIrChoice choice = new SchemaIrChoice(SchemaCardinality.ONE, List.of(element("branch")));
    SchemaIrGroup group = new SchemaIrGroup("sequence", SchemaCardinality.ONE, List.of(wildcard));

    SchemaIrParticleNormalization.WildcardAmbiguityInputs inputs =
        SchemaIrParticleNormalization.wildcardAmbiguityInputs(
            List.of(element("direct"), choice, group));

    assertEquals(List.of(q("direct"), q("branch")), inputs.elementNames());
    assertTrue(inputs.wildcards().contains(wildcard));
  }

  @Test
  void composesCardinalityForSupportedParticleKinds() {
    SchemaCardinality repeated = new SchemaCardinality(0, "2");
    SchemaIrWildcard wildcard =
        new SchemaIrWildcard(
            SchemaCardinality.ONE, new SchemaIrWildcardNamespace("any", List.of()));
    SchemaIrChoice choice = new SchemaIrChoice(SchemaCardinality.ONE, List.of(element("choice")));
    SchemaIrAll all = new SchemaIrAll(SchemaCardinality.ONE, List.of(element("all")));
    SchemaIrGroup group =
        new SchemaIrGroup("group", SchemaCardinality.ONE, List.of(element("group")));

    assertEquals(
        repeated,
        assertInstanceOf(
                SchemaIrWildcard.class,
                SchemaIrParticleNormalization.withCardinality(wildcard, repeated))
            .cardinality());
    assertEquals(
        repeated,
        assertInstanceOf(
                SchemaIrChoice.class,
                SchemaIrParticleNormalization.withCardinality(choice, repeated))
            .cardinality());
    assertEquals(
        repeated,
        assertInstanceOf(
                SchemaIrAll.class, SchemaIrParticleNormalization.withCardinality(all, repeated))
            .cardinality());
    assertEquals(
        repeated,
        assertInstanceOf(
                SchemaIrGroup.class, SchemaIrParticleNormalization.withCardinality(group, repeated))
            .cardinality());
  }

  @Test
  void detectsNestedGroupReferencesInSyntaxChildren() {
    XsdSyntaxNode node =
        new XsdSyntaxNode(
            XsdSyntaxKind.SEQUENCE,
            Map.of(),
            List.of(
                new XsdSyntaxNode(XsdSyntaxKind.GROUP, Map.of("ref", "tns:Shared"), List.of())));

    assertTrue(SchemaIrParticleNormalization.containsGroupReference(node));
  }

  private SchemaIrElement element(String localName) {
    return new SchemaIrElement(
        q(localName),
        SchemaIrTypeReference.named(new SchemaQName("", "string")),
        SchemaCardinality.ONE,
        null,
        false);
  }

  private SchemaQName q(String localName) {
    return new SchemaQName("urn:orders", localName);
  }
}
