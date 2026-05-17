package com.example.purchase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.purchase.xml.OrderXmlReader;
import com.example.purchase.xml.OrderXmlValidator;
import com.example.purchase.xml.OrderXmlWriter;
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

final class PurchaseOrderRoundTripTest {
  @Test
  void xmlFixtureRoundTripsThroughGeneratedReaderAndWriter()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    Order expected =
        new Order(
            Optional.of("1.0"),
            "PO-1000",
            Optional.of("Expedite"),
            List.of(new Line("SKU-1", 2), new Line("SKU-2", 5)));

    Order fromXml = readXml(resource("/xml/purchase-order.xml"));
    String writtenXml = writeXml(fromXml);
    Order fromWrittenXml = readXml(writtenXml);

    assertEquals(expected, fromXml);
    assertEquals(expected, fromWrittenXml);
    assertTrue(OrderXmlValidator.validate(fromXml).isValid());
    assertTrue(OrderXmlValidator.validate(readerFor(writtenXml)).isValid());
  }

  @Test
  void objectWithoutOptionalValuesRoundTripsThroughGeneratedWriterAndReader()
      throws XMLStreamException, XmlReadException, XmlWriteException {
    Order order =
        new Order(Optional.empty(), "PO-2000", Optional.empty(), List.of(new Line("SKU-3", 1)));

    String writtenXml = writeXml(order);
    Order fromWrittenXml = readXml(writtenXml);

    assertEquals(order, fromWrittenXml);
    assertTrue(OrderXmlValidator.validate(order).isValid());
  }

  @Test
  void objectValidatorReportsTooFewRepeatedValues() {
    Order order = new Order(Optional.empty(), "PO-2000", Optional.empty(), List.of());

    ValidationResult result = OrderXmlValidator.validate(order);

    assertFalse(result.isValid());
    assertEquals("MXJB-GV-002", result.errors().get(0).code());
  }

  @Test
  void generatedXmlValidatorPreservesMissingRequiredReaderDiagnostic() throws XMLStreamException {
    String xml =
        """
        <p:order xmlns:p="urn:purchase">
          <p:line><p:sku>SKU-1</p:sku><p:quantity>2</p:quantity></p:line>
        </p:order>
        """;

    ValidationResult result = OrderXmlValidator.validate(readerFor(xml));

    assertFalse(result.isValid());
    assertEquals("MXJB-GR-004", result.errors().get(0).code());
  }

  @Test
  void generatedXmlValidatorPreservesInvalidScalarReaderDiagnostic() throws XMLStreamException {
    String xml =
        """
        <p:order xmlns:p="urn:purchase">
          <p:id>PO-1000</p:id>
          <p:line><p:sku>SKU-1</p:sku><p:quantity>two</p:quantity></p:line>
        </p:order>
        """;

    ValidationResult result = OrderXmlValidator.validate(readerFor(xml));

    assertFalse(result.isValid());
    assertEquals("MXJB-GR-006", result.errors().get(0).code());
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
    try (InputStream input = PurchaseOrderRoundTripTest.class.getResourceAsStream(resourceName)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
