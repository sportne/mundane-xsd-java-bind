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
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

final class XpXsd10DocumentConformanceTest {
  @TempDir private Path tempDirectory;

  @Test
  void wildcardFixturesMatchJdkSchemaValidationAndGeneratedBindings()
      throws IOException, SAXException, ReflectiveOperationException, XMLStreamException {
    Schema schema = jdkSchema();
    String validXml = resource("/xp-xsd10-document/document-valid.xml");
    String invalidXml = resource("/xp-xsd10-document/document-invalid.xml");

    schema.newValidator().validate(new StreamSource(new StringReader(validXml)));
    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(invalidXml))));

    try (CompiledGeneratedDocumentBindings bindings = generateAndCompileDocumentBindings()) {
      Class<?> orderClass = bindings.load("com.example.document.Order");
      Class<?> readerClass = bindings.load("com.example.document.xml.OrderXmlReader");
      Class<?> validatorClass = bindings.load("com.example.document.xml.OrderXmlValidator");

      Object order =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(validXml));
      Object wildcardContent = orderClass.getMethod("wildcardContent").invoke(order);
      assertTrue(wildcardContent instanceof List<?>);
      assertFalse(((List<?>) wildcardContent).isEmpty());

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
  void mixedContentFixturesMatchJdkSchemaValidationAndGeneratedBindings()
      throws IOException, SAXException, ReflectiveOperationException, XMLStreamException {
    Schema schema = jdkSchema("/xp-xsd10-document/mixed-order.xsd");
    String validXml = resource("/xp-xsd10-document/mixed-valid.xml");
    String invalidXml = resource("/xp-xsd10-document/mixed-invalid.xml");

    schema.newValidator().validate(new StreamSource(new StringReader(validXml)));
    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(invalidXml))));

    try (CompiledGeneratedDocumentBindings bindings =
        generateAndCompileDocumentBindings("/xp-xsd10-document/mixed-order.xsd")) {
      Class<?> orderClass = bindings.load("com.example.document.Order");
      Class<?> readerClass = bindings.load("com.example.document.xml.OrderXmlReader");
      Class<?> writerClass = bindings.load("com.example.document.xml.OrderXmlWriter");
      Class<?> validatorClass = bindings.load("com.example.document.xml.OrderXmlValidator");

      Object order =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(validXml));
      Object content = orderClass.getMethod("content").invoke(order);
      assertTrue(content instanceof List<?>);
      assertTrue(((List<?>) content).size() >= 5);

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
      String serialized = writeWithGeneratedWriter(writerClass, orderClass, order);
      assertTrue(serialized.indexOf("before") < serialized.indexOf("A-100"));
      assertTrue(serialized.indexOf("A-100") < serialized.indexOf("between"));
      assertTrue(serialized.indexOf("between") < serialized.indexOf("note"));
      assertTrue(serialized.indexOf("note") < serialized.indexOf("done"));
      assertTrue(serialized.indexOf("done") < serialized.indexOf("after"));
    }
  }

  @Test
  void wildcardSerializationPolicyRoundTripsThroughGeneratedBindings()
      throws IOException, SAXException, ReflectiveOperationException, XMLStreamException {
    Schema schema = jdkSchema();
    String validXml = resource("/xp-xsd10-document/document-valid.xml");

    try (CompiledGeneratedDocumentBindings bindings = generateAndCompileDocumentBindings()) {
      Class<?> orderClass = bindings.load("com.example.document.Order");
      Class<?> readerClass = bindings.load("com.example.document.xml.OrderXmlReader");
      Class<?> writerClass = bindings.load("com.example.document.xml.OrderXmlWriter");
      Class<?> validatorClass = bindings.load("com.example.document.xml.OrderXmlValidator");

      Object order =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(validXml));
      String serialized = writeWithGeneratedWriter(writerClass, orderClass, order);
      Object reparsed =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(serialized));
      ValidationResult validation =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, reparsed);

      schema.newValidator().validate(new StreamSource(new StringReader(serialized)));
      assertSerializationPolicyText(serialized);
      assertTrue(serialized.contains("xmlns:ns1=\"urn:document\""));
      assertTrue(serialized.contains("xmlns:ns2=\"urn:extension\""));
      assertFalse(serialized.contains("ext:"));
      assertTrue(serialized.indexOf("id") < serialized.indexOf("note"));
      assertEquals(order, reparsed);
      assertTrue(validation.isValid());
    }
  }

  @Test
  void mixedSerializationPolicyPreservesSemanticContentOrderWithoutCanonicalXmlClaims()
      throws IOException, SAXException, ReflectiveOperationException, XMLStreamException {
    Schema schema = jdkSchema("/xp-xsd10-document/mixed-order.xsd");
    String validXml = resource("/xp-xsd10-document/mixed-valid.xml");

    try (CompiledGeneratedDocumentBindings bindings =
        generateAndCompileDocumentBindings("/xp-xsd10-document/mixed-order.xsd")) {
      Class<?> orderClass = bindings.load("com.example.document.Order");
      Class<?> readerClass = bindings.load("com.example.document.xml.OrderXmlReader");
      Class<?> writerClass = bindings.load("com.example.document.xml.OrderXmlWriter");
      Class<?> validatorClass = bindings.load("com.example.document.xml.OrderXmlValidator");

      Object order =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(validXml));
      String serialized = writeWithGeneratedWriter(writerClass, orderClass, order);
      Object reparsed =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(serialized));
      ValidationResult validation =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, reparsed);

      schema.newValidator().validate(new StreamSource(new StringReader(serialized)));
      assertSerializationPolicyText(serialized);
      assertTrue(serialized.contains("xmlns:ns1=\"urn:mixed-document\""));
      assertTrue(serialized.contains("xmlns:ns2=\"urn:extension\""));
      assertFalse(serialized.contains("ext:"));
      assertTrue(serialized.indexOf("before") < serialized.indexOf("A-100"));
      assertTrue(serialized.indexOf("A-100") < serialized.indexOf("between"));
      assertTrue(serialized.indexOf("between") < serialized.indexOf("note"));
      assertTrue(serialized.indexOf("note") < serialized.indexOf("done"));
      assertTrue(serialized.indexOf("done") < serialized.indexOf("after"));
      assertEquals(order, reparsed);
      assertTrue(validation.isValid());
    }
  }

  private CompiledGeneratedDocumentBindings generateAndCompileDocumentBindings()
      throws IOException {
    return generateAndCompileDocumentBindings("/xp-xsd10-document/order.xsd");
  }

  private CompiledGeneratedDocumentBindings generateAndCompileDocumentBindings(
      String schemaResource) throws IOException {
    Path output = tempDirectory.resolve("generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(resourcePath(schemaResource)),
            output,
            GeneratorProfile.XP_XSD10_DOCUMENT,
            "com.example.generated",
            Map.of(
                "urn:document",
                "com.example.document",
                "urn:mixed-document",
                "com.example.document"),
            List.of(),
            Map.of());
    GeneratorResult result = new CoreGenerator().generate(request);
    assertTrue(result.successful(), result.diagnostics().toString());
    compileGeneratedSources(output, result.generatedSources());
    return new CompiledGeneratedDocumentBindings(
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
    return jdkSchema("/xp-xsd10-document/order.xsd");
  }

  private Schema jdkSchema(String schemaResource) throws SAXException {
    return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(resourcePath(schemaResource).toFile());
  }

  private String writeWithGeneratedWriter(Class<?> writerClass, Class<?> orderClass, Object order)
      throws ReflectiveOperationException, XMLStreamException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    XMLStreamWriter streamWriter =
        XMLOutputFactory.newFactory().createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
    writerClass
        .getMethod("write", io.github.mundanej.mxjb.runtime.XmlOutput.class, orderClass)
        .invoke(null, JdkXmlAdapters.output(streamWriter), order);
    streamWriter.close();
    return output.toString(StandardCharsets.UTF_8);
  }

  private XmlEventReader readerFor(String xml) throws XMLStreamException {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();
    XMLStreamReader streamReader = factory.createXMLStreamReader(new StringReader(xml));
    return JdkXmlAdapters.eventReader(streamReader);
  }

  private void assertSerializationPolicyText(String serialized) {
    assertFalse(serialized.contains(System.getProperty("user.dir")));
    assertFalse(serialized.contains(tempDirectory.toString()));
    assertFalse(serialized.contains("memory://"));
    assertFalse(serialized.contains("Canonical XML"));
    assertFalse(serialized.contains("XML Signature"));
  }

  private Path resourcePath(String resourceName) {
    URL resource = XpXsd10DocumentConformanceTest.class.getResource(resourceName);
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
        XpXsd10DocumentConformanceTest.class.getResourceAsStream(resourceName)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private record CompiledGeneratedDocumentBindings(URLClassLoader loader) implements AutoCloseable {
    Class<?> load(String className) throws ClassNotFoundException {
      return loader.loadClass(className);
    }

    @Override
    public void close() throws IOException {
      loader.close();
    }
  }
}
