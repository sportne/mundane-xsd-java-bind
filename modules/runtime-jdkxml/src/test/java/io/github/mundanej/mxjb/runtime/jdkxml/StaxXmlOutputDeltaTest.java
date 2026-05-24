package io.github.mundanej.mxjb.runtime.jdkxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mxjb.runtime.XmlName;
import io.github.mundanej.mxjb.runtime.XmlOutput;
import io.github.mundanej.mxjb.runtime.XmlQName;
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

final class StaxXmlOutputDeltaTest {
  @Test
  void qNameTextDeclaresNamespaceAndReturnsGeneratedPrefixedLexicalValue()
      throws XMLStreamException, XmlWriteException {
    StringWriter xml = new StringWriter();
    XmlOutput output = outputFor(xml);

    output.startElement(new XmlName("", "root"));
    String lexicalValue = output.qNameText(new XmlQName("urn:values", "code"));
    output.text(lexicalValue);
    output.endElement(new XmlName("", "root"));
    output.flush();

    assertEquals("ns1:code", lexicalValue);
    assertEquals("<root xmlns:ns1=\"urn:values\">ns1:code</root>", xml.toString());
  }

  @Test
  void qNameTextLeavesUnqualifiedQNameUnprefixedAndUndeclared()
      throws XMLStreamException, XmlWriteException {
    StringWriter xml = new StringWriter();
    XmlOutput output = outputFor(xml);

    output.startElement(new XmlName("", "root"));
    String lexicalValue = output.qNameText(new XmlQName("", "local"));
    output.text(lexicalValue);
    output.endElement(new XmlName("", "root"));
    output.flush();

    assertEquals("local", lexicalValue);
    assertEquals("<root>local</root>", xml.toString());
  }

  @Test
  void qNameTextReusesExistingWriterPrefix() throws XMLStreamException, XmlWriteException {
    StringWriter xml = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(xml);
    streamWriter.setPrefix("known", "urn:values");
    XmlOutput output = JdkXmlAdapters.output(streamWriter);

    output.startElement(new XmlName("", "root"));
    String firstValue = output.qNameText(new XmlQName("urn:values", "first"));
    output.text(firstValue);
    String secondValue = output.qNameText(new XmlQName("urn:values", "second"));
    output.text(" " + secondValue);
    output.endElement(new XmlName("", "root"));
    output.flush();

    assertEquals("known:first", firstValue);
    assertEquals("known:second", secondValue);
    assertEquals(
        "<root xmlns:known=\"urn:values\">known:first known:second</root>", xml.toString());
  }

  @Test
  void qNameTextDoesNotUseDefaultNamespaceAsLexicalQNamePrefix()
      throws XMLStreamException, XmlWriteException {
    StringWriter xml = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(xml);
    streamWriter.setDefaultNamespace("urn:values");
    XmlOutput output = JdkXmlAdapters.output(streamWriter);

    output.startElement(new XmlName("", "root"));
    String lexicalValue = output.qNameText(new XmlQName("urn:values", "code"));
    output.text(lexicalValue);
    output.endElement(new XmlName("", "root"));
    output.flush();

    assertNotEquals(":code", lexicalValue);
    assertEquals("ns1:code", lexicalValue);
    assertEquals("<root xmlns:ns1=\"urn:values\">ns1:code</root>", xml.toString());
    assertWellFormed(xml.toString());
  }

  @Test
  void qNameTextWrapsWriterFailureWithDiagnosticAndCause() throws XmlWriteException {
    XMLStreamException failure = new XMLStreamException("namespace declaration failed");
    XmlOutput output = JdkXmlAdapters.output(new FailingQNameNamespaceWriter(failure));

    XmlWriteException exception =
        assertThrows(
            XmlWriteException.class,
            () -> assertNotNull(output.qNameText(new XmlQName("urn:values", "code"))));

    assertEquals("MXJB-JDKXML-W-008", exception.diagnostic().code());
    assertSame(failure, exception.getCause());
  }

  @Test
  void nullNamesAndQNameValuesFailAsPublicNullContract() throws XMLStreamException {
    StringWriter xml = new StringWriter();
    XmlOutput output = outputFor(xml);

    assertThrows(NullPointerException.class, () -> output.startElement(null));
    assertThrows(NullPointerException.class, () -> output.attribute(null, "value"));
    assertThrows(NullPointerException.class, () -> assertNotNull(output.qNameText(null)));
  }

  @Test
  void nullTextAndAttributeValuesExposeDelegateNullBehavior()
      throws XMLStreamException, XmlWriteException {
    StringWriter attributeXml = new StringWriter();
    XmlOutput attributeOutput = outputFor(attributeXml);
    attributeOutput.startElement(new XmlName("", "root"));

    assertThrows(
        NullPointerException.class, () -> attributeOutput.attribute(new XmlName("", "id"), null));

    StringWriter textXml = new StringWriter();
    XmlOutput textOutput = outputFor(textXml);
    textOutput.startElement(new XmlName("", "root"));
    textOutput.text(null);
    textOutput.endElement(new XmlName("", "root"));
    textOutput.flush();

    assertEquals("<root></root>", textXml.toString());
  }

  private static XmlOutput outputFor(StringWriter xml) throws XMLStreamException {
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(xml);
    return JdkXmlAdapters.output(streamWriter);
  }

  private static void assertWellFormed(String xml) throws XMLStreamException {
    XMLStreamReader reader =
        XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(xml));
    while (reader.hasNext()) {
      reader.next();
    }
  }

  private static final class FailingQNameNamespaceWriter implements XMLStreamWriter {
    private final XMLStreamException failure;

    private FailingQNameNamespaceWriter(XMLStreamException failure) {
      this.failure = failure;
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {}

    @Override
    public void writeStartElement(String namespaceUri, String localName)
        throws XMLStreamException {}

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceUri)
        throws XMLStreamException {}

    @Override
    public void writeEmptyElement(String namespaceUri, String localName)
        throws XMLStreamException {}

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceUri)
        throws XMLStreamException {}

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {}

    @Override
    public void writeEndElement() throws XMLStreamException {}

    @Override
    public void writeEndDocument() throws XMLStreamException {}

    @Override
    public void close() throws XMLStreamException {}

    @Override
    public void flush() throws XMLStreamException {}

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {}

    @Override
    public void writeAttribute(String prefix, String namespaceUri, String localName, String value)
        throws XMLStreamException {}

    @Override
    public void writeAttribute(String namespaceUri, String localName, String value)
        throws XMLStreamException {}

    @Override
    public void writeNamespace(String prefix, String namespaceUri) throws XMLStreamException {
      throw failure;
    }

    @Override
    public void writeDefaultNamespace(String namespaceUri) throws XMLStreamException {}

    @Override
    public void writeComment(String data) throws XMLStreamException {}

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {}

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {}

    @Override
    public void writeCData(String data) throws XMLStreamException {}

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {}

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {}

    @Override
    public void writeStartDocument() throws XMLStreamException {}

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {}

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {}

    @Override
    public void writeCharacters(String text) throws XMLStreamException {}

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {}

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
      return null;
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {}

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {}

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {}

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
