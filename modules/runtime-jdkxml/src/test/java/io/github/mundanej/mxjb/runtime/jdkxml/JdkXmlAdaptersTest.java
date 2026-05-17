package io.github.mundanej.mxjb.runtime.jdkxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.runtime.XmlEventKind;
import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.XmlName;
import io.github.mundanej.mxjb.runtime.XmlOutput;
import io.github.mundanej.mxjb.runtime.XmlReadException;
import io.github.mundanej.mxjb.runtime.XmlWriteException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.Test;

final class JdkXmlAdaptersTest {
  @Test
  void secureInputFactoryDisablesDtdAndExternalEntities() {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();

    assertEquals(false, factory.getProperty(XMLInputFactory.SUPPORT_DTD));
    assertEquals(false, factory.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES));
    assertNotNull(factory.getXMLResolver());
  }

  @Test
  void secureInputFactoryRejectsExternalEntityExpansion() throws XMLStreamException {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();
    XMLStreamReader streamReader =
        factory.createXMLStreamReader(
            new StringReader(
                "<!DOCTYPE root [<!ENTITY ext SYSTEM \"https://example.invalid/secret.txt\">]>"
                    + "<root>&ext;</root>"));
    XmlEventReader reader = JdkXmlAdapters.eventReader(streamReader);

    assertEquals(XmlEventKind.START_DOCUMENT, reader.kind());
    assertThrows(XmlReadException.class, () -> drain(reader));
  }

  @Test
  void eventReaderMapsElementsAttributesTextAndLocations()
      throws XMLStreamException, XmlReadException {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();
    XMLStreamReader streamReader =
        factory.createXMLStreamReader(
            "memory://order.xml",
            new StringReader("<order id=\"A-1\"><line xmlns=\"urn:lines\">sku</line></order>"));
    XmlEventReader reader = JdkXmlAdapters.eventReader(streamReader);

    assertEquals(XmlEventKind.START_DOCUMENT, reader.kind());
    assertNull(reader.name());
    assertEquals("", reader.text());
    assertEquals(0, reader.attributeCount());
    assertTrue(reader.location().lineNumber() > 0);

    assertTrue(reader.next());
    assertEquals(XmlEventKind.START_ELEMENT, reader.kind());
    assertEquals(new XmlName("", "order"), reader.name());
    assertEquals(1, reader.attributeCount());
    assertEquals(new XmlName("", "id"), reader.attributeName(0));
    assertEquals("A-1", reader.attributeValue(0));

    assertTrue(reader.next());
    assertEquals(XmlEventKind.START_ELEMENT, reader.kind());
    assertEquals(new XmlName("urn:lines", "line"), reader.name());
    assertEquals(0, reader.attributeCount());

    assertTrue(reader.next());
    assertEquals(XmlEventKind.TEXT, reader.kind());
    assertEquals("sku", reader.text());

    assertTrue(reader.next());
    assertEquals(XmlEventKind.END_ELEMENT, reader.kind());
    assertEquals(new XmlName("urn:lines", "line"), reader.name());

    assertTrue(reader.next());
    assertEquals(XmlEventKind.END_ELEMENT, reader.kind());
    assertEquals(new XmlName("", "order"), reader.name());

    assertTrue(reader.next());
    assertEquals(XmlEventKind.END_DOCUMENT, reader.kind());
    assertFalse(reader.next());
    assertEquals(XmlEventKind.END_DOCUMENT, reader.kind());
  }

  @Test
  void adaptersRequireDelegateInstances() {
    assertThrows(NullPointerException.class, () -> JdkXmlAdapters.eventReader(null));
    assertThrows(NullPointerException.class, () -> JdkXmlAdapters.output(null));
  }

  @Test
  void outputWritesDeterministicNamespacePrefixes() throws XMLStreamException, XmlWriteException {
    StringWriter xml = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(xml);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);

    output.startDocument();
    output.startElement(new XmlName("urn:orders", "order"));
    output.attribute(new XmlName("", "id"), "A-1");
    output.startElement(new XmlName("urn:lines", "line"));
    output.attribute(new XmlName("urn:lines", "code"), "L-1");
    output.text("sku");
    output.endElement(new XmlName("urn:lines", "line"));
    output.endElement(new XmlName("urn:orders", "order"));
    output.endDocument();
    output.flush();

    assertEquals(
        "<?xml version=\"1.0\" ?><ns1:order xmlns:ns1=\"urn:orders\" id=\"A-1\">"
            + "<ns2:line xmlns:ns2=\"urn:lines\" ns2:code=\"L-1\">sku</ns2:line></ns1:order>",
        xml.toString());
  }

  @Test
  void outputUsesExistingWriterPrefixWhenAvailable() throws XMLStreamException, XmlWriteException {
    StringWriter xml = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(xml);
    streamWriter.setPrefix("ord", "urn:orders");
    XmlOutput output = JdkXmlAdapters.output(streamWriter);

    output.startElement(new XmlName("urn:orders", "order"));
    output.endElement(new XmlName("urn:orders", "order"));
    output.flush();

    assertEquals("<ord:order xmlns:ord=\"urn:orders\"></ord:order>", xml.toString());
  }

  @Test
  void outputDeclaresNamespaceForNamespacedAttributeOnUnqualifiedElement()
      throws XMLStreamException, XmlWriteException {
    StringWriter xml = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(xml);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);

    output.startElement(new XmlName("", "root"));
    output.attribute(new XmlName("urn:attrs", "code"), "A-1");
    output.endElement(new XmlName("", "root"));
    output.flush();

    assertEquals("<root xmlns:ns1=\"urn:attrs\" ns1:code=\"A-1\"></root>", xml.toString());
  }

  @Test
  void outputWrapsWriterFailures() {
    FailingXmlStreamWriter failingWriter = new FailingXmlStreamWriter();
    XmlOutput output = JdkXmlAdapters.output(failingWriter);

    XmlWriteException exception =
        assertThrows(XmlWriteException.class, () -> output.startElement(new XmlName("", "root")));

    assertEquals("MXJB-JDKXML-W-003", exception.diagnostic().code());
    assertInstanceOf(XMLStreamException.class, exception.getCause());
  }

  @Test
  void outputWrapsAllWriterOperations() {
    FailingXmlStreamWriter failingWriter = new FailingXmlStreamWriter();
    XmlOutput output = JdkXmlAdapters.output(failingWriter);

    assertEquals(
        "MXJB-JDKXML-W-001",
        assertThrows(XmlWriteException.class, output::startDocument).diagnostic().code());
    assertEquals(
        "MXJB-JDKXML-W-002",
        assertThrows(XmlWriteException.class, output::endDocument).diagnostic().code());
    assertEquals(
        "MXJB-JDKXML-W-004",
        assertThrows(XmlWriteException.class, () -> output.attribute(new XmlName("", "id"), "A-1"))
            .diagnostic()
            .code());
    assertEquals(
        "MXJB-JDKXML-W-005",
        assertThrows(XmlWriteException.class, () -> output.text("body")).diagnostic().code());
    assertEquals(
        "MXJB-JDKXML-W-006",
        assertThrows(XmlWriteException.class, () -> output.endElement(new XmlName("", "root")))
            .diagnostic()
            .code());
    assertEquals(
        "MXJB-JDKXML-W-007",
        assertThrows(XmlWriteException.class, output::flush).diagnostic().code());
  }

  private static void drain(XmlEventReader reader) throws XmlReadException {
    while (reader.next()) {
      assertNotNull(reader.kind());
    }
  }

  private static final class FailingXmlStreamWriter implements XMLStreamWriter {
    private static final XMLStreamException FAILURE = new XMLStreamException("boom");

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeStartElement(String namespaceUri, String localName) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceUri)
        throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeEmptyElement(String namespaceUri, String localName) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceUri)
        throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void close() throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void flush() throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeAttribute(String prefix, String namespaceUri, String localName, String value)
        throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeAttribute(String namespaceUri, String localName, String value)
        throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeNamespace(String prefix, String namespaceUri) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeDefaultNamespace(String namespaceUri) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
      throw FAILURE;
    }

    @Override
    public NamespaceContext getNamespaceContext() {
      return null;
    }

    @Override
    public Object getProperty(String name) {
      return null;
    }
  }
}
