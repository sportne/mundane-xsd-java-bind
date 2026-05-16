package io.github.xsdbind.generator.core.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.xsdbind.generator.core.diagnostics.DiagnosticCode;
import io.github.xsdbind.generator.core.diagnostics.SchemaDiagnostic;
import java.io.IOException;
import java.net.URI;
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

  private List<DiagnosticCode> diagnosticCodes(SchemaResolutionResult result) {
    return result.diagnostics().stream().map(SchemaDiagnostic::code).toList();
  }
}
