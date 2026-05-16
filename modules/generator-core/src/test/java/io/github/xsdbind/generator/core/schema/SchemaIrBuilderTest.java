package io.github.xsdbind.generator.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.xsdbind.generator.core.diagnostics.DiagnosticCode;
import io.github.xsdbind.generator.core.diagnostics.SchemaDiagnostic;
import io.github.xsdbind.generator.core.resolver.SchemaResolutionResult;
import io.github.xsdbind.generator.core.resolver.SchemaResolver;
import io.github.xsdbind.generator.core.resolver.SchemaResolverPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SchemaIrBuilderTest {
  @TempDir private Path tempDirectory;

  @Test
  void buildsIrForGlobalSimpleElementWithBuiltInType() throws IOException {
    write("main.xsd", schema("urn:orders", "<xs:element name=\"title\" type=\"xs:string\"/>"));

    SchemaIrResult result = build("main.xsd");

    assertTrue(result.diagnostics().isEmpty());
    assertEquals("element {urn:orders}title @ main.xsd\n", result.graph().toText());
    assertEquals(
        """
                schema-ir
                  element {urn:orders}title type=xs:string cardinality=1..1
                """,
        result.model().toText());
  }

  @Test
  void buildsIrForComplexTypeSequenceAttributesAndCardinality() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:element name="line" type="tns:Line" minOccurs="0" maxOccurs="unbounded"/>
                  </xs:sequence>
                  <xs:attribute name="version" type="xs:string" use="required"/>
                </xs:complexType>
                <xs:complexType name="Line">
                  <xs:sequence>
                    <xs:element name="sku" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd");

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                schema-ir
                  element {urn:orders}order type={urn:orders}Order cardinality=1..1
                  complexType {urn:orders}Order
                    sequence cardinality=1..1
                      element {urn:orders}id type=xs:string cardinality=1..1
                      element {urn:orders}line type={urn:orders}Line cardinality=0..unbounded
                    attribute {urn:orders}version type=xs:string use=required
                  complexType {urn:orders}Line
                    sequence cardinality=1..1
                      element {urn:orders}sku type=xs:string cardinality=1..1
                """,
        result.model().toText());
  }

  @Test
  void buildsIrForInlineAnonymousComplexType() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order">
                  <xs:complexType>
                    <xs:sequence>
                      <xs:element name="id" type="xs:string"/>
                    </xs:sequence>
                  </xs:complexType>
                </xs:element>
                """));

    SchemaIrResult result = build("main.xsd");

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                schema-ir
                  element {urn:orders}order type=anonymous cardinality=1..1
                    complexType anonymous
                      sequence cardinality=1..1
                        element {urn:orders}id type=xs:string cardinality=1..1
                """,
        result.model().toText());
  }

  @Test
  void resolvesCrossDocumentQNameReferences() throws IOException {
    write(
        "main.xsd",
        """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:tns="urn:orders"
                xmlns:addr="urn:address"
                targetNamespace="urn:orders">
              <xs:import namespace="urn:address" schemaLocation="address.xsd"/>
              <xs:element name="orderAddress" type="addr:Address"/>
            </xs:schema>
            """);
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

    SchemaIrResult result = build("main.xsd");

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                schema-ir
                  element {urn:orders}orderAddress type={urn:address}Address cardinality=1..1
                  complexType {urn:address}Address
                    sequence cardinality=1..1
                      element {urn:address}postalCode type=xs:string cardinality=1..1
                """,
        result.model().toText());
  }

  @Test
  void buildsIrForSimpleTypesAndGlobalAttributes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="OrderCode"/>
                <xs:attribute name="code" type="tns:OrderCode"/>
                """));

    SchemaIrResult result = build("main.xsd");

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                schema-ir
                  simpleType {urn:orders}OrderCode
                  attribute {urn:orders}code type={urn:orders}OrderCode use=optional
                """,
        result.model().toText());
  }

  @Test
  void resolvesElementAndAttributeReferences() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="shared" type="xs:string"/>
                <xs:attribute name="code" type="xs:string"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element ref="tns:shared"/>
                  </xs:sequence>
                  <xs:attribute ref="tns:code"/>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd");

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                schema-ir
                  element {urn:orders}shared type=xs:string cardinality=1..1
                  complexType {urn:orders}Order
                    sequence cardinality=1..1
                      elementRef {urn:orders}shared type={urn:orders}shared cardinality=1..1
                    attributeRef {urn:orders}code type={urn:orders}code use=optional
                  attribute {urn:orders}code type=xs:string use=optional
                """,
        result.model().toText());
  }

  @Test
  void reportsDuplicateGlobalDeclarations() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="xs:string"/>
                <xs:element name="order" type="xs:string"/>
                """));

    SchemaIrResult result = build("main.xsd");

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_DUPLICATE_COMPONENT), diagnosticCodes(result));
    assertEquals("", result.model().toText());
  }

  @Test
  void reportsMissingGlobalNames() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType/>
                <xs:simpleType/>
                <xs:attribute type="xs:string"/>
                """));

    SchemaIrResult result = build("main.xsd");

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_MISSING_NAME,
            DiagnosticCode.SCHEMA_IR_MISSING_NAME,
            DiagnosticCode.SCHEMA_IR_MISSING_NAME),
        diagnosticCodes(result));
  }

  @Test
  void reportsUnresolvedTypeReferences() throws IOException {
    write("main.xsd", schema("urn:orders", "<xs:element name=\"order\" type=\"tns:Missing\"/>"));

    SchemaIrResult result = build("main.xsd");

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE), diagnosticCodes(result));
    assertEquals(
        "SCHEMA_IR_UNRESOLVED_REFERENCE | main.xsd | Unresolved type reference {urn:orders}Missing.",
        result.diagnostics().getFirst().toManifestLine());
  }

  @Test
  void reportsNamespacePrefixFailures() throws IOException {
    write(
        "main.xsd",
        """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" targetNamespace="urn:orders">
              <xs:element name="order" type="missing:Order"/>
            </xs:schema>
            """);

    SchemaIrResult result = build("main.xsd");

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_NAMESPACE_CONFLICT), diagnosticCodes(result));
  }

  @Test
  void reportsUnresolvedElementAndAttributeReferences() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element ref="tns:missingElement"/>
                  </xs:sequence>
                  <xs:attribute ref="tns:missingAttribute"/>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd");

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE,
            DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE),
        diagnosticCodes(result));
  }

  @Test
  void reportsInvalidCardinality() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            "<xs:element name=\"order\" type=\"xs:string\" minOccurs=\"2\" maxOccurs=\"1\"/>"));

    SchemaIrResult result = build("main.xsd");

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY), diagnosticCodes(result));
    assertEquals(
        "SCHEMA_IR_INVALID_CARDINALITY | main.xsd | maxOccurs must be greater than or equal to minOccurs.",
        result.diagnostics().getFirst().toManifestLine());
  }

  @Test
  void reportsInvalidComponentShapes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:sequence/>
                <xs:element name="missingType"/>
                <xs:element name="conflicting" type="xs:string">
                  <xs:complexType/>
                </xs:element>
                <xs:complexType name="BadChild">
                  <xs:simpleType name="Nested"/>
                </xs:complexType>
                <xs:attribute name="missingType"/>
                """));

    SchemaIrResult result = build("main.xsd");

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
        diagnosticCodes(result));
  }

  @Test
  void reportsInvalidMinAndMaxOccurrenceLexemes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="badMin" type="xs:string" minOccurs="many"/>
                <xs:element name="badMax" type="xs:string" maxOccurs="many"/>
                """));

    SchemaIrResult result = build("main.xsd");

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY,
            DiagnosticCode.SCHEMA_IR_INVALID_CARDINALITY),
        diagnosticCodes(result));
  }

  @Test
  void reportsDeterministicDiagnostics() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="first" type="tns:Missing"/>
                <xs:element name="second" type="unknown:Missing"/>
                """));

    SchemaIrResult first = build("main.xsd");
    SchemaIrResult second = build("main.xsd");

    assertEquals(first.diagnostics(), second.diagnostics());
    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_NAMESPACE_CONFLICT,
            DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE),
        diagnosticCodes(first));
  }

  @Test
  void propagatesFrontendDiagnosticsWithoutPartialIr() {
    XsdSyntaxResult syntaxResult =
        new XsdSyntaxResult(
            new XsdSyntaxModel(List.of()),
            List.of(
                new SchemaDiagnostic(
                    DiagnosticCode.SCHEMA_FRONTEND_EXPECTED_SCHEMA_ROOT,
                    "broken.xsd",
                    "Expected xs:schema root but found not-schema.")));

    SchemaIrResult result = new SchemaIrBuilder().build(syntaxResult);

    assertEquals(
        List.of(DiagnosticCode.SCHEMA_FRONTEND_EXPECTED_SCHEMA_ROOT), diagnosticCodes(result));
    assertEquals("", result.graph().toText());
    assertEquals("", result.model().toText());
  }

  private SchemaIrResult build(String primarySchema) {
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve(primarySchema));
    assertTrue(resolution.diagnostics().isEmpty());
    XsdSyntaxResult syntaxResult = new XsdSyntaxParser().parse(resolution.manifest());
    assertTrue(syntaxResult.diagnostics().isEmpty());
    return new SchemaIrBuilder().build(syntaxResult);
  }

  private List<DiagnosticCode> diagnosticCodes(SchemaIrResult result) {
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
