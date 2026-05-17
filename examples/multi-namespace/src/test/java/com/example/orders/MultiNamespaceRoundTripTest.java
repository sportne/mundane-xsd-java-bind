package com.example.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.lines.Line;
import com.example.orders.xml.OrderXmlReader;
import com.example.orders.xml.OrderXmlValidator;
import com.example.orders.xml.OrderXmlWriter;
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
import java.util.List;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.Test;

final class MultiNamespaceRoundTripTest {
  @Test
  void xmlFixtureRoundTripsAcrossOrderAndLineNamespaces()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    Order expected =
        new Order(
            Optional.of("1.0"),
            "ORD-2000",
            Optional.of("Cross-namespace order"),
            List.of(new Line("SKU-A"), new Line("SKU-B")));

    Order fromXml = readXml(resource("/xml/order.xml"));
    String writtenXml = writeXml(fromXml);
    Order fromWrittenXml = readXml(writtenXml);

    assertEquals(expected, fromXml);
    assertEquals(expected, fromWrittenXml);
    assertTrue(writtenXml.contains("urn:orders"));
    assertTrue(writtenXml.contains("urn:lines"));
    assertTrue(OrderXmlValidator.validate(fromXml).isValid());
    assertTrue(OrderXmlValidator.validate(readerFor(writtenXml)).isValid());
  }

  @Test
  void objectWithoutOptionalValuesRoundTripsThroughGeneratedWriterAndReader()
      throws XMLStreamException, XmlReadException, XmlWriteException {
    Order order =
        new Order(Optional.empty(), "ORD-3000", Optional.empty(), List.of(new Line("SKU-C")));

    String writtenXml = writeXml(order);
    Order fromWrittenXml = readXml(writtenXml);

    assertEquals(order, fromWrittenXml);
    assertTrue(OrderXmlValidator.validate(order).isValid());
  }

  @Test
  void objectValidatorReportsTooFewRepeatedValues() {
    Order order = new Order(Optional.empty(), "ORD-3000", Optional.empty(), List.of());

    ValidationResult result = OrderXmlValidator.validate(order);

    assertFalse(result.isValid());
    assertEquals("MXJB-GV-002", result.errors().get(0).code());
  }

  @Test
  void generatedXmlValidatorRejectsLineNamespaceMismatch() throws XMLStreamException {
    String xml =
        """
        <o:order xmlns:o="urn:orders" xmlns:l="urn:not-lines">
          <o:id>ORD-2000</o:id>
          <l:line><l:sku>SKU-A</l:sku></l:line>
        </o:order>
        """;

    ValidationResult result = OrderXmlValidator.validate(readerFor(xml));

    assertFalse(result.isValid());
    assertEquals("MXJB-GR-002", result.errors().get(0).code());
  }

  @Test
  void generatedXmlValidatorRejectsEntityReferenceThroughSecureAdapter() throws XMLStreamException {
    String xml =
        """
        <!DOCTYPE order [<!ENTITY externalId SYSTEM "https://example.invalid/id.txt">]>
        <o:order xmlns:o="urn:orders" xmlns:l="urn:lines">
          <o:id>&externalId;</o:id>
          <l:line><l:sku>SKU-A</l:sku></l:line>
        </o:order>
        """;

    ValidationResult result = OrderXmlValidator.validate(readerFor(xml));

    assertFalse(result.isValid());
    assertEquals("MXJB-JDKXML-R-001", result.errors().get(0).code());
  }

  private static Order readXml(String xml) throws XMLStreamException, XmlReadException {
    return OrderXmlReader.read(readerFor(xml));
  }

  private static String writeXml(Order order) throws XMLStreamException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    OrderXmlWriter.write(output, order);
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
    try (InputStream input = MultiNamespaceRoundTripTest.class.getResourceAsStream(resourceName)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
