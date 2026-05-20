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
import io.github.mundanej.mxjb.runtime.XmlOutput;
import io.github.mundanej.mxjb.runtime.XmlWriteException;
import io.github.mundanej.mxjb.runtime.jdkxml.JdkXmlAdapters;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
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

final class XpData10ChoiceConformanceTest {
  // Selected fixture manifest ID: T-CONF-XP-DATA-10-CHOICE-DOMESTIC.

  @TempDir private Path tempDirectory;

  @Test
  void choiceFixtureMatchesJdkSchemaValidationAndGeneratedBindings()
      throws IOException,
          SAXException,
          ReflectiveOperationException,
          XMLStreamException,
          XmlWriteException {
    Schema schema = jdkSchema();
    String validXml = resource("/xp-data-10-choice/choice-domestic-valid.xml");
    String invalidXml = resource("/xp-data-10-choice/choice-repeated-invalid.xml");

    schema.newValidator().validate(new StreamSource(new StringReader(validXml)));
    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(invalidXml))));

    try (CompiledGeneratedChoiceBindings bindings = generateAndCompileChoiceBindings()) {
      Class<?> orderClass = bindings.load("com.example.choice.Order");
      Class<?> readerClass = bindings.load("com.example.choice.xml.OrderXmlReader");
      Class<?> writerClass = bindings.load("com.example.choice.xml.OrderXmlWriter");
      Class<?> validatorClass = bindings.load("com.example.choice.xml.OrderXmlValidator");

      Object order =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(validXml));
      String written = writeXml(writerClass, orderClass, order);
      Object roundTripped =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(written));

      assertEquals(order, roundTripped);
      assertTrue(
          ((ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, order))
              .isValid());
      ValidationResult invalidResult =
          (ValidationResult)
              validatorClass
                  .getMethod("validate", XmlEventReader.class)
                  .invoke(null, readerFor(invalidXml));
      assertFalse(invalidResult.isValid());
      assertEquals("MXJB-GR-005", invalidResult.errors().getFirst().code());
    }
  }

  private CompiledGeneratedChoiceBindings generateAndCompileChoiceBindings() throws IOException {
    Path output = tempDirectory.resolve("generated");
    Path schema = resourcePath("/xp-data-10-choice/order.xsd");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_DATA_10_CHOICE,
            "com.example.generated",
            Map.of("urn:choice", "com.example.choice"),
            List.of(),
            Map.of());
    GeneratorResult result = new CoreGenerator().generate(request);
    assertTrue(result.successful(), result.diagnostics().toString());
    compileGeneratedSources(output, result.generatedSources());
    return new CompiledGeneratedChoiceBindings(
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

  private String writeXml(Class<?> writerClass, Class<?> orderClass, Object order)
      throws XMLStreamException, ReflectiveOperationException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    writerClass.getMethod("write", XmlOutput.class, orderClass).invoke(null, output, order);
    output.endDocument();
    output.flush();
    streamWriter.close();
    return target.toString();
  }

  private Schema jdkSchema() throws SAXException {
    return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(resourcePath("/xp-data-10-choice/order.xsd").toFile());
  }

  private XmlEventReader readerFor(String xml) throws XMLStreamException {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();
    XMLStreamReader streamReader = factory.createXMLStreamReader(new StringReader(xml));
    return JdkXmlAdapters.eventReader(streamReader);
  }

  private Path resourcePath(String resourceName) {
    URL resource = XpData10ChoiceConformanceTest.class.getResource(resourceName);
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
        XpData10ChoiceConformanceTest.class.getResourceAsStream(resourceName)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private record CompiledGeneratedChoiceBindings(URLClassLoader loader) implements AutoCloseable {
    Class<?> load(String className) throws ClassNotFoundException {
      return loader.loadClass(className);
    }

    @Override
    public void close() throws IOException {
      loader.close();
    }
  }
}
