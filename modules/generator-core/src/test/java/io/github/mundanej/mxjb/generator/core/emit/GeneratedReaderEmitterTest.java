package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.bind.BindingCardinality;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingModel;
import io.github.mundanej.mxjb.generator.core.bind.BindingResult;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.BindingValidationPlan;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import io.github.mundanej.mxjb.runtime.XmlEventKind;
import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.XmlLocation;
import io.github.mundanej.mxjb.runtime.XmlName;
import io.github.mundanej.mxjb.runtime.XmlReadException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GeneratedReaderEmitterTest {
  @TempDir private Path tempDirectory;

  @Test
  void emitsRootStaticReaderSourceForSupportedModel() {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field("attribute", "version", scalar("string"), optional(), 0),
                        field("element", "id", scalar("string"), required(), 1)))));

    GeneratedReaderEmissionResult result = new GeneratedReaderEmitter().emit(model);

    assertFalse(result.hasErrors());
    assertEquals(
        Path.of("com/example/orders/xml/OrderXmlReader.java"),
        result.sources().getFirst().relativePath());
    String source = result.sources().getFirst().sourceText();
    assertTrue(source.contains("public final class OrderXmlReader"));
    assertTrue(source.contains("public static com.example.orders.Order read("));
    assertTrue(source.contains("io.github.mundanej.mxjb.runtime.XmlEventReader input"));
    assertTrue(source.contains("private static com.example.orders.Order readOrder("));
    assertTrue(source.contains("MXJB-GR-004"));
    assertFalse(source.contains("\nimport "));
    assertFalse(source.contains("java.lang.reflect"));
    assertFalse(source.contains("java.lang.invoke"));
    assertFalse(source.contains("ServiceLoader"));
    assertFalse(source.contains("ClassLoader"));
    assertFalse(source.contains("Class.forName"));
    assertFalse(source.contains("Proxy"));
    assertFalse(source.contains("ObjectInputStream"));
    assertFalse(source.contains("ObjectOutputStream"));
    assertFalse(source.contains("Serializable"));
    assertFalse(source.contains("Externalizable"));
    assertFalse(source.contains("ProcessBuilder"));
    assertFalse(source.contains("System.exit"));
    assertFalse(source.contains("javax.xml"));
  }

  @Test
  void generatedReaderUsesQualifiedSupportTypesToAvoidModelNameCollisions()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("xmlEventReader", model("com.example.collide.XmlEventReader"))),
            List.of(
                type(
                    "com.example.collide",
                    "XmlEventReader",
                    List.of(
                        field(
                            "element",
                            "xmlName",
                            model("com.example.collide.XmlName"),
                            required(),
                            1))),
                type(
                    "com.example.collide",
                    "XmlName",
                    List.of(field("element", "value", scalar("string"), required(), 1)))));
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());
    String readerSource = readerResult.sources().getFirst().sourceText();

    assertFalse(readerSource.contains("\nimport "));
    assertTrue(readerSource.contains("io.github.mundanej.mxjb.runtime.XmlEventReader input"));
    assertTrue(readerSource.contains("com.example.collide.XmlEventReader read("));

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> rootClass = compiledSources.load("com.example.collide.XmlEventReader");
      Class<?> nestedClass = compiledSources.load("com.example.collide.XmlName");
      Class<?> readerClass =
          compiledSources.load("com.example.collide.xml.XmlEventReaderXmlReader");

      Object root =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, collideInput());

      Object nested = rootClass.getMethod("xmlName").invoke(root);
      assertInstanceOf(nestedClass, nested);
      assertEquals("value-1", nestedClass.getMethod("value").invoke(nested));
    }
  }

  @Test
  void generatedReaderCompilesAndReadsOptionalRepeatedNestedAndNamespacedContent()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model = orderModel();
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> lineClass = compiledSources.load("com.example.lines.Line");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");

      Object order = readerClass.getMethod("read", XmlEventReader.class).invoke(null, orderInput());

      assertEquals(Optional.of("v1"), orderClass.getMethod("version").invoke(order));
      assertEquals("A-1", orderClass.getMethod("id").invoke(order));
      assertEquals(Optional.of("gift"), orderClass.getMethod("note").invoke(order));
      Object lines = orderClass.getMethod("line").invoke(order);
      assertInstanceOf(List.class, lines);
      assertEquals(2, ((List<?>) lines).size());
      assertEquals("SKU-1", lineClass.getMethod("sku").invoke(((List<?>) lines).get(0)));
      assertEquals("SKU-2", lineClass.getMethod("sku").invoke(((List<?>) lines).get(1)));
    }
  }

  @Test
  void generatedReaderReportsDeterministicDiagnostics()
      throws IOException, ClassNotFoundException, NoSuchMethodException {
    BindingModel model = orderModel();
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");

      assertReadDiagnostic(readerClass, wrongRootNamespaceInput(), "MXJB-GR-001");
      assertReadDiagnostic(readerClass, unexpectedAttributeInput(), "MXJB-GR-003");
      assertReadDiagnostic(readerClass, missingRequiredElementInput(), "MXJB-GR-004");
      assertReadDiagnostic(readerClass, repeatedSingletonInput(), "MXJB-GR-005");
      assertReadDiagnostic(readerClass, outOfOrderInput(), "MXJB-GR-002");
      assertReadDiagnostic(readerClass, trailingRootContentInput(), "MXJB-GR-007");
    }
  }

  @Test
  void generatedReaderEnforcesFiniteListMaxOccurs()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model = orderModelWithLineLimit("2");
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");

      Object order = readerClass.getMethod("read", XmlEventReader.class).invoke(null, orderInput());

      assertEquals(2, ((List<?>) orderClass.getMethod("line").invoke(order)).size());
      assertReadDiagnostic(readerClass, tooManyLinesInput(), "MXJB-GR-005");
    }
  }

  @Test
  void generatedReaderConvertsSupportedScalarTypesAndRejectsInvalidLexicalValues()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("totals", model("com.example.orders.Totals"))),
            List.of(
                type(
                    "com.example.orders",
                    "Totals",
                    List.of(
                        field("element", "active", scalar("boolean"), required(), 1),
                        field("element", "quantity", scalar("int"), required(), 2),
                        field("element", "count", scalar("integer"), required(), 3),
                        field("element", "distance", scalar("long"), required(), 4),
                        field("element", "amount", scalar("decimal"), required(), 5)))));
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> totalsClass = compiledSources.load("com.example.orders.Totals");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.TotalsXmlReader");

      Object totals =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, totalsInput("1"));

      assertEquals(true, totalsClass.getMethod("active").invoke(totals));
      assertEquals(7, totalsClass.getMethod("quantity").invoke(totals));
      assertEquals(
          new java.math.BigInteger("12345678901234567890"),
          totalsClass.getMethod("count").invoke(totals));
      assertEquals(12L, totalsClass.getMethod("distance").invoke(totals));
      assertEquals(
          new java.math.BigDecimal("19.95"), totalsClass.getMethod("amount").invoke(totals));

      assertReadDiagnostic(readerClass, totalsInput("maybe"), "MXJB-GR-006");
    }
  }

  @Test
  void emitsDeterministicSourcesSortedByRootName() {
    BindingModel model =
        new BindingModel(
            List.of(
                root("zeta", model("com.example.orders.Zeta")),
                root("alpha", model("com.example.orders.Alpha"))),
            List.of(
                type("com.example.orders", "Zeta", List.of()),
                type("com.example.orders", "Alpha", List.of())));

    GeneratedReaderEmissionResult first = new GeneratedReaderEmitter().emit(model);
    GeneratedReaderEmissionResult second = new GeneratedReaderEmitter().emit(model);

    assertFalse(first.hasErrors());
    new GeneratedSourceVerifier(tempDirectory)
        .assertDeterministic(first.sources(), second.sources());
    assertEquals(
        List.of(
            Path.of("com/example/orders/xml/AlphaXmlReader.java"),
            Path.of("com/example/orders/xml/ZetaXmlReader.java")),
        first.sources().stream().map(GeneratedJavaSource::relativePath).toList());
  }

  @Test
  void propagatesBindingDiagnosticsWithoutSources() {
    SchemaDiagnostic diagnostic =
        new SchemaDiagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL, "binding", "Invalid binding model.");
    BindingResult bindingResult = BindingResult.empty(List.of(diagnostic));

    GeneratedReaderEmissionResult result = new GeneratedReaderEmitter().emit(bindingResult);

    assertEquals(List.of(diagnostic), result.diagnostics());
    assertTrue(result.sources().isEmpty());
  }

  @Test
  void rejectsUnsupportedReaderModelsWithoutPartialSources() {
    BindingModel model =
        new BindingModel(
            List.of(root("order", scalar("string"))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field(
                            "attribute", "owner", model("com.example.orders.Owner"), required(), 0),
                        field("attribute", "tag", scalar("string"), list(), 0),
                        field("element", "date", scalar("date"), required(), 1))),
                type("com.example.orders", "Owner", List.of())));

    GeneratedReaderEmissionResult result = new GeneratedReaderEmitter().emit(model);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_READER_EMISSION_INVALID_MODEL,
            DiagnosticCode.SCHEMA_READER_EMISSION_INVALID_MODEL,
            DiagnosticCode.SCHEMA_READER_EMISSION_INVALID_MODEL,
            DiagnosticCode.SCHEMA_READER_EMISSION_INVALID_MODEL),
        result.diagnostics().stream().map(SchemaDiagnostic::code).toList());
    assertTrue(result.sources().isEmpty());
  }

  private void assertReadDiagnostic(Class<?> readerClass, XmlEventReader input, String code) {
    InvocationTargetException exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            InvocationTargetException.class,
            () -> readerClass.getMethod("read", XmlEventReader.class).invoke(null, input));
    XmlReadException readException = assertInstanceOf(XmlReadException.class, exception.getCause());
    assertEquals(code, readException.diagnostic().code());
  }

  private BindingModel orderModel() {
    return orderModelWithLineLimit("unbounded");
  }

  private BindingModel orderModelWithLineLimit(String lineMaxOccurs) {
    return new BindingModel(
        List.of(root("order", model("com.example.orders.Order"))),
        List.of(
            type(
                "com.example.orders",
                "Order",
                List.of(
                    field("attribute", "version", scalar("string"), optional(), 0),
                    field("element", "id", scalar("string"), required(), 1),
                    field("element", "note", scalar("string"), optional(), 2),
                    field(
                        "element",
                        new SchemaQName("urn:lines", "line"),
                        "line",
                        model("com.example.lines.Line"),
                        list(lineMaxOccurs),
                        3))),
            type(
                "com.example.lines",
                "Line",
                List.of(
                    field(
                        "element",
                        new SchemaQName("urn:lines", "sku"),
                        "sku",
                        scalar("string"),
                        required(),
                        1)))));
  }

  private EventXmlReader orderInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(
            XmlEventKind.START_ELEMENT,
            new XmlName("urn:orders", "order"),
            Map.of(new XmlName("urn:orders", "version"), "v1")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")),
        text("A-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "note")),
        text("gift"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "note")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "sku")),
        text("SKU-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "sku")),
        text("SKU-2"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader trailingRootContentInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(
            XmlEventKind.START_ELEMENT,
            new XmlName("urn:orders", "order"),
            Map.of(new XmlName("urn:orders", "version"), "v1")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")),
        text("A-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "extra")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "extra")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader tooManyLinesInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(
            XmlEventKind.START_ELEMENT,
            new XmlName("urn:orders", "order"),
            Map.of(new XmlName("urn:orders", "version"), "v1")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")),
        text("A-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "sku")),
        text("SKU-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "sku")),
        text("SKU-2"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "sku")),
        text("SKU-3"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader wrongRootNamespaceInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:wrong", "order")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:wrong", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader unexpectedAttributeInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(
            XmlEventKind.START_ELEMENT,
            new XmlName("urn:orders", "order"),
            Map.of(new XmlName("urn:orders", "unexpected"), "value")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader missingRequiredElementInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader repeatedSingletonInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")),
        text("A-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")),
        text("A-2"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader outOfOrderInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "sku")),
        text("SKU-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "note")),
        text("late"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "note")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader totalsInput(String activeValue) {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "totals")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "active")),
        text(activeValue),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "active")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "quantity")),
        text("7"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "quantity")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "count")),
        text("12345678901234567890"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "count")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "distance")),
        text("12"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "distance")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "amount")),
        text("19.95"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "amount")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "totals")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader collideInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "xmlEventReader")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "xmlName")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "value")),
        text("value-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "value")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "xmlName")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "xmlEventReader")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private BindingRootElement root(String localName, BindingTypeReference type) {
    return new BindingRootElement(schemaName(localName), type, required());
  }

  private BindingType type(String packageName, String simpleName, List<BindingField> fields) {
    return new BindingType(
        new BindingJavaName(packageName, simpleName),
        schemaName(simpleName),
        "record",
        fields,
        new BindingValidationPlan(List.of()));
  }

  private BindingField field(
      String kind,
      String localName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order) {
    return field(kind, schemaName(localName), localName, type, cardinality, order);
  }

  private BindingField field(
      String kind,
      SchemaQName xmlName,
      String javaName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order) {
    return new BindingField(
        kind, xmlName, javaName, type, cardinality, order, cardinality.minOccurs() > 0);
  }

  private BindingTypeReference scalar(String name) {
    return new BindingTypeReference("scalar", name);
  }

  private BindingTypeReference model(String name) {
    return new BindingTypeReference("model", name);
  }

  private BindingCardinality required() {
    return new BindingCardinality("required", 1, "1");
  }

  private BindingCardinality optional() {
    return new BindingCardinality("optional", 0, "1");
  }

  private BindingCardinality list() {
    return new BindingCardinality("list", 0, "unbounded");
  }

  private BindingCardinality list(String maxOccurs) {
    return new BindingCardinality("list", 0, maxOccurs);
  }

  private SchemaQName schemaName(String localName) {
    return new SchemaQName("urn:orders", localName);
  }

  private EventXmlReader reader(Event... events) {
    return new EventXmlReader(List.of(events));
  }

  private Event event(XmlEventKind kind, XmlName name) {
    return new Event(kind, name, "", Map.of());
  }

  private Event event(XmlEventKind kind, XmlName name, Map<XmlName, String> attributes) {
    return new Event(kind, name, "", attributes);
  }

  private Event text(String value) {
    return new Event(XmlEventKind.TEXT, null, value, Map.of());
  }

  private record Event(
      XmlEventKind kind, XmlName name, String text, Map<XmlName, String> attributes) {}

  private static final class EventXmlReader implements XmlEventReader {
    private final List<Event> events;
    private int index;

    private EventXmlReader(List<Event> events) {
      this.events = events;
    }

    @Override
    public XmlEventKind kind() {
      return current().kind();
    }

    @Override
    public XmlName name() {
      return current().name();
    }

    @Override
    public String text() {
      return current().text();
    }

    @Override
    public int attributeCount() {
      return current().attributes().size();
    }

    @Override
    public XmlName attributeName(int indexValue) {
      return current().attributes().keySet().stream().toList().get(indexValue);
    }

    @Override
    public String attributeValue(int indexValue) {
      return current().attributes().values().stream().toList().get(indexValue);
    }

    @Override
    public XmlLocation location() {
      return new XmlLocation("test.xml", index + 1, 1);
    }

    @Override
    public boolean next() {
      if (index + 1 >= events.size()) {
        return false;
      }
      index++;
      return true;
    }

    private Event current() {
      return events.get(index);
    }
  }
}
