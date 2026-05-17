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
  void reportsRestrictionAsUnsupportedProfileByDefault() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="OrderCode">
                  <xs:restriction base="xs:string">
                    <xs:pattern value="[A-Z]+"/>
                  </xs:restriction>
                </xs:simpleType>
                """));

    XsdSyntaxResult result = parseWithLocalResolver("main.xsd");

    assertEquals(
        List.of(DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE), diagnosticCodes(result));
    assertEquals(
        "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE | main.xsd | xs:restriction requires profile XP-VALIDATION-10-BASIC.",
        result.diagnostics().getFirst().toManifestLine());
  }

  @Test
  void parsesSimpleRestrictionWhenBasicValidationProfileIsSelected() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="OrderCode">
                  <xs:restriction base="xs:string">
                    <xs:minLength value="3"/>
                    <xs:maxLength value="8"/>
                    <xs:pattern value="[A-Z0-9]+"/>
                  </xs:restriction>
                </xs:simpleType>
                """));
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve("main.xsd"));

    XsdSyntaxResult result =
        new XsdSyntaxParser().parse(resolution.manifest(), GeneratorProfile.XP_VALIDATION_10_BASIC);

    assertTrue(result.diagnostics().isEmpty());
    assertTrue(result.model().toText().contains("restriction base=xs:string"));
    assertTrue(result.model().toText().contains("pattern value=[A-Z0-9]+"));
  }

  @Test
  void reportsGroupAndAttributeGroupAsUnsupportedProfileByDefault() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:group name="OrderFields">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                  </xs:sequence>
                </xs:group>
                <xs:attributeGroup name="OrderAttributes">
                  <xs:attribute name="version" type="xs:string"/>
                </xs:attributeGroup>
                """));

    XsdSyntaxResult result = parseWithLocalResolver("main.xsd");

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
            DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE),
        diagnosticCodes(result));
    assertEquals(
        "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE | main.xsd | xs:group requires profile XP-XSD10-COMPOSED.",
        result.diagnostics().get(0).toManifestLine());
    assertEquals(
        "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE | main.xsd | xs:attributeGroup requires profile XP-XSD10-COMPOSED.",
        result.diagnostics().get(1).toManifestLine());
  }

  @Test
  void reportsListAndUnionAsUnsupportedOutsideComposedProfile() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="Codes">
                  <xs:list itemType="xs:string"/>
                </xs:simpleType>
                <xs:simpleType name="CodeOrPriority">
                  <xs:union memberTypes="xs:string xs:int"/>
                </xs:simpleType>
                """));

    XsdSyntaxResult result =
        new XsdSyntaxParser()
            .parse(
                new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)))
                    .resolve(tempDirectory.resolve("main.xsd"))
                    .manifest(),
                GeneratorProfile.XP_VALIDATION_10_BASIC);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
            DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE),
        diagnosticCodes(result));
    assertEquals(
        "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE | main.xsd | xs:list requires profile XP-XSD10-COMPOSED.",
        result.diagnostics().get(0).toManifestLine());
    assertEquals(
        "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE | main.xsd | xs:union requires profile XP-XSD10-COMPOSED.",
        result.diagnostics().get(1).toManifestLine());
  }

  @Test
  void reportsDerivationAsUnsupportedOutsideComposedProfile() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="BaseCode">
                  <xs:restriction base="xs:string">
                    <xs:minLength value="3"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="DerivedCode">
                  <xs:restriction base="tns:BaseCode">
                    <xs:maxLength value="8"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:complexType name="BaseOrder">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
                <xs:complexType name="Order">
                  <xs:complexContent>
                    <xs:extension base="tns:BaseOrder">
                      <xs:sequence>
                        <xs:element name="total" type="xs:decimal"/>
                      </xs:sequence>
                    </xs:extension>
                  </xs:complexContent>
                </xs:complexType>
                """));

    XsdSyntaxResult result =
        new XsdSyntaxParser()
            .parse(
                new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)))
                    .resolve(tempDirectory.resolve("main.xsd"))
                    .manifest(),
                GeneratorProfile.XP_VALIDATION_10_BASIC);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE,
            DiagnosticCode.SCHEMA_FRONTEND_UNSUPPORTED_PROFILE),
        diagnosticCodes(result));
    assertEquals(
        "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE | main.xsd | "
            + "xs:restriction derivation chains require profile XP-XSD10-COMPOSED.",
        result.diagnostics().get(0).toManifestLine());
    assertEquals(
        "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE | main.xsd | xs:complexContent requires profile XP-XSD10-COMPOSED.",
        result.diagnostics().get(1).toManifestLine());
  }

  @Test
  void composedProfileParsesGroupsChoicesAndRestrictions() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="Code">
                  <xs:restriction base="xs:string">
                    <xs:minLength value="3"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="Codes">
                  <xs:list itemType="tns:Code"/>
                </xs:simpleType>
                <xs:simpleType name="CodeOrPriority">
                  <xs:union memberTypes="tns:Code xs:int"/>
                </xs:simpleType>
                <xs:group name="OrderFields">
                  <xs:sequence>
                    <xs:element name="id" type="tns:Code"/>
                    <xs:choice minOccurs="0">
                      <xs:element name="domestic" type="xs:string"/>
                    </xs:choice>
                  </xs:sequence>
                </xs:group>
                <xs:attributeGroup name="OrderAttributes">
                  <xs:attribute name="version" type="xs:string"/>
                </xs:attributeGroup>
                <xs:complexType name="BaseOrder">
                  <xs:sequence>
                    <xs:element name="id" type="tns:Code"/>
                  </xs:sequence>
                </xs:complexType>
                <xs:complexType name="Order">
                  <xs:complexContent>
                    <xs:extension base="tns:BaseOrder">
                      <xs:sequence>
                        <xs:element name="total" type="xs:decimal"/>
                      </xs:sequence>
                    </xs:extension>
                  </xs:complexContent>
                </xs:complexType>
                """));
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve("main.xsd"));

    XsdSyntaxResult result =
        new XsdSyntaxParser().parse(resolution.manifest(), GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(result.diagnostics().isEmpty());
    String syntaxText = result.model().toText();
    assertTrue(syntaxText.contains("group name=OrderFields minOccurs=1 maxOccurs=1"), syntaxText);
    assertTrue(syntaxText.contains("choice minOccurs=0 maxOccurs=1"), syntaxText);
    assertTrue(syntaxText.contains("attributeGroup name=OrderAttributes"), syntaxText);
    assertTrue(syntaxText.contains("restriction base=xs:string"), syntaxText);
    assertTrue(syntaxText.contains("list itemType=tns:Code"), syntaxText);
    assertTrue(syntaxText.contains("union memberTypes=tns:Code xs:int"), syntaxText);
    assertTrue(syntaxText.contains("complexContent"), syntaxText);
    assertTrue(syntaxText.contains("extension base=tns:BaseOrder"), syntaxText);
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
