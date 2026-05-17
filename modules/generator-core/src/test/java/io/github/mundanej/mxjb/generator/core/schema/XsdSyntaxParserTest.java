package io.github.mundanej.mxjb.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.resolver.SchemaResolutionResult;
import io.github.mundanej.mxjb.generator.core.resolver.SchemaResolver;
import io.github.mundanej.mxjb.generator.core.resolver.SchemaResolverPolicy;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class XsdSyntaxParserTest {
  @TempDir private Path tempDirectory;

  @Test
  void parsesSimpleGlobalElementSyntax() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="purchaseOrder" type="tns:PurchaseOrder"/>
                """));

    XsdSyntaxResult result = parseWithLocalResolver("main.xsd");

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                document main.xsd namespace=urn:orders
                  namespace xmlns:tns=urn:orders
                  namespace xmlns:xs=http://www.w3.org/2001/XMLSchema
                  element name=purchaseOrder type=tns:PurchaseOrder minOccurs=1 maxOccurs=1
                """,
        result.model().toText());
  }

  @Test
  void parsesComplexTypeSequenceAttributesAndCardinality() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="PurchaseOrder">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:element name="line" type="tns:Line" minOccurs="0" maxOccurs="unbounded"/>
                  </xs:sequence>
                  <xs:attribute name="version" type="xs:string" use="required"/>
                </xs:complexType>
                """));

    XsdSyntaxResult result = parseWithLocalResolver("main.xsd");

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                document main.xsd namespace=urn:orders
                  namespace xmlns:tns=urn:orders
                  namespace xmlns:xs=http://www.w3.org/2001/XMLSchema
                  complexType name=PurchaseOrder
                    sequence minOccurs=1 maxOccurs=1
                      element name=id type=xs:string minOccurs=1 maxOccurs=1
                      element name=line type=tns:Line minOccurs=0 maxOccurs=unbounded
                    attribute name=version type=xs:string use=required
                """,
        result.model().toText());
  }

  @Test
  void parsesMultipleResolverApprovedDocumentsWithoutParsingImportsAsSyntax() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:import namespace="urn:address" schemaLocation="address.xsd"/>
                <xs:element name="order" type="tns:Order"/>
                """));
    write(
        "address.xsd",
        schema(
            "urn:address",
            """
                <xs:complexType name="Address">
                  <xs:sequence>
                    <xs:element name="postalCode" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    XsdSyntaxResult result = parseWithLocalResolver("main.xsd");

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                document main.xsd namespace=urn:orders
                  namespace xmlns:tns=urn:orders
                  namespace xmlns:xs=http://www.w3.org/2001/XMLSchema
                  element name=order type=tns:Order minOccurs=1 maxOccurs=1

                document address.xsd namespace=urn:address
                  namespace xmlns:tns=urn:address
                  namespace xmlns:xs=http://www.w3.org/2001/XMLSchema
                  complexType name=Address
                    sequence minOccurs=1 maxOccurs=1
                      element name=postalCode type=xs:string minOccurs=1 maxOccurs=1
                """,
        result.model().toText());
  }

  @Test
  void parsesHttpCatalogImportsThroughResolverApprovedLocalFiles() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:import namespace="urn:address" schemaLocation="https://schemas.example.test/address.xsd"/>
                <xs:element name="order" type="tns:Order"/>
                """));
    write(
        "address.xsd",
        schema(
            "urn:address",
            """
                <xs:element name="address" type="xs:string"/>
                """));
    SchemaResolver resolver =
        new SchemaResolver(
            SchemaResolverPolicy.withCatalog(
                List.of(tempDirectory),
                Map.of(
                    URI.create("https://schemas.example.test/address.xsd"),
                    tempDirectory.resolve("address.xsd"))));

    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve("main.xsd"));
    XsdSyntaxResult result = new XsdSyntaxParser().parse(resolution.manifest());

    assertTrue(resolution.diagnostics().isEmpty());
    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                document main.xsd namespace=urn:orders
                  namespace xmlns:tns=urn:orders
                  namespace xmlns:xs=http://www.w3.org/2001/XMLSchema
                  element name=order type=tns:Order minOccurs=1 maxOccurs=1

                document address.xsd namespace=urn:address
                  namespace xmlns:tns=urn:address
                  namespace xmlns:xs=http://www.w3.org/2001/XMLSchema
                  element name=address type=xs:string minOccurs=1 maxOccurs=1
                """,
        result.model().toText());
  }

  @Test
  void reportsChoiceAsUnsupportedProfile() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:choice>
                    <xs:element name="domestic" type="xs:string"/>
                  </xs:choice>
                </xs:complexType>
                """));

    XsdSyntaxResult result = parseWithLocalResolver("main.xsd");

    assertEquals(
        List.of(DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE), diagnosticCodes(result));
    assertEquals(
        "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE | main.xsd | xs:choice requires profile XP-DATA-10-CHOICE.",
        result.diagnostics().getFirst().toManifestLine());
  }

  @Test
  void parsesChoiceWhenChoiceProfileIsSelected() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:choice minOccurs="0">
                      <xs:element name="domestic" type="xs:string"/>
                      <xs:element name="international" type="xs:string"/>
                    </xs:choice>
                  </xs:sequence>
                </xs:complexType>
                """));
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve("main.xsd"));

    XsdSyntaxResult result =
        new XsdSyntaxParser().parse(resolution.manifest(), GeneratorProfile.XP_DATA_10_CHOICE);

    assertTrue(result.diagnostics().isEmpty());
    assertTrue(result.model().toText().contains("choice minOccurs=0 maxOccurs=1"));
  }

  @Test
  void reportsFutureProfileConstructsAsUnsupported() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:any namespace="##other"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    XsdSyntaxResult result = parseWithLocalResolver("main.xsd");

    assertEquals(
        List.of(DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_CONSTRUCT), diagnosticCodes(result));
    assertEquals(
        "SCHEMA_FRONTEND_UNSUPPORTED_CONSTRUCT | main.xsd | Unsupported XSD construct xs:any for profile XP-DATA-10.",
        result.diagnostics().getFirst().toManifestLine());
  }

  @Test
  void reportsNonSchemaDocumentRoot() throws IOException {
    write("main.xsd", "<not-schema/>");

    XsdSyntaxResult result = parseWithLocalResolver("main.xsd");

    assertEquals(
        List.of(DiagnosticCode.SCHEMA_FRONTEND_EXPECTED_SCHEMA_ROOT), diagnosticCodes(result));
    assertEquals(
        "SCHEMA_FRONTEND_EXPECTED_SCHEMA_ROOT | main.xsd | Expected xs:schema root but found not-schema.",
        result.diagnostics().getFirst().toManifestLine());
    assertEquals("", result.model().toText());
  }

  private XsdSyntaxResult parseWithLocalResolver(String primarySchema) {
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve(primarySchema));
    assertTrue(resolution.diagnostics().isEmpty());
    return new XsdSyntaxParser().parse(resolution.manifest());
  }

  private List<DiagnosticCode> diagnosticCodes(XsdSyntaxResult result) {
    return result.diagnostics().stream().map(SchemaDiagnostic::code).toList();
  }

  private void write(String fileName, String contents) throws IOException {
    Files.writeString(tempDirectory.resolve(fileName), contents);
  }

  private String schema(String namespace, String body) {
    return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
        + "xmlns:tns=\""
        + namespace
        + "\" targetNamespace=\""
        + namespace
        + "\">\n"
        + body
        + "\n</xs:schema>\n";
  }
}
