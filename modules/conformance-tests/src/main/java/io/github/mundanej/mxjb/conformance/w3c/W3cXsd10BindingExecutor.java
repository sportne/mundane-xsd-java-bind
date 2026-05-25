package io.github.mundanej.mxjb.conformance.w3c;

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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
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
import org.xml.sax.SAXException;

/** Executes explicitly mapped W3C rows through generated bindings. */
final class W3cXsd10BindingExecutor {
  private static final List<Mapping> MAPPINGS =
      List.of(
          new Mapping(
              "sunData/AttrDecl/AD_name/AD_name00101m/AD_name00101m1.xsd",
              List.of("sunData/AttrDecl/AD_name/AD_name00101m/AD_name00101m1_p.xml"),
              List.of("sunData/AttrDecl/AD_name/AD_name00101m/AD_name00101m1_n.xml"),
              "AttrDecl/name",
              "com.example.w3c.attrdecl",
              "Root"),
          new Mapping(
              "sunData/Wildcard/nsConstraint/nsConstraint00101m/nsConstraint00101m1.xsd",
              List.of("sunData/Wildcard/nsConstraint/nsConstraint00101m/nsConstraint00101m1_p.xml"),
              List.of("sunData/Wildcard/nsConstraint/nsConstraint00101m/nsConstraint00101m1_n.xml"),
              "nsConstraint",
              "com.example.w3c.wildcard",
              "A"),
          new Mapping(
              "sunData/Wildcard/psContents/psContents00102m/psContents00102m1.xsd",
              List.of("sunData/Wildcard/psContents/psContents00102m/psContents00102m1_p.xml"),
              List.of("sunData/Wildcard/psContents/psContents00102m/psContents00102m1_n.xml"),
              "psContents",
              "com.example.w3c.wildcard.strict",
              "A"));

  List<Execution> execute(
      List<W3cXsd10SuiteIntake.Fixture> fixtures, Path suiteRoot, Path reportDirectory)
      throws IOException {
    List<Execution> executions = new java.util.ArrayList<>();
    for (Mapping mapping : MAPPINGS) {
      List<W3cXsd10SuiteIntake.Fixture> mappedFixtures =
          fixtures.stream()
              .filter(fixture -> Objects.equals(mappingFor(fixture.document()), mapping))
              .sorted(Comparator.comparing(W3cXsd10SuiteIntake.Fixture::id))
              .toList();
      int expectedRows =
          1 + mapping.validInstanceDocuments().size() + mapping.invalidInstanceDocuments().size();
      if (mappedFixtures.size() != expectedRows) {
        throw new IllegalStateException(
            "Expected "
                + expectedRows
                + " mapped W3C rows for "
                + mapping.schemaDocument()
                + " but found "
                + mappedFixtures.size());
      }
      if (mappedFixtures.stream()
          .anyMatch(
              fixture ->
                  !W3cXsd10SuiteIntake.Category.BINDING_SUPPORTED.equals(fixture.category()))) {
        throw new IllegalStateException(
            "Mapped W3C rows must be classified as binding-supported for "
                + mapping.schemaDocument());
      }
      executeMapping(mapping, suiteRoot, reportDirectory);
      executions.add(
          new Execution(
              mapping.id(),
              mapping.schemaDocument(),
              mapping.validInstanceDocuments().size(),
              mapping.invalidInstanceDocuments().size(),
              "passed"));
    }
    return List.copyOf(executions);
  }

  static Mapping mappingFor(String document) {
    return MAPPINGS.stream()
        .filter(
            mapping ->
                mapping.schemaDocument().equals(document)
                    || mapping.validInstanceDocuments().contains(document)
                    || mapping.invalidInstanceDocuments().contains(document))
        .findFirst()
        .orElse(null);
  }

  private static void executeMapping(Mapping mapping, Path suiteRoot, Path reportDirectory)
      throws IOException {
    Path schemaPath = suiteRoot.resolve(mapping.schemaDocument());
    Path outputDirectory = reportDirectory.resolve("generated-bindings").resolve(mapping.id());
    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schemaPath),
                    outputDirectory,
                    GeneratorProfile.XP_XSD10_DOCUMENT,
                    "com.example.w3c.generated",
                    Map.of(mapping.namespace(), mapping.packageName()),
                    List.of(),
                    Map.of()));
    if (!result.successful()) {
      throw new IllegalStateException(
          "Mapped W3C generated-binding fixture failed generation: "
              + mapping.schemaDocument()
              + " diagnostics="
              + result.diagnostics());
    }
    Path classesDirectory = reportDirectory.resolve("classes").resolve(mapping.id());
    compileGeneratedSources(outputDirectory, result.generatedSources(), classesDirectory);
    try (URLClassLoader classLoader =
        new URLClassLoader(
            new URL[] {classesDirectory.toUri().toURL()},
            W3cXsd10BindingExecutor.class.getClassLoader())) {
      executeGeneratedRoundTrips(mapping, suiteRoot, schemaPath, classLoader);
    }
  }

  private static void compileGeneratedSources(
      Path outputDirectory, List<Path> relativeSources, Path classesDirectory) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new IllegalStateException("A JDK with JavaCompiler is required for W3C mapping.");
    }
    Files.createDirectories(classesDirectory);
    List<String> compilerArguments =
        Stream.concat(
                Stream.of(
                    "--release",
                    "21",
                    "-Xlint:all",
                    "-Werror",
                    "-classpath",
                    existingJavaClasspath(),
                    "-d",
                    classesDirectory.toString()),
                relativeSources.stream().map(path -> outputDirectory.resolve(path).toString()))
            .toList();
    ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
    int exitCode =
        compiler.run(
            null, compilerOutput, compilerOutput, compilerArguments.toArray(String[]::new));
    if (exitCode != 0) {
      throw new IllegalStateException(
          "Generated W3C binding compilation failed: "
              + compilerOutput.toString(StandardCharsets.UTF_8));
    }
  }

  private static void executeGeneratedRoundTrips(
      Mapping mapping, Path suiteRoot, Path schemaPath, ClassLoader classLoader)
      throws IOException {
    try {
      Schema schema = secureSchemaFactory().newSchema(schemaPath.toFile());
      Class<?> modelClass =
          classLoader.loadClass(mapping.packageName() + "." + mapping.rootClass());
      Class<?> readerClass =
          classLoader.loadClass(
              mapping.packageName() + ".xml." + mapping.rootClass() + "XmlReader");
      Class<?> writerClass =
          classLoader.loadClass(
              mapping.packageName() + ".xml." + mapping.rootClass() + "XmlWriter");
      Class<?> validatorClass =
          classLoader.loadClass(
              mapping.packageName() + ".xml." + mapping.rootClass() + "XmlValidator");
      for (String document : mapping.validInstanceDocuments()) {
        String xml = Files.readString(suiteRoot.resolve(document), StandardCharsets.UTF_8);
        String bindingXml = generatedBindingXml(xml);
        schema.newValidator().validate(new StreamSource(new StringReader(xml)));
        Object model =
            readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(bindingXml));
        ValidationResult result =
            (ValidationResult) validatorClass.getMethod("validate", modelClass).invoke(null, model);
        if (!result.isValid()) {
          throw new IllegalStateException("Mapped W3C value failed validation: " + result.errors());
        }
        String writtenXml = writeGenerated(writerClass, modelClass, model);
        schema.newValidator().validate(new StreamSource(new StringReader(writtenXml)));
        Object reread =
            readerClass.getMethod("read", XmlEventReader.class).invoke(null, readerFor(writtenXml));
        ValidationResult rereadResult =
            (ValidationResult)
                validatorClass.getMethod("validate", modelClass).invoke(null, reread);
        if (!rereadResult.isValid()) {
          throw new IllegalStateException(
              "Mapped W3C written value failed validation: " + rereadResult.errors());
        }
      }
      for (String document : mapping.invalidInstanceDocuments()) {
        String xml = Files.readString(suiteRoot.resolve(document), StandardCharsets.UTF_8);
        String bindingXml = generatedBindingXml(xml);
        assertJdkInvalid(schema, xml, document);
        ValidationResult result =
            (ValidationResult)
                validatorClass
                    .getMethod("validate", XmlEventReader.class)
                    .invoke(null, readerFor(bindingXml));
        if (result.isValid()) {
          throw new IllegalStateException(
              "Mapped W3C invalid instance passed generated validation: " + document);
        }
      }
    } catch (ClassNotFoundException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException
        | SAXException
        | XMLStreamException
        | XmlWriteException exception) {
      throw new IOException(
          "Unable to execute mapped W3C generated binding " + mapping.id(), exception);
    }
  }

  private static XmlEventReader readerFor(String xml) throws XMLStreamException {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();
    XMLStreamReader streamReader = factory.createXMLStreamReader(new StringReader(xml));
    return JdkXmlAdapters.eventReader(streamReader);
  }

  static SchemaFactory secureSchemaFactory() throws SAXException {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  private static String generatedBindingXml(String xml) throws XMLStreamException {
    XMLStreamReader reader =
        JdkXmlAdapters.secureInputFactory().createXMLStreamReader(new StringReader(xml));
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
    XMLStreamWriter writer =
        XMLOutputFactory.newFactory()
            .createXMLStreamWriter(outputBytes, StandardCharsets.UTF_8.name());
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamReader.START_ELEMENT) {
        writer.writeStartElement(
            nullToEmpty(reader.getPrefix()),
            reader.getLocalName(),
            nullToEmpty(reader.getNamespaceURI()));
        for (int index = 0; index < reader.getNamespaceCount(); index++) {
          writer.writeNamespace(
              nullToEmpty(reader.getNamespacePrefix(index)),
              nullToEmpty(reader.getNamespaceURI(index)));
        }
        for (int index = 0; index < reader.getAttributeCount(); index++) {
          if (isSchemaLocationHint(
              reader.getAttributeNamespace(index), reader.getAttributeLocalName(index))) {
            continue;
          }
          writer.writeAttribute(
              nullToEmpty(reader.getAttributePrefix(index)),
              nullToEmpty(reader.getAttributeNamespace(index)),
              reader.getAttributeLocalName(index),
              reader.getAttributeValue(index));
        }
      } else if (event == XMLStreamReader.END_ELEMENT) {
        writer.writeEndElement();
      } else if (event == XMLStreamReader.CHARACTERS || event == XMLStreamReader.SPACE) {
        writer.writeCharacters(reader.getText());
      } else if (event == XMLStreamReader.CDATA) {
        writer.writeCData(reader.getText());
      }
    }
    writer.close();
    reader.close();
    return outputBytes.toString(StandardCharsets.UTF_8);
  }

  private static boolean isSchemaLocationHint(String namespace, String localName) {
    return XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI.equals(namespace)
        && ("schemaLocation".equals(localName) || "noNamespaceSchemaLocation".equals(localName));
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String writeGenerated(Class<?> writerClass, Class<?> modelClass, Object model)
      throws IllegalAccessException,
          InvocationTargetException,
          NoSuchMethodException,
          XMLStreamException,
          XmlWriteException {
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
    XMLStreamWriter streamWriter =
        XMLOutputFactory.newFactory()
            .createXMLStreamWriter(outputBytes, StandardCharsets.UTF_8.name());
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    writerClass.getMethod("write", XmlOutput.class, modelClass).invoke(null, output, model);
    output.flush();
    streamWriter.close();
    return outputBytes.toString(StandardCharsets.UTF_8);
  }

  private static void assertJdkInvalid(Schema schema, String xml, String document)
      throws IOException {
    try {
      schema.newValidator().validate(new StreamSource(new StringReader(xml)));
    } catch (SAXException exception) {
      return;
    }
    throw new IllegalStateException(
        "Mapped W3C invalid instance passed JDK validation: " + document);
  }

  private static String existingJavaClasspath() {
    return Stream.of(System.getProperty("java.class.path").split(File.pathSeparator))
        .filter(entry -> !entry.isBlank())
        .filter(entry -> Files.exists(Path.of(entry)))
        .collect(java.util.stream.Collectors.joining(File.pathSeparator));
  }

  private static String sanitize(String value) {
    return value.replaceAll("[^A-Za-z0-9_.-]", "_");
  }

  record Mapping(
      String schemaDocument,
      List<String> validInstanceDocuments,
      List<String> invalidInstanceDocuments,
      String namespace,
      String packageName,
      String rootClass) {
    Mapping {
      validInstanceDocuments = List.copyOf(validInstanceDocuments);
      invalidInstanceDocuments = List.copyOf(invalidInstanceDocuments);
    }

    String id() {
      return sanitize(schemaDocument);
    }
  }

  record Execution(
      String id, String schemaDocument, int validInstances, int invalidInstances, String status) {
    String toTsv() {
      return String.join(
          "\t",
          id,
          schemaDocument,
          Integer.toString(validInstances),
          Integer.toString(invalidInstances),
          status);
    }
  }
}
