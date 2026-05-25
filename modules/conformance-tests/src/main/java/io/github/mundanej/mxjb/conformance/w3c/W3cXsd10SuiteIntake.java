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
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** Dependency-free intake for the pinned W3C XML Schema 1.0 suite metadata. */
public final class W3cXsd10SuiteIntake {
  static final String EXPECTED_ROOT_NAME = "xmlschema2006-11-06";
  static final int EXPECTED_TEST_SET_COUNT = 15;
  static final String RELEASE_URL =
      "https://www.w3.org/XML/2004/xml-schema-test-suite/xmlschema2006-11-06/"
          + "xsts-2007-06-20.tar.gz";
  static final String ARCHIVE_SHA256 =
      "902176b25e4111cf96b08663107521a4992e8ea67aad6b815592a6a5b4b9ea06";
  private static final String TS_NS = "http://www.w3.org/XML/2004/xml-schema-test-suite/";
  private static final String XLINK_NS = "http://www.w3.org/1999/xlink";
  private static final List<BindingMapping> BINDING_MAPPINGS =
      List.of(
          new BindingMapping(
              "sunData/AttrDecl/AD_name/AD_name00101m/AD_name00101m1.xsd",
              List.of("sunData/AttrDecl/AD_name/AD_name00101m/AD_name00101m1_p.xml"),
              List.of("sunData/AttrDecl/AD_name/AD_name00101m/AD_name00101m1_n.xml"),
              "AttrDecl/name",
              "com.example.w3c.attrdecl",
              "Root"),
          new BindingMapping(
              "sunData/Wildcard/nsConstraint/nsConstraint00101m/nsConstraint00101m1.xsd",
              List.of("sunData/Wildcard/nsConstraint/nsConstraint00101m/nsConstraint00101m1_p.xml"),
              List.of("sunData/Wildcard/nsConstraint/nsConstraint00101m/nsConstraint00101m1_n.xml"),
              "nsConstraint",
              "com.example.w3c.wildcard",
              "A"));
  private static final Set<String> KNOWN_VALIDITY = Set.of("valid", "invalid");
  private static final Set<String> KNOWN_STATUS = Set.of("accepted", "queried");

  public Report run(Path suiteRoot, Path reportDirectory) throws IOException {
    List<Fixture> fixtures = parse(suiteRoot);
    Files.createDirectories(reportDirectory);
    for (Fixture fixture : fixtures) {
      executeExpectedDiagnostic(fixture, suiteRoot, reportDirectory);
    }
    List<BindingExecution> bindingExecutions =
        executeBindingMappings(fixtures, suiteRoot, reportDirectory);
    writeReport(reportDirectory.resolve("fixtures.tsv"), fixtures);
    writeBindingExecutions(reportDirectory.resolve("binding-executions.tsv"), bindingExecutions);
    writeSummary(reportDirectory.resolve("summary.txt"), suiteRoot, fixtures, bindingExecutions);
    return Report.from(fixtures, bindingExecutions.size());
  }

  List<Fixture> parse(Path suiteRoot) throws IOException {
    validateSuiteRoot(suiteRoot);
    List<Path> testSets;
    try (var stream = Files.walk(suiteRoot)) {
      testSets = stream.filter(path -> fileName(path).endsWith(".testSet")).sorted().toList();
    }
    if (testSets.size() != EXPECTED_TEST_SET_COUNT) {
      throw new IllegalArgumentException(
          "Expected "
              + EXPECTED_TEST_SET_COUNT
              + " W3C XSD 1.0 .testSet files but found "
              + testSets.size()
              + " under "
              + suiteRoot);
    }

    List<Fixture> fixtures = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    for (Path testSet : testSets) {
      fixtures.addAll(parseTestSet(suiteRoot, testSet, ids));
    }
    fixtures.sort(Comparator.comparing(Fixture::id));
    return List.copyOf(fixtures);
  }

  private static void validateSuiteRoot(Path suiteRoot) {
    if (!Files.isDirectory(suiteRoot)) {
      throw new IllegalArgumentException("Missing W3C XSD 1.0 suite directory: " + suiteRoot);
    }
    if (!EXPECTED_ROOT_NAME.equals(fileName(suiteRoot))) {
      throw new IllegalArgumentException(
          "Expected W3C XSD 1.0 suite root named "
              + EXPECTED_ROOT_NAME
              + " but got "
              + fileName(suiteRoot));
    }
  }

  private static List<Fixture> parseTestSet(Path suiteRoot, Path testSet, Set<String> ids)
      throws IOException {
    Document document = parseXml(testSet);
    Element root = document.getDocumentElement();
    String testSetName = required(root, "name", testSet);
    String contributor = required(root, "contributor", testSet);
    String featureArea = featureArea(testSetName, testSet);
    List<Fixture> fixtures = new ArrayList<>();

    for (Element group : directChildren(root, "testGroup")) {
      String groupName = required(group, "name", testSet);
      String groupText = group.getTextContent();
      List<Element> schemaTests = directChildren(group, "schemaTest");
      List<Path> groupSchemaDocuments = schemaDocuments(testSet, schemaTests);
      for (Element schemaTest : schemaTests) {
        for (Element schemaDocument : directChildren(schemaTest, "schemaDocument")) {
          fixtures.add(
              fixture(
                  suiteRoot,
                  testSet,
                  testSetName,
                  contributor,
                  featureArea,
                  groupName,
                  "schema",
                  schemaTest,
                  schemaDocument,
                  groupText,
                  List.of(),
                  ids));
        }
      }
      for (Element instanceTest : directChildren(group, "instanceTest")) {
        Element instanceDocument = singleDirectChild(instanceTest, "instanceDocument", testSet);
        fixtures.add(
            fixture(
                suiteRoot,
                testSet,
                testSetName,
                contributor,
                featureArea,
                groupName,
                "instance",
                instanceTest,
                instanceDocument,
                groupText,
                groupSchemaDocuments,
                ids));
      }
    }
    return fixtures;
  }

  private static Fixture fixture(
      Path suiteRoot,
      Path testSet,
      String testSetName,
      String contributor,
      String featureArea,
      String groupName,
      String kind,
      Element test,
      Element testDocument,
      String groupText,
      List<Path> schemaDocuments,
      Set<String> ids) {
    String testName = required(test, "name", testSet);
    String href = href(testDocument, testSet);
    String expectedValidity = expectedValidity(test, testSet);
    String status = currentStatus(test, testSet);
    Path documentPath = parent(testSet).resolve(href).normalize();
    if (!Files.isRegularFile(documentPath)) {
      throw new IllegalArgumentException("Missing W3C test document " + documentPath);
    }
    if (containsXml11OrXsd11(href, groupText)) {
      throw new IllegalArgumentException("Unexpected XSD 1.1 or XML 1.1 fixture in " + href);
    }
    String document = suiteRoot.relativize(documentPath).toString().replace('\\', '/');
    Classification classification =
        classify(featureArea, kind, testName, href, document, expectedValidity, status, groupText);
    String id = stableId(testSetName, groupName, kind, testName, href);
    if (!ids.add(id)) {
      throw new IllegalArgumentException("Duplicate W3C suite fixture id " + id);
    }
    return new Fixture(
        id,
        testSetName,
        contributor,
        groupName,
        kind,
        testName,
        document,
        expectedValidity,
        status,
        classification.category(),
        featureArea,
        classification.reason(),
        List.copyOf(schemaDocuments));
  }

  private static Classification classify(
      String featureArea,
      String kind,
      String testName,
      String href,
      String document,
      String expectedValidity,
      String status,
      String groupText) {
    if (!"accepted".equals(status)) {
      return new Classification(Category.BLOCKED, "W3C metadata status is " + status + ".");
    }
    String text = (testName + " " + href + " " + groupText).toLowerCase(Locale.ROOT);
    if (text.contains("annotation") || text.contains("documentation")) {
      return new Classification(
          Category.TOLERATED_METADATA, "Annotation/documentation metadata is tolerated.");
    }
    if ("schema".equals(kind) && text.contains("redefine")) {
      return new Classification(
          Category.EXPECTED_DIAGNOSTIC,
          "Redefine fixtures remain deterministic generator diagnostics.");
    }
    if (bindingMappingFor(document) != null) {
      return new Classification(
          Category.BINDING_SUPPORTED, "Mapped to generated-binding execution evidence.");
    }
    if (featureArea.equals("notation")
        || text.contains("notation")
        || text.contains("xsi:type")
        || text.contains("block")
        || text.contains("final")) {
      return new Classification(
          Category.PRODUCT_SCOPE_INCOMPATIBLE,
          "Fixture exercises behavior outside current generated-binding scope.");
    }
    if ("invalid".equals(expectedValidity) && "schema".equals(kind)) {
      return new Classification(
          Category.VALIDATION_ONLY,
          "Invalid schema fixture is a schema-processor validation oracle, not a binding claim.");
    }
    return new Classification(
        Category.VALIDATION_ONLY,
        "W3C suite fixture is classified as validation oracle evidence until mapped to a binding-supported shape.");
  }

  private static void executeExpectedDiagnostic(
      Fixture fixture, Path suiteRoot, Path reportDirectory) throws IOException {
    if (!Category.EXPECTED_DIAGNOSTIC.equals(fixture.category())) {
      return;
    }
    Path schema = suiteRoot.resolve(fixture.document());
    Path output = reportDirectory.resolve("generated").resolve(fixture.id());
    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    GeneratorProfile.XP_XSD10_DOCUMENT,
                    "com.example.w3c",
                    Map.of(),
                    List.of(),
                    Map.of()));
    if (result.successful()) {
      throw new IllegalStateException(
          "Expected diagnostic fixture generated successfully: " + fixture.id());
    }
    if (result.generatedSources().isEmpty() && !result.diagnostics().isEmpty()) {
      return;
    }
    if (!result.generatedSources().isEmpty()) {
      throw new IllegalStateException(
          "Expected diagnostic fixture produced generated sources: " + fixture.id());
    }
    throw new IllegalStateException(
        "Expected diagnostic fixture produced no diagnostics: " + fixture.id());
  }

  private static List<Path> schemaDocuments(Path testSet, List<Element> schemaTests) {
    List<Path> documents = new ArrayList<>();
    for (Element schemaTest : schemaTests) {
      for (Element schemaDocument : directChildren(schemaTest, "schemaDocument")) {
        documents.add(parent(testSet).resolve(href(schemaDocument, testSet)).normalize());
      }
    }
    return documents;
  }

  private static Document parseXml(Path path) throws IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    try {
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      try (InputStream input = Files.newInputStream(path)) {
        return factory.newDocumentBuilder().parse(input);
      }
    } catch (ParserConfigurationException | SAXException exception) {
      throw new IOException("Unable to parse W3C test-set metadata " + path, exception);
    }
  }

  private static List<BindingExecution> executeBindingMappings(
      List<Fixture> fixtures, Path suiteRoot, Path reportDirectory) throws IOException {
    List<BindingExecution> executions = new ArrayList<>();
    for (BindingMapping mapping : BINDING_MAPPINGS) {
      List<Fixture> mappedFixtures =
          fixtures.stream()
              .filter(fixture -> Objects.equals(bindingMappingFor(fixture.document()), mapping))
              .sorted(Comparator.comparing(Fixture::id))
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
          .anyMatch(fixture -> !Category.BINDING_SUPPORTED.equals(fixture.category()))) {
        throw new IllegalStateException(
            "Mapped W3C rows must be classified as binding-supported for "
                + mapping.schemaDocument());
      }
      executeBindingMapping(mapping, suiteRoot, reportDirectory);
      executions.add(
          new BindingExecution(
              mapping.id(),
              mapping.schemaDocument(),
              mapping.validInstanceDocuments().size(),
              mapping.invalidInstanceDocuments().size(),
              "passed"));
    }
    return List.copyOf(executions);
  }

  private static void executeBindingMapping(
      BindingMapping mapping, Path suiteRoot, Path reportDirectory) throws IOException {
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
            W3cXsd10SuiteIntake.class.getClassLoader())) {
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
      BindingMapping mapping, Path suiteRoot, Path schemaPath, ClassLoader classLoader)
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

  private static Element singleDirectChild(Element element, String localName, Path path) {
    List<Element> children = directChildren(element, localName);
    if (children.size() != 1) {
      throw new IllegalArgumentException(
          "Expected exactly one "
              + localName
              + " child in "
              + path
              + " but found "
              + children.size());
    }
    return children.get(0);
  }

  private static List<Element> directChildren(Element element, String localName) {
    List<Element> children = new ArrayList<>();
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element childElement
          && TS_NS.equals(childElement.getNamespaceURI())
          && localName.equals(childElement.getLocalName())) {
        children.add(childElement);
      }
    }
    return children;
  }

  private static String expectedValidity(Element test, Path path) {
    String validity = required(singleDirectChild(test, "expected", path), "validity", path);
    if (!KNOWN_VALIDITY.contains(validity)) {
      throw new IllegalArgumentException(
          "Unknown W3C expected validity '" + validity + "' in " + path);
    }
    return validity;
  }

  private static String currentStatus(Element test, Path path) {
    List<Element> current = directChildren(test, "current");
    if (current.isEmpty()) {
      return "accepted";
    }
    if (current.size() > 1) {
      throw new IllegalArgumentException(
          "Expected at most one current child in " + path + " but found " + current.size());
    }
    String status = required(current.get(0), "status", path);
    if (!KNOWN_STATUS.contains(status)) {
      throw new IllegalArgumentException("Unknown W3C current status '" + status + "' in " + path);
    }
    return status;
  }

  private static String href(Element element, Path path) {
    String href = element.getAttributeNS(XLINK_NS, "href");
    if (href.isBlank()) {
      href = element.getAttribute("href");
    }
    if (href.isBlank()) {
      throw new IllegalArgumentException("Missing xlink:href in " + path);
    }
    return href;
  }

  private static String required(Element element, String attribute, Path path) {
    String value = element.getAttribute(attribute);
    if (value.isBlank()) {
      throw new IllegalArgumentException("Missing " + attribute + " in " + path);
    }
    return value;
  }

  private static String featureArea(String testSetName, Path testSet) {
    String fileName = fileName(testSet);
    if (fileName.equals("NISTXMLSchemaDatatypes.testSet")) {
      return "datatypes";
    }
    return switch (testSetName) {
      case "AGroupDef" -> "attribute-groups";
      case "AttrDecl" -> "attributes";
      case "AttrUse" -> "attribute-use";
      case "CType" -> "complex-types";
      case "ElemDecl" -> "elements";
      case "IdConstrDefs" -> "identity-constraints";
      case "MGroup" -> "model-groups";
      case "MGroupDef" -> "model-group-definitions";
      case "Notation" -> "notation";
      case "SType" -> "simple-types";
      case "Schema" -> "schema";
      case "Wildcard" -> "wildcards";
      case "suntest" -> "sun-regression";
      case "BoeingXSDTestCases" -> "boeing-regression";
      default -> throw new IllegalArgumentException("Unknown W3C test-set name " + testSetName);
    };
  }

  private static String stableId(
      String testSetName, String groupName, String kind, String testName, String href) {
    return sanitize(testSetName)
        + "/"
        + sanitize(groupName)
        + "/"
        + kind
        + ":"
        + sanitize(testName)
        + "@"
        + Integer.toHexString(href.hashCode());
  }

  private static String sanitize(String value) {
    return value.replaceAll("[^A-Za-z0-9_.-]", "_");
  }

  private static boolean containsXml11OrXsd11(String href, String text) {
    String haystack = (href + " " + text).toLowerCase(Locale.ROOT);
    return haystack.contains("xsd 1.1")
        || haystack.contains("xsd1.1")
        || haystack.contains("xml 1.1")
        || haystack.contains("xml1.1");
  }

  private static Path parent(Path path) {
    return Objects.requireNonNull(path.getParent(), "Path has no parent: " + path);
  }

  private static BindingMapping bindingMappingFor(String document) {
    return BINDING_MAPPINGS.stream()
        .filter(
            mapping ->
                mapping.schemaDocument().equals(document)
                    || mapping.validInstanceDocuments().contains(document)
                    || mapping.invalidInstanceDocuments().contains(document))
        .findFirst()
        .orElse(null);
  }

  private static String fileName(Path path) {
    return Objects.requireNonNull(path.getFileName(), "Path has no file name: " + path).toString();
  }

  static void writeReport(Path report, List<Fixture> fixtures) throws IOException {
    Files.createDirectories(parent(report));
    try (BufferedWriter writer = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
      writer.write(
          "id\ttestSet\tcontributor\tgroup\tkind\tname\tdocument\texpectedValidity"
              + "\tstatus\tcategory\tfeatureArea\treason\n");
      for (Fixture fixture : fixtures) {
        writer.write(fixture.toTsv());
        writer.newLine();
      }
    }
  }

  private static void writeBindingExecutions(Path report, List<BindingExecution> executions)
      throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
      writer.write("id\tschemaDocument\tvalidInstances\tinvalidInstances\tstatus\n");
      for (BindingExecution execution : executions) {
        writer.write(execution.toTsv());
        writer.newLine();
      }
    }
  }

  private static void writeSummary(
      Path summary, Path suiteRoot, List<Fixture> fixtures, List<BindingExecution> executions)
      throws IOException {
    Report report = Report.from(fixtures, executions.size());
    try (BufferedWriter writer = Files.newBufferedWriter(summary, StandardCharsets.UTF_8)) {
      writer.write("releaseUrl=" + RELEASE_URL);
      writer.newLine();
      writer.write("archiveSha256=" + ARCHIVE_SHA256);
      writer.newLine();
      writer.write("suiteRoot=" + suiteRoot.toAbsolutePath());
      writer.newLine();
      writer.write(report.toSummaryLine());
      writer.newLine();
      writer.write("bindingExecution.passed=" + executions.size());
      writer.newLine();
      for (Map.Entry<Category, Long> entry : report.categoryCounts().entrySet()) {
        writer.write("category." + entry.getKey().token() + "=" + entry.getValue());
        writer.newLine();
      }
      for (Map.Entry<String, Long> entry : report.featureCounts().entrySet()) {
        writer.write("feature." + entry.getKey() + "=" + entry.getValue());
        writer.newLine();
      }
    }
  }

  public enum Category {
    BINDING_SUPPORTED("binding-supported"),
    VALIDATION_ONLY("validation-only"),
    TOLERATED_METADATA("tolerated-metadata"),
    EXPECTED_DIAGNOSTIC("expected-diagnostic"),
    PRODUCT_SCOPE_INCOMPATIBLE("product-scope-incompatible"),
    BLOCKED("blocked");

    private final String token;

    Category(String token) {
      this.token = token;
    }

    public String token() {
      return token;
    }
  }

  record Classification(Category category, String reason) {}

  record BindingMapping(
      String schemaDocument,
      List<String> validInstanceDocuments,
      List<String> invalidInstanceDocuments,
      String namespace,
      String packageName,
      String rootClass) {
    BindingMapping {
      validInstanceDocuments = List.copyOf(validInstanceDocuments);
      invalidInstanceDocuments = List.copyOf(invalidInstanceDocuments);
    }

    String id() {
      return sanitize(schemaDocument);
    }
  }

  record BindingExecution(
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

  record Fixture(
      String id,
      String testSet,
      String contributor,
      String group,
      String kind,
      String name,
      String document,
      String expectedValidity,
      String status,
      Category category,
      String featureArea,
      String reason,
      List<Path> schemaDocuments) {
    String toTsv() {
      return String.join(
          "\t",
          id,
          testSet,
          contributor,
          group,
          kind,
          name,
          document,
          expectedValidity,
          status,
          category.token(),
          featureArea,
          reason);
    }
  }

  public static final class Report {
    private final long total;
    private final int bindingExecutionCount;
    private final Map<Category, Long> categoryCounts;
    private final Map<String, Long> featureCounts;

    private Report(
        long total,
        int bindingExecutionCount,
        Map<Category, Long> categoryCounts,
        Map<String, Long> featureCounts) {
      this.total = total;
      this.bindingExecutionCount = bindingExecutionCount;
      this.categoryCounts = Collections.unmodifiableMap(new LinkedHashMap<>(categoryCounts));
      this.featureCounts = Collections.unmodifiableMap(new LinkedHashMap<>(featureCounts));
    }

    static Report from(List<Fixture> fixtures) {
      return from(fixtures, 0);
    }

    static Report from(List<Fixture> fixtures, int bindingExecutionCount) {
      Map<Category, Long> categoryCounts = new EnumMap<>(Category.class);
      for (Category category : Category.values()) {
        categoryCounts.put(category, 0L);
      }
      Map<String, Long> featureCounts = new HashMap<>();
      for (Fixture fixture : fixtures) {
        categoryCounts.merge(fixture.category(), 1L, Long::sum);
        featureCounts.merge(fixture.featureArea(), 1L, Long::sum);
      }
      Map<Category, Long> orderedCategoryCounts = new LinkedHashMap<>();
      for (Category category : Category.values()) {
        orderedCategoryCounts.put(category, categoryCounts.get(category));
      }
      Map<String, Long> orderedFeatureCounts =
          featureCounts.entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .collect(
                  java.util.stream.Collectors.toMap(
                      Map.Entry::getKey,
                      Map.Entry::getValue,
                      (left, right) -> left,
                      LinkedHashMap::new));
      return new Report(
          fixtures.size(),
          bindingExecutionCount,
          Collections.unmodifiableMap(orderedCategoryCounts),
          Collections.unmodifiableMap(orderedFeatureCounts));
    }

    public long total() {
      return total;
    }

    public Map<Category, Long> categoryCounts() {
      return Collections.unmodifiableMap(new LinkedHashMap<>(categoryCounts));
    }

    public Map<String, Long> featureCounts() {
      return Collections.unmodifiableMap(new LinkedHashMap<>(featureCounts));
    }

    public int bindingExecutionCount() {
      return bindingExecutionCount;
    }

    String toSummaryLine() {
      return "w3c-xsd10-summary total="
          + total
          + " binding-supported="
          + categoryCounts.get(Category.BINDING_SUPPORTED)
          + " validation-only="
          + categoryCounts.get(Category.VALIDATION_ONLY)
          + " tolerated-metadata="
          + categoryCounts.get(Category.TOLERATED_METADATA)
          + " expected-diagnostic="
          + categoryCounts.get(Category.EXPECTED_DIAGNOSTIC)
          + " product-scope-incompatible="
          + categoryCounts.get(Category.PRODUCT_SCOPE_INCOMPATIBLE)
          + " blocked="
          + categoryCounts.get(Category.BLOCKED);
    }
  }
}
