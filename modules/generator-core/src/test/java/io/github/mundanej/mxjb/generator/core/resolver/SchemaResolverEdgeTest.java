package io.github.mundanej.mxjb.generator.core.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SchemaResolverEdgeTest {
  @TempDir private Path tempDirectory;

  @Test
  void reportsRelativePrimarySchemaWithoutBaseDirectory() {
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));

    SchemaResolutionResult result = resolver.resolve(URI.create("relative.xsd"));

    assertEquals(
        List.of(DiagnosticCode.SCHEMA_RESOURCE_OUTSIDE_LOCAL_ROOT), diagnosticCodes(result));
    assertEquals(true, result.hasErrors());
  }

  @Test
  void reportsPrimarySchemaOutsideConfiguredLocalRoots() {
    Path localRoot = tempDirectory.resolve("allowed");
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(localRoot)));

    SchemaResolutionResult result = resolver.resolve(tempDirectory.resolve("outside.xsd"));

    assertEquals(
        List.of(DiagnosticCode.SCHEMA_RESOURCE_OUTSIDE_LOCAL_ROOT), diagnosticCodes(result));
  }

  @Test
  void reportsMissingSchemaResourceInsideLocalRoot() {
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));

    SchemaResolutionResult result = resolver.resolve(tempDirectory.resolve("missing.xsd"));

    assertEquals(List.of(DiagnosticCode.SCHEMA_RESOURCE_NOT_FOUND), diagnosticCodes(result));
  }

  @Test
  void reportsMalformedSchemaXml() throws IOException {
    Files.writeString(tempDirectory.resolve("broken.xsd"), "<xs:schema");
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));

    SchemaResolutionResult result = resolver.resolve(tempDirectory.resolve("broken.xsd"));

    assertEquals(List.of(DiagnosticCode.SCHEMA_RESOURCE_XML_ERROR), diagnosticCodes(result));
  }

  @Test
  void disambiguatesSameBasenameSchemasAcrossMultipleLocalRoots() throws IOException {
    Path firstRoot = tempDirectory.resolve("first");
    Path secondRoot = tempDirectory.resolve("second");
    Path first = writeSchema(firstRoot.resolve("order.xsd"), "urn:first");
    Path second = writeSchema(secondRoot.resolve("order.xsd"), "urn:second");
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(firstRoot, secondRoot)));

    SchemaResolutionResult firstResult = resolver.resolve(first);
    SchemaResolutionResult secondResult = resolver.resolve(second);

    assertEquals(List.of("root[first]/order.xsd"), resourceIds(firstResult));
    assertEquals(List.of("root[second]/order.xsd"), resourceIds(secondResult));
  }

  @Test
  void keepsRootPrefixSeparateFromSchemaRelativePath() throws IOException {
    Path firstRoot = tempDirectory.resolve("a/x/root");
    Path secondRoot = tempDirectory.resolve("b/y/root");
    Path thirdRoot = tempDirectory.resolve("c/x");
    Path first = writeSchema(firstRoot.resolve("order.xsd"), "urn:first");
    Path third = writeSchema(thirdRoot.resolve("root/order.xsd"), "urn:third");
    SchemaResolver resolver =
        new SchemaResolver(
            SchemaResolverPolicy.localRoots(List.of(firstRoot, secondRoot, thirdRoot)));

    SchemaResolutionResult firstResult = resolver.resolve(first);
    SchemaResolutionResult thirdResult = resolver.resolve(third);

    assertEquals(List.of("root[x/root]/order.xsd"), resourceIds(firstResult));
    assertEquals(List.of("root[x]/root/order.xsd"), resourceIds(thirdResult));
  }

  private List<DiagnosticCode> diagnosticCodes(SchemaResolutionResult result) {
    return result.diagnostics().stream().map(SchemaDiagnostic::code).toList();
  }

  private List<String> resourceIds(SchemaResolutionResult result) {
    return result.manifest().schemas().stream().map(schema -> schema.resourceId()).toList();
  }

  private Path writeSchema(Path path, String namespace) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(
        path,
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\""
            + namespace
            + "\"/>",
        StandardCharsets.UTF_8);
    return path;
  }
}
