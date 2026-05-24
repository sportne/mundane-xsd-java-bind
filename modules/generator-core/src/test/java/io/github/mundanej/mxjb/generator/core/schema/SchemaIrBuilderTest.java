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
                    attribute version type=xs:string use=required
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
                    <xs:minExclusive value="0"/>
                    <xs:maxExclusive value="10"/>
                    <xs:totalDigits value="1"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="Tokenized">
                  <xs:restriction base="xs:token">
                    <xs:whiteSpace value="collapse"/>
                    <xs:enumeration value="READY"/>
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
            "simpleType {urn:orders}Priority restriction base=xs:int minExclusive=0 maxExclusive=10 totalDigits=1"));
    assertTrue(irText.contains("whiteSpace=collapse"));
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
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_VALIDATION_10_BASIC);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT, DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
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
  void buildsIrForAcceptedListAndUnionSimpleTypes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="Quantity">
                  <xs:restriction base="xs:int">
                    <xs:minInclusive value="1"/>
                    <xs:maxInclusive value="9"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="QuantityList">
                  <xs:list itemType="tns:Quantity"/>
                </xs:simpleType>
                <xs:simpleType name="QuantityOrCode">
                  <xs:union memberTypes="tns:Quantity xs:string"/>
                </xs:simpleType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(result.diagnostics().isEmpty());
    String irText = result.model().toText();
    assertTrue(
        irText.contains("simpleType {urn:orders}QuantityList list itemType={urn:orders}Quantity"));
    assertTrue(
        irText.contains(
            "simpleType {urn:orders}QuantityOrCode union memberTypes={urn:orders}Quantity,xs:string"));
  }

  @Test
  void reportsUnsupportedListAndUnionMemberShapes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="NestedList">
                  <xs:list itemType="tns:OtherList"/>
                </xs:simpleType>
                <xs:simpleType name="OtherList">
                  <xs:list itemType="xs:string"/>
                </xs:simpleType>
                <xs:simpleType name="BadUnion">
                  <xs:union memberTypes="xs:anyType tns:OtherList"/>
                </xs:simpleType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
        diagnosticCodes(result));
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
                  <xs:restriction base="xs:anyType">
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
  void buildsIrForFlattenedModelGroupAndAttributeGroupRefs() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:group name="OrderFields">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:choice minOccurs="0">
                      <xs:element name="domestic" type="xs:string"/>
                      <xs:element name="international" type="xs:string"/>
                    </xs:choice>
                  </xs:sequence>
                </xs:group>
                <xs:attributeGroup name="OrderAttributes">
                  <xs:attribute name="version" type="xs:string" use="required"/>
                </xs:attributeGroup>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:group ref="tns:OrderFields"/>
                    <xs:element name="total" type="xs:decimal"/>
                  </xs:sequence>
                  <xs:attributeGroup ref="tns:OrderAttributes"/>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(result.diagnostics().isEmpty());
    String irText = result.model().toText();
    assertTrue(irText.contains("modelGroup {urn:orders}OrderFields"), irText);
    assertTrue(irText.contains("attributeGroup {urn:orders}OrderAttributes"), irText);
    assertTrue(irText.contains("element {urn:orders}id type=xs:string cardinality=1..1"), irText);
    assertTrue(irText.contains("choice cardinality=0..1"), irText);
    assertTrue(
        irText.contains("element {urn:orders}total type=xs:decimal cardinality=1..1"), irText);
    assertTrue(irText.contains("attribute version type=xs:string use=required"), irText);
  }

  @Test
  void reportsUnsupportedGroupAndAttributeGroupShapes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:group name="Nested">
                  <xs:sequence>
                    <xs:group ref="tns:Missing"/>
                  </xs:sequence>
                </xs:group>
                <xs:group name="Repeated">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                  </xs:sequence>
                </xs:group>
                <xs:attributeGroup name="Attrs">
                  <xs:attributeGroup ref="tns:OtherAttrs"/>
                </xs:attributeGroup>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:group ref="tns:Repeated" minOccurs="0"/>
                  </xs:sequence>
                  <xs:attributeGroup ref="tns:MissingAttrs"/>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE,
            DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE),
        diagnosticCodes(result));
  }

  @Test
  void buildsIrForLegalAllNestedSingletonSequenceAndRepeatedChoice() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="AllOrder">
                  <xs:all minOccurs="0">
                    <xs:element name="id" type="xs:string" minOccurs="0"/>
                    <xs:element name="note" type="xs:string" minOccurs="0"/>
                  </xs:all>
                </xs:complexType>
                <xs:complexType name="NestedOrder">
                  <xs:sequence>
                    <xs:sequence minOccurs="0" maxOccurs="3">
                      <xs:element name="line" type="xs:string"/>
                    </xs:sequence>
                    <xs:choice minOccurs="0" maxOccurs="unbounded">
                      <xs:element name="domestic" type="xs:string"/>
                      <xs:element name="international" type="xs:string"/>
                    </xs:choice>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String irText = result.model().toText();
    assertTrue(irText.contains("all cardinality=0..1"), irText);
    assertTrue(irText.contains("element {urn:orders}id type=xs:string cardinality=0..1"), irText);
    assertTrue(irText.contains("element {urn:orders}line type=xs:string cardinality=0..3"), irText);
    assertTrue(irText.contains("choice cardinality=0..unbounded"), irText);
  }

  @Test
  void buildsIrForRepeatedNestedSequenceAsGroupedContent() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:sequence minOccurs="0" maxOccurs="unbounded">
                      <xs:element name="id" type="xs:string"/>
                      <xs:element name="line" type="xs:string"/>
                    </xs:sequence>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String irText = result.model().toText();
    assertTrue(irText.contains("sequenceGroup cardinality=0..unbounded"), irText);
    assertTrue(irText.contains("element {urn:orders}id type=xs:string cardinality=1..1"), irText);
    assertTrue(irText.contains("element {urn:orders}line type=xs:string cardinality=1..1"), irText);
  }

  @Test
  void buildsIrForRepeatedNestedSequenceWithOptionalChildrenAsGroupedContent() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:sequence minOccurs="0" maxOccurs="unbounded">
                      <xs:element name="id" type="xs:string" minOccurs="0"/>
                      <xs:element name="line" type="xs:string"/>
                    </xs:sequence>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String irText = result.model().toText();
    assertTrue(irText.contains("sequenceGroup cardinality=0..unbounded"), irText);
    assertTrue(irText.contains("element {urn:orders}id type=xs:string cardinality=0..1"), irText);
    assertTrue(irText.contains("element {urn:orders}line type=xs:string cardinality=1..1"), irText);
  }

  @Test
  void rejectsInvalidAllCardinalityChildrenAndEmptyAll() throws IOException {
    write(
        "bad-cardinality.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:all maxOccurs="2">
                    <xs:element name="id" type="xs:string"/>
                  </xs:all>
                </xs:complexType>
                """));
    write(
        "bad-child.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:all>
                    <xs:element name="id" type="xs:string" maxOccurs="2"/>
                    <xs:choice>
                      <xs:element name="note" type="xs:string"/>
                    </xs:choice>
                  </xs:all>
                </xs:complexType>
                """));
    write(
        "optional-required.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:all minOccurs="0">
                    <xs:element name="id" type="xs:string"/>
                  </xs:all>
                </xs:complexType>
                """));
    write(
        "empty.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:all/>
                </xs:complexType>
                """));

    assertEquals(
        List.of(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
        diagnosticCodes(build("bad-cardinality.xsd", GeneratorProfile.XP_XSD10_COMPOSED)));
    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
        diagnosticCodes(build("bad-child.xsd", GeneratorProfile.XP_XSD10_COMPOSED)));
    SchemaIrResult optionalRequired =
        build("optional-required.xsd", GeneratorProfile.XP_XSD10_COMPOSED);
    assertTrue(optionalRequired.diagnostics().isEmpty(), optionalRequired.diagnostics().toString());
    assertTrue(optionalRequired.model().toText().contains("all cardinality=0..1"));
    assertEquals(
        List.of(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
        diagnosticCodes(build("empty.xsd", GeneratorProfile.XP_XSD10_COMPOSED)));
  }

  @Test
  void reportsDuplicateFlattenedElementAndAttributeNames() throws IOException {
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
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:group ref="tns:OrderFields"/>
                    <xs:element name="id" type="xs:string"/>
                  </xs:sequence>
                  <xs:attributeGroup ref="tns:OrderAttributes"/>
                  <xs:attribute name="version" type="xs:string"/>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT, DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
        diagnosticCodes(result));
    assertTrue(result.diagnostics().get(0).message().contains("Duplicate flattened XML attribute"));
    assertTrue(result.diagnostics().get(1).message().contains("Duplicate flattened XML element"));
  }

  @Test
  void buildsIrForComplexExtensionAndSimpleRestrictionDerivationChains() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="OrderCode">
                  <xs:restriction base="xs:string">
                    <xs:minLength value="3"/>
                    <xs:maxLength value="8"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="DomesticOrderCode">
                  <xs:restriction base="tns:OrderCode">
                    <xs:pattern value="[A-Z0-9]+"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:complexType name="BaseOrder">
                  <xs:sequence>
                    <xs:element name="id" type="tns:DomesticOrderCode"/>
                  </xs:sequence>
                  <xs:attribute name="version" type="xs:string" use="required"/>
                </xs:complexType>
                <xs:complexType name="Order">
                  <xs:complexContent>
                    <xs:extension base="tns:BaseOrder">
                      <xs:sequence>
                        <xs:element name="total" type="xs:decimal"/>
                      </xs:sequence>
                      <xs:attribute name="region" type="xs:string" use="required"/>
                    </xs:extension>
                  </xs:complexContent>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String irText = result.model().toText();
    assertTrue(
        irText.contains(
            "simpleType {urn:orders}DomesticOrderCode restriction base=xs:string "
                + "minLength=3 maxLength=8 pattern=[A-Z0-9]+"),
        irText);
    assertTrue(irText.contains("complexType {urn:orders}Order"), irText);
    assertTrue(
        irText.indexOf("element {urn:orders}id") < irText.indexOf("element {urn:orders}total"));
    assertTrue(irText.indexOf("attribute version") < irText.indexOf("attribute region"));
  }

  @Test
  void reportsUnsupportedDerivationShapes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="Codes">
                  <xs:list itemType="xs:string"/>
                </xs:simpleType>
                <xs:simpleType name="BadCode">
                  <xs:restriction base="tns:Codes">
                    <xs:minLength value="3"/>
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
                        <xs:element name="id" type="xs:string"/>
                      </xs:sequence>
                    </xs:extension>
                  </xs:complexContent>
                </xs:complexType>
                <xs:complexType name="SimpleContentOrder">
                  <xs:simpleContent>
                    <xs:extension base="xs:string"/>
                  </xs:simpleContent>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT, DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
        diagnosticCodes(result));
  }

  @Test
  void reportsInvalidDerivationDetails() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="BaseCode">
                  <xs:restriction base="xs:string">
                    <xs:length value="4"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="BadCode">
                  <xs:restriction base="tns:BaseCode">
                    <xs:length value="5"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:complexType name="AbstractOrder" abstract="true"/>
                <xs:complexType name="MissingBaseOrder">
                  <xs:complexContent>
                    <xs:extension/>
                  </xs:complexContent>
                </xs:complexType>
                <xs:complexType name="RestrictedOrder">
                  <xs:complexContent>
                    <xs:restriction base="tns:AbstractOrder"/>
                  </xs:complexContent>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(
        diagnosticCodes(result).stream()
            .allMatch(code -> code == DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT));
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(d -> d.message().contains("incompatible length facets")));
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.message().contains("abstract")));
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.message().contains("missing a base")));
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.message().contains("restriction")));
  }

  @Test
  void normalizesSimpleContentRestrictionWithAttributes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Note">
                  <xs:simpleContent>
                    <xs:restriction base="xs:string">
                      <xs:maxLength value="12"/>
                      <xs:attribute name="lang" type="xs:string" use="required"/>
                    </xs:restriction>
                  </xs:simpleContent>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String irText = result.model().toText();
    assertTrue(irText.contains("complexType {urn:orders}Note"), irText);
    assertTrue(
        irText.contains("simpleContent type=xs:string restriction base=xs:string maxLength=12"),
        irText);
    assertTrue(irText.contains("attribute lang type=xs:string use=required"), irText);
  }

  @Test
  void reportsInvalidSimpleContentShapes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="MissingBase">
                  <xs:simpleContent>
                    <xs:extension/>
                  </xs:simpleContent>
                </xs:complexType>
                <xs:complexType name="Order"/>
                <xs:complexType name="UnsupportedBase">
                  <xs:simpleContent>
                    <xs:extension base="tns:Order"/>
                  </xs:simpleContent>
                </xs:complexType>
                <xs:complexType name="BadChild">
                  <xs:simpleContent>
                    <xs:extension base="xs:string">
                      <xs:sequence/>
                    </xs:extension>
                  </xs:simpleContent>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT,
            DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT),
        diagnosticCodes(result));
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.message().contains("missing a base")));
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(d -> d.message().contains("Unsupported xs:simpleContent base")));
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(d -> d.message().contains("derivation child xs:sequence")));
  }

  @Test
  void normalizesComplexRestrictionWhenMembersAreBaseMembers() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="BaseOrder">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:element name="note" type="xs:string" minOccurs="0"/>
                  </xs:sequence>
                  <xs:attribute name="version" type="xs:string" use="required"/>
                </xs:complexType>
                <xs:complexType name="RestrictedOrder">
                  <xs:complexContent>
                    <xs:restriction base="tns:BaseOrder">
                      <xs:sequence>
                        <xs:element name="id" type="xs:string"/>
                      </xs:sequence>
                      <xs:attribute name="version" type="xs:string" use="required"/>
                    </xs:restriction>
                  </xs:complexContent>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String irText = result.model().toText();
    assertTrue(irText.contains("complexType {urn:orders}RestrictedOrder"), irText);
    assertTrue(irText.contains("element {urn:orders}id type=xs:string"), irText);
    assertTrue(irText.contains("attribute version type=xs:string use=required"), irText);
  }

  @Test
  void reportsRecursiveDerivationChains() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="FirstCode">
                  <xs:restriction base="tns:SecondCode"/>
                </xs:simpleType>
                <xs:simpleType name="SecondCode">
                  <xs:restriction base="tns:FirstCode"/>
                </xs:simpleType>
                <xs:complexType name="FirstOrder">
                  <xs:complexContent>
                    <xs:extension base="tns:SecondOrder"/>
                  </xs:complexContent>
                </xs:complexType>
                <xs:complexType name="SecondOrder">
                  <xs:complexContent>
                    <xs:extension base="tns:FirstOrder"/>
                  </xs:complexContent>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(
        diagnosticCodes(result).stream()
            .allMatch(code -> code == DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT));
    assertTrue(diagnosticCodes(result).size() >= 2);
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.message().contains("Recursive")));
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
  void indexesNotationAndChameleonIncludedComponents() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:include schemaLocation="common.xsd"/>
                <xs:notation name="gif" public="image/gif"/>
                <xs:element name="order" type="tns:Order"/>
                """));
    write(
        "common.xsd",
        """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:complexType name="Order">
                <xs:sequence>
                  <xs:element name="id" type="xs:string"/>
                </xs:sequence>
              </xs:complexType>
            </xs:schema>
            """);

    SchemaIrResult result = build("main.xsd");

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    assertTrue(result.graph().toText().contains("notation {urn:orders}gif @ main.xsd"));
    assertTrue(result.graph().toText().contains("complexType {urn:orders}Order @ common.xsd"));
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
  void recognizedFullXsd10ConstructsFailBeforeBinding() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:redefine schemaLocation="base.xsd"/>
                <xs:complexType name="Order">
                  <xs:all>
                    <xs:element name="id" type="xs:string"/>
                  </xs:all>
                  <xs:anyAttribute processContents="skip"/>
                </xs:complexType>
                <xs:element name="orders">
                  <xs:complexType>
                    <xs:sequence>
                      <xs:element name="order" type="xs:string"/>
                    </xs:sequence>
                  </xs:complexType>
                  <xs:key name="orderId">
                    <xs:selector xpath="tns:order"/>
                    <xs:field xpath="@id"/>
                  </xs:key>
                </xs:element>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_FULL);

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT), diagnosticCodes(result));
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.message().contains("Global xs:redefine")));
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
  void buildsIrForDirectSubstitutionGroup() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" type="xs:string"/>
                <xs:element name="cardPayment" substitutionGroup="tns:payment" type="xs:string"/>
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element ref="tns:payment" minOccurs="0"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String irText = result.model().toText();
    assertTrue(irText.contains("substitutionGroup head={urn:orders}payment"), irText);
    assertTrue(
        irText.contains("element {urn:orders}payment type=xs:string cardinality=1..1"), irText);
    assertTrue(
        irText.contains(
            "element {urn:orders}cardPayment type=xs:string cardinality=1..1 "
                + "substitutionGroup={urn:orders}payment"),
        irText);
  }

  @Test
  void normalizesNestedSubstitutionGroupHead() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" type="xs:string"/>
                <xs:element name="cardPayment" substitutionGroup="tns:payment" type="xs:string"/>
                <xs:element name="rewardCardPayment" substitutionGroup="tns:cardPayment" type="xs:string"/>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String irText = result.model().toText();
    assertTrue(irText.contains("substitutionGroup head={urn:orders}payment"), irText);
    assertTrue(irText.contains("element {urn:orders}rewardCardPayment"), irText);
  }

  @Test
  void reportsSubstitutionGroupCycles() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" substitutionGroup="tns:rewardPayment" type="xs:string"/>
                <xs:element name="cardPayment" substitutionGroup="tns:payment" type="xs:string"/>
                <xs:element name="rewardPayment" substitutionGroup="tns:cardPayment" type="xs:string"/>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertTrue(
        diagnosticCodes(result).stream()
            .allMatch(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT::equals));
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(d -> d.message().contains("Substitution group cycle")));
  }

  @Test
  void rejectsMissingSubstitutionGroupHead() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="cardPayment" substitutionGroup="tns:payment" type="xs:string"/>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE), diagnosticCodes(result));
    assertTrue(result.diagnostics().getFirst().message().contains("is not declared"));
  }

  @Test
  void rejectsAbstractSubstitutionElements() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" type="xs:string" abstract="true"/>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertTrue(
        diagnosticCodes(result).stream()
            .allMatch(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT::equals));
    assertTrue(result.diagnostics().getFirst().message().contains("has no substitution members"));
  }

  @Test
  void rejectsElementRefSubstitutionMetadata() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" type="xs:string"/>
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element ref="tns:payment" substitutionGroup="tns:payment"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT), diagnosticCodes(result));
    assertTrue(
        result.diagnostics().getFirst().message().contains("ref uses cannot declare substitution"));
  }

  @Test
  void rejectsElementSubstitutionControls() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" type="xs:string" block="substitution"/>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT), diagnosticCodes(result));
    assertTrue(result.diagnostics().getFirst().message().contains("block/final"));
  }

  @Test
  void buildsIrForIdentityConstraintMetadata() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order">
                  <xs:key name="lineSkuKey">
                    <xs:selector xpath="tns:line"/>
                    <xs:field xpath="tns:sku"/>
                  </xs:key>
                  <xs:keyref name="referenceSkuKeyref" refer="tns:lineSkuKey">
                    <xs:selector xpath="tns:reference"/>
                    <xs:field xpath="tns:sku"/>
                  </xs:keyref>
                </xs:element>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="line" type="tns:Line" maxOccurs="unbounded"/>
                    <xs:element name="reference" type="tns:Reference" maxOccurs="unbounded"/>
                  </xs:sequence>
                </xs:complexType>
                <xs:complexType name="Line">
                  <xs:sequence>
                    <xs:element name="sku" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
                <xs:complexType name="Reference">
                  <xs:sequence>
                    <xs:element name="sku" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String irText = result.model().toText();
    assertTrue(
        irText.contains(
            "key {urn:orders}lineSkuKey selector={urn:orders}line fields={urn:orders}sku"),
        irText);
    assertTrue(
        irText.contains(
            "keyref {urn:orders}referenceSkuKeyref refer={urn:orders}lineSkuKey "
                + "selector={urn:orders}reference fields={urn:orders}sku"),
        irText);
  }

  @Test
  void rejectsUnsupportedIdentityConstraintXpath() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="xs:string">
                  <xs:unique name="lineSkuUnique">
                    <xs:selector xpath="tns:line[position()=1]"/>
                    <xs:field xpath="tns:sku"/>
                  </xs:unique>
                </xs:element>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT), diagnosticCodes(result));
    assertTrue(result.diagnostics().getFirst().message().contains("Unsupported identity"));
  }

  @Test
  void rejectsUnresolvedIdentityConstraintKeyref() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="xs:string">
                  <xs:keyref name="missingReference" refer="tns:missingKey">
                    <xs:selector xpath="."/>
                    <xs:field xpath="."/>
                  </xs:keyref>
                </xs:element>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE), diagnosticCodes(result));
    assertTrue(result.diagnostics().getFirst().message().contains("refers to missing key"));
  }

  @Test
  void rejectsLocalSubstitutionGroupMemberDeclarations() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" type="xs:string"/>
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="cardPayment" substitutionGroup="tns:payment" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT), diagnosticCodes(result));
    assertTrue(result.diagnostics().getFirst().message().contains("only on global"));
  }

  @Test
  void rejectsSelfSubstitution() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" substitutionGroup="tns:payment" type="xs:string"/>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT), diagnosticCodes(result));
    assertTrue(result.diagnostics().getFirst().message().contains("cannot substitute itself"));
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

  @Test
  void normalizesAcceptedWildcardParticlesForDocumentProfile() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:any namespace="##other" processContents="skip" minOccurs="0" maxOccurs="unbounded"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertTrue(result.diagnostics().isEmpty());
    assertTrue(
        result
            .model()
            .toText()
            .contains(
                "wildcard namespace=other:urn:orders processContents=skip cardinality=0..unbounded"));
  }

  @Test
  void normalizesAcceptedMixedSequenceForDocumentProfile() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order" mixed="true">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:any namespace="##other" processContents="skip" minOccurs="0" maxOccurs="unbounded"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertTrue(result.diagnostics().isEmpty());
    assertTrue(result.model().toText().contains("complexType {urn:orders}Order mixed=true"));
    assertTrue(
        result
            .model()
            .toText()
            .contains(
                "wildcard namespace=other:urn:orders processContents=skip cardinality=0..unbounded"));
  }

  @Test
  void buildsIrForMixedChoiceForDocumentProfile() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order" mixed="true">
                  <xs:choice>
                    <xs:element name="id" type="xs:string"/>
                  </xs:choice>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String irText = result.model().toText();
    assertTrue(irText.contains("complexType {urn:orders}Order mixed=true"), irText);
    assertTrue(irText.contains("choice cardinality=1..1"), irText);
    assertTrue(irText.contains("element {urn:orders}id type=xs:string cardinality=1..1"), irText);
  }

  @Test
  void acceptsWildcardDefaultAndStrictProcessContents() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:any namespace="##any"/>
                  </xs:sequence>
                </xs:complexType>
                """));
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve("main.xsd"));
    XsdSyntaxResult syntaxResult =
        new XsdSyntaxParser().parse(resolution.manifest(), GeneratorProfile.XP_XSD10_DOCUMENT);

    SchemaIrResult result = new SchemaIrBuilder().build(syntaxResult);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    assertTrue(result.model().toText().contains("processContents=strict"));
  }

  @Test
  void normalizesAnyAttributeAndAttributeFormDefaults() throws IOException {
    write(
        "main.xsd",
        """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:tns="urn:orders"
            targetNamespace="urn:orders"
            attributeFormDefault="qualified">
          <xs:complexType name="Order">
            <xs:attribute name="qualifiedByDefault" type="xs:string"/>
            <xs:attribute name="local" type="xs:string" form="unqualified"/>
            <xs:anyAttribute namespace="##local ##targetNamespace" processContents="lax"/>
          </xs:complexType>
        </xs:schema>
        """);

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    String text = result.model().toText();
    assertTrue(text.contains("attribute {urn:orders}qualifiedByDefault type=xs:string"));
    assertTrue(text.contains("attribute local type=xs:string"));
    assertTrue(text.contains("anyAttribute namespace=explicit:,urn:orders processContents=lax"));
  }

  @Test
  void composesAnyAttributeAcrossAttributeGroups() throws IOException {
    write(
        "main.xsd",
        """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:tns="urn:orders"
            targetNamespace="urn:orders">
          <xs:attributeGroup name="ExternalAttributes">
            <xs:anyAttribute namespace="urn:external" processContents="skip"/>
          </xs:attributeGroup>
          <xs:complexType name="Order">
            <xs:attributeGroup ref="tns:ExternalAttributes"/>
            <xs:anyAttribute namespace="##targetNamespace" processContents="lax"/>
          </xs:complexType>
        </xs:schema>
        """);

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    assertTrue(
        result
            .model()
            .toText()
            .contains(
                "anyAttribute namespace=explicit:urn:external,urn:orders processContents=lax"));
  }

  @Test
  void reportsInvalidAnyAttributeProcessContentsAndNamespaceComposition() throws IOException {
    write(
        "main.xsd",
        """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:tns="urn:orders"
            targetNamespace="urn:orders">
          <xs:attributeGroup name="OtherAttributes">
            <xs:anyAttribute namespace="##other" processContents="skip"/>
          </xs:attributeGroup>
          <xs:complexType name="Order">
            <xs:attributeGroup ref="tns:OtherAttributes"/>
            <xs:anyAttribute namespace="##targetNamespace" processContents="invalid"/>
          </xs:complexType>
        </xs:schema>
        """);

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT), diagnosticCodes(result));
    assertTrue(result.diagnostics().getFirst().message().contains("invalid processContents"));
  }

  @Test
  void rejectsWildcardOverlappingLaterNamedElement() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:any namespace="##any" processContents="skip" minOccurs="0" maxOccurs="unbounded"/>
                    <xs:element name="tail" type="xs:string" minOccurs="0"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertEquals(2, result.diagnostics().size());
    assertTrue(
        diagnosticCodes(result).stream()
            .allMatch(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT::equals));
    assertEquals(
        "SCHEMA_IR_INVALID_COMPONENT | main.xsd | xs:any namespace constraint overlaps XML "
            + "element {urn:orders}id in the same sequence.",
        result.diagnostics().getFirst().toManifestLine());
  }

  @Test
  void rejectsExplicitWildcardOverlappingChoiceBranch() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:any namespace="urn:orders" processContents="skip" minOccurs="0"/>
                    <xs:choice minOccurs="0">
                      <xs:element name="card" type="xs:string"/>
                      <xs:element name="cash" type="xs:string"/>
                    </xs:choice>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertEquals(2, result.diagnostics().size());
    assertTrue(
        diagnosticCodes(result).stream()
            .allMatch(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT::equals));
    assertTrue(
        result
            .diagnostics()
            .getFirst()
            .message()
            .contains("xs:any namespace constraint overlaps XML element {urn:orders}card"));
  }

  @Test
  void rejectsWildcardChoiceBranchOverlappingNamedChoiceBranch() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:choice>
                      <xs:element name="card" type="xs:string"/>
                      <xs:any namespace="##any" processContents="skip"/>
                    </xs:choice>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertEquals(1, result.diagnostics().size());
    assertEquals(
        DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT, result.diagnostics().getFirst().code());
    assertTrue(
        result
            .diagnostics()
            .getFirst()
            .message()
            .contains("xs:any namespace constraint overlaps XML element {urn:orders}card"));
  }

  @Test
  void rejectsOverlappingWildcardChoiceBranches() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:choice>
                      <xs:any namespace="##local" processContents="skip"/>
                      <xs:any namespace="##any" processContents="skip"/>
                    </xs:choice>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertEquals(1, result.diagnostics().size());
    assertEquals(
        DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT, result.diagnostics().getFirst().code());
    assertEquals(
        "xs:any namespace constraints overlap in the same sequence.",
        result.diagnostics().getFirst().message());
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
