package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.bind.BindingCardinality;
import io.github.mundanej.mxjb.generator.core.bind.BindingChoice;
import io.github.mundanej.mxjb.generator.core.bind.BindingChoiceBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingContent;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentGroup;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentPosition;
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
  void generatedReaderReadsChoiceBranchAndRejectsRepeatedChoice()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model = choiceOrderModel();
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> domesticChoiceClass = compiledSources.load("com.example.orders.DomesticChoice");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");

      Object order =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, choiceOrderInput(false));

      Object choice =
          ((Optional<?>) orderClass.getMethod("orderChoice").invoke(order)).orElseThrow();
      assertInstanceOf(domesticChoiceClass, choice);
      assertEquals("US", domesticChoiceClass.getMethod("value").invoke(choice));
      assertReadDiagnostic(readerClass, choiceOrderInput(true), "MXJB-GR-005");
    }
  }

  @Test
  void generatedReaderAcceptsAllGroupFieldsInAnyOrder()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field("element", "id", scalar("string"), required(), 1),
                        field("element", "note", scalar("string"), optional(), 1)))));
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");

      Object order = readerClass.getMethod("read", XmlEventReader.class).invoke(null, allInput());

      assertEquals("A-1", orderClass.getMethod("id").invoke(order));
      assertEquals(Optional.of("gift"), orderClass.getMethod("note").invoke(order));
    }
  }

  @Test
  void generatedReaderCompilesAndReadsRepeatedChoiceLists()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(type("com.example.orders", "Order", List.of(repeatedChoiceField()))));
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> domesticChoiceClass = compiledSources.load("com.example.orders.DomesticChoice");
      Class<?> internationalChoiceClass =
          compiledSources.load("com.example.orders.InternationalChoice");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");

      Object order =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, repeatedChoiceInput());

      Object choices = orderClass.getMethod("orderChoice").invoke(order);
      assertInstanceOf(List.class, choices);
      assertEquals(2, ((List<?>) choices).size());
      assertInstanceOf(domesticChoiceClass, ((List<?>) choices).get(0));
      assertInstanceOf(internationalChoiceClass, ((List<?>) choices).get(1));
    }
  }

  @Test
  void generatedReaderEnforcesGroupedSequenceOccurrences()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(type("com.example.orders", "Order", List.of(groupedSequenceField()))));
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");

      Object order =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, groupedSequenceInput());

      Object content = orderClass.getMethod("orderSequenceContent").invoke(order);
      assertInstanceOf(List.class, content);
      assertEquals(4, ((List<?>) content).size());
      assertReadDiagnostic(readerClass, groupedSequenceBadOrderInput(), "MXJB-GR-005");
    }
  }

  @Test
  void generatedReaderUsesAutomataPositionsForNestedChoiceInGroupedSequence()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(
                type("com.example.orders", "Order", List.of(groupedSequenceWithChoiceField()))));
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");

      Object order =
          readerClass
              .getMethod("read", XmlEventReader.class)
              .invoke(null, groupedSequenceWithChoiceInput());

      Object content = orderClass.getMethod("orderSequenceContent").invoke(order);
      assertInstanceOf(List.class, content);
      assertEquals(4, ((List<?>) content).size());
      assertReadDiagnostic(readerClass, groupedSequenceWithChoiceBadOrderInput(), "MXJB-GR-005");
    }
  }

  @Test
  void generatedReaderAcceptsOptionalAllGroupOmissionAndUnorderedMembers()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(type("com.example.orders", "Order", List.of(groupedAllField()))));
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");

      Object emptyOrder =
          readerClass
              .getMethod("read", XmlEventReader.class)
              .invoke(null, groupedAllOmittedInput());
      Object unorderedOrder =
          readerClass
              .getMethod("read", XmlEventReader.class)
              .invoke(null, groupedAllUnorderedInput());

      assertEquals(
          0, ((List<?>) orderClass.getMethod("orderAllContent").invoke(emptyOrder)).size());
      assertEquals(
          2, ((List<?>) orderClass.getMethod("orderAllContent").invoke(unorderedOrder)).size());
      assertReadDiagnostic(readerClass, groupedAllMissingRequiredInput(), "MXJB-GR-004");
    }
  }

  @Test
  void generatedReaderCapturesAnyAttributesAndRejectsProhibitedMatches()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field(
                            "attribute",
                            new SchemaQName("", "id"),
                            "id",
                            scalar("string"),
                            required(),
                            0),
                        anyAttributeField()))));
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");
      Object order =
          readerClass
              .getMethod("read", XmlEventReader.class)
              .invoke(null, anyAttributeInput(false));

      Object attributes = orderClass.getMethod("wildcardAttributes").invoke(order);
      assertInstanceOf(List.class, attributes);
      assertEquals(1, ((List<?>) attributes).size());
      assertReadDiagnostic(readerClass, anyAttributeInput(true), "MXJB-GR-003");
    }
  }

  @Test
  void generatedReaderRejectsStrictWildcardElementsWithoutKnownDeclaration()
      throws IOException, ClassNotFoundException, NoSuchMethodException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(type("com.example.orders", "Order", List.of(strictKnownWildcardField()))));
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");

      assertReadDiagnostic(readerClass, strictWildcardInput(false), "MXJB-GR-002");
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

      assertReadDiagnostic(readerClass, totalsInput("maybe"), "MXJB-DT-001");
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
                        field("element", "date", scalar("unsupported"), required(), 1))),
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

  private BindingModel choiceOrderModel() {
    return new BindingModel(
        List.of(root("order", model("com.example.orders.Order"))),
        List.of(
            type(
                "com.example.orders",
                "Order",
                List.of(field("element", "id", scalar("string"), required(), 1), choiceField()))));
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

  private EventXmlReader choiceOrderInput(boolean repeatedChoice) {
    List<Event> events = new ArrayList<>();
    events.add(event(XmlEventKind.START_DOCUMENT, null));
    events.add(event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")));
    events.add(event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")));
    events.add(text("A-1"));
    events.add(event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")));
    events.add(event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "domestic")));
    events.add(text("US"));
    events.add(event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "domestic")));
    if (repeatedChoice) {
      events.add(event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "international")));
      events.add(text("CA"));
      events.add(event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "international")));
    }
    events.add(event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")));
    events.add(event(XmlEventKind.END_DOCUMENT, null));
    return new EventXmlReader(events);
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

  private EventXmlReader allInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "note")),
        text("gift"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "note")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")),
        text("A-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader repeatedChoiceInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "domestic")),
        text("US"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "domestic")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "international")),
        text("CA"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "international")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader groupedSequenceInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        elementTextStart("id"),
        text("A-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        elementTextStart("line"),
        text("L-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "line")),
        elementTextStart("id"),
        text("A-2"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        elementTextStart("line"),
        text("L-2"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "line")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader groupedSequenceBadOrderInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        elementTextStart("id"),
        text("A-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        elementTextStart("id"),
        text("A-2"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        elementTextStart("line"),
        text("L-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "line")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader groupedSequenceWithChoiceInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        elementTextStart("card"),
        text("visa"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "card")),
        elementTextStart("line"),
        text("L-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "line")),
        elementTextStart("cash"),
        text("cash"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "cash")),
        elementTextStart("line"),
        text("L-2"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "line")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader groupedSequenceWithChoiceBadOrderInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        elementTextStart("card"),
        text("visa"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "card")),
        elementTextStart("cash"),
        text("cash"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "cash")),
        elementTextStart("line"),
        text("L-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "line")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader groupedAllOmittedInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader groupedAllUnorderedInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        elementTextStart("note"),
        text("gift"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "note")),
        elementTextStart("id"),
        text("A-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader groupedAllMissingRequiredInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        elementTextStart("note"),
        text("gift"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "note")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader anyAttributeInput(boolean prohibited) {
    Map<XmlName, String> attributes =
        prohibited
            ? Map.of(new XmlName("", "id"), "A-1", new XmlName("", "blocked"), "no")
            : Map.of(new XmlName("", "id"), "A-1", new XmlName("urn:extension", "flag"), "yes");
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order"), attributes),
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

  private BindingField choiceField() {
    BindingJavaName choiceName = new BindingJavaName("com.example.orders", "OrderChoice");
    BindingChoice choice =
        new BindingChoice(
            choiceName,
            List.of(
                new BindingChoiceBranch(
                    schemaName("domestic"),
                    "domestic",
                    scalar("string"),
                    new BindingJavaName("com.example.orders", "DomesticChoice")),
                new BindingChoiceBranch(
                    schemaName("international"),
                    "international",
                    scalar("string"),
                    new BindingJavaName("com.example.orders", "InternationalChoice"))));
    return new BindingField(
        "choice",
        schemaName("orderChoice"),
        "orderChoice",
        new BindingTypeReference("choice", choiceName.qualifiedName()),
        optional(),
        2,
        false,
        choice);
  }

  private BindingField repeatedChoiceField() {
    BindingJavaName choiceName = new BindingJavaName("com.example.orders", "OrderChoice");
    BindingChoice choice =
        new BindingChoice(
            choiceName,
            List.of(
                new BindingChoiceBranch(
                    schemaName("domestic"),
                    "domestic",
                    scalar("string"),
                    new BindingJavaName("com.example.orders", "DomesticChoice")),
                new BindingChoiceBranch(
                    schemaName("international"),
                    "international",
                    scalar("string"),
                    new BindingJavaName("com.example.orders", "InternationalChoice"))));
    return new BindingField(
        "choice",
        schemaName("orderChoice"),
        "orderChoice",
        new BindingTypeReference("choice", choiceName.qualifiedName()),
        list(),
        1,
        false,
        choice);
  }

  private BindingField groupedSequenceField() {
    BindingJavaName contentName = new BindingJavaName("com.example.orders", "OrderSequenceContent");
    BindingContentBranch id =
        contentBranch("element", "id", scalar("string"), optional(), 1, "IdContent");
    BindingContentBranch line =
        contentBranch("element", "line", scalar("string"), required(), 2, "LineContent");
    BindingContent content =
        new BindingContent(
            contentName,
            List.of(id, line),
            "sequence",
            List.of(new BindingContentGroup("sequence", list(), List.of(id, line))));
    return new BindingField(
        "content",
        schemaName("orderSequenceContent"),
        "orderSequenceContent",
        new BindingTypeReference("choice", contentName.qualifiedName()),
        list(),
        1,
        false,
        content);
  }

  private BindingField groupedSequenceWithChoiceField() {
    BindingJavaName contentName = new BindingJavaName("com.example.orders", "OrderSequenceContent");
    BindingContentBranch card =
        contentBranch("element", "card", scalar("string"), optional(), 1, "CardContent");
    BindingContentBranch cash =
        contentBranch("element", "cash", scalar("string"), optional(), 1, "CashContent");
    BindingContentBranch line =
        contentBranch("element", "line", scalar("string"), required(), 2, "LineContent");
    BindingContent content =
        new BindingContent(
            contentName,
            List.of(card, cash, line),
            "sequence",
            List.of(
                new BindingContentGroup(
                    "choice",
                    list(),
                    List.of(card, cash),
                    List.of(new BindingContentPosition(list(), List.of(card, cash)))),
                new BindingContentGroup(
                    "sequence",
                    list(),
                    List.of(card, cash, line),
                    List.of(
                        new BindingContentPosition(required(), List.of(card, cash)),
                        new BindingContentPosition(required(), List.of(line))))));
    return new BindingField(
        "content",
        schemaName("orderSequenceContent"),
        "orderSequenceContent",
        new BindingTypeReference("choice", contentName.qualifiedName()),
        list(),
        1,
        false,
        content);
  }

  private BindingField groupedAllField() {
    BindingJavaName contentName = new BindingJavaName("com.example.orders", "OrderAllContent");
    BindingContentBranch id =
        contentBranch("element", "id", scalar("string"), required(), 1, "IdContent");
    BindingContentBranch note =
        contentBranch("element", "note", scalar("string"), required(), 2, "NoteContent");
    BindingContent content =
        new BindingContent(
            contentName,
            List.of(id, note),
            "all",
            List.of(new BindingContentGroup("all", list("1"), List.of(id, note))));
    return new BindingField(
        "content",
        schemaName("orderAllContent"),
        "orderAllContent",
        new BindingTypeReference("choice", contentName.qualifiedName()),
        list("1"),
        1,
        false,
        content);
  }

  private BindingContentBranch contentBranch(
      String kind,
      String localName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order,
      String simpleName) {
    return new BindingContentBranch(
        kind,
        schemaName(localName),
        localName,
        type,
        new BindingJavaName("com.example.orders", simpleName),
        cardinality,
        order,
        null);
  }

  private BindingField anyAttributeField() {
    return new BindingField(
        "anyAttribute",
        new SchemaQName("", "@*"),
        "wildcardAttributes",
        new BindingTypeReference("xmlAttribute", "io.github.mundanej.mxjb.runtime.XmlAttribute"),
        list(),
        0,
        false,
        new io.github.mundanej.mxjb.generator.core.bind.BindingWildcard(
            new io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace(
                "any", List.of()),
            "lax",
            List.of(new SchemaQName("", "blocked"))));
  }

  private BindingField strictKnownWildcardField() {
    return new BindingField(
        "wildcard",
        new SchemaQName("", "*"),
        "wildcardContent",
        new BindingTypeReference("fragment", "io.github.mundanej.mxjb.runtime.XmlFragment"),
        list(),
        1,
        false,
        new io.github.mundanej.mxjb.generator.core.bind.BindingWildcard(
            new io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace(
                "explicit", List.of("urn:orders")),
            "strict",
            List.of(),
            List.of(
                new io.github.mundanej.mxjb.generator.core.bind.BindingWildcardElement(
                    schemaName("discount"), scalar("int"))),
            List.of()));
  }

  private EventXmlReader strictWildcardInput(boolean known) {
    String localName = known ? "discount" : "unknown";
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", localName)),
        text("10"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", localName)),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
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

  private Event elementTextStart(String localName) {
    return event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", localName));
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
