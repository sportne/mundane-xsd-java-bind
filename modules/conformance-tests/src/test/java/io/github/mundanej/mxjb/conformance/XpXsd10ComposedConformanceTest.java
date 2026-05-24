package io.github.mundanej.mxjb.conformance;

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

final class XpXsd10ComposedConformanceTest {
  // Selected fixture manifest IDs:
  // - T-CONF-XP-XSD10-COMPOSED-GROUPS
  // - T-CONF-XP-XSD10-COMPOSED-CONTENT-MODEL
  // - T-CONF-XP-XSD10-COMPOSED-SIMPLE-CONTENT

  @TempDir private Path tempDirectory;

  @Test
  void groupFixturesMatchJdkSchemaValidationAndGeneratedBindings()
      throws IOException, SAXException, ReflectiveOperationException, XMLStreamException {
    Schema schema = jdkSchema();
    String validXml = resource("/xp-xsd10-composed/composed-valid.xml");
    String invalidXml = resource("/xp-xsd10-composed/composed-invalid.xml");

    schema.newValidator().validate(new StreamSource(new StringReader(validXml)));
    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(invalidXml))));

    try (CompiledGeneratedComposedBindings bindings = generateAndCompileComposedBindings()) {
      Class<?> orderClass = bindings.load("com.example.composed.Order");
      Class<?> readerClass = bindings.load("com.example.composed.xml.OrderXmlReader");
      Class<?> validatorClass = bindings.load("com.example.composed.xml.OrderXmlValidator");

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
  void contentModelFixturesMatchJdkSchemaValidationAndGeneratedBindings()
      throws IOException, SAXException, ReflectiveOperationException, XMLStreamException {
    Schema schema = jdkSchema("/xp-xsd10-composed/content-model.xsd");
    String allValidXml = resource("/xp-xsd10-composed/content-model-all-valid.xml");
    String allInvalidXml = resource("/xp-xsd10-composed/content-model-all-invalid.xml");
    String choiceValidXml = resource("/xp-xsd10-composed/content-model-choice-valid.xml");
    String automataValidXml = resource("/xp-xsd10-composed/content-model-automata-valid.xml");
    String automataInvalidXml = resource("/xp-xsd10-composed/content-model-automata-invalid.xml");

    schema.newValidator().validate(new StreamSource(new StringReader(allValidXml)));
    schema.newValidator().validate(new StreamSource(new StringReader(choiceValidXml)));
    schema.newValidator().validate(new StreamSource(new StringReader(automataValidXml)));
    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(allInvalidXml))));
    assertThrows(
        SAXException.class,
        () ->
            schema.newValidator().validate(new StreamSource(new StringReader(automataInvalidXml))));

    try (CompiledGeneratedComposedBindings bindings =
        generateAndCompileComposedBindings("/xp-xsd10-composed/content-model.xsd", "content")) {
      Class<?> allOrderClass = bindings.load("com.example.content.Allorder");
      Class<?> allReaderClass = bindings.load("com.example.content.xml.AllorderXmlReader");
      Class<?> allValidatorClass = bindings.load("com.example.content.xml.AllorderXmlValidator");
      Class<?> choiceOrderClass = bindings.load("com.example.content.Choiceorder");
      Class<?> choiceReaderClass = bindings.load("com.example.content.xml.ChoiceorderXmlReader");
      Class<?> choiceValidatorClass =
          bindings.load("com.example.content.xml.ChoiceorderXmlValidator");
      Class<?> automataOrderClass = bindings.load("com.example.content.Automataorder");
      Class<?> automataReaderClass =
          bindings.load("com.example.content.xml.AutomataorderXmlReader");
      Class<?> automataValidatorClass =
          bindings.load("com.example.content.xml.AutomataorderXmlValidator");

      Object allOrder =
          allReaderClass
              .getMethod("read", XmlEventReader.class)
              .invoke(null, readerFor(allValidXml));
      Object choiceOrder =
          choiceReaderClass
              .getMethod("read", XmlEventReader.class)
              .invoke(null, readerFor(choiceValidXml));
      Object automataOrder =
          automataReaderClass
              .getMethod("read", XmlEventReader.class)
              .invoke(null, readerFor(automataValidXml));
      ValidationResult allValidResult =
          (ValidationResult)
              allValidatorClass.getMethod("validate", allOrderClass).invoke(null, allOrder);
      ValidationResult choiceValidResult =
          (ValidationResult)
              choiceValidatorClass
                  .getMethod("validate", choiceOrderClass)
                  .invoke(null, choiceOrder);
      ValidationResult automataValidResult =
          (ValidationResult)
              automataValidatorClass
                  .getMethod("validate", automataOrderClass)
                  .invoke(null, automataOrder);
      ValidationResult allInvalidResult =
          (ValidationResult)
              allValidatorClass
                  .getMethod("validate", XmlEventReader.class)
                  .invoke(null, readerFor(allInvalidXml));
      ValidationResult automataInvalidResult =
          (ValidationResult)
              automataValidatorClass
                  .getMethod("validate", XmlEventReader.class)
                  .invoke(null, readerFor(automataInvalidXml));

      assertTrue(allValidResult.isValid());
      assertTrue(choiceValidResult.isValid());
      assertTrue(automataValidResult.isValid());
      assertFalse(allInvalidResult.isValid());
      assertFalse(allInvalidResult.errors().isEmpty());
      assertFalse(automataInvalidResult.isValid());
      assertFalse(automataInvalidResult.errors().isEmpty());
    }
  }

  @Test
  void simpleContentFixturesMatchJdkSchemaValidationAndGeneratedBindings()
      throws IOException,
          SAXException,
          ReflectiveOperationException,
          XMLStreamException,
          XmlWriteException {
    Schema schema = jdkSchema("/xp-xsd10-composed/simple-content.xsd");
    String validXml = resource("/xp-xsd10-composed/simple-content-valid.xml");
    String invalidXml = resource("/xp-xsd10-composed/simple-content-invalid.xml");

    schema.newValidator().validate(new StreamSource(new StringReader(validXml)));
    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(invalidXml))));

    try (CompiledGeneratedComposedBindings bindings =
        generateAndCompileComposedBindings("/xp-xsd10-composed/simple-content.xsd", "simple")) {
      Class<?> noteClass = bindings.load("com.example.simplecontent.Note");
      Class<?> readerClass = bindings.load("com.example.simplecontent.xml.NoteXmlReader");
      Class<?> writerClass = bindings.load("com.example.simplecontent.xml.NoteXmlWriter");
      Class<?> validatorClass = bindings.load("com.example.simplecontent.xml.NoteXmlValidator");

      Object note =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(validXml));
      assertTrue(noteClass.getMethod("value").invoke(note).equals("ready"));
      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", noteClass).invoke(null, note);
      ValidationResult invalidResult =
          (ValidationResult)
              validatorClass
                  .getMethod("validate", XmlEventReader.class)
                  .invoke(null, readerFor(invalidXml));
      String writtenXml = writeGenerated(writerClass, noteClass, note);

      schema.newValidator().validate(new StreamSource(new StringReader(writtenXml)));
      assertTrue(validResult.isValid());
      assertFalse(invalidResult.isValid());
      assertFalse(invalidResult.errors().isEmpty());
    }
  }

  private CompiledGeneratedComposedBindings generateAndCompileComposedBindings()
      throws IOException {
    return generateAndCompileComposedBindings("/xp-xsd10-composed/order.xsd", "generated");
  }

  private CompiledGeneratedComposedBindings generateAndCompileComposedBindings(
      String schemaResource, String outputName) throws IOException {
    Path schema = resourcePath(schemaResource);
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            tempDirectory.resolve(outputName),
            GeneratorProfile.XP_XSD10_COMPOSED,
            "com.example.generated",
            Map.of(
                "urn:composed",
                "com.example.composed",
                "urn:content-model",
                "com.example.content",
                "urn:simple-content",
                "com.example.simplecontent"),
            List.of(),
            Map.of());
    GeneratorResult result = new CoreGenerator().generate(request);
    assertTrue(result.successful(), result.diagnostics().toString());
    compileGeneratedSources(tempDirectory.resolve(outputName), result.generatedSources());
    return new CompiledGeneratedComposedBindings(
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
    return jdkSchema("/xp-xsd10-composed/order.xsd");
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

  private String writeGenerated(Class<?> writerClass, Class<?> valueClass, Object value)
      throws ReflectiveOperationException, XMLStreamException, XmlWriteException {
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
    XMLStreamWriter streamWriter =
        XMLOutputFactory.newFactory()
            .createXMLStreamWriter(outputBytes, StandardCharsets.UTF_8.name());
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    writerClass.getMethod("write", XmlOutput.class, valueClass).invoke(null, output, value);
    output.flush();
    streamWriter.close();
    return outputBytes.toString(StandardCharsets.UTF_8);
  }

  private Path resourcePath(String resourceName) {
    URL resource = XpXsd10ComposedConformanceTest.class.getResource(resourceName);
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
        XpXsd10ComposedConformanceTest.class.getResourceAsStream(resourceName)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private record CompiledGeneratedComposedBindings(URLClassLoader loader) implements AutoCloseable {
    Class<?> load(String className) throws ClassNotFoundException {
      return loader.loadClass(className);
    }

    @Override
    public void close() throws IOException {
      loader.close();
    }
  }
}
