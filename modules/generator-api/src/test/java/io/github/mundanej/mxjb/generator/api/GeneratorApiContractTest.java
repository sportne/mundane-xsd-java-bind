package io.github.mundanej.mxjb.generator.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class GeneratorApiContractTest {
  @Test
  void requestAppliesDefaultsAndCopiesInputs() {
    ArrayList<Path> schemas = new ArrayList<>(List.of(Path.of("order.xsd")));
    LinkedHashMap<String, String> packages = new LinkedHashMap<>();
    packages.put("urn:orders", "com.example.orders");
    ArrayList<Path> roots = new ArrayList<>(List.of(Path.of("schema")));
    LinkedHashMap<URI, Path> catalog = new LinkedHashMap<>();
    catalog.put(URI.create("urn:external"), Path.of("external.xsd"));

    GeneratorRequest request =
        new GeneratorRequest(schemas, Path.of("out"), null, "", packages, roots, catalog);
    schemas.add(Path.of("later.xsd"));
    packages.put("urn:later", "com.example.later");
    roots.add(Path.of("later"));
    catalog.put(URI.create("urn:later"), Path.of("later.xsd"));

    assertEquals(List.of(Path.of("order.xsd")), request.schemaPaths());
    assertEquals(Path.of("out"), request.outputDirectory());
    assertEquals(GeneratorProfile.XP_DATA_10, request.profile());
    assertEquals(GeneratorRequest.DEFAULT_PACKAGE, request.defaultPackage());
    assertEquals(Map.of("urn:orders", "com.example.orders"), request.namespacePackages());
    assertEquals(List.of(Path.of("schema")), request.localRoots());
    assertEquals(
        Map.of(URI.create("urn:external"), Path.of("external.xsd")), request.catalogMappings());
  }

  @Test
  void resultAndDiagnosticExposeStableStatusAndText() {
    GeneratorDiagnostic diagnostic =
        new GeneratorDiagnostic("SCHEMA_RESOURCE_NOT_FOUND", "order.xsd", "Schema not found.");

    assertEquals(
        "SCHEMA_RESOURCE_NOT_FOUND | order.xsd | Schema not found.", diagnostic.toManifestLine());
    assertFalse(GeneratorResult.failure(List.of(diagnostic)).successful());
    assertTrue(GeneratorResult.success(List.of(Path.of("com/example/Order.java"))).successful());
    assertThrows(IllegalArgumentException.class, () -> new GeneratorDiagnostic("", "x", "message"));
  }

  @Test
  void profileParsesCliTokenOnlyForSupportedProfiles() {
    assertEquals(
        GeneratorProfile.XP_DATA_10, GeneratorProfile.fromCliToken("XP-DATA-10").orElseThrow());
    assertEquals(
        GeneratorProfile.XP_DATA_10_CHOICE,
        GeneratorProfile.fromCliToken("XP-DATA-10-CHOICE").orElseThrow());
    assertEquals(
        GeneratorProfile.XP_VALIDATION_10_BASIC,
        GeneratorProfile.fromCliToken("XP-VALIDATION-10-BASIC").orElseThrow());
    assertEquals(
        GeneratorProfile.XP_XSD10_COMPOSED,
        GeneratorProfile.fromCliToken("XP-XSD10-COMPOSED").orElseThrow());
    assertEquals(
        GeneratorProfile.XP_XSD10_SEMANTIC,
        GeneratorProfile.fromCliToken("XP-XSD10-SEMANTIC").orElseThrow());
    assertEquals(
        GeneratorProfile.XP_XSD10_DOCUMENT,
        GeneratorProfile.fromCliToken("XP-XSD10-DOCUMENT").orElseThrow());
    assertEquals(
        GeneratorProfile.XP_XSD10_FULL,
        GeneratorProfile.fromCliToken("XP-XSD10-FULL").orElseThrow());
    assertTrue(GeneratorProfile.fromCliToken("XP-DATA-11").isEmpty());
    assertTrue(GeneratorProfile.fromCliToken("XP-XSD11-ASSERT").isEmpty());
    assertTrue(GeneratorProfile.fromCliToken(null).isEmpty());
  }

  @Test
  void publicApiDoesNotExposeImplementationPackages() {
    List<Class<?>> publicTypes =
        List.of(
            Generator.class,
            GeneratorRequest.class,
            GeneratorResult.class,
            GeneratorDiagnostic.class,
            GeneratorProfile.class);

    for (Class<?> publicType : publicTypes) {
      assertTrue(publicType.getPackageName().startsWith("io.github.mundanej.mxjb.generator.api"));
      assertFalse(publicType.getName().contains(".core."));
      assertFalse(publicType.getName().contains(".schema."));
      assertFalse(publicType.getName().contains(".bind."));
      assertFalse(publicType.getName().contains(".emit."));
    }
  }
}
