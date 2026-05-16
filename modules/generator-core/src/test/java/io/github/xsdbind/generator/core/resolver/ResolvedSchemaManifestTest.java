package io.github.xsdbind.generator.core.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ResolvedSchemaManifestTest {
  @TempDir private Path tempDirectory;

  @Test
  void writesStableManifestForResolvedSchemasInTraversalOrder() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:main",
            """
                <xs:include schemaLocation="types.xsd"/>
                <xs:import namespace="urn:external" schemaLocation="external.xsd"/>
                """));
    write("types.xsd", schema("urn:main", ""));
    write("external.xsd", schema("urn:external", ""));

    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));

    SchemaResolutionResult first = resolver.resolve(tempDirectory.resolve("main.xsd"));
    SchemaResolutionResult second = resolver.resolve(tempDirectory.resolve("main.xsd"));

    assertTrue(first.diagnostics().isEmpty());
    assertEquals(first.manifest().toText(), second.manifest().toText());
    assertEquals(
        """
                main.xsd | namespace=urn:main | references=[include:types.xsd,import:urn:external->external.xsd]
                types.xsd | namespace=urn:main | references=[]
                external.xsd | namespace=urn:external | references=[]
                """,
        first.manifest().toText());
  }

  private void write(String fileName, String contents) throws IOException {
    Files.writeString(tempDirectory.resolve(fileName), contents);
  }

  private String schema(String namespace, String body) {
    return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\""
        + namespace
        + "\">\n"
        + body
        + "\n</xs:schema>\n";
  }
}
