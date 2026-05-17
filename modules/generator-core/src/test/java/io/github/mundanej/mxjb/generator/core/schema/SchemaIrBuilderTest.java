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
  void buildsIrForAcceptedSimpleRestrictionFacets() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="OrderCode">
                  <xs:restriction base="xs:string">
                    <xs:enumeration value="ABC"/>
                    <xs:minLength value="3"/>
                    <xs:maxLength value="8"/>
                    <xs:pattern value="[A-Z0-9]+"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="Priority">
                  <xs:restriction base="xs:int">
                    <xs:minInclusive value="1"/>
                    <xs:maxInclusive value="9"/>
                  </xs:restriction>
                </xs:simpleType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_VALIDATION_10_BASIC);

    assertTrue(result.diagnostics().isEmpty());
    String irText = result.model().toText();
    assertTrue(irText.contains("simpleType {urn:orders}OrderCode restriction base=xs:string"));
    assertTrue(irText.contains("enumeration=ABC"));
    assertTrue(irText.contains("minLength=3 maxLength=8 pattern=[A-Z0-9]+"));
    assertTrue(
        irText.contains(
            "simpleType {urn:orders}Priority restriction base=xs:int minInclusive=1 maxInclusive=9"));
  }

  @Test
  void reportsUnsupportedAndInvalidSimpleRestrictionFacets() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="BadPattern">
                  <xs:restriction base="xs:string">
                    <xs:pattern value="["/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="BadRange">
                  <xs:restriction base="xs:string">
                    <xs:minInclusive value="1"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="ListType">
                  <xs:list itemType="xs:string"/>
                </xs:simpleType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_VALIDATION_10_BASIC);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
        diagnosticCodes(result));
  }

  @Test
  void buildsIrForAllAcceptedSimpleRestrictionFacetCategories() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="Flag">
                  <xs:restriction base="xs:boolean">
                    <xs:enumeration value="true"/>
                    <xs:enumeration value="0"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="FixedCode">
                  <xs:restriction base="xs:string">
                    <xs:length value="5"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="Amount">
                  <xs:restriction base="xs:decimal">
                    <xs:minInclusive value="1.25"/>
                    <xs:maxInclusive value="9.75"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="Count">
                  <xs:restriction base="xs:integer">
                    <xs:enumeration value="7"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="LongCount">
                  <xs:restriction base="xs:long">
                    <xs:enumeration value="9"/>
                  </xs:restriction>
                </xs:simpleType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_VALIDATION_10_BASIC);

    assertTrue(result.diagnostics().isEmpty());
    String irText = result.model().toText();
    assertTrue(irText.contains("simpleType {urn:orders}Flag restriction base=xs:boolean"));
    assertTrue(irText.contains("enumeration=true,0"));
    assertTrue(irText.contains("simpleType {urn:orders}FixedCode restriction base=xs:string"));
    assertTrue(irText.contains("length=5"));
    assertTrue(irText.contains("simpleType {urn:orders}Amount restriction base=xs:decimal"));
    assertTrue(irText.contains("minInclusive=1.25 maxInclusive=9.75"));
    assertTrue(irText.contains("simpleType {urn:orders}Count restriction base=xs:integer"));
    assertTrue(irText.contains("simpleType {urn:orders}LongCount restriction base=xs:long"));
  }

  @Test
  void reportsInvalidSimpleRestrictionDefinitions() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="MissingBase">
                  <xs:restriction>
                    <xs:enumeration value="A"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="UnsupportedBase">
                  <xs:restriction base="xs:date">
                    <xs:enumeration value="2026-05-17"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="InvalidEnum">
                  <xs:restriction base="xs:int">
                    <xs:enumeration value="not-int"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="InvalidLength">
                  <xs:restriction base="xs:string">
                    <xs:length value="-1"/>
                    <xs:length value="2"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="CombinedLength">
                  <xs:restriction base="xs:string">
                    <xs:length value="2"/>
                    <xs:minLength value="1"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="BadLengthRange">
                  <xs:restriction base="xs:string">
                    <xs:minLength value="4"/>
                    <xs:maxLength value="3"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="BadNumericRange">
                  <xs:restriction base="xs:int">
                    <xs:minInclusive value="9"/>
                    <xs:maxInclusive value="1"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="MissingFacetValue">
                  <xs:restriction base="xs:string">
                    <xs:pattern/>
                  </xs:restriction>
                </xs:simpleType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_VALIDATION_10_BASIC);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
        diagnosticCodes(result));
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
  void buildsIrForAcceptedChoiceParticle() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:choice minOccurs="0">
                      <xs:element name="domestic" type="xs:string"/>
                      <xs:element name="international" type="xs:string"/>
                    </xs:choice>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_DATA_10_CHOICE);

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        """
                schema-ir
                  complexType {urn:orders}Order
                    sequence cardinality=1..1
                      element {urn:orders}id type=xs:string cardinality=1..1
                      choice cardinality=0..1
                        element {urn:orders}domestic type=xs:string cardinality=1..1
                        element {urn:orders}international type=xs:string cardinality=1..1
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
    return build(primarySchema, GeneratorProfile.XP_DATA_10);
  }

  private SchemaIrResult build(String primarySchema, GeneratorProfile profile) {
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve(primarySchema));
    assertTrue(resolution.diagnostics().isEmpty());
    XsdSyntaxResult syntaxResult = new XsdSyntaxParser().parse(resolution.manifest(), profile);
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
