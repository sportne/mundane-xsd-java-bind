package io.github.mundanej.mxjb.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class XmlSchemaValueDeltaTest {
  @Test
  void qNamesCompareByExpandedNameAndRetainCollapsedLexicalName() throws XmlReadException {
    XmlQName parsed =
        (XmlQName)
            XmlDatatypes.parse("QName", " \t p:item \n ", prefixReader(), XmlLocation.UNKNOWN);
    XmlQName sameExpandedName = new XmlQName("urn:test", "item", "alt:item");

    assertEquals("urn:test", parsed.namespaceUri());
    assertEquals("item", parsed.localName());
    assertEquals("p:item", parsed.lexicalName());
    assertEquals(sameExpandedName, parsed);
    assertEquals(sameExpandedName.hashCode(), parsed.hashCode());
    assertNotEquals(new XmlQName("urn:other", "item", "p:item"), parsed);
    assertNotEquals(new XmlQName("urn:test", "other", "p:other"), parsed);
  }

  @Test
  void qNamesRejectLexicalNamesThatDisagreeWithLocalName() {
    assertThrows(IllegalArgumentException.class, () -> new XmlQName("urn:test", "item", "other"));
    assertThrows(IllegalArgumentException.class, () -> new XmlQName("urn:test", "item", "p:other"));
  }

  @Test
  void binaryValuesDefensivelyCopyBytesAndFormatCanonicalLexicalValues()
      throws XmlReadException, XmlWriteException {
    byte[] source = new byte[] {0, 10, -1};
    XmlBinary binary = new XmlBinary(source);
    source[0] = 99;

    assertArrayEquals(new byte[] {0, 10, -1}, binary.bytes());

    byte[] returned = binary.bytes();
    returned[1] = 99;

    assertArrayEquals(new byte[] {0, 10, -1}, binary.bytes());
    assertEquals(binary, XmlDatatypes.parse("hexBinary", "000AFF", null, XmlLocation.UNKNOWN));
    assertEquals(binary, XmlDatatypes.parse("base64Binary", " AAr/ ", null, XmlLocation.UNKNOWN));
    assertEquals("000aff", XmlDatatypes.format("hexBinary", binary, output()));
    assertEquals("AAr/", XmlDatatypes.format("base64Binary", binary, output()));
    assertEquals("AAr/", binary.toString());
  }

  @Test
  void temporalValuesAcceptTimezoneAndLeapYearBoundaries()
      throws XmlReadException, XmlWriteException {
    assertEquals(
        new XmlDateTime("2024-02-29T24:00:00+14:00"),
        XmlDatatypes.parse("dateTime", " 2024-02-29T24:00:00+14:00 ", null, XmlLocation.UNKNOWN));
    assertEquals(
        "2024-02-29T24:00:00+14:00",
        XmlDatatypes.format("dateTime", new XmlDateTime("2024-02-29T24:00:00+14:00"), output()));
    assertEquals(
        new XmlDate("2024-02-29Z"),
        XmlDatatypes.parse("date", "2024-02-29Z", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlTime("00:00:00-14:00"),
        XmlDatatypes.parse("time", "00:00:00-14:00", null, XmlLocation.UNKNOWN));
  }

  @Test
  void temporalValuesRejectInvalidTimezoneLeapAndClockBoundaries() {
    assertThrows(IllegalArgumentException.class, () -> new XmlDate("2023-02-29"));
    assertThrows(IllegalArgumentException.class, () -> new XmlDateTime("2024-02-29T24:00:01Z"));
    assertThrows(IllegalArgumentException.class, () -> new XmlTime("12:30:00+14:01"));
    assertThrows(IllegalArgumentException.class, () -> new XmlTime("23:59:60Z"));
    assertThrows(
        XmlReadException.class,
        () -> XmlDatatypes.parse("date", "2023-02-29", null, XmlLocation.UNKNOWN));
    assertThrows(
        XmlReadException.class,
        () -> XmlDatatypes.parse("time", "12:30:00+14:01", null, XmlLocation.UNKNOWN));
  }

  @Test
  void anyUriKeepsEmptyLexicalValueAndRejectsNull() throws XmlReadException, XmlWriteException {
    assertEquals("", new XmlAnyUri("").lexicalValue());
    assertEquals("", new XmlAnyUri("").toString());
    assertEquals(
        new XmlAnyUri(""), XmlDatatypes.parse("anyURI", " \t\n ", null, XmlLocation.UNKNOWN));
    assertEquals("", XmlDatatypes.format("anyURI", new XmlAnyUri(""), output()));
    assertThrows(NullPointerException.class, () -> new XmlAnyUri(null));
    assertThrows(
        NullPointerException.class,
        () -> XmlDatatypes.parse("anyURI", null, null, XmlLocation.UNKNOWN));
  }

  private static XmlEventReader prefixReader() {
    return new XmlEventReader() {
      @Override
      public XmlEventKind kind() {
        return XmlEventKind.START_DOCUMENT;
      }

      @Override
      public XmlName name() {
        return null;
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
        return XmlLocation.UNKNOWN;
      }

      @Override
      public String namespaceUriForPrefix(String prefix) {
        return "p".equals(prefix) || "alt".equals(prefix) ? "urn:test" : null;
      }

      @Override
      public boolean next() {
        return false;
      }
    };
  }

  private static XmlOutput output() {
    return new XmlOutput() {
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
      public void endElement(XmlName name) {}

      @Override
      public void flush() {}
    };
  }
}
