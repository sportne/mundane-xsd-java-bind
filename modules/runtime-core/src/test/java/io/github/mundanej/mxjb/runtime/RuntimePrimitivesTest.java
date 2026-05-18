package io.github.mundanej.mxjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RuntimePrimitivesTest {
  @Test
  void xmlNamesRequireLocalNamesAndAllowEmptyNamespaces() {
    XmlName name = new XmlName("", "order");

    assertEquals("", name.namespaceUri());
    assertEquals("order", name.localName());
    assertThrows(NullPointerException.class, () -> new XmlName(null, "order"));
    assertThrows(NullPointerException.class, () -> new XmlName("", null));
    assertThrows(IllegalArgumentException.class, () -> new XmlName("", " "));
  }

  @Test
  void xmlLocationsRepresentUnknownAndValidateKnownCoordinates() {
    assertEquals("", XmlLocation.UNKNOWN.systemId());
    assertEquals(-1, XmlLocation.UNKNOWN.lineNumber());
    assertEquals(-1, XmlLocation.UNKNOWN.columnNumber());

    XmlLocation location = new XmlLocation("order.xml", 12, 4);

    assertEquals("order.xml", location.systemId());
    assertEquals(12, location.lineNumber());
    assertEquals(4, location.columnNumber());
    assertThrows(NullPointerException.class, () -> new XmlLocation(null, -1, -1));
    assertThrows(IllegalArgumentException.class, () -> new XmlLocation("", 0, -1));
    assertThrows(IllegalArgumentException.class, () -> new XmlLocation("", -1, 0));
  }

  @Test
  void diagnosticsAndCheckedExceptionsRetainStableData() {
    XmlDiagnostic diagnostic =
        new XmlDiagnostic(
            XmlDiagnosticSeverity.ERROR, "MXJB-R-001", "Expected order.", XmlLocation.UNKNOWN);
    IllegalStateException cause = new IllegalStateException("cause");

    XmlReadException readException = new XmlReadException(diagnostic, cause);
    XmlWriteException writeException = new XmlWriteException(diagnostic, cause);

    assertSame(diagnostic, readException.diagnostic());
    assertSame(cause, readException.getCause());
    assertEquals("Expected order.", readException.getMessage());
    assertSame(diagnostic, writeException.diagnostic());
    assertSame(cause, writeException.getCause());
    assertEquals("Expected order.", writeException.getMessage());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new XmlDiagnostic(
                XmlDiagnosticSeverity.ERROR, " ", "Expected order.", XmlLocation.UNKNOWN));
  }

  @Test
  void validationResultsAreImmutableAndReportValidity() {
    ValidationError error =
        new ValidationError("MXJB-V-001", "Missing required element.", XmlLocation.UNKNOWN);
    List<ValidationError> errors = new ArrayList<>();
    errors.add(error);

    ValidationResult result = ValidationResult.invalid(errors);
    errors.clear();

    assertFalse(result.isValid());
    assertEquals(List.of(error), result.errors());
    assertEquals(List.of(error), ValidationResult.invalid(error).errors());
    assertThrows(UnsupportedOperationException.class, () -> result.errors().add(error));
    assertTrue(ValidationResult.valid().isValid());
    assertThrows(
        IllegalArgumentException.class,
        () -> new ValidationError("", "message", XmlLocation.UNKNOWN));
    assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid(List.of()));
    assertThrows(NullPointerException.class, () -> new ValidationResult(null));
    assertThrows(
        NullPointerException.class, () -> ValidationResult.invalid((List<ValidationError>) null));
    assertThrows(NullPointerException.class, () -> ValidationResult.invalid(null, error));
    assertThrows(
        NullPointerException.class,
        () -> ValidationResult.invalid(error, (ValidationError[]) null));
  }

  @Test
  void xmlFragmentsRetainExpandedNamesAttributesAndContentImmutably() {
    XmlName extensionName = new XmlName("urn:extension", "note");
    XmlName attributeName = new XmlName("", "code");
    XmlAttribute attribute = new XmlAttribute(attributeName, "A-1");
    XmlFragment child = new XmlFragment(new XmlName("", "child"), List.of(), List.of());
    List<XmlAttribute> attributes = new ArrayList<>();
    attributes.add(attribute);
    List<XmlFragmentContent> content = new ArrayList<>();
    content.add(new XmlFragmentText("before"));
    content.add(new XmlFragmentElement(child));

    XmlFragment fragment = new XmlFragment(extensionName, attributes, content);
    attributes.clear();
    content.clear();

    assertEquals(extensionName, fragment.name());
    assertEquals(List.of(attribute), fragment.attributes());
    assertEquals(2, fragment.content().size());
    assertThrows(UnsupportedOperationException.class, () -> fragment.attributes().add(attribute));
    assertThrows(UnsupportedOperationException.class, () -> fragment.content().clear());
    assertThrows(NullPointerException.class, () -> new XmlAttribute(null, "value"));
    assertThrows(NullPointerException.class, () -> new XmlAttribute(attributeName, null));
    assertThrows(NullPointerException.class, () -> new XmlFragmentText(null));
    assertThrows(NullPointerException.class, () -> new XmlFragmentElement(null));
    assertThrows(NullPointerException.class, () -> new XmlFragment(null, List.of(), List.of()));
    assertThrows(NullPointerException.class, () -> new XmlFragment(extensionName, null, List.of()));
    assertThrows(NullPointerException.class, () -> new XmlFragment(extensionName, List.of(), null));
  }

  @Test
  void noCauseExceptionConstructorsRetainDiagnostics() {
    XmlDiagnostic diagnostic =
        new XmlDiagnostic(
            XmlDiagnosticSeverity.WARNING, "MXJB-W-001", "Optional warning.", XmlLocation.UNKNOWN);

    assertSame(diagnostic, new XmlReadException(diagnostic).diagnostic());
    assertSame(diagnostic, new XmlWriteException(diagnostic).diagnostic());
    assertThrows(NullPointerException.class, () -> readException(null));
    assertThrows(NullPointerException.class, () -> writeException(null));
  }

  @Test
  void pullInterfacesSupportGeneratedReaderAndWriterShapes()
      throws XmlReadException, XmlWriteException {
    XmlName order = new XmlName("urn:orders", "order");
    XmlName id = new XmlName("", "id");
    FakeReader reader = new FakeReader(order, id, "A-1");
    RecordingOutput output = new RecordingOutput();

    assertEquals(XmlEventKind.START_ELEMENT, reader.kind());
    assertEquals(order, reader.name());
    assertEquals(1, reader.attributeCount());
    assertEquals(id, reader.attributeName(0));
    assertEquals("A-1", reader.attributeValue(0));
    assertTrue(reader.next());
    assertEquals(XmlEventKind.END_ELEMENT, reader.kind());
    assertFalse(reader.next());

    output.startDocument();
    output.startElement(order);
    output.attribute(id, "A-1");
    output.text("body");
    output.endElement(order);
    output.endDocument();
    output.flush();

    assertEquals(
        List.of(
            "startDocument",
            "start:urn:orders:order",
            "attr:id=A-1",
            "text:body",
            "end:order",
            "endDocument",
            "flush"),
        output.events);
  }

  private static final class FakeReader implements XmlEventReader {
    private final XmlName elementName;
    private final XmlName attributeName;
    private final String attributeValue;
    private boolean ended;

    private FakeReader(XmlName elementName, XmlName attributeName, String attributeValue) {
      this.elementName = elementName;
      this.attributeName = attributeName;
      this.attributeValue = attributeValue;
    }

    @Override
    public XmlEventKind kind() {
      return ended ? XmlEventKind.END_ELEMENT : XmlEventKind.START_ELEMENT;
    }

    @Override
    public XmlName name() {
      return elementName;
    }

    @Override
    public String text() {
      return "";
    }

    @Override
    public int attributeCount() {
      return ended ? 0 : 1;
    }

    @Override
    public XmlName attributeName(int index) {
      if (index != 0 || ended) {
        throw new IndexOutOfBoundsException(index);
      }
      return attributeName;
    }

    @Override
    public String attributeValue(int index) {
      if (index != 0 || ended) {
        throw new IndexOutOfBoundsException(index);
      }
      return attributeValue;
    }

    @Override
    public XmlLocation location() {
      return XmlLocation.UNKNOWN;
    }

    @Override
    public boolean next() {
      if (ended) {
        return false;
      }
      ended = true;
      return true;
    }
  }

  private static XmlReadException readException(XmlDiagnostic diagnostic) {
    return new XmlReadException(diagnostic);
  }

  private static XmlWriteException writeException(XmlDiagnostic diagnostic) {
    return new XmlWriteException(diagnostic);
  }

  private static final class RecordingOutput implements XmlOutput {
    private final List<String> events = new ArrayList<>();

    @Override
    public void startDocument() {
      events.add("startDocument");
    }

    @Override
    public void endDocument() {
      events.add("endDocument");
    }

    @Override
    public void startElement(XmlName name) {
      events.add("start:" + name.namespaceUri() + ":" + name.localName());
    }

    @Override
    public void attribute(XmlName name, String value) {
      events.add("attr:" + name.localName() + "=" + value);
    }

    @Override
    public void text(String value) {
      events.add("text:" + value);
    }

    @Override
    public void endElement(XmlName name) {
      events.add("end:" + name.localName());
    }

    @Override
    public void flush() {
      events.add("flush");
    }
  }
}
