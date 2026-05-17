package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import io.github.mundanej.mxjb.runtime.XmlName;
import io.github.mundanej.mxjb.runtime.XmlOutput;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GeneratedWriterEmitterTest {
  @TempDir private Path tempDirectory;

  @Test
  void emitsRootStaticWriterSourceForRequiredFieldsAndAttributes() {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field("element", "id", scalar("string"), required(), 1),
                        field("element", "quantity", scalar("int"), required(), 2),
                        field("attribute", "version", scalar("string"), required(), 0)))));

    GeneratedWriterEmissionResult result = new GeneratedWriterEmitter().emit(model);

    assertFalse(result.hasErrors());
    assertEquals(
        Path.of("com/example/orders/xml/OrderXmlWriter.java"),
        result.sources().getFirst().relativePath());
    assertEquals(
        """
        package com.example.orders.xml;

        /** Generated XML writer for {@link com.example.orders.Order}. */
        public final class OrderXmlWriter {
          private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
              new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "order");
          private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
              new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "version");
          private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
              new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "id");
          private static final io.github.mundanej.mxjb.runtime.XmlName NAME_4 =
              new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "quantity");

          private OrderXmlWriter() {}

          public static void write(
              io.github.mundanej.mxjb.runtime.XmlOutput output,
              com.example.orders.Order value)
              throws io.github.mundanej.mxjb.runtime.XmlWriteException {
            java.util.Objects.requireNonNull(output, "output");
            java.util.Objects.requireNonNull(value, "value");
            writeOrder(output, NAME_1, value);
          }

          private static void writeOrder(
              io.github.mundanej.mxjb.runtime.XmlOutput output,
              io.github.mundanej.mxjb.runtime.XmlName elementName,
              com.example.orders.Order value)
              throws io.github.mundanej.mxjb.runtime.XmlWriteException {
            output.startElement(elementName);
            output.attribute(NAME_2, value.version());
            output.startElement(NAME_3);
            output.text(value.id());
            output.endElement(NAME_3);
            output.startElement(NAME_4);
            output.text(String.valueOf(value.quantity()));
            output.endElement(NAME_4);
            output.endElement(elementName);
          }
        }
        """,
        result.sources().getFirst().sourceText());
  }

  @Test
  void generatedWriterUsesQualifiedSupportTypesToAvoidModelNameCollisions()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("xmlOutput", model("com.example.collide.XmlOutput"))),
            List.of(
                type(
                    "com.example.collide",
                    "XmlOutput",
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
    GeneratedWriterEmissionResult writerResult = new GeneratedWriterEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(writerResult.sources());
    String writerSource = writerResult.sources().getFirst().sourceText();

    assertFalse(writerSource.contains("\nimport "));
    assertTrue(writerSource.contains("io.github.mundanej.mxjb.runtime.XmlOutput output"));
    assertTrue(writerSource.contains("com.example.collide.XmlOutput value"));

    RecordingXmlOutput output = new RecordingXmlOutput();
    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> rootClass = compiledSources.load("com.example.collide.XmlOutput");
      Class<?> nestedClass = compiledSources.load("com.example.collide.XmlName");
      Class<?> writerClass = compiledSources.load("com.example.collide.xml.XmlOutputXmlWriter");
      Object nested = nestedClass.getConstructor(String.class).newInstance("value-1");
      Object root = rootClass.getConstructor(nestedClass).newInstance(nested);

      writerClass.getMethod("write", XmlOutput.class, rootClass).invoke(null, output, root);
    }

    assertEquals(
        List.of(
            "start:{urn:orders}xmlOutput",
            "start:{urn:orders}xmlName",
            "start:{urn:orders}value",
            "text:value-1",
            "end:{urn:orders}value",
            "end:{urn:orders}xmlName",
            "end:{urn:orders}xmlOutput"),
        output.events);
  }

  @Test
  void generatedWriterCompilesAndWritesOptionalRepeatedAndNestedContent()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
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
                        field("attribute", "version", scalar("string"), optional(), 0),
                        field("element", "id", scalar("string"), required(), 1),
                        field("element", "note", scalar("string"), optional(), 2),
                        field("element", "line", model("com.example.lines.Line"), list(), 3))),
                type(
                    "com.example.lines",
                    "Line",
                    List.of(field("element", "sku", scalar("string"), required(), 1)))));
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedWriterEmissionResult writerResult = new GeneratedWriterEmitter().emit(model);
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(writerResult.sources());

    RecordingXmlOutput output = new RecordingXmlOutput();
    GeneratedSourceVerifier verifier = new GeneratedSourceVerifier(tempDirectory);
    try (GeneratedSourceVerifier.CompiledSources compiledSources = verifier.compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> lineClass = compiledSources.load("com.example.lines.Line");
      Class<?> writerClass = compiledSources.load("com.example.orders.xml.OrderXmlWriter");
      Object firstLine = lineClass.getConstructor(String.class).newInstance("SKU-1");
      Object secondLine = lineClass.getConstructor(String.class).newInstance("SKU-2");
      Object order =
          orderClass
              .getConstructor(Optional.class, String.class, Optional.class, List.class)
              .newInstance(
                  Optional.of("v1"), "A-1", Optional.empty(), List.of(firstLine, secondLine));

      writerClass.getMethod("write", XmlOutput.class, orderClass).invoke(null, output, order);
    }

    assertEquals(
        List.of(
            "start:{urn:orders}order",
            "attr:{urn:orders}version=v1",
            "start:{urn:orders}id",
            "text:A-1",
            "end:{urn:orders}id",
            "start:{urn:orders}line",
            "start:{urn:orders}sku",
            "text:SKU-1",
            "end:{urn:orders}sku",
            "end:{urn:orders}line",
            "start:{urn:orders}line",
            "start:{urn:orders}sku",
            "text:SKU-2",
            "end:{urn:orders}sku",
            "end:{urn:orders}line",
            "end:{urn:orders}order"),
        output.events);

    RecordingXmlOutput noteOutput = new RecordingXmlOutput();
    try (GeneratedSourceVerifier.CompiledSources compiledSources = verifier.compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> writerClass = compiledSources.load("com.example.orders.xml.OrderXmlWriter");
      Object order =
          orderClass
              .getConstructor(Optional.class, String.class, Optional.class, List.class)
              .newInstance(Optional.empty(), "A-2", Optional.of("gift"), List.of());

      writerClass.getMethod("write", XmlOutput.class, orderClass).invoke(null, noteOutput, order);
    }

    assertEquals(
        List.of(
            "start:{urn:orders}order",
            "start:{urn:orders}id",
            "text:A-2",
            "end:{urn:orders}id",
            "start:{urn:orders}note",
            "text:gift",
            "end:{urn:orders}note",
            "end:{urn:orders}order"),
        noteOutput.events);
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

    GeneratedWriterEmissionResult first = new GeneratedWriterEmitter().emit(model);
    GeneratedWriterEmissionResult second = new GeneratedWriterEmitter().emit(model);

    assertFalse(first.hasErrors());
    new GeneratedSourceVerifier(tempDirectory)
        .assertDeterministic(first.sources(), second.sources());
    assertEquals(
        List.of(
            Path.of("com/example/orders/xml/AlphaXmlWriter.java"),
            Path.of("com/example/orders/xml/ZetaXmlWriter.java")),
        first.sources().stream().map(GeneratedJavaSource::relativePath).toList());
  }

  @Test
  void propagatesBindingDiagnosticsWithoutSources() {
    SchemaDiagnostic diagnostic =
        new SchemaDiagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL, "binding", "Invalid binding model.");
    BindingResult bindingResult = BindingResult.empty(List.of(diagnostic));

    GeneratedWriterEmissionResult result = new GeneratedWriterEmitter().emit(bindingResult);

    assertEquals(List.of(diagnostic), result.diagnostics());
    assertTrue(result.sources().isEmpty());
  }

  @Test
  void rejectsUnsupportedWriterModelsWithoutPartialSources() {
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
                        field("element", "date", scalar("date"), required(), 1))),
                type("com.example.orders", "Owner", List.of())));

    GeneratedWriterEmissionResult result = new GeneratedWriterEmitter().emit(model);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_WRITER_EMISSION_INVALID_MODEL,
            DiagnosticCode.SCHEMA_WRITER_EMISSION_INVALID_MODEL,
            DiagnosticCode.SCHEMA_WRITER_EMISSION_INVALID_MODEL),
        diagnosticCodes(result));
    assertTrue(result.sources().isEmpty());
  }

  private List<DiagnosticCode> diagnosticCodes(GeneratedWriterEmissionResult result) {
    return result.diagnostics().stream().map(SchemaDiagnostic::code).toList();
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
    return new BindingField(
        kind,
        schemaName(localName),
        localName,
        type,
        cardinality,
        order,
        cardinality.minOccurs() > 0);
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

  private SchemaQName schemaName(String localName) {
    return new SchemaQName("urn:orders", localName);
  }

  private static final class RecordingXmlOutput implements XmlOutput {
    private final List<String> events = new ArrayList<>();

    @Override
    public void startDocument() {}

    @Override
    public void endDocument() {}

    @Override
    public void startElement(XmlName name) {
      events.add("start:" + toText(name));
    }

    @Override
    public void attribute(XmlName name, String value) {
      events.add("attr:" + toText(name) + "=" + value);
    }

    @Override
    public void text(String value) {
      events.add("text:" + value);
    }

    @Override
    public void endElement(XmlName name) {
      events.add("end:" + toText(name));
    }

    @Override
    public void flush() {}

    private String toText(XmlName name) {
      return "{" + name.namespaceUri() + "}" + name.localName();
    }
  }
}
