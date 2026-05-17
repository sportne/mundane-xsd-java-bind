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
      Class<?> writerClass = compiledSources.load("com.example.orders.xml.OrderXmlWriter");
      Object firstLine = lineClass.getConstructor(String.class).newInstance("SKU-1");
      Object secondLine = lineClass.getConstructor(String.class).newInstance("SKU-2");
      Object order =
          orderClass
              .getConstructor(Optional.class, String.class, Optional.class, List.class)
              .newInstance(
                  Optional.of("v1"), "A-1", Optional.of("gift"), List.of(firstLine, secondLine));

      writerClass.getMethod("write", XmlOutput.class, orderClass).invoke(null, output, order);
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
    GeneratedWriterEmissionResult writerResult = new GeneratedWriterEmitter().emit(model);
    assertFalse(modelResult.hasErrors());
    assertFalse(writerResult.hasErrors());
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
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
