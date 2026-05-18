package io.github.mundanej.mxjb.generator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MxjbCliTest {
  @TempDir private Path tempDirectory;

  @Test
  void helpReturnsUsage() {
    CliResult result = run("--help");

    assertEquals(0, result.exitCode());
    assertTrue(result.out().contains("mxjb generate --schema <path> --output <dir>"));
    assertTrue(result.out().contains("XP-XSD10-SEMANTIC"));
    assertTrue(result.out().contains("XP-XSD10-DOCUMENT"));
    assertEquals("", result.err());
  }

  @Test
  void generateWritesDeterministicSourcesAndPrintsRelativePaths() throws IOException {
    Path schema = writeSchema("purchase-order.xsd", purchaseOrderSchema());
    Path output = tempDirectory.resolve("generated");

    CliResult first =
        run(
            "generate",
            "--schema",
            schema.toString(),
            "--output",
            output.toString(),
            "--namespace-package",
            "urn:purchase=com.example.purchase");
    CliResult second =
        run(
            "generate",
            "--schema",
            schema.toString(),
            "--output",
            output.toString(),
            "--namespace-package",
            "urn:purchase=com.example.purchase");

    assertEquals(0, first.exitCode(), first.err());
    assertEquals(first.out(), second.out());
    assertTrue(first.out().contains("com/example/purchase/Order.java"));
    assertTrue(Files.exists(output.resolve("com/example/purchase/xml/OrderXmlReader.java")));
    assertEquals("", first.err());
  }

  @Test
  void generateAcceptsChoiceProfileToken() throws IOException {
    Path schema = writeSchema("choice-order.xsd", choiceOrderSchema());
    Path output = tempDirectory.resolve("choice-generated");

    CliResult result =
        run(
            "generate",
            "--schema",
            schema.toString(),
            "--output",
            output.toString(),
            "--profile",
            "XP-DATA-10-CHOICE",
            "--namespace-package",
            "urn:orders=com.example.orders");

    assertEquals(0, result.exitCode(), result.err());
    assertTrue(result.out().contains("com/example/orders/OrderChoice.java"));
    assertTrue(Files.exists(output.resolve("com/example/orders/DomesticChoice.java")));
  }

  @Test
  void generateAcceptsBasicValidationProfileToken() throws IOException {
    Path schema = writeSchema("facet-order.xsd", facetOrderSchema());
    Path output = tempDirectory.resolve("facet-generated");

    CliResult result =
        run(
            "generate",
            "--schema",
            schema.toString(),
            "--output",
            output.toString(),
            "--profile",
            "XP-VALIDATION-10-BASIC",
            "--namespace-package",
            "urn:orders=com.example.orders");

    assertEquals(0, result.exitCode(), result.err());
    assertTrue(result.out().contains("com/example/orders/Order.java"));
    assertTrue(
        Files.readString(output.resolve("com/example/orders/xml/OrderXmlValidator.java"))
            .contains("MXJB-GV-007"));
  }

  @Test
  void generateAcceptsComposedProfileToken() throws IOException {
    Path schema = writeSchema("composed-order.xsd", composedOrderSchema());
    Path output = tempDirectory.resolve("composed-generated");

    CliResult result =
        run(
            "generate",
            "--schema",
            schema.toString(),
            "--output",
            output.toString(),
            "--profile",
            "XP-XSD10-COMPOSED",
            "--namespace-package",
            "urn:orders=com.example.orders");

    assertEquals(0, result.exitCode(), result.err());
    assertTrue(result.out().contains("com/example/orders/Order.java"));
    assertTrue(
        Files.readString(output.resolve("com/example/orders/Order.java"))
            .contains("String version"));
  }

  @Test
  void generateResolvesCatalogAndRepeatedSchemaOptions() throws IOException {
    Path order = writeSchema("schemas/order.xsd", orderSchema("https://example.invalid/line.xsd"));
    Path line = writeSchema("catalog/line.xsd", lineSchema());
    Path output = tempDirectory.resolve("multi");

    CliResult result =
        run(
            "generate",
            "--schema",
            order.toString(),
            "--schema",
            line.toString(),
            "--output",
            output.toString(),
            "--namespace-package",
            "urn:orders=com.example.orders",
            "--namespace-package",
            "urn:lines=com.example.lines",
            "--local-root",
            tempDirectory.resolve("catalog").toString(),
            "--catalog",
            "https://example.invalid/line.xsd=" + line);

    assertEquals(0, result.exitCode(), result.err());
    assertTrue(result.out().contains("com/example/orders/Order.java"));
    assertTrue(result.out().contains("com/example/lines/Line.java"));
    assertTrue(
        Files.readString(output.resolve("com/example/orders/Order.java"))
            .contains("com.example.lines"));
  }

  @Test
  void invalidOptionsReturnUsageExitCodeAndDiagnostics() {
    CliResult result =
        run(
            "generate",
            "--schema",
            "--output",
            "out",
            "--profile",
            "XP-DATA-11",
            "--unknown",
            "--code-to-schema");

    assertEquals(2, result.exitCode());
    assertTrue(result.err().contains("GENERATOR_CLI_INVALID_ARGUMENT | --schema | Missing value"));
    assertTrue(result.err().contains("Unsupported generator profile XP-DATA-11"));
    assertTrue(result.err().contains("Unknown option --unknown"));
    assertTrue(result.err().contains("Code-to-schema generation is not supported"));
  }

  @Test
  void emptyInlineOptionValuesReturnUsageExitCodeAndDiagnostics() {
    CliResult result = run("generate", "--schema=", "--output=", "--profile=");

    assertEquals(2, result.exitCode());
    assertTrue(result.err().contains("GENERATOR_CLI_INVALID_ARGUMENT | --schema | Missing value"));
    assertTrue(result.err().contains("GENERATOR_CLI_INVALID_ARGUMENT | --output | Missing value"));
    assertTrue(result.err().contains("GENERATOR_CLI_INVALID_ARGUMENT | --profile | Missing value"));
  }

  @Test
  void generationDiagnosticsReturnFailureExitCode() throws IOException {
    Path schema = writeSchema("order.xsd", orderSchema("https://example.invalid/line.xsd"));
    Path output = tempDirectory.resolve("denied");

    CliResult result =
        run("generate", "--schema", schema.toString(), "--output", output.toString());

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains("SCHEMA_RESOURCE_NETWORK_DENIED"));
    assertFalse(Files.exists(output));
  }

  @Test
  void invalidSubcommandReturnsUsageExitCode() {
    CliResult result = run("schema", "--output", "out");

    assertEquals(2, result.exitCode());
    assertTrue(result.err().contains("Expected subcommand generate"));
  }

  private CliResult run(String... args) {
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
    int exitCode =
        MxjbCli.run(
            args,
            new PrintStream(outBytes, true, StandardCharsets.UTF_8),
            new PrintStream(errBytes, true, StandardCharsets.UTF_8));
    return new CliResult(
        exitCode,
        outBytes.toString(StandardCharsets.UTF_8),
        errBytes.toString(StandardCharsets.UTF_8));
  }

  private Path writeSchema(String relativePath, String content) throws IOException {
    Path target = tempDirectory.resolve(relativePath);
    Path parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(target, content, StandardCharsets.UTF_8);
    return target;
  }

  private String purchaseOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:purchase"
            xmlns:p="urn:purchase"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:element name="order" type="p:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String choiceOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            elementFormDefault="qualified">
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:choice minOccurs="0">
                <xs:element name="domestic" type="xs:string"/>
                <xs:element name="international" type="xs:string"/>
              </xs:choice>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String facetOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            elementFormDefault="qualified">
          <xs:simpleType name="OrderCode">
            <xs:restriction base="xs:string">
              <xs:minLength value="3"/>
              <xs:maxLength value="8"/>
              <xs:pattern value="[A-Z0-9]+"/>
            </xs:restriction>
          </xs:simpleType>
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="code" type="o:OrderCode"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String composedOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:group name="OrderFields">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
            </xs:sequence>
          </xs:group>
          <xs:attributeGroup name="OrderAttributes">
            <xs:attribute name="version" type="xs:string" use="required"/>
          </xs:attributeGroup>
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:group ref="o:OrderFields"/>
            </xs:sequence>
            <xs:attributeGroup ref="o:OrderAttributes"/>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String orderSchema(String lineLocation) {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            xmlns:l="urn:lines"
            elementFormDefault="qualified">
          <xs:import namespace="urn:lines" schemaLocation="LINE_SCHEMA_LOCATION"/>
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:element ref="l:line" maxOccurs="unbounded"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """
        .replace("LINE_SCHEMA_LOCATION", lineLocation);
  }

  private String lineSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:lines"
            xmlns:l="urn:lines"
            elementFormDefault="qualified">
          <xs:element name="line" type="l:Line"/>
          <xs:complexType name="Line">
            <xs:sequence>
              <xs:element name="sku" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private record CliResult(int exitCode, String out, String err) {}
}
