package io.github.mundanej.mxjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

final class XmlDatatypesDeltaTest {
  private static final XmlLocation LOCATION = new XmlLocation("delta.xml", 7, 11);

  @Test
  void parseAndFormatRejectNullRequiredArguments() {
    assertNullPointer("type", () -> XmlDatatypes.parse(null, "value", null, LOCATION));
    assertNullPointer("value", () -> XmlDatatypes.parse("string", null, null, LOCATION));
    assertNullPointer("type", () -> XmlDatatypes.format(null, "value", new RecordingOutput()));
    assertNullPointer("value", () -> XmlDatatypes.format("string", null, new RecordingOutput()));
    assertNullPointer("javaType", () -> XmlDatatypes.parseList("int", "1", null, LOCATION, null));
    assertNullPointer(
        "values", () -> XmlDatatypes.formatList("string", null, new RecordingOutput()));
  }

  @Test
  void unsupportedDatatypeReportsStableParseDiagnostic() {
    XmlReadException exception =
        assertThrows(
            XmlReadException.class,
            () -> XmlDatatypes.parse("unknownType", "value", null, LOCATION));

    assertDatatypeDiagnostic(exception, "Invalid unknownType value.");
    assertTrue(exception.getCause() instanceof IllegalArgumentException);
    assertEquals("Unsupported XML Schema datatype unknownType.", exception.getCause().getMessage());
  }

  @Test
  void parseFailuresRetainDiagnosticCodeLocationAndCause() {
    XmlReadException exception =
        assertThrows(
            XmlReadException.class, () -> XmlDatatypes.parse("boolean", "yes", null, LOCATION));

    assertDatatypeDiagnostic(exception, "Invalid boolean value.");
    assertTrue(exception.getCause() instanceof IllegalArgumentException);
    assertEquals("Invalid boolean value.", exception.getCause().getMessage());
    assertEquals("Invalid boolean value.", exception.getMessage());
  }

  @Test
  void typedParseListReturnsTypedImmutableValues() throws XmlReadException {
    List<Integer> values =
        XmlDatatypes.parseList("int", " \t1\n2\r 3 ", null, LOCATION, Integer.class);

    assertEquals(List.of(1, 2, 3), values);
    assertThrows(UnsupportedOperationException.class, () -> values.add(4));
  }

  @Test
  void formatListFormatsQNamesThroughOutput() throws XmlWriteException {
    RecordingOutput output = new RecordingOutput();
    XmlQName first = new XmlQName("urn:first", "one", "f:one");
    XmlQName second = new XmlQName("urn:second", "two", "s:two");

    String lexical = XmlDatatypes.formatList("QName", List.of(first, second), output);

    assertEquals("{urn:first}one {urn:second}two", lexical);
    assertEquals(List.of(first, second), output.qNames);
  }

  @Test
  void decimalDigitFacetsUseCanonicalDecimalBoundaries() {
    BigDecimal value = new BigDecimal("00123.4500");

    assertTrue(matchesDecimalDigits(value, 5, 2));
    assertTrue(matchesDecimalDigits(BigDecimal.ZERO, 1, 0));
    assertTrue(matchesDecimalDigits(new BigDecimal("-12.30"), 3, 1));
    assertFalse(matchesDecimalDigits(value, 4, 2));
    assertFalse(matchesDecimalDigits(value, 5, 1));
  }

  @Test
  void qNameParseFailuresReportUnresolvedAndMalformedLexicalDiagnostics() {
    XmlReadException unresolved =
        assertThrows(
            XmlReadException.class,
            () -> XmlDatatypes.parse("QName", "p:local", new EmptyNamespaceReader(), LOCATION));
    XmlReadException multipleColon =
        assertThrows(
            XmlReadException.class,
            () -> XmlDatatypes.parse("QName", "p:local:again", prefixReader(), LOCATION));

    assertDatatypeDiagnostic(unresolved, "Invalid QName value.");
    assertTrue(unresolved.getCause() instanceof IllegalArgumentException);
    assertEquals("Unresolved QName prefix p.", unresolved.getCause().getMessage());
    assertDatatypeDiagnostic(multipleColon, "Invalid QName value.");
    assertTrue(multipleColon.getCause() instanceof IllegalArgumentException);
    assertEquals("Invalid QName local name value.", multipleColon.getCause().getMessage());
  }

  private static void assertNullPointer(String message, Executable action) {
    assertEquals(message, assertThrows(NullPointerException.class, action).getMessage());
  }

  private static void assertDatatypeDiagnostic(XmlReadException exception, String message) {
    XmlDiagnostic diagnostic = exception.diagnostic();

    assertEquals(XmlDiagnosticSeverity.ERROR, diagnostic.severity());
    assertEquals("MXJB-DT-001", diagnostic.code());
    assertEquals(message, diagnostic.message());
    assertSame(LOCATION, diagnostic.location());
  }

  private static boolean matchesDecimalDigits(
      BigDecimal value, Integer totalDigits, Integer fractionDigits) {
    return XmlDatatypes.matchesFacets(
        "decimal",
        value,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        totalDigits,
        fractionDigits,
        List.of());
  }

  private static XmlEventReader prefixReader() {
    return new EmptyNamespaceReader() {
      @Override
      public String namespaceUriForPrefix(String prefix) {
        return "p".equals(prefix) ? "urn:test" : null;
      }
    };
  }

  private static class EmptyNamespaceReader implements XmlEventReader {
    @Override
    public XmlEventKind kind() {
      return XmlEventKind.TEXT;
    }

    @Override
    public XmlName name() {
      return new XmlName("", "text");
    }

    @Override
    public String text() {
      return "";
    }

    @Override
    public int attributeCount() {
      return 0;
    }

    @Override
    public XmlName attributeName(int index) {
      throw new IndexOutOfBoundsException(index);
    }

    @Override
    public String attributeValue(int index) {
      throw new IndexOutOfBoundsException(index);
    }

    @Override
    public XmlLocation location() {
      return LOCATION;
    }

    @Override
    public boolean next() {
      return false;
    }
  }

  private static final class RecordingOutput implements XmlOutput {
    private final List<XmlQName> qNames = new ArrayList<>();

    @Override
    public void startDocument() {}

    @Override
    public void endDocument() {}

    @Override
    public void startElement(XmlName name) {}

    @Override
    public void attribute(XmlName name, String value) {}

    @Override
    public void text(String value) {}

    @Override
    public String qNameText(XmlQName value) {
      qNames.add(value);
      return "{" + value.namespaceUri() + "}" + value.localName();
    }

    @Override
    public void endElement(XmlName name) {}

    @Override
    public void flush() {}
  }
}
