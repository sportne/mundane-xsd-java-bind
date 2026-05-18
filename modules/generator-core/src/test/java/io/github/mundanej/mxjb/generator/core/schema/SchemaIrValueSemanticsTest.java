package io.github.mundanej.mxjb.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SchemaIrValueSemanticsTest {
  @Test
  void rendersEmptyAndPresentSemanticsDeterministically() {
    assertFalse(SchemaIrValueSemantics.NONE.hasAny());
    assertFalse(SchemaIrValueSemantics.NONE.hasDefault());
    assertFalse(SchemaIrValueSemantics.NONE.hasFixed());
    assertEquals("", SchemaIrValueSemantics.NONE.toText());

    SchemaIrValueSemantics semantics = new SchemaIrValueSemantics(true, "abc", "xyz");

    assertTrue(semantics.hasAny());
    assertTrue(semantics.hasDefault());
    assertTrue(semantics.hasFixed());
    assertEquals(" semantics[nillable=true,default=abc,fixed=xyz]", semantics.toText());
  }
}
