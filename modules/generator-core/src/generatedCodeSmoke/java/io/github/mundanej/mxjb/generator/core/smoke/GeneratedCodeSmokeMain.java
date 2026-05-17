package io.github.mundanej.mxjb.generator.core.smoke;

import com.example.lines.Line;
import com.example.orders.Order;
import com.example.orders.xml.OrderXmlReader;
import com.example.orders.xml.OrderXmlValidator;
import com.example.orders.xml.OrderXmlWriter;
import io.github.mundanej.mxjb.runtime.ValidationResult;
import io.github.mundanej.mxjb.runtime.XmlEventKind;
import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.XmlLocation;
import io.github.mundanej.mxjb.runtime.XmlName;
import io.github.mundanej.mxjb.runtime.XmlOutput;
import io.github.mundanej.mxjb.runtime.XmlReadException;
import io.github.mundanej.mxjb.runtime.XmlWriteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Executable smoke check for approved generated model, reader, and writer fixtures. */
public final class GeneratedCodeSmokeMain {
  private GeneratedCodeSmokeMain() {}

  public static void main(String[] args) throws XmlReadException, XmlWriteException {
    RecordingXmlOutput output = new RecordingXmlOutput();
    Order order =
        new Order(
            Optional.of("v1"),
            "A-1",
            Optional.of("gift"),
            List.of(new Line("SKU-1"), new Line("SKU-2")));

    OrderXmlWriter.write(output, order);

    List<String> expected =
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
            "end:{urn:orders}order");
    if (!expected.equals(output.events)) {
      throw new AssertionError("Generated-code smoke output mismatch: " + output.events);
    }

    Order parsed = OrderXmlReader.read(orderInput());
    if (!order.version().equals(parsed.version())
        || !order.id().equals(parsed.id())
        || !order.note().equals(parsed.note())
        || parsed.line().size() != 2
        || !"SKU-1".equals(parsed.line().get(0).sku())
        || !"SKU-2".equals(parsed.line().get(1).sku())) {
      throw new AssertionError("Generated-code smoke reader mismatch: " + parsed);
    }

    ValidationResult objectValidation = OrderXmlValidator.validate(order);
    ValidationResult xmlValidation = OrderXmlValidator.validate(orderInput());
    if (!objectValidation.isValid() || !xmlValidation.isValid()) {
      throw new AssertionError(
          "Generated-code smoke validator mismatch: "
              + objectValidation.errors()
              + " / "
              + xmlValidation.errors());
    }
    runChoiceSmoke();
    runFacetSmoke();
    runComposedSmoke();
  }

  private static void runChoiceSmoke() throws XmlReadException, XmlWriteException {
    RecordingXmlOutput output = new RecordingXmlOutput();
    com.example.choice.Order order =
        new com.example.choice.Order(
            "C-1", Optional.of(new com.example.choice.DomesticChoice("US")));

    com.example.choice.xml.OrderXmlWriter.write(output, order);

    List<String> expected =
        List.of(
            "start:{urn:choice}order",
            "start:{urn:choice}id",
            "text:C-1",
            "end:{urn:choice}id",
            "start:{urn:choice}domestic",
            "text:US",
            "end:{urn:choice}domestic",
            "end:{urn:choice}order");
    if (!expected.equals(output.events)) {
      throw new AssertionError("Generated-code choice smoke output mismatch: " + output.events);
    }

    com.example.choice.Order parsed = com.example.choice.xml.OrderXmlReader.read(choiceInput());
    if (!order.equals(parsed)) {
      throw new AssertionError("Generated-code choice smoke reader mismatch: " + parsed);
    }

    ValidationResult objectValidation = com.example.choice.xml.OrderXmlValidator.validate(order);
    ValidationResult xmlValidation =
        com.example.choice.xml.OrderXmlValidator.validate(choiceInput());
    if (!objectValidation.isValid() || !xmlValidation.isValid()) {
      throw new AssertionError(
          "Generated-code choice smoke validator mismatch: "
              + objectValidation.errors()
              + " / "
              + xmlValidation.errors());
    }
  }

  private static void runFacetSmoke() throws XmlReadException, XmlWriteException {
    RecordingXmlOutput output = new RecordingXmlOutput();
    com.example.facet.Order order = new com.example.facet.Order("AB12", 3);

    com.example.facet.xml.OrderXmlWriter.write(output, order);

    List<String> expected =
        List.of(
            "start:{urn:facet}order",
            "start:{urn:facet}code",
            "text:AB12",
            "end:{urn:facet}code",
            "start:{urn:facet}priority",
            "text:3",
            "end:{urn:facet}priority",
            "end:{urn:facet}order");
    if (!expected.equals(output.events)) {
      throw new AssertionError("Generated-code facet smoke output mismatch: " + output.events);
    }

    com.example.facet.Order parsed = com.example.facet.xml.OrderXmlReader.read(facetInput());
    if (!order.equals(parsed)) {
      throw new AssertionError("Generated-code facet smoke reader mismatch: " + parsed);
    }

    ValidationResult objectValidation = com.example.facet.xml.OrderXmlValidator.validate(order);
    ValidationResult xmlValidation = com.example.facet.xml.OrderXmlValidator.validate(facetInput());
    ValidationResult invalidValidation =
        com.example.facet.xml.OrderXmlValidator.validate(new com.example.facet.Order("ab", 12));
    if (!objectValidation.isValid() || !xmlValidation.isValid() || invalidValidation.isValid()) {
      throw new AssertionError(
          "Generated-code facet smoke validator mismatch: "
              + objectValidation.errors()
              + " / "
              + xmlValidation.errors()
              + " / "
              + invalidValidation.errors());
    }
  }

  private static void runComposedSmoke() throws XmlReadException, XmlWriteException {
    RecordingXmlOutput output = new RecordingXmlOutput();
    com.example.composed.Order order =
        new com.example.composed.Order("v1", "PO-100", new java.math.BigDecimal("42.50"));

    com.example.composed.xml.OrderXmlWriter.write(output, order);

    List<String> expected =
        List.of(
            "start:{urn:composed}order",
            "attr:{urn:composed}version=v1",
            "start:{urn:composed}id",
            "text:PO-100",
            "end:{urn:composed}id",
            "start:{urn:composed}total",
            "text:42.50",
            "end:{urn:composed}total",
            "end:{urn:composed}order");
    if (!expected.equals(output.events)) {
      throw new AssertionError("Generated-code composed smoke output mismatch: " + output.events);
    }

    com.example.composed.Order parsed =
        com.example.composed.xml.OrderXmlReader.read(composedInput());
    if (!order.equals(parsed)) {
      throw new AssertionError("Generated-code composed smoke reader mismatch: " + parsed);
    }

    ValidationResult objectValidation = com.example.composed.xml.OrderXmlValidator.validate(order);
    ValidationResult xmlValidation =
        com.example.composed.xml.OrderXmlValidator.validate(composedInput());
    if (!objectValidation.isValid() || !xmlValidation.isValid()) {
      throw new AssertionError(
          "Generated-code composed smoke validator mismatch: "
              + objectValidation.errors()
              + " / "
              + xmlValidation.errors());
    }
  }

  private static EventXmlReader orderInput() {
    return new EventXmlReader(
        List.of(
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
            event(XmlEventKind.END_DOCUMENT, null)));
  }

  private static EventXmlReader choiceInput() {
    return new EventXmlReader(
        List.of(
            event(XmlEventKind.START_DOCUMENT, null),
            event(XmlEventKind.START_ELEMENT, new XmlName("urn:choice", "order")),
            event(XmlEventKind.START_ELEMENT, new XmlName("urn:choice", "id")),
            text("C-1"),
            event(XmlEventKind.END_ELEMENT, new XmlName("urn:choice", "id")),
            event(XmlEventKind.START_ELEMENT, new XmlName("urn:choice", "domestic")),
            text("US"),
            event(XmlEventKind.END_ELEMENT, new XmlName("urn:choice", "domestic")),
            event(XmlEventKind.END_ELEMENT, new XmlName("urn:choice", "order")),
            event(XmlEventKind.END_DOCUMENT, null)));
  }

  private static EventXmlReader facetInput() {
    return new EventXmlReader(
        List.of(
            event(XmlEventKind.START_DOCUMENT, null),
            event(XmlEventKind.START_ELEMENT, new XmlName("urn:facet", "order")),
            event(XmlEventKind.START_ELEMENT, new XmlName("urn:facet", "code")),
            text("AB12"),
            event(XmlEventKind.END_ELEMENT, new XmlName("urn:facet", "code")),
            event(XmlEventKind.START_ELEMENT, new XmlName("urn:facet", "priority")),
            text("3"),
            event(XmlEventKind.END_ELEMENT, new XmlName("urn:facet", "priority")),
            event(XmlEventKind.END_ELEMENT, new XmlName("urn:facet", "order")),
            event(XmlEventKind.END_DOCUMENT, null)));
  }

  private static EventXmlReader composedInput() {
    return new EventXmlReader(
        List.of(
            event(XmlEventKind.START_DOCUMENT, null),
            event(
                XmlEventKind.START_ELEMENT,
                new XmlName("urn:composed", "order"),
                Map.of(new XmlName("urn:composed", "version"), "v1")),
            event(XmlEventKind.START_ELEMENT, new XmlName("urn:composed", "id")),
            text("PO-100"),
            event(XmlEventKind.END_ELEMENT, new XmlName("urn:composed", "id")),
            event(XmlEventKind.START_ELEMENT, new XmlName("urn:composed", "total")),
            text("42.50"),
            event(XmlEventKind.END_ELEMENT, new XmlName("urn:composed", "total")),
            event(XmlEventKind.END_ELEMENT, new XmlName("urn:composed", "order")),
            event(XmlEventKind.END_DOCUMENT, null)));
  }

  private static Event event(XmlEventKind kind, XmlName name) {
    return new Event(kind, name, "", Map.of());
  }

  private static Event event(XmlEventKind kind, XmlName name, Map<XmlName, String> attributes) {
    return new Event(kind, name, "", attributes);
  }

  private static Event text(String value) {
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
      return new XmlLocation("smoke.xml", index + 1, 1);
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
