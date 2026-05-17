package io.github.mundanej.mxjb.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.runtime.ValidationResult;
import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.XmlOutput;
import io.github.mundanej.mxjb.runtime.XmlReadException;
import io.github.mundanej.mxjb.runtime.XmlWriteException;
import io.github.mundanej.mxjb.runtime.jdkxml.JdkXmlAdapters;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.Test;

final class XpData10ConformanceTest {
  @Test
  void purchaseOrderFixtureRoundTripsThroughGeneratedBindings()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    com.example.purchase.Order order =
        com.example.purchase.xml.OrderXmlReader.read(
            readerFor(resource("/xp-data-10/purchase-order-valid.xml")));

    String xml = writePurchaseOrder(order);
    com.example.purchase.Order roundTripped =
        com.example.purchase.xml.OrderXmlReader.read(readerFor(xml));

    assertEquals(order, roundTripped);
    assertTrue(com.example.purchase.xml.OrderXmlValidator.validate(order).isValid());
  }

  @Test
  void multiNamespaceFixtureRoundTripsThroughGeneratedBindings()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    com.example.orders.Order order =
        com.example.orders.xml.OrderXmlReader.read(
            readerFor(resource("/xp-data-10/multi-namespace-valid.xml")));

    String xml = writeMultiNamespaceOrder(order);
    com.example.orders.Order roundTripped =
        com.example.orders.xml.OrderXmlReader.read(readerFor(xml));

    assertEquals(order, roundTripped);
    assertTrue(xml.contains("urn:orders"));
    assertTrue(xml.contains("urn:lines"));
    assertTrue(com.example.orders.xml.OrderXmlValidator.validate(order).isValid());
  }

  @Test
  void missingRequiredElementProducesReaderDiagnostic() throws IOException, XMLStreamException {
    ValidationResult result =
        com.example.purchase.xml.OrderXmlValidator.validate(
            readerFor(resource("/xp-data-10/purchase-order-missing-id.xml")));

    assertFalse(result.isValid());
    assertEquals("MXJB-GR-004", result.errors().get(0).code());
  }

  @Test
  void outOfOrderSequenceProducesReaderDiagnostic() throws IOException, XMLStreamException {
    ValidationResult result =
        com.example.orders.xml.OrderXmlValidator.validate(
            readerFor(resource("/xp-data-10/multi-namespace-out-of-order.xml")));

    assertFalse(result.isValid());
    assertEquals("MXJB-GR-002", result.errors().get(0).code());
  }

  @Test
  void namespaceMismatchProducesReaderDiagnostic() throws IOException, XMLStreamException {
    ValidationResult result =
        com.example.orders.xml.OrderXmlValidator.validate(
            readerFor(resource("/xp-data-10/multi-namespace-wrong-line-namespace.xml")));

    assertFalse(result.isValid());
    assertEquals("MXJB-GR-002", result.errors().get(0).code());
  }

  @Test
  void invalidScalarLexicalValueProducesReaderDiagnostic() throws IOException, XMLStreamException {
    ValidationResult result =
        com.example.purchase.xml.OrderXmlValidator.validate(
            readerFor(resource("/xp-data-10/purchase-order-invalid-int.xml")));

    assertFalse(result.isValid());
    assertEquals("MXJB-GR-006", result.errors().get(0).code());
  }

  private static String writePurchaseOrder(com.example.purchase.Order order)
      throws XMLStreamException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    com.example.purchase.xml.OrderXmlWriter.write(output, order);
    output.endDocument();
    output.flush();
    streamWriter.close();
    return target.toString();
  }

  private static String writeMultiNamespaceOrder(com.example.orders.Order order)
      throws XMLStreamException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    com.example.orders.xml.OrderXmlWriter.write(output, order);
    output.endDocument();
    output.flush();
    streamWriter.close();
    return target.toString();
  }

  private static XmlEventReader readerFor(String xml) throws XMLStreamException {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();
    XMLStreamReader streamReader = factory.createXMLStreamReader(new StringReader(xml));
    return JdkXmlAdapters.eventReader(streamReader);
  }

  private static String resource(String resourceName) throws IOException {
    try (InputStream input = XpData10ConformanceTest.class.getResourceAsStream(resourceName)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
