package io.github.mundanej.mxjb.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class SchemaIrAttributeNormalizationTest {
  @Test
  void qualifiesGlobalAndFormDefaultQualifiedAttributes() {
    assertEquals(
        new SchemaQName("urn:orders", "id"),
        SchemaIrAttributeNormalization.name("urn:orders", Map.of(), Map.of(), "id", true));
    assertEquals(
        new SchemaQName("urn:orders", "region"),
        SchemaIrAttributeNormalization.name(
            "urn:orders", Map.of("attributeFormDefault", "qualified"), Map.of(), "region", false));
  }

  @Test
  void localAttributeFormOverridesSchemaDefault() {
    assertEquals(
        new SchemaQName("", "local"),
        SchemaIrAttributeNormalization.name(
            "urn:orders",
            Map.of("attributeFormDefault", "qualified"),
            Map.of("form", "unqualified"),
            "local",
            false));
    assertEquals(
        new SchemaQName("urn:orders", "qualified"),
        SchemaIrAttributeNormalization.name(
            "urn:orders", Map.of(), Map.of("form", "qualified"), "qualified", false));
  }

  @Test
  void capturesDefaultFixedAndNillableSemantics() {
    SchemaIrValueSemantics semantics =
        SchemaIrAttributeNormalization.semantics(
            Map.of("nillable", "true", "default", "new", "fixed", "fixed"));

    assertEquals(new SchemaIrValueSemantics(true, "new", "fixed"), semantics);
  }
}
