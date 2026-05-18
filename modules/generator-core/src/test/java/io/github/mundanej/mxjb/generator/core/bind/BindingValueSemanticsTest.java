package io.github.mundanej.mxjb.generator.core.bind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BindingValueSemanticsTest {
  @Test
  void rendersEmptyAndPresentSemanticsDeterministically() {
    assertFalse(BindingValueSemantics.NONE.hasAny());
    assertFalse(BindingValueSemantics.NONE.hasDefault());
    assertFalse(BindingValueSemantics.NONE.hasFixed());
    assertEquals("", BindingValueSemantics.NONE.toText());

    BindingValueSemantics semantics = new BindingValueSemantics(true, "abc", "xyz");

    assertTrue(semantics.hasAny());
    assertTrue(semantics.hasDefault());
    assertTrue(semantics.hasFixed());
    assertEquals(" semantics[nillable=true,default=abc,fixed=xyz]", semantics.toText());
  }
}
