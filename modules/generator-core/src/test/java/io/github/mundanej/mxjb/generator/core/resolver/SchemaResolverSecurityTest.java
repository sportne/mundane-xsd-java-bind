package io.github.mundanej.mxjb.generator.core.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SchemaResolverSecurityTest {
  @TempDir private Path tempDirectory;

  @Test
  void deniesHttpPrimarySchemaByDefault() {
    SchemaResolutionResult result =
        resolver().resolve(URI.create("http://example.test/schema.xsd"));

    assertEquals(List.of(DiagnosticCode.SCHEMA_RESOURCE_NETWORK_DENIED), diagnosticCodes(result));
    assertEquals("", result.manifest().toText());
  }

  @Test
  void deniesHttpsPrimarySchemaByDefault() {
    SchemaResolutionResult result =
        resolver().resolve(URI.create("https://example.test/schema.xsd"));

    assertEquals(List.of(DiagnosticCode.SCHEMA_RESOURCE_NETWORK_DENIED), diagnosticCodes(result));
    assertEquals("", result.manifest().toText());
  }

  @Test
  void reportsIncludeCyclesDeterministically() throws IOException {
    write("a.xsd", schema("<xs:include schemaLocation=\"b.xsd\"/>"));
    write("b.xsd", schema("<xs:include schemaLocation=\"a.xsd\"/>"));

    SchemaResolutionResult first = resolver().resolve(tempDirectory.resolve("a.xsd"));
    SchemaResolutionResult second = resolver().resolve(tempDirectory.resolve("a.xsd"));

    assertEquals(first.diagnostics(), second.diagnostics());
    assertEquals(List.of(DiagnosticCode.SCHEMA_RESOURCE_CYCLE), diagnosticCodes(first));
    assertEquals(
        "SCHEMA_RESOURCE_CYCLE | a.xsd | Schema include/import cycle: a.xsd -> b.xsd -> a.xsd",
        first.diagnostics().getFirst().toManifestLine());
  }

  @Test
  void resolvesCatalogMappingsWithoutNetworkAccess() throws IOException {
    write(
        "main.xsd",
        schema("<xs:import namespace=\"urn:address\" schemaLocation=\"urn:catalog:address\"/>"));
    write("address.xsd", schemaWithNamespace("urn:address", ""));
    SchemaResolver resolver =
        new SchemaResolver(
            SchemaResolverPolicy.withCatalog(
                List.of(tempDirectory),
                java.util.Map.of(
                    URI.create("urn:catalog:address"), tempDirectory.resolve("address.xsd"))));

    SchemaResolutionResult result = resolver.resolve(tempDirectory.resolve("main.xsd"));

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                main.xsd | namespace=urn:test | references=[import:urn:address->urn:catalog:address]
                address.xsd | namespace=urn:address | references=[]
                """,
        result.manifest().toText());
  }

  @Test
  void resolvesMappedHttpImportsWithoutNetworkAccess() throws IOException {
    write(
        "main.xsd",
        schema(
            "<xs:import namespace=\"urn:address\" "
                + "schemaLocation=\"https://schemas.example.test/address.xsd\"/>"));
    write("address.xsd", schemaWithNamespace("urn:address", ""));
    SchemaResolver resolver =
        new SchemaResolver(
            SchemaResolverPolicy.withCatalog(
                List.of(tempDirectory),
                java.util.Map.of(
                    URI.create("https://schemas.example.test/address.xsd"),
                    tempDirectory.resolve("address.xsd"))));

    SchemaResolutionResult result = resolver.resolve(tempDirectory.resolve("main.xsd"));

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                main.xsd | namespace=urn:test | references=[import:urn:address->https://schemas.example.test/address.xsd]
                address.xsd | namespace=urn:address | references=[]
                """,
        result.manifest().toText());
  }

  private SchemaResolver resolver() {
    return new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
  }

  private List<DiagnosticCode> diagnosticCodes(SchemaResolutionResult result) {
    return result.diagnostics().stream().map(SchemaDiagnostic::code).toList();
  }

  private void write(String fileName, String contents) throws IOException {
    Files.writeString(tempDirectory.resolve(fileName), contents);
  }

  private String schema(String body) {
    return schemaWithNamespace("urn:test", body);
  }

  private String schemaWithNamespace(String namespace, String body) {
    return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\""
        + namespace
        + "\">\n"
        + body
        + "\n</xs:schema>\n";
  }
}
