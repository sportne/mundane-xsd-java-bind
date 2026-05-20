package io.github.mundanej.mxjb.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import io.github.mundanej.mxjb.generator.core.CoreGenerator;
import io.github.mundanej.mxjb.runtime.ValidationResult;
import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.jdkxml.JdkXmlAdapters;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

final class XpXsd10SemanticConformanceTest {
  // Selected fixture manifest IDs:
  // T-CONF-XP-XSD10-SEMANTIC-DEFAULTS, T-CONF-XP-XSD10-SEMANTIC-SUBSTITUTION.

  @TempDir private Path tempDirectory;

  @Test
  void semanticFixturesMatchJdkSchemaValidationAndGeneratedBindings()
      throws IOException, SAXException, ReflectiveOperationException, XMLStreamException {
    Schema schema = jdkSchema();
    String validXml = resource("/xp-xsd10-semantic/semantic-valid.xml");
    String invalidXml = resource("/xp-xsd10-semantic/semantic-invalid.xml");

    schema.newValidator().validate(new StreamSource(new StringReader(validXml)));
    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(invalidXml))));

    try (CompiledGeneratedSemanticBindings bindings = generateAndCompileSemanticBindings()) {
      Class<?> orderClass = bindings.load("com.example.semantic.Order");
      Class<?> readerClass = bindings.load("com.example.semantic.xml.OrderXmlReader");
      Class<?> validatorClass = bindings.load("com.example.semantic.xml.OrderXmlValidator");

      Object order =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(validXml));
      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, order);
      ValidationResult invalidResult =
          (ValidationResult)
              validatorClass
                  .getMethod("validate", XmlEventReader.class)
                  .invoke(null, readerFor(invalidXml));

      assertTrue(validResult.isValid());
      assertFalse(invalidResult.isValid());
      assertFalse(invalidResult.errors().isEmpty());
    }
  }

  @Test
  void semanticValidationFixturesReportDeterministicGeneratedDiagnostics()
      throws IOException, SAXException, ReflectiveOperationException, XMLStreamException {
    Schema schema = jdkSchema();
    String nilContentXml = resource("/xp-xsd10-semantic/semantic-nil-content-invalid.xml");
    String fixedMismatchXml = resource("/xp-xsd10-semantic/semantic-fixed-invalid.xml");

    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(nilContentXml))));
    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(fixedMismatchXml))));

    try (CompiledGeneratedSemanticBindings bindings = generateAndCompileSemanticBindings()) {
      Class<?> validatorClass = bindings.load("com.example.semantic.xml.OrderXmlValidator");

      ValidationResult nilResult =
          (ValidationResult)
              validatorClass
                  .getMethod("validate", XmlEventReader.class)
                  .invoke(null, readerFor(nilContentXml));
      ValidationResult fixedResult =
          (ValidationResult)
              validatorClass
                  .getMethod("validate", XmlEventReader.class)
                  .invoke(null, readerFor(fixedMismatchXml));

      assertEquals(List.of("MXJB-GR-009"), codes(nilResult));
      assertEquals(List.of("MXJB-GR-008"), codes(fixedResult));
    }
  }

  @Test
  void substitutionFixturesMatchJdkSchemaValidationAndGeneratedBindings()
      throws IOException, SAXException, ReflectiveOperationException, XMLStreamException {
    Schema schema = jdkSchema("/xp-xsd10-semantic/substitution-order.xsd");
    String validXml = resource("/xp-xsd10-semantic/substitution-valid.xml");
    String invalidXml = resource("/xp-xsd10-semantic/substitution-invalid.xml");

    schema.newValidator().validate(new StreamSource(new StringReader(validXml)));
    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(invalidXml))));

    try (CompiledGeneratedSemanticBindings bindings =
        generateAndCompileSemanticBindings(
            "/xp-xsd10-semantic/substitution-order.xsd",
            Map.of("urn:semantic-substitution", "com.example.substitution"))) {
      Class<?> orderClass = bindings.load("com.example.substitution.Order");
      Class<?> readerClass = bindings.load("com.example.substitution.xml.OrderXmlReader");
      Class<?> validatorClass = bindings.load("com.example.substitution.xml.OrderXmlValidator");

      Object order =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(validXml));
      Object payment = orderClass.getMethod("payment").invoke(order);
      assertTrue(payment instanceof java.util.Optional<?>);
      assertTrue(((java.util.Optional<?>) payment).isPresent());
      assertTrue(
          ((java.util.Optional<?>) payment)
              .orElseThrow()
              .getClass()
              .getName()
              .endsWith("CardpaymentSubstitutionBranch"));
      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, order);
      ValidationResult invalidResult =
          (ValidationResult)
              validatorClass
                  .getMethod("validate", XmlEventReader.class)
                  .invoke(null, readerFor(invalidXml));

      assertTrue(validResult.isValid());
      assertFalse(invalidResult.isValid());
      assertFalse(invalidResult.errors().isEmpty());
    }
  }

  private CompiledGeneratedSemanticBindings generateAndCompileSemanticBindings()
      throws IOException {
    return generateAndCompileSemanticBindings(
        "/xp-xsd10-semantic/order.xsd", Map.of("urn:semantic", "com.example.semantic"));
  }

  private CompiledGeneratedSemanticBindings generateAndCompileSemanticBindings(
      String schemaResource, Map<String, String> namespacePackages) throws IOException {
    Path output = tempDirectory.resolve("generated");
    Path schema = resourcePath(schemaResource);
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_XSD10_SEMANTIC,
            "com.example.generated",
            namespacePackages,
            List.of(),
            Map.of());
    GeneratorResult result = new CoreGenerator().generate(request);
    assertTrue(result.successful(), result.diagnostics().toString());
    compileGeneratedSources(output, result.generatedSources());
    return new CompiledGeneratedSemanticBindings(
        new URLClassLoader(
            new URL[] {tempDirectory.resolve("classes").toUri().toURL()},
            getClass().getClassLoader()));
  }

  private void compileGeneratedSources(Path output, List<Path> relativePaths) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "A JDK with JavaCompiler is required.");
    Path classes = tempDirectory.resolve("classes");
    Files.createDirectories(classes);
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

  private Schema jdkSchema() throws SAXException {
    return jdkSchema("/xp-xsd10-semantic/order.xsd");
  }

  private Schema jdkSchema(String schemaResource) throws SAXException {
    return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(resourcePath(schemaResource).toFile());
  }

  private XmlEventReader readerFor(String xml) throws XMLStreamException {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();
    XMLStreamReader streamReader = factory.createXMLStreamReader(new StringReader(xml));
    return JdkXmlAdapters.eventReader(streamReader);
  }

  private Path resourcePath(String resourceName) {
    URL resource = XpXsd10SemanticConformanceTest.class.getResource(resourceName);
    if (resource == null) {
      throw new IllegalArgumentException("Missing resource " + resourceName);
    }
    try {
      return Path.of(resource.toURI());
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException("Missing resource " + resourceName, exception);
    }
  }

  private static String resource(String resourceName) throws IOException {
    try (InputStream input =
        XpXsd10SemanticConformanceTest.class.getResourceAsStream(resourceName)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private List<String> codes(ValidationResult result) {
    return result.errors().stream().map(error -> error.code()).toList();
  }

  private record CompiledGeneratedSemanticBindings(URLClassLoader loader) implements AutoCloseable {
    Class<?> load(String className) throws ClassNotFoundException {
      return loader.loadClass(className);
    }

    @Override
    public void close() throws IOException {
      loader.close();
    }
  }
}
