package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.mundanej.mxjb.generator.core.bind.BindingCardinality;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingModel;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.BindingValidationPlan;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import io.github.mundanej.mxjb.runtime.ValidationResult;
import io.github.mundanej.mxjb.runtime.XmlEventKind;
import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.XmlLocation;
import io.github.mundanej.mxjb.runtime.XmlName;
import io.github.mundanej.mxjb.runtime.XmlOutput;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GeneratedSourceVerificationHarnessTest {
  private static final String GOLDEN_ROOT =
      "io/github/mundanej/mxjb/generator/core/emit/golden/basic-order";

  @TempDir private Path tempDirectory;

  @Test
  void verifiesGoldenCompileDeterminismAndWriterBehavior()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model = basicOrderModel();
    List<GeneratedJavaSource> first = generatedSources(model);
    List<GeneratedJavaSource> second = generatedSources(model);
    GeneratedSourceVerifier verifier = new GeneratedSourceVerifier(tempDirectory);

    verifier.assertDeterministic(first, second);
    verifier.assertGoldenSources(GOLDEN_ROOT, first);
    RecordingXmlOutput output = new RecordingXmlOutput();
    try (GeneratedSourceVerifier.CompiledSources compiledSources = verifier.compile(first)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> lineClass = compiledSources.load("com.example.lines.Line");
      Class<?> readerClass = compiledSources.load("com.example.orders.xml.OrderXmlReader");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Class<?> writerClass = compiledSources.load("com.example.orders.xml.OrderXmlWriter");
      Object firstLine = lineClass.getConstructor(String.class).newInstance("SKU-1");
      Object secondLine = lineClass.getConstructor(String.class).newInstance("SKU-2");
      Object order =
          orderClass
              .getConstructor(Optional.class, String.class, Optional.class, List.class)
              .newInstance(
                  Optional.of("v1"), "A-1", Optional.of("gift"), List.of(firstLine, secondLine));

      writerClass.getMethod("write", XmlOutput.class, orderClass).invoke(null, output, order);
      Object parsed =
          readerClass.getMethod("read", XmlEventReader.class).invoke(null, orderInput());
      ValidationResult objectValidation =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, order);
      ValidationResult xmlValidation =
          (ValidationResult)
              validatorClass.getMethod("validate", XmlEventReader.class).invoke(null, orderInput());
      assertEquals(Optional.of("v1"), orderClass.getMethod("version").invoke(parsed));
      assertEquals("A-1", orderClass.getMethod("id").invoke(parsed));
      assertEquals(Optional.of("gift"), orderClass.getMethod("note").invoke(parsed));
      assertEquals(2, ((List<?>) orderClass.getMethod("line").invoke(parsed)).size());
      assertEquals(true, objectValidation.isValid());
      assertEquals(true, xmlValidation.isValid());
    }

    assertEquals(
        List.of(
            "start:{urn:orders}order",
            "attr:{urn:orders}version=v1",
            "start:{urn:orders}id",
            "text:A-1",
            "end:{urn:orders}id",
            "start:{urn:orders}note",
            "text:gift",
            "end:{urn:orders}note",
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
  }

  private List<GeneratedJavaSource> generatedSources(BindingModel model) {
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    GeneratedValidatorEmissionResult validatorResult = new GeneratedValidatorEmitter().emit(model);
    GeneratedWriterEmissionResult writerResult = new GeneratedWriterEmitter().emit(model);
    assertFalse(modelResult.hasErrors());
    assertFalse(readerResult.hasErrors());
    assertFalse(validatorResult.hasErrors());
    assertFalse(writerResult.hasErrors());
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());
    sources.addAll(validatorResult.sources());
    sources.addAll(writerResult.sources());
    return sources;
  }

  private BindingModel basicOrderModel() {
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
                    field("element", "line", model("com.example.lines.Line"), list(), 3))),
            type(
                "com.example.lines",
                "Line",
                List.of(field("element", "sku", scalar("string"), required(), 1)))));
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
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "sku")),
        text("SKU-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "sku")),
        text("SKU-2"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "line")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
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
      return new XmlLocation("golden.xml", index + 1, 1);
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
