package io.github.mundanej.mxjb.conformance.nativeimage;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import io.github.mundanej.mxjb.generator.core.CoreGenerator;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/** Selected conformance checks for the opt-in GraalVM Native Image lane. */
public final class NativeConformanceMain {
  private static final List<UnsupportedFixture> UNSUPPORTED_FIXTURES =
      List.of(
          new UnsupportedFixture(
              "T-CONF-UNSUPPORTED-ANY-ATTRIBUTE",
              "unsupported/any-attribute.xsd",
              List.of("SCHEMA_FRONTEND_UNSUPPORTED_CONSTRUCT")),
          new UnsupportedFixture(
              "T-CONF-UNSUPPORTED-WILDCARD-STRICT",
              "unsupported/wildcard-strict.xsd",
              List.of("SCHEMA_IR_INVALID_COMPONENT")),
          new UnsupportedFixture(
              "T-CONF-UNSUPPORTED-MIXED-CHOICE",
              "unsupported/mixed-choice.xsd",
              List.of("SCHEMA_IR_INVALID_COMPONENT", "SCHEMA_IR_INVALID_COMPONENT")),
          new UnsupportedFixture(
              "T-CONF-UNSUPPORTED-IDENTITY-CONSTRAINT",
              "unsupported/identity-constraint.xsd",
              List.of("SCHEMA_FRONTEND_UNSUPPORTED_CONSTRUCT")));

  private NativeConformanceMain() {}

  public static void main(String[] args)
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    runPurchaseOrderRoundTrip();
    runMultiNamespaceRoundTrip();
    runChoiceRoundTrip();
    runFacetRoundTrip();
    runComposedRoundTrip();
    runSemanticRoundTrip();
    runSubstitutionRoundTrip();
    runDocumentRoundTrip();
    runMixedRoundTrip();
    runUnsupportedDiagnostics();
    runResolverDenial();
    System.out.println(
        "NATIVE_CONFORMANCE status=passed supportedFixtures=9 unsupportedFixtures=4");
  }

  private static void runPurchaseOrderRoundTrip()
      throws XMLStreamException, XmlReadException, XmlWriteException {
    String xml = purchaseOrderXml();
    com.example.purchase.Order order = com.example.purchase.xml.OrderXmlReader.read(readerFor(xml));
    requireValid(
        com.example.purchase.xml.OrderXmlValidator.validate(order),
        "purchase-order object validation");
    String written = writePurchaseOrder(order);
    com.example.purchase.Order reparsed =
        com.example.purchase.xml.OrderXmlReader.read(readerFor(written));
    require(order.equals(reparsed), "purchase-order round trip changed the model");
    requireValid(
        com.example.purchase.xml.OrderXmlValidator.validate(readerFor(written)),
        "purchase-order XML validation");
  }

  private static void runMultiNamespaceRoundTrip()
      throws XMLStreamException, XmlReadException, XmlWriteException {
    String xml = multiNamespaceXml();
    com.example.orders.Order order = com.example.orders.xml.OrderXmlReader.read(readerFor(xml));
    requireValid(
        com.example.orders.xml.OrderXmlValidator.validate(order),
        "multi-namespace object validation");
    String written = writeMultiNamespaceOrder(order);
    com.example.orders.Order reparsed =
        com.example.orders.xml.OrderXmlReader.read(readerFor(written));
    require(order.equals(reparsed), "multi-namespace round trip changed the model");
    requireValid(
        com.example.orders.xml.OrderXmlValidator.validate(readerFor(written)),
        "multi-namespace XML validation");
  }

  private static void runChoiceRoundTrip()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    String xml = resource("/xp-data-10-choice/choice-domestic-valid.xml");
    com.example.nativeconf.choice.Order order =
        com.example.nativeconf.choice.xml.OrderXmlReader.read(readerFor(xml));
    requireValid(
        com.example.nativeconf.choice.xml.OrderXmlValidator.validate(order),
        "choice object validation");
    String written = writeChoiceOrder(order);
    com.example.nativeconf.choice.Order reparsed =
        com.example.nativeconf.choice.xml.OrderXmlReader.read(readerFor(written));
    require(order.equals(reparsed), "choice round trip changed the model");
    requireValid(
        com.example.nativeconf.choice.xml.OrderXmlValidator.validate(readerFor(written)),
        "choice XML validation");
  }

  private static void runFacetRoundTrip()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    String xml = resource("/xp-validation-10-basic/facet-valid.xml");
    com.example.nativeconf.facet.Order order =
        com.example.nativeconf.facet.xml.OrderXmlReader.read(readerFor(xml));
    requireValid(
        com.example.nativeconf.facet.xml.OrderXmlValidator.validate(order),
        "facet object validation");
    String written = writeFacetOrder(order);
    com.example.nativeconf.facet.Order reparsed =
        com.example.nativeconf.facet.xml.OrderXmlReader.read(readerFor(written));
    require(order.equals(reparsed), "facet round trip changed the model");
    requireValid(
        com.example.nativeconf.facet.xml.OrderXmlValidator.validate(readerFor(written)),
        "facet XML validation");
  }

  private static void runComposedRoundTrip()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    String xml = resource("/xp-xsd10-composed/composed-valid.xml");
    com.example.nativeconf.composed.Order order =
        com.example.nativeconf.composed.xml.OrderXmlReader.read(readerFor(xml));
    requireValid(
        com.example.nativeconf.composed.xml.OrderXmlValidator.validate(order),
        "composed object validation");
    String written = writeComposedOrder(order);
    com.example.nativeconf.composed.Order reparsed =
        com.example.nativeconf.composed.xml.OrderXmlReader.read(readerFor(written));
    require(order.equals(reparsed), "composed round trip changed the model");
    requireValid(
        com.example.nativeconf.composed.xml.OrderXmlValidator.validate(readerFor(written)),
        "composed XML validation");
  }

  private static void runSemanticRoundTrip()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    String xml = resource("/xp-xsd10-semantic/semantic-valid.xml");
    com.example.nativeconf.semantic.Order order =
        com.example.nativeconf.semantic.xml.OrderXmlReader.read(readerFor(xml));
    requireValid(
        com.example.nativeconf.semantic.xml.OrderXmlValidator.validate(order),
        "semantic object validation");
    String written = writeSemanticOrder(order);
    com.example.nativeconf.semantic.Order reparsed =
        com.example.nativeconf.semantic.xml.OrderXmlReader.read(readerFor(written));
    require(order.equals(reparsed), "semantic round trip changed the model");
    requireValid(
        com.example.nativeconf.semantic.xml.OrderXmlValidator.validate(readerFor(written)),
        "semantic XML validation");
  }

  private static void runSubstitutionRoundTrip()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    String xml = resource("/xp-xsd10-semantic/substitution-valid.xml");
    com.example.nativeconf.substitution.Order order =
        com.example.nativeconf.substitution.xml.OrderXmlReader.read(readerFor(xml));
    require(order.payment().isPresent(), "substitution fixture did not preserve payment branch");
    requireValid(
        com.example.nativeconf.substitution.xml.OrderXmlValidator.validate(order),
        "substitution object validation");
    String written = writeSubstitutionOrder(order);
    com.example.nativeconf.substitution.Order reparsed =
        com.example.nativeconf.substitution.xml.OrderXmlReader.read(readerFor(written));
    require(order.equals(reparsed), "substitution round trip changed the model");
    requireValid(
        com.example.nativeconf.substitution.xml.OrderXmlValidator.validate(readerFor(written)),
        "substitution XML validation");
  }

  private static void runDocumentRoundTrip()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    String xml = resource("/xp-xsd10-document/document-valid.xml");
    com.example.nativeconf.document.Order order =
        com.example.nativeconf.document.xml.OrderXmlReader.read(readerFor(xml));
    require(!order.wildcardContent().isEmpty(), "document wildcard content was not retained");
    requireValid(
        com.example.nativeconf.document.xml.OrderXmlValidator.validate(order),
        "document object validation");
    String written = writeDocumentOrder(order);
    com.example.nativeconf.document.Order reparsed =
        com.example.nativeconf.document.xml.OrderXmlReader.read(readerFor(written));
    require(order.equals(reparsed), "document round trip changed the model");
    requireValid(
        com.example.nativeconf.document.xml.OrderXmlValidator.validate(readerFor(written)),
        "document XML validation");
  }

  private static void runMixedRoundTrip()
      throws IOException, XMLStreamException, XmlReadException, XmlWriteException {
    String xml = resource("/xp-xsd10-document/mixed-valid.xml");
    com.example.nativeconf.mixed.Order order =
        com.example.nativeconf.mixed.xml.OrderXmlReader.read(readerFor(xml));
    require(order.content().size() >= 5, "mixed content order was not retained");
    requireValid(
        com.example.nativeconf.mixed.xml.OrderXmlValidator.validate(order),
        "mixed object validation");
    String written = writeMixedOrder(order);
    require(
        written.indexOf("before") < written.indexOf("A-100")
            && written.indexOf("A-100") < written.indexOf("between")
            && written.indexOf("between") < written.indexOf("note")
            && written.indexOf("note") < written.indexOf("done")
            && written.indexOf("done") < written.indexOf("after"),
        "mixed writer did not preserve content-list order");
    com.example.nativeconf.mixed.Order reparsed =
        com.example.nativeconf.mixed.xml.OrderXmlReader.read(readerFor(written));
    require(order.equals(reparsed), "mixed round trip changed the model");
    requireValid(
        com.example.nativeconf.mixed.xml.OrderXmlValidator.validate(readerFor(written)),
        "mixed XML validation");
  }

  private static void runUnsupportedDiagnostics() throws IOException {
    Path workDirectory = Files.createTempDirectory("mxjb-native-conformance");
    for (UnsupportedFixture fixture : UNSUPPORTED_FIXTURES) {
      Path schema = copyResource(fixture.schemaResource(), workDirectory.resolve("schemas"));
      GeneratorResult result =
          new CoreGenerator()
              .generate(
                  new GeneratorRequest(
                      List.of(schema),
                      workDirectory.resolve(fixture.id().toLowerCase(Locale.ROOT)),
                      GeneratorProfile.XP_XSD10_DOCUMENT,
                      "com.example.nativeconf.unsupported",
                      Map.of("urn:unsupported", "com.example.nativeconf.unsupported"),
                      List.of(),
                      Map.of()));
      require(!result.successful(), fixture.id() + " unexpectedly generated sources");
      require(result.generatedSources().isEmpty(), fixture.id() + " produced generated sources");
      List<String> actualCodes =
          result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList();
      require(
          fixture.expectedCodes().equals(actualCodes),
          fixture.id() + " diagnostics mismatch: " + actualCodes);
    }
  }

  private static void runResolverDenial() throws XMLStreamException {
    String xml =
        """
        <!DOCTYPE order [<!ENTITY externalId SYSTEM "https://example.invalid/id.txt">]>
        <p:order xmlns:p="urn:purchase">
          <p:id>&externalId;</p:id>
          <p:line><p:sku>SKU-1</p:sku><p:quantity>2</p:quantity></p:line>
        </p:order>
        """;
    ValidationResult result = com.example.purchase.xml.OrderXmlValidator.validate(readerFor(xml));
    require(!result.isValid(), "resolver-denial XML unexpectedly validated");
    require(
        "MXJB-JDKXML-R-001".equals(result.errors().getFirst().code()),
        "resolver-denial diagnostic mismatch: " + result.errors());
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

  private static String writeChoiceOrder(com.example.nativeconf.choice.Order order)
      throws XMLStreamException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    com.example.nativeconf.choice.xml.OrderXmlWriter.write(output, order);
    output.endDocument();
    output.flush();
    streamWriter.close();
    return target.toString();
  }

  private static String writeFacetOrder(com.example.nativeconf.facet.Order order)
      throws XMLStreamException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    com.example.nativeconf.facet.xml.OrderXmlWriter.write(output, order);
    output.endDocument();
    output.flush();
    streamWriter.close();
    return target.toString();
  }

  private static String writeComposedOrder(com.example.nativeconf.composed.Order order)
      throws XMLStreamException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    com.example.nativeconf.composed.xml.OrderXmlWriter.write(output, order);
    output.endDocument();
    output.flush();
    streamWriter.close();
    return target.toString();
  }

  private static String writeSemanticOrder(com.example.nativeconf.semantic.Order order)
      throws XMLStreamException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    com.example.nativeconf.semantic.xml.OrderXmlWriter.write(output, order);
    output.endDocument();
    output.flush();
    streamWriter.close();
    return target.toString();
  }

  private static String writeSubstitutionOrder(com.example.nativeconf.substitution.Order order)
      throws XMLStreamException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    com.example.nativeconf.substitution.xml.OrderXmlWriter.write(output, order);
    output.endDocument();
    output.flush();
    streamWriter.close();
    return target.toString();
  }

  private static String writeDocumentOrder(com.example.nativeconf.document.Order order)
      throws XMLStreamException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    com.example.nativeconf.document.xml.OrderXmlWriter.write(output, order);
    output.endDocument();
    output.flush();
    streamWriter.close();
    return target.toString();
  }

  private static String writeMixedOrder(com.example.nativeconf.mixed.Order order)
      throws XMLStreamException, XmlWriteException {
    StringWriter target = new StringWriter();
    XMLStreamWriter streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(target);
    XmlOutput output = JdkXmlAdapters.output(streamWriter);
    output.startDocument();
    com.example.nativeconf.mixed.xml.OrderXmlWriter.write(output, order);
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

  private static String purchaseOrderXml() {
    return """
        <p:order xmlns:p="urn:purchase" p:version="1.0">
          <p:id>PO-NATIVE</p:id>
          <p:note>Native conformance</p:note>
          <p:line><p:sku>SKU-1</p:sku><p:quantity>2</p:quantity></p:line>
          <p:line><p:sku>SKU-2</p:sku><p:quantity>5</p:quantity></p:line>
        </p:order>
        """;
  }

  private static String multiNamespaceXml() {
    return """
        <o:order xmlns:o="urn:orders" xmlns:l="urn:lines" o:version="1.0">
          <o:id>ORD-NATIVE</o:id>
          <o:note>Native conformance</o:note>
          <l:line><l:sku>SKU-A</l:sku></l:line>
          <l:line><l:sku>SKU-B</l:sku></l:line>
        </o:order>
        """;
  }

  private static String resource(String resourceName) throws IOException {
    try (InputStream input = NativeConformanceMain.class.getResourceAsStream(resourceName)) {
      if (input == null) {
        throw new IllegalArgumentException("Missing conformance resource " + resourceName);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static Path copyResource(String resourceName, Path schemaDirectory) throws IOException {
    Path target = schemaDirectory.resolve(resourceName);
    Files.createDirectories(Objects.requireNonNull(target.getParent()));
    try (InputStream input = NativeConformanceMain.class.getResourceAsStream("/" + resourceName)) {
      if (input == null) {
        throw new IllegalArgumentException("Missing conformance resource " + resourceName);
      }
      Files.copy(input, target);
    }
    return target;
  }

  private static void requireValid(ValidationResult result, String description) {
    require(result.isValid(), description + " failed: " + result.errors());
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }

  private record UnsupportedFixture(String id, String schemaResource, List<String> expectedCodes) {}
}
