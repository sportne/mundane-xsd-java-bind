package io.github.mundanej.mxjb.conformance;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

final class ConformanceSchemaFactoriesTest {
  @TempDir private Path tempDirectory;

  @Test
  void secureFactoryRejectsExternalSchemaAccess() throws IOException {
    Path schema = tempDirectory.resolve("external-schema.xsd");
    Files.writeString(
        schema,
        """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:include schemaLocation="https://example.invalid/external.xsd"/>
            </xs:schema>
            """);

    SAXException exception =
        assertThrows(SAXException.class, () -> ConformanceSchemaFactories.newSchema(schema));

    assertTrue(exception.getMessage().contains("accessExternalSchema"));
  }
}
