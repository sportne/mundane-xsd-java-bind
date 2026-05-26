package io.github.mundanej.mxjb.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SchemaIrWildcardNormalizationTest {
  @Test
  void normalizesExplicitNamespaceTokensDeterministically() {
    List<String> diagnostics = new ArrayList<>();

    SchemaIrWildcardNamespace namespace =
        SchemaIrWildcardNormalization.namespace(
            "##local ##targetNamespace urn:other", "urn:orders", diagnostics::add);

    assertEquals(
        new SchemaIrWildcardNamespace("explicit", List.of("", "urn:orders", "urn:other")),
        namespace);
    assertTrue(diagnostics.isEmpty());
  }

  @Test
  void rejectsDuplicateNamespaceTokensWithStableDiagnostic() {
    List<String> diagnostics = new ArrayList<>();

    SchemaIrWildcardNamespace namespace =
        SchemaIrWildcardNormalization.namespace("##local ##local", "urn:orders", diagnostics::add);

    assertNull(namespace);
    assertEquals(List.of("xs:any namespace constraint contains duplicate tokens."), diagnostics);
  }

  @Test
  void keepsWildcardOverlapAndRestrictionPolicyExplicit() {
    SchemaIrWildcardNamespace any = new SchemaIrWildcardNamespace("any", List.of());
    SchemaIrWildcardNamespace otherOrders =
        new SchemaIrWildcardNamespace("other", List.of("urn:orders"));
    SchemaIrWildcardNamespace explicitExternal =
        new SchemaIrWildcardNamespace("explicit", List.of("urn:external"));
    SchemaIrWildcardNamespace explicitOrders =
        new SchemaIrWildcardNamespace("explicit", List.of("urn:orders"));

    assertTrue(SchemaIrWildcardNormalization.matches(q("id"), any));
    assertTrue(SchemaIrWildcardNormalization.overlap(otherOrders, explicitExternal));
    assertFalse(SchemaIrWildcardNormalization.overlap(otherOrders, explicitOrders));
    assertTrue(SchemaIrWildcardNormalization.namespaceSubset(explicitExternal, otherOrders));
    assertFalse(SchemaIrWildcardNormalization.namespaceSubset(any, explicitExternal));
    assertTrue(SchemaIrWildcardNormalization.processContentsAllowsRestriction("skip", "strict"));
    assertFalse(SchemaIrWildcardNormalization.processContentsAllowsRestriction("strict", "skip"));
  }

  @Test
  void unionsExplicitAttributeWildcardsAndKeepsStricterProcessContents() {
    List<String> diagnostics = new ArrayList<>();

    SchemaIrWildcardNamespace union =
        SchemaIrWildcardNormalization.union(
            new SchemaIrWildcardNamespace("explicit", List.of("urn:a")),
            new SchemaIrWildcardNamespace("explicit", List.of("urn:b", "urn:a")),
            diagnostics::add);

    assertEquals(new SchemaIrWildcardNamespace("explicit", List.of("urn:a", "urn:b")), union);
    assertEquals("strict", SchemaIrWildcardNormalization.stricterProcessContents("lax", "strict"));
    assertTrue(diagnostics.isEmpty());
  }

  private SchemaQName q(String localName) {
    return new SchemaQName("urn:orders", localName);
  }
}
