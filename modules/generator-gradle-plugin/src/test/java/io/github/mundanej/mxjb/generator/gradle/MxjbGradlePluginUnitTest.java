package io.github.mundanej.mxjb.generator.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MxjbGradlePluginUnitTest {
  @TempDir private Path projectDirectory;

  @Test
  void pluginRegistersExtensionTaskAndWiresJavaCompilation() {
    Project project = ProjectBuilder.builder().withProjectDir(projectDirectory.toFile()).build();
    project.getPluginManager().apply("java");
    project.getPluginManager().apply(MxjbGradlePlugin.class);

    assertTrue(
        project.getExtensions().findByName(MxjbGradlePlugin.EXTENSION_NAME)
            instanceof MxjbExtension);
    assertTrue(
        project.getTasks().findByName(MxjbGradlePlugin.GENERATE_TASK_NAME)
            instanceof MxjbGenerateTask);
    org.gradle.api.Task compileJava = project.getTasks().getByName("compileJava");
    org.gradle.api.Task generateMxjbSources =
        project.getTasks().getByName(MxjbGradlePlugin.GENERATE_TASK_NAME);
    assertTrue(
        compileJava
            .getTaskDependencies()
            .getDependencies(compileJava)
            .contains(generateMxjbSources));
  }

  @Test
  void taskGeneratesSourcesAndReplacesStaleOutput() throws IOException {
    Project project = configuredProject();
    Path schema =
        writeSchema("src/main/resources/schema/purchase-order.xsd", purchaseOrderSchema());
    Path stale = projectDirectory.resolve("build/generated/sources/mxjb/java/stale/Stale.java");
    Files.createDirectories(requireParent(stale));
    Files.writeString(stale, "final class Stale {}", StandardCharsets.UTF_8);

    MxjbExtension extension = extension(project);
    extension.schema(schema.toFile());
    extension.localRoot(requireParent(schema).toFile());
    extension.namespacePackage("urn:purchase", "com.example.purchase");
    task(project).generate();

    assertTrue(Files.exists(generatedPath("com/example/purchase/Order.java")));
    assertTrue(Files.exists(generatedPath("com/example/purchase/xml/OrderXmlReader.java")));
    assertTrue(Files.notExists(stale));
  }

  @Test
  void taskAcceptsChoiceProfileToken() throws IOException {
    Project project = configuredProject();
    Path schema = writeSchema("src/main/resources/schema/choice-order.xsd", choiceOrderSchema());

    MxjbExtension extension = extension(project);
    extension.schema(schema.toFile());
    extension.localRoot(requireParent(schema).toFile());
    extension.namespacePackage("urn:orders", "com.example.orders");
    extension.getProfile().set("XP-DATA-10-CHOICE");
    task(project).generate();

    assertTrue(Files.exists(generatedPath("com/example/orders/OrderChoice.java")));
    assertTrue(Files.exists(generatedPath("com/example/orders/DomesticChoice.java")));
  }

  @Test
  void taskAcceptsBasicValidationProfileToken() throws IOException {
    Project project = configuredProject();
    Path schema = writeSchema("src/main/resources/schema/facet-order.xsd", facetOrderSchema());

    MxjbExtension extension = extension(project);
    extension.schema(schema.toFile());
    extension.localRoot(requireParent(schema).toFile());
    extension.namespacePackage("urn:orders", "com.example.orders");
    extension.getProfile().set("XP-VALIDATION-10-BASIC");
    task(project).generate();

    assertTrue(Files.exists(generatedPath("com/example/orders/Order.java")));
    assertTrue(
        Files.readString(generatedPath("com/example/orders/xml/OrderXmlValidator.java"))
            .contains("MXJB-GV-007"));
  }

  @Test
  void taskAcceptsComposedProfileToken() throws IOException {
    Project project = configuredProject();
    Path schema =
        writeSchema("src/main/resources/schema/composed-order.xsd", composedOrderSchema());

    MxjbExtension extension = extension(project);
    extension.schema(schema.toFile());
    extension.localRoot(requireParent(schema).toFile());
    extension.namespacePackage("urn:orders", "com.example.orders");
    extension.getProfile().set("XP-XSD10-COMPOSED");
    task(project).generate();

    assertTrue(Files.exists(generatedPath("com/example/orders/Order.java")));
    assertTrue(
        Files.readString(generatedPath("com/example/orders/Order.java"))
            .contains("String version"));
  }

  @Test
  void taskAcceptsDocumentProfileToken() throws IOException {
    Project project = configuredProject();
    Path schema =
        writeSchema("src/main/resources/schema/document-order.xsd", documentOrderSchema());

    MxjbExtension extension = extension(project);
    extension.schema(schema.toFile());
    extension.localRoot(requireParent(schema).toFile());
    extension.namespacePackage("urn:orders", "com.example.orders");
    extension.getProfile().set("XP-XSD10-DOCUMENT");
    task(project).generate();

    assertTrue(Files.exists(generatedPath("com/example/orders/Order.java")));
    assertTrue(
        Files.readString(generatedPath("com/example/orders/Order.java"))
            .contains("List<XmlFragment> wildcardContent"));
  }

  @Test
  void taskResolvesCatalogMappings() throws IOException {
    Project project = configuredProject();
    Path order =
        writeSchema(
            "src/main/resources/schema/order.xsd", orderSchema("https://example.invalid/line.xsd"));
    Path line = writeSchema("src/main/resources/schema/line.xsd", lineSchema());

    MxjbExtension extension = extension(project);
    extension.schema(order.toFile());
    extension.localRoot(requireParent(order).toFile());
    extension.catalog("https://example.invalid/line.xsd", line.toFile());
    extension.namespacePackage("urn:orders", "com.example.orders");
    extension.namespacePackage("urn:lines", "com.example.lines");
    task(project).generate();

    assertTrue(Files.exists(generatedPath("com/example/orders/Order.java")));
    assertTrue(Files.exists(generatedPath("com/example/lines/Line.java")));
  }

  @Test
  void invalidProfileThrowsStableGradleDiagnostic() throws IOException {
    Project project = configuredProject();
    Path schema =
        writeSchema("src/main/resources/schema/purchase-order.xsd", purchaseOrderSchema());
    MxjbExtension extension = extension(project);
    extension.schema(schema.toFile());
    extension.getProfile().set("XP-DATA-11");

    GradleException exception = assertThrows(GradleException.class, () -> task(project).generate());

    assertTrue(exception.getMessage().contains("GENERATOR_GRADLE_INVALID_ARGUMENT"));
    assertTrue(exception.getMessage().contains("XP-DATA-11"));
  }

  @Test
  void generatorDiagnosticsAreReportedAsManifestLines() throws IOException {
    Project project = configuredProject();
    Path schema =
        writeSchema(
            "src/main/resources/schema/order.xsd", orderSchema("https://example.invalid/line.xsd"));
    extension(project).schema(schema.toFile());

    GradleException exception = assertThrows(GradleException.class, () -> task(project).generate());

    assertTrue(exception.getMessage().contains("SCHEMA_RESOURCE_NETWORK_DENIED"));
    assertTrue(exception.getMessage().contains(" | "));
  }

  @Test
  void missingSchemaReportsRequestDiagnostic() {
    Project project = configuredProject();

    GradleException exception = assertThrows(GradleException.class, () -> task(project).generate());

    assertTrue(exception.getMessage().contains("GENERATOR_REQUEST_INVALID"));
    assertEquals("XP-DATA-10", task(project).getProfile().get());
  }

  private Project configuredProject() {
    Project project = ProjectBuilder.builder().withProjectDir(projectDirectory.toFile()).build();
    project.getPluginManager().apply("java");
    project.getPluginManager().apply(MxjbGradlePlugin.class);
    return project;
  }

  private MxjbExtension extension(Project project) {
    return (MxjbExtension) project.getExtensions().getByName(MxjbGradlePlugin.EXTENSION_NAME);
  }

  private MxjbGenerateTask task(Project project) {
    return (MxjbGenerateTask) project.getTasks().getByName(MxjbGradlePlugin.GENERATE_TASK_NAME);
  }

  private Path generatedPath(String relativePath) {
    return projectDirectory.resolve("build/generated/sources/mxjb/java").resolve(relativePath);
  }

  private Path writeSchema(String relativePath, String content) throws IOException {
    Path target = projectDirectory.resolve(relativePath);
    Path parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(target, content, StandardCharsets.UTF_8);
    return target;
  }

  private Path requireParent(Path path) {
    Path parent = path.getParent();
    if (parent == null) {
      throw new IllegalArgumentException("Path has no parent: " + path);
    }
    return parent;
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

  private String documentOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            elementFormDefault="qualified">
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:any namespace="##other" processContents="skip" minOccurs="0" maxOccurs="unbounded"/>
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
}
