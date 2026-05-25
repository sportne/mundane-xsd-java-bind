package io.github.mundanej.mxjb.generator.core.bind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class BindingNameAllocatorTest {
  @Test
  void allocatesNamespacePackagesAndDuplicateLocalTypeNamesDeterministically() {
    BindingNameAllocator names =
        new BindingNameAllocator(
            BindingConfiguration.withNamespacePackages(Map.of("urn:orders", "com.acme.orders")));

    assertEquals(new BindingJavaName("com.acme.orders", "Order"), names.javaName(q("urn:orders")));
    assertEquals(new BindingJavaName("com.acme.orders", "Order2"), names.javaName(q("urn:orders")));
    assertEquals(
        new BindingJavaName("io.github.mundanej.mxjb.generated.catalog", "Order"),
        names.javaName(q("urn:catalog")));
  }

  @Test
  void allocatesInlineAndGeneratedTypeNamesInTheSamePackageScope() {
    BindingNameAllocator names = new BindingNameAllocator(BindingConfiguration.defaults());
    String packageName = names.javaName(q("urn:orders")).packageName();

    assertEquals("Order2", names.uniqueTypeName(packageName, "Order"));
    assertEquals("OrderContent", names.uniqueTypeName(packageName, "OrderContent"));
  }

  @Test
  void allocatesFieldCollisionsWithoutChangingXmlNames() {
    BindingNameAllocator names = new BindingNameAllocator(BindingConfiguration.defaults());
    Set<String> used = new HashSet<>();

    assertEquals("_class", names.uniqueFieldName(new SchemaQName("urn:orders", "class"), used));
    assertEquals("_class2", names.uniqueFieldName(new SchemaQName("urn:orders", "class"), used));
    assertEquals("wildcardContent", names.unique("wildcardContent", used));
  }

  @Test
  void reportsInvalidPackageConfiguration() {
    BindingNameAllocator names =
        new BindingNameAllocator(new BindingConfiguration("not-valid-package!", Map.of()));

    assertEquals(
        DiagnosticCode.SCHEMA_BINDING_INVALID_CONFIGURATION,
        names.validateConfiguration().getFirst().code());
    assertTrue(names.validateConfiguration().getFirst().message().contains("default package"));
  }

  private SchemaQName q(String namespace) {
    return new SchemaQName(namespace, "order");
  }
}
