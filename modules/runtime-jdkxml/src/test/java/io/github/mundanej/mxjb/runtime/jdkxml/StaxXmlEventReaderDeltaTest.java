package io.github.mundanej.mxjb.runtime.jdkxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.runtime.XmlEventKind;
import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.XmlName;
import io.github.mundanej.mxjb.runtime.XmlReadException;
import java.io.StringReader;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.junit.jupiter.api.Test;

final class StaxXmlEventReaderDeltaTest {
  @Test
  void mapsCdataAndSpaceEventsToText() throws XMLStreamException, XmlReadException {
    XmlEventReader cdataReader = readerFor("<root><![CDATA[raw < & data]]></root>");

    assertTrue(cdataReader.next());
    assertEquals(XmlEventKind.START_ELEMENT, cdataReader.kind());
    assertTrue(cdataReader.next());
    assertEquals(XmlEventKind.TEXT, cdataReader.kind());
    assertEquals("raw < & data", cdataReader.text());

    XmlEventReader spaceReader = JdkXmlAdapters.eventReader(new SpaceXmlStreamReader());

    assertTrue(spaceReader.next());
    assertEquals(XmlEventKind.TEXT, spaceReader.kind());
    assertEquals(" \n\t", spaceReader.text());
  }

  @Test
  void skipsCommentsAndProcessingInstructions() throws XMLStreamException, XmlReadException {
    XmlEventReader reader =
        readerFor("<root><?target instruction?><!--comment--><child>value</child></root>");

    assertTrue(reader.next());
    assertEquals(new XmlName("", "root"), reader.name());
    assertTrue(reader.next());
    assertEquals(XmlEventKind.START_ELEMENT, reader.kind());
    assertEquals(new XmlName("", "child"), reader.name());
    assertTrue(reader.next());
    assertEquals(XmlEventKind.TEXT, reader.kind());
    assertEquals("value", reader.text());
  }

  @Test
  void resolvesDefaultNamespaceAndMissingPrefix() throws XMLStreamException, XmlReadException {
    XmlEventReader reader =
        readerFor("<root xmlns=\"urn:default\" xmlns:p=\"urn:prefixed\"><p:child/></root>");

    assertTrue(reader.next());

    assertEquals("urn:default", reader.namespaceUriForPrefix(""));
    assertEquals("urn:prefixed", reader.namespaceUriForPrefix("p"));
    assertNull(reader.namespaceUriForPrefix("missing"));
  }

  @Test
  void rejectsInvalidAttributeIndexes() throws XMLStreamException, XmlReadException {
    XmlEventReader reader = readerFor("<root id=\"A-1\"/>");

    assertTrue(reader.next());
    assertEquals(1, reader.attributeCount());
    assertEquals(new XmlName("", "id"), reader.attributeName(0));
    assertEquals("A-1", reader.attributeValue(0));

    assertThrows(IndexOutOfBoundsException.class, () -> reader.attributeName(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> reader.attributeName(1));
    assertThrows(IndexOutOfBoundsException.class, () -> reader.attributeValue(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> reader.attributeValue(1));
  }

  @Test
  void wrapsXmlStreamExceptionWithDiagnosticCodeAndCause() {
    XMLStreamException failure =
        new XMLStreamException("boom", new FixedLocation("memory://broken.xml", 12, 3));
    XmlEventReader reader = JdkXmlAdapters.eventReader(new ThrowingXmlStreamReader(failure));

    XmlReadException exception = assertThrows(XmlReadException.class, reader::next);

    assertEquals("MXJB-JDKXML-R-001", exception.diagnostic().code());
    assertEquals("JDK XML reader failed.", exception.diagnostic().message());
    assertEquals("memory://broken.xml", exception.diagnostic().location().systemId());
    assertEquals(12, exception.diagnostic().location().lineNumber());
    assertEquals(3, exception.diagnostic().location().columnNumber());
    assertSame(failure, exception.getCause());
    assertInstanceOf(XMLStreamException.class, exception.getCause());
  }

  private static XmlEventReader readerFor(String xml) throws XMLStreamException {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();
    factory.setProperty(XMLInputFactory.IS_COALESCING, false);
    return JdkXmlAdapters.eventReader(factory.createXMLStreamReader(new StringReader(xml)));
  }

  private static final class SpaceXmlStreamReader extends AbstractXmlStreamReader {
    private int eventType = XMLStreamConstants.START_DOCUMENT;

    @Override
    public int next() {
      eventType = XMLStreamConstants.SPACE;
      return eventType;
    }

    @Override
    public boolean hasNext() {
      return eventType == XMLStreamConstants.START_DOCUMENT;
    }

    @Override
    public int getEventType() {
      return eventType;
    }

    @Override
    public String getText() {
      return " \n\t";
    }

    @Override
    public boolean hasText() {
      return eventType == XMLStreamConstants.SPACE;
    }
  }

  private static final class ThrowingXmlStreamReader extends AbstractXmlStreamReader {
    private final XMLStreamException failure;

    private ThrowingXmlStreamReader(XMLStreamException failure) {
      this.failure = failure;
    }

    @Override
    public int next() throws XMLStreamException {
      throw failure;
    }

    @Override
    public boolean hasNext() {
      return true;
    }
  }

  private static final class FixedLocation implements Location {
    private final String systemId;
    private final int lineNumber;
    private final int columnNumber;

    private FixedLocation(String systemId, int lineNumber, int columnNumber) {
      this.systemId = systemId;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }

    @Override
    public int getLineNumber() {
      return lineNumber;
    }

    @Override
    public int getColumnNumber() {
      return columnNumber;
    }

    @Override
    public int getCharacterOffset() {
      return -1;
    }

    @Override
    public String getPublicId() {
      return null;
    }

    @Override
    public String getSystemId() {
      return systemId;
    }
  }

  private abstract static class AbstractXmlStreamReader implements XMLStreamReader {
    @Override
    public Object getProperty(String name) {
      return null;
    }

    @Override
    public void require(int type, String namespaceUri, String localName) {}

    @Override
    public String getElementText() {
      return "";
    }

    @Override
    public int nextTag() {
      return XMLStreamConstants.END_DOCUMENT;
    }

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public void close() {}

    @Override
    public String getNamespaceURI(String prefix) {
      return null;
    }

    @Override
    public boolean isStartElement() {
      return getEventType() == XMLStreamConstants.START_ELEMENT;
    }

    @Override
    public boolean isEndElement() {
      return getEventType() == XMLStreamConstants.END_ELEMENT;
    }

    @Override
    public boolean isCharacters() {
      return getEventType() == XMLStreamConstants.CHARACTERS;
    }

    @Override
    public boolean isWhiteSpace() {
      return getEventType() == XMLStreamConstants.SPACE;
    }

    @Override
    public String getAttributeValue(String namespaceUri, String localName) {
      return null;
    }

    @Override
    public int getAttributeCount() {
      return 0;
    }

    @Override
    public QName getAttributeName(int index) {
      return null;
    }

    @Override
    public String getAttributeNamespace(int index) {
      return null;
    }

    @Override
    public String getAttributeLocalName(int index) {
      return null;
    }

    @Override
    public String getAttributePrefix(int index) {
      return null;
    }

    @Override
    public String getAttributeType(int index) {
      return null;
    }

    @Override
    public String getAttributeValue(int index) {
      return null;
    }

    @Override
    public boolean isAttributeSpecified(int index) {
      return false;
    }

    @Override
    public int getNamespaceCount() {
      return 0;
    }

    @Override
    public String getNamespacePrefix(int index) {
      return null;
    }

    @Override
    public String getNamespaceURI(int index) {
      return null;
    }

    @Override
    public NamespaceContext getNamespaceContext() {
      return null;
    }

    @Override
    public int getEventType() {
      return XMLStreamConstants.START_DOCUMENT;
    }

    @Override
    public String getText() {
      return "";
    }

    @Override
    public char[] getTextCharacters() {
      return getText().toCharArray();
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) {
      char[] text = getTextCharacters();
      int copied = Math.min(length, text.length - sourceStart);
      System.arraycopy(text, sourceStart, target, targetStart, copied);
      return copied;
    }

    @Override
    public int getTextStart() {
      return 0;
    }

    @Override
    public int getTextLength() {
      return getText().length();
    }

    @Override
    public String getEncoding() {
      return null;
    }

    @Override
    public boolean hasText() {
      return false;
    }

    @Override
    public Location getLocation() {
      return null;
    }

    @Override
    public QName getName() {
      return null;
    }

    @Override
    public String getLocalName() {
      return null;
    }

    @Override
    public boolean hasName() {
      return false;
    }

    @Override
    public String getNamespaceURI() {
      return null;
    }

    @Override
    public String getPrefix() {
      return null;
    }

    @Override
    public String getVersion() {
      return null;
    }

    @Override
    public boolean isStandalone() {
      return false;
    }

    @Override
    public boolean standaloneSet() {
      return false;
    }

    @Override
    public String getCharacterEncodingScheme() {
      return null;
    }

    @Override
    public String getPITarget() {
      return null;
    }

    @Override
    public String getPIData() {
      return null;
    }
  }
}
