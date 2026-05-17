package io.github.mundanej.mxjb.generator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CoreGeneratorTest {
  @TempDir private Path tempDirectory;

  @Test
  void generatesDeterministicPurchaseOrderSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("purchase-order.xsd", purchaseOrderSchema());
    Path output = tempDirectory.resolve("generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            null,
            "com.acme.generated",
            Map.of("urn:purchase", "com.acme.purchase"),
            List.of(),
            Map.of());

    GeneratorResult first = new CoreGenerator().generate(request);
    Map<Path, String> firstSources = readGeneratedSources(output, first.generatedSources());
    GeneratorResult second = new CoreGenerator().generate(request);
    Map<Path, String> secondSources = readGeneratedSources(output, second.generatedSources());

    assertTrue(first.successful(), first.diagnostics().toString());
    assertEquals(first.generatedSources(), second.generatedSources());
    assertEquals(firstSources, secondSources);
    assertEquals(
        List.of(
            Path.of("com/acme/purchase/Line.java"),
            Path.of("com/acme/purchase/Order.java"),
            Path.of("com/acme/purchase/xml/OrderXmlReader.java"),
            Path.of("com/acme/purchase/xml/OrderXmlValidator.java"),
            Path.of("com/acme/purchase/xml/OrderXmlWriter.java")),
        first.generatedSources());
    compileGeneratedSources(output, first.generatedSources());
  }

  @Test
  void generatesMultiNamespaceSourcesThroughCatalogAndLocalRoot() throws IOException {
    Path primary =
        writeSchema("schemas/order.xsd", orderSchema("https://example.invalid/line.xsd"));
    Path line = writeSchema("catalog/line.xsd", lineSchema());
    Path output = tempDirectory.resolve("generated-multi");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(primary),
            output,
            null,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders", "urn:lines", "com.acme.lines"),
            List.of(tempDirectory.resolve("catalog")),
            Map.of(URI.create("https://example.invalid/line.xsd"), line));

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    assertTrue(result.generatedSources().contains(Path.of("com/acme/orders/Order.java")));
    assertTrue(result.generatedSources().contains(Path.of("com/acme/lines/Line.java")));
    assertTrue(
        Files.readString(output.resolve("com/acme/orders/Order.java"), StandardCharsets.UTF_8)
            .contains("List<com.acme.lines.Line>"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void defaultProfileRejectsChoiceWithoutWritingSources() throws IOException {
    Path schema = writeSchema("choice-order.xsd", choiceOrderSchema());
    Path output = tempDirectory.resolve("choice-default");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    null,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(Files.exists(output));
  }

  @Test
  void choiceProfileGeneratesChoiceSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("choice-order.xsd", choiceOrderSchema());
    Path output = tempDirectory.resolve("choice-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_DATA_10_CHOICE,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    assertEquals(
        List.of(
            Path.of("com/acme/orders/DomesticChoice.java"),
            Path.of("com/acme/orders/InternationalChoice.java"),
            Path.of("com/acme/orders/Order.java"),
            Path.of("com/acme/orders/OrderChoice.java"),
            Path.of("com/acme/orders/xml/OrderXmlReader.java"),
            Path.of("com/acme/orders/xml/OrderXmlValidator.java"),
            Path.of("com/acme/orders/xml/OrderXmlWriter.java")),
        result.generatedSources());
    assertTrue(
        Files.readString(output.resolve("com/acme/orders/Order.java"), StandardCharsets.UTF_8)
            .contains("Optional<OrderChoice> orderChoice"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void defaultProfileRejectsRestrictedSimpleTypeWithoutWritingSources() throws IOException {
    Path schema = writeSchema("facet-order.xsd", facetOrderSchema());
    Path output = tempDirectory.resolve("facet-default");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    null,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(Files.exists(output));
  }

  @Test
  void basicValidationProfileGeneratesFacetValidationSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("facet-order.xsd", facetOrderSchema());
    Path output = tempDirectory.resolve("facet-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_VALIDATION_10_BASIC,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    assertTrue(result.generatedSources().contains(Path.of("com/acme/orders/Order.java")));
    String validator =
        Files.readString(output.resolve("com/acme/orders/xml/OrderXmlValidator.java"));
    assertTrue(validator.contains("MXJB-GV-005"));
    assertTrue(validator.contains("MXJB-GV-006"));
    assertTrue(validator.contains("MXJB-GV-007"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void defaultProfileRejectsGroupAndAttributeGroupWithoutWritingSources() throws IOException {
    Path schema = writeSchema("composed-order.xsd", composedOrderSchema());
    Path output = tempDirectory.resolve("composed-default");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    null,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(Files.exists(output));
  }

  @Test
  void composedProfileGeneratesFlattenedGroupSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("composed-order.xsd", composedOrderSchema());
    Path output = tempDirectory.resolve("composed-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_XSD10_COMPOSED,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    assertTrue(result.generatedSources().contains(Path.of("com/acme/orders/Order.java")));
    String order = Files.readString(output.resolve("com/acme/orders/Order.java"));
    assertTrue(order.contains("String id"));
    assertTrue(order.contains("BigDecimal total"));
    assertTrue(order.contains("String version"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void deniesNetworkResolutionAndWritesNoSources() throws IOException {
    Path schema = writeSchema("order.xsd", orderSchema("https://example.invalid/line.xsd"));
    Path output = tempDirectory.resolve("network-denied");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), output));

    assertFalse(result.successful());
    assertEquals("SCHEMA_RESOURCE_NETWORK_DENIED", result.diagnostics().get(0).code());
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(Files.exists(output));
  }

  @Test
  void invalidBindingConfigurationWritesNoSources() throws IOException {
    Path schema = writeSchema("purchase-order.xsd", purchaseOrderSchema());
    Path output = tempDirectory.resolve("invalid-package");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema), output, null, "not-valid!", Map.of(), List.of(), Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertFalse(result.successful());
    assertEquals("SCHEMA_BINDING_INVALID_CONFIGURATION", result.diagnostics().get(0).code());
    assertFalse(Files.exists(output));
  }

  @Test
  void writeFailureDoesNotLeaveEarlierGeneratedSources() throws IOException {
    Path schema = writeSchema("purchase-order.xsd", purchaseOrderSchema());
    Path output = tempDirectory.resolve("blocked-output");
    Files.createDirectories(output.resolve("com/acme/purchase"));
    Files.writeString(output.resolve("com/acme/purchase/xml"), "not a directory");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            null,
            "com.acme.generated",
            Map.of("urn:purchase", "com.acme.purchase"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertFalse(result.successful());
    assertEquals(CoreGenerator.WRITE_FAILED, result.diagnostics().get(0).code());
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(Files.exists(output.resolve("com/acme/purchase/Line.java")));
    assertFalse(Files.exists(output.resolve("com/acme/purchase/Order.java")));
  }

  @Test
  void requestValidationReportsMissingInputs() {
    GeneratorRequest request = GeneratorRequest.of(List.of(), null);

    GeneratorResult result = new CoreGenerator().generate(request);

    assertFalse(result.successful());
    assertEquals(
        List.of(CoreGenerator.REQUEST_INVALID, CoreGenerator.REQUEST_INVALID),
        result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
  }

  @Test
  void duplicateRootHelperNamesReturnDiagnosticsWithoutWriting() throws IOException {
    Path schema = writeSchema("duplicate-roots.xsd", duplicateRootSchema());
    Path output = tempDirectory.resolve("duplicate-output");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), output));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    "SCHEMA_WRITER_EMISSION_INVALID_MODEL".equals(diagnostic.code())
                        || "SCHEMA_READER_EMISSION_INVALID_MODEL".equals(diagnostic.code())
                        || "SCHEMA_VALIDATOR_EMISSION_INVALID_MODEL".equals(diagnostic.code())));
    assertFalse(Files.exists(output));
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

  private Map<Path, String> readGeneratedSources(Path output, List<Path> relativePaths)
      throws IOException {
    java.util.LinkedHashMap<Path, String> sources = new java.util.LinkedHashMap<>();
    for (Path relativePath : relativePaths) {
      sources.put(
          relativePath, Files.readString(output.resolve(relativePath), StandardCharsets.UTF_8));
    }
    return Map.copyOf(sources);
  }

  private void compileGeneratedSources(Path output, List<Path> relativePaths) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "A JDK with JavaCompiler is required.");
    Path classes =
        tempDirectory.resolve("classes-" + Math.floorMod(output.hashCode(), Integer.MAX_VALUE));
    try {
      Files.createDirectories(classes);
    } catch (IOException exception) {
      fail(exception);
    }
    List<String> compilerArguments =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.of(
                    "--release",
                    "21",
                    "-Xlint:all",
                    "-Werror",
                    "-classpath",
                    System.getProperty("java.class.path"),
                    "-d",
                    classes.toString()),
                relativePaths.stream().map(path -> output.resolve(path).toString()))
            .toList();
    ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
    int exitCode =
        compiler.run(
            null, compilerOutput, compilerOutput, compilerArguments.toArray(String[]::new));
    if (exitCode != 0) {
      fail(compilerOutput.toString(StandardCharsets.UTF_8));
    }
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
              <xs:element name="line" type="p:Line" maxOccurs="unbounded"/>
            </xs:sequence>
            <xs:attribute name="version" type="xs:string" use="optional"/>
          </xs:complexType>
          <xs:complexType name="Line">
            <xs:sequence>
              <xs:element name="sku" type="xs:string"/>
              <xs:element name="quantity" type="xs:int"/>
            </xs:sequence>
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
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
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

  private String choiceOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
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
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:simpleType name="OrderCode">
            <xs:restriction base="xs:string">
              <xs:minLength value="3"/>
              <xs:maxLength value="8"/>
              <xs:pattern value="[A-Z0-9]+"/>
            </xs:restriction>
          </xs:simpleType>
          <xs:simpleType name="Priority">
            <xs:restriction base="xs:int">
              <xs:minInclusive value="1"/>
              <xs:maxInclusive value="9"/>
            </xs:restriction>
          </xs:simpleType>
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="code" type="o:OrderCode"/>
              <xs:element name="priority" type="o:Priority"/>
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
              <xs:element name="total" type="xs:decimal"/>
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

  private String duplicateRootSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:duplicate"
            xmlns:d="urn:duplicate"
            elementFormDefault="qualified">
          <xs:element name="first" type="d:Order"/>
          <xs:element name="second" type="d:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }
}
