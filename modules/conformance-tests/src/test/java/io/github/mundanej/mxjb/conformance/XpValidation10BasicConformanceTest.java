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
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

final class XpValidation10BasicConformanceTest {
  // Selected fixture manifest ID: T-CONF-XP-VALIDATION-10-BASIC-FACET.

  @TempDir private Path tempDirectory;

  @Test
  void facetFixturesMatchJdkSchemaValidationAndGeneratedBindings()
      throws IOException, SAXException, ReflectiveOperationException, XMLStreamException {
    Schema schema = jdkSchema();
    String validXml = resource("/xp-validation-10-basic/facet-valid.xml");
    String invalidXml = resource("/xp-validation-10-basic/facet-invalid.xml");

    schema.newValidator().validate(new StreamSource(new StringReader(validXml)));
    assertThrows(
        SAXException.class,
        () -> schema.newValidator().validate(new StreamSource(new StringReader(invalidXml))));

    try (CompiledGeneratedFacetBindings bindings = generateAndCompileFacetBindings()) {
      Class<?> orderClass = bindings.load("com.example.facet.Order");
      Class<?> readerClass = bindings.load("com.example.facet.xml.OrderXmlReader");
      Class<?> validatorClass = bindings.load("com.example.facet.xml.OrderXmlValidator");

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
      assertEquals(
          List.of("MXJB-GV-005", "MXJB-GV-007", "MXJB-GV-006"),
          invalidResult.errors().stream().map(error -> error.code()).toList());
    }
  }

  private CompiledGeneratedFacetBindings generateAndCompileFacetBindings() throws IOException {
    Path output = tempDirectory.resolve("generated");
    Path schema = resourcePath("/xp-validation-10-basic/order.xsd");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_VALIDATION_10_BASIC,
            "com.example.generated",
            Map.of("urn:facet", "com.example.facet"),
            List.of(),
            Map.of());
    GeneratorResult result = new CoreGenerator().generate(request);
    assertTrue(result.successful(), result.diagnostics().toString());
    compileGeneratedSources(output, result.generatedSources());
    return new CompiledGeneratedFacetBindings(
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
    return ConformanceSchemaFactories.newSchema(resourcePath("/xp-validation-10-basic/order.xsd"));
  }

  private XmlEventReader readerFor(String xml) throws XMLStreamException {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();
    XMLStreamReader streamReader = factory.createXMLStreamReader(new StringReader(xml));
    return JdkXmlAdapters.eventReader(streamReader);
  }

  private Path resourcePath(String resourceName) {
    URL resource = XpValidation10BasicConformanceTest.class.getResource(resourceName);
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
        XpValidation10BasicConformanceTest.class.getResourceAsStream(resourceName)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private record CompiledGeneratedFacetBindings(URLClassLoader loader) implements AutoCloseable {
    Class<?> load(String className) throws ClassNotFoundException {
      return loader.loadClass(className);
    }

    @Override
    public void close() throws IOException {
      loader.close();
    }
  }
}
