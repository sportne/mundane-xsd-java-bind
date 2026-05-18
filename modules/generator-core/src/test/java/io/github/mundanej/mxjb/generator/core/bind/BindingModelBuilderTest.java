package io.github.mundanej.mxjb.generator.core.bind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.resolver.SchemaResolutionResult;
import io.github.mundanej.mxjb.generator.core.resolver.SchemaResolver;
import io.github.mundanej.mxjb.generator.core.resolver.SchemaResolverPolicy;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrBuilder;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrResult;
import io.github.mundanej.mxjb.generator.core.schema.XsdSyntaxParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BindingModelBuilderTest {
  @TempDir private Path tempDirectory;

  @Test
  void bindsRootElementComplexTypeFieldsAndValidationPlan() throws IOException {
    write(
        "main.xsd",
        schema(
            "http://schemas.example.com/orders/v1",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:element name="line" type="xs:int" minOccurs="0" maxOccurs="unbounded"/>
                  </xs:sequence>
                  <xs:attribute name="version" type="xs:string" use="required"/>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd");

    assertFalse(result.hasErrors());
    assertEquals(
        """
                binding-model
                  root {http://schemas.example.com/orders/v1}order type=model:com.example.schemas.orders.v1.Order cardinality=required 1..1
                  type com.example.schemas.orders.v1.Order shape=record schema={http://schemas.example.com/orders/v1}Order
                    element id xml={http://schemas.example.com/orders/v1}id type=scalar:string cardinality=required 1..1 order=1 required=true
                    element line xml={http://schemas.example.com/orders/v1}line type=scalar:int cardinality=list 0..unbounded order=2 required=false
                    attribute version xml={http://schemas.example.com/orders/v1}version type=scalar:string cardinality=required 1..1 order=0 required=true
                    validation
                      rule element id required 1..1
                      rule element line list 0..unbounded
                      rule attribute version use=required
                """,
        result.model().toText());
  }

  @Test
  void appliesExplicitNamespacePackageOverrides() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order"/>
                """));

    BindingResult result =
        bind(
            "main.xsd",
            BindingConfiguration.withNamespacePackages(Map.of("urn:orders", "com.acme.orders")));

    assertFalse(result.hasErrors());
    assertTrue(result.model().toText().contains("type com.acme.orders.Order shape=record"));
  }

  @Test
  void derivesUrnPackagesFromDefaultPackageAndNamespaceTokens() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:example:orders",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order"/>
                """));

    BindingResult result = bind("main.xsd");

    assertFalse(result.hasErrors());
    assertTrue(
        result.model().toText().contains("io.github.mundanej.mxjb.generated.example.orders.Order"));
  }

  @Test
  void bindsInlineComplexTypesWithOwnerDerivedRecordCandidateName() throws IOException {
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

    BindingResult result = bind("main.xsd");

    assertFalse(result.hasErrors());
    assertTrue(
        result
            .model()
            .toText()
            .contains(
                "root {urn:orders}order type=model:io.github.mundanej.mxjb.generated.orders.Order"));
    assertTrue(
        result
            .model()
            .toText()
            .contains("type io.github.mundanej.mxjb.generated.orders.Order shape=record"));
  }

  @Test
  void bindsNestedInlineComplexTypesRecursively() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="line">
                      <xs:complexType>
                        <xs:sequence>
                          <xs:element name="sku" type="xs:string"/>
                        </xs:sequence>
                      </xs:complexType>
                    </xs:element>
                  </xs:sequence>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd");

    assertFalse(result.hasErrors());
    String text = result.model().toText();
    assertTrue(text.contains("root {urn:orders}order"));
    assertTrue(text.contains("type io.github.mundanej.mxjb.generated.orders.Line shape=record"));
    assertTrue(text.contains("element sku xml={urn:orders}sku type=scalar:string"));
    assertTrue(text.contains("type io.github.mundanej.mxjb.generated.orders.Order shape=record"));
    assertTrue(
        text.contains(
            "element line xml={urn:orders}line"
                + " type=model:io.github.mundanej.mxjb.generated.orders.Line"));
  }

  @Test
  void resolvesInlineTypeNameCollisionsWithNamedTypes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order"/>
                <xs:element name="order">
                  <xs:complexType/>
                </xs:element>
                """));

    BindingResult result = bind("main.xsd");

    assertFalse(result.hasErrors());
    String text = result.model().toText();
    assertTrue(
        text.contains(
            "root {urn:orders}order type=model:io.github.mundanej.mxjb.generated.orders.Order2"));
    assertTrue(
        text.contains(
            "type io.github.mundanej.mxjb.generated.orders.Order shape=record"
                + " schema={urn:orders}Order"));
    assertTrue(text.contains("type io.github.mundanej.mxjb.generated.orders.Order2 shape=record"));
  }

  @Test
  void sanitizesKeywordsAndResolvesFieldAndTypeCollisions() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="line-item"/>
                <xs:complexType name="line_item"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="class" type="xs:string"/>
                  </xs:sequence>
                  <xs:attribute name="class" type="xs:string"/>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd");

    assertFalse(result.hasErrors());
    assertTrue(
        result
            .model()
            .toText()
            .contains("type io.github.mundanej.mxjb.generated.orders.LineItem "));
    assertTrue(
        result
            .model()
            .toText()
            .contains("type io.github.mundanej.mxjb.generated.orders.LineItem2 "));
    assertTrue(result.model().toText().contains("element _class "));
    assertTrue(result.model().toText().contains("attribute _class2 "));
  }

  @Test
  void resolvesElementAndAttributeReferencesToReferencedTypes() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="shared" type="xs:string"/>
                <xs:attribute name="code" type="xs:boolean"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element ref="tns:shared"/>
                  </xs:sequence>
                  <xs:attribute ref="tns:code"/>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd");

    assertFalse(result.hasErrors());
    assertTrue(
        result
            .model()
            .toText()
            .contains("element shared xml={urn:orders}shared type=scalar:string"));
    assertTrue(
        result
            .model()
            .toText()
            .contains("attribute code xml={urn:orders}code type=scalar:boolean"));
  }

  @Test
  void bindsAcceptedChoiceParticle() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order"/>
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

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_DATA_10_CHOICE);

    assertFalse(result.hasErrors());
    String bindingText = result.model().toText();
    assertTrue(bindingText.contains("choiceType"), bindingText);
    assertTrue(bindingText.contains("choice orderChoice xml={urn:orders}orderChoice"), bindingText);
    assertTrue(
        bindingText.contains("type=choice:io.github.mundanej.mxjb.generated.orders.OrderChoice"),
        bindingText);
    assertTrue(bindingText.contains("cardinality=optional 0..1 order=2"), bindingText);
    assertTrue(bindingText.contains("branch domestic"), bindingText);
    assertTrue(bindingText.contains("branch international"), bindingText);
  }

  @Test
  void bindsRestrictedSimpleTypesAsScalarAliasesWithFacetRules() throws IOException {
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
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="code" type="tns:OrderCode"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_VALIDATION_10_BASIC);

    assertFalse(result.hasErrors());
    String bindingText = result.model().toText();
    assertTrue(bindingText.contains("element code"), bindingText);
    assertTrue(bindingText.contains("type=scalar:string facets["), bindingText);
    assertTrue(bindingText.contains("minLength=3"), bindingText);
    assertTrue(bindingText.contains("maxLength=8"), bindingText);
    assertTrue(bindingText.contains("pattern=[A-Z0-9]+"), bindingText);
  }

  @Test
  void bindsFlattenedGroupAndAttributeGroupFieldsInReferenceOrder() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:group name="OrderFields">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
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

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertFalse(result.hasErrors());
    String bindingText = result.model().toText();
    assertTrue(
        bindingText.contains("element id xml={urn:orders}id type=scalar:string"), bindingText);
    assertTrue(
        bindingText.contains("element total xml={urn:orders}total type=scalar:decimal"),
        bindingText);
    assertTrue(
        bindingText.contains("attribute version xml={urn:orders}version type=scalar:string"),
        bindingText);
    assertTrue(
        bindingText.contains(
            "element id xml={urn:orders}id type=scalar:string cardinality=required 1..1 order=1"),
        bindingText);
    assertTrue(
        bindingText.contains(
            "element total xml={urn:orders}total type=scalar:decimal cardinality=required 1..1 order=2"),
        bindingText);
  }

  @Test
  void bindsListAndUnionSimpleTypesForComposedProfile() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="Quantity">
                  <xs:restriction base="xs:int">
                    <xs:minInclusive value="1"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:simpleType name="QuantityList">
                  <xs:list itemType="tns:Quantity"/>
                </xs:simpleType>
                <xs:simpleType name="QuantityOrCode">
                  <xs:union memberTypes="tns:Quantity xs:string"/>
                </xs:simpleType>
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="quantities" type="tns:QuantityList"/>
                    <xs:element name="status" type="tns:QuantityOrCode"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertFalse(result.hasErrors(), result.diagnostics().toString());
    String bindingText = result.model().toText();
    assertTrue(
        bindingText.contains(
            "element quantities xml={urn:orders}quantities type=list:scalar:int facets[minInclusive=1]"),
        bindingText);
    assertTrue(
        bindingText.contains("element status xml={urn:orders}status type=union:"), bindingText);
    assertTrue(
        bindingText.contains("scalar:int facets[minInclusive=1]|scalar:string"), bindingText);
  }

  @Test
  void rejectsOptionalListValuedElementsForComposedProfile() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="Codes">
                  <xs:list itemType="xs:string"/>
                </xs:simpleType>
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="codes" type="tns:Codes" minOccurs="0"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertEquals(List.of(DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE), diagnosticCodes(result));
    assertTrue(result.diagnostics().getFirst().message().contains("required singleton XML values"));
  }

  @Test
  void bindsComplexExtensionAsFlattenedRecordFields() throws IOException {
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
                <xs:element name="order" type="tns:Order"/>
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

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertFalse(result.hasErrors(), result.diagnostics().toString());
    String bindingText = result.model().toText();
    assertTrue(
        bindingText.contains(
            "element id xml={urn:orders}id type=scalar:string "
                + "facets[minLength=3,maxLength=8,pattern=[A-Z0-9]+] "
                + "cardinality=required 1..1 order=1"),
        bindingText);
    assertTrue(
        bindingText.contains(
            "element total xml={urn:orders}total type=scalar:decimal cardinality=required 1..1 order=2"),
        bindingText);
    assertTrue(
        bindingText.contains(
            "attribute version xml={urn:orders}version type=scalar:string cardinality=required 1..1 order=0"),
        bindingText);
    assertTrue(
        bindingText.contains(
            "attribute region xml={urn:orders}region type=scalar:string cardinality=required 1..1 order=0"),
        bindingText);
  }

  @Test
  void reportsUnsupportedBuiltInScalarTypesWithoutPartialModel() throws IOException {
    write("main.xsd", schema("urn:orders", "<xs:element name=\"when\" type=\"xs:date\"/>"));

    BindingResult result = bind("main.xsd");

    assertEquals(List.of(DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE), diagnosticCodes(result));
    assertEquals("", result.model().toText());
  }

  @Test
  void bindsDirectSubstitutionGroupHeadReference() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" type="tns:Payment"/>
                <xs:element name="cardPayment" substitutionGroup="tns:payment" type="tns:CardPayment"/>
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Payment">
                  <xs:sequence>
                    <xs:element name="amount" type="xs:decimal"/>
                  </xs:sequence>
                </xs:complexType>
                <xs:complexType name="CardPayment">
                  <xs:complexContent>
                    <xs:extension base="tns:Payment">
                      <xs:sequence>
                        <xs:element name="cardLast4" type="xs:string"/>
                      </xs:sequence>
                    </xs:extension>
                  </xs:complexContent>
                </xs:complexType>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element ref="tns:payment" minOccurs="0"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertFalse(result.hasErrors(), result.diagnostics().toString());
    String bindingText = result.model().toText();
    assertTrue(
        bindingText.contains(
            "choice payment xml={urn:orders}payment "
                + "type=choice:io.github.mundanej.mxjb.generated.orders.PaymentSubstitution "
                + "cardinality=optional 0..1"),
        bindingText);
    assertTrue(
        bindingText.contains(
            "substitutionType io.github.mundanej.mxjb.generated.orders.PaymentSubstitution"),
        bindingText);
    assertTrue(
        bindingText.contains(
            "branch cardpayment xml={urn:orders}cardPayment "
                + "type=model:io.github.mundanej.mxjb.generated.orders.Cardpayment"),
        bindingText);
  }

  @Test
  void rejectsRepeatedSubstitutionGroupHeadReference() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" type="tns:Payment"/>
                <xs:element name="cardPayment" substitutionGroup="tns:payment" type="tns:Payment"/>
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Payment"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element ref="tns:payment" minOccurs="0" maxOccurs="unbounded"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertEquals(List.of(DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL), diagnosticCodes(result));
    assertTrue(result.model().toText().isEmpty());
  }

  @Test
  void reportsInvalidBindingConfigurationWithoutPartialModel() throws IOException {
    write("main.xsd", schema("urn:orders", "<xs:element name=\"title\" type=\"xs:string\"/>"));

    BindingResult result =
        bind("main.xsd", new BindingConfiguration("not-valid-package!", Map.of()));

    assertEquals(
        List.of(DiagnosticCode.SCHEMA_BINDING_INVALID_CONFIGURATION), diagnosticCodes(result));
    assertEquals("", result.model().toText());
  }

  @Test
  void propagatesIrDiagnosticsWithoutPartialBindingModel() throws IOException {
    write("main.xsd", schema("urn:orders", "<xs:element name=\"order\" type=\"tns:Missing\"/>"));

    BindingResult result = new BindingModelBuilder().build(ir("main.xsd"));

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_UNRESOLVED_REFERENCE), diagnosticCodes(result));
    assertEquals("", result.model().toText());
  }

  @Test
  void producesDeterministicOutput() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order"/>
                """));

    BindingResult first = bind("main.xsd");
    BindingResult second = bind("main.xsd");

    assertEquals(first.model().toText(), second.model().toText());
    assertEquals(first.diagnostics(), second.diagnostics());
  }

  @Test
  void bindsWildcardParticlesAsXmlFragmentLists() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:any namespace="##other" processContents="skip" minOccurs="0" maxOccurs="unbounded"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertTrue(result.diagnostics().isEmpty());
    assertTrue(
        result
            .model()
            .toText()
            .contains(
                "wildcard wildcardContent xml=* type=fragment:"
                    + "io.github.mundanej.mxjb.runtime.XmlFragment cardinality=list 0..unbounded"));
    assertTrue(result.model().toText().contains("wildcard namespace=other:urn:orders"));
  }

  @Test
  void bindsMixedContentAsGeneratedContentList() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order" mixed="true">
                  <xs:sequence>
                    <xs:element name="id" type="xs:string"/>
                    <xs:any namespace="##other" processContents="skip" minOccurs="0" maxOccurs="unbounded"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertTrue(result.diagnostics().isEmpty());
    assertTrue(
        result
            .model()
            .toText()
            .contains(
                "content content xml={urn:orders}Order type=choice:"
                    + "io.github.mundanej.mxjb.generated.orders.OrderContent "
                    + "cardinality=list 0..unbounded"));
    assertTrue(
        result
            .model()
            .toText()
            .contains("contentType io.github.mundanej.mxjb.generated.orders.OrderContent"));
    assertTrue(result.model().toText().contains("branch text text xml=#text type=scalar:string"));
    assertTrue(
        result.model().toText().contains("branch wildcard wildcardContent xml=* type=fragment:"));
  }

  private BindingResult bind(String primarySchema) {
    return bind(primarySchema, BindingConfiguration.defaults());
  }

  private BindingResult bind(String primarySchema, GeneratorProfile profile) {
    return new BindingModelBuilder()
        .build(ir(primarySchema, profile), BindingConfiguration.defaults());
  }

  private BindingResult bind(String primarySchema, BindingConfiguration configuration) {
    return new BindingModelBuilder().build(ir(primarySchema), configuration);
  }

  private SchemaIrResult ir(String primarySchema) {
    return ir(primarySchema, GeneratorProfile.XP_DATA_10);
  }

  private SchemaIrResult ir(String primarySchema, GeneratorProfile profile) {
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve(primarySchema));
    assertTrue(resolution.diagnostics().isEmpty());
    return new SchemaIrBuilder().build(new XsdSyntaxParser().parse(resolution.manifest(), profile));
  }

  private List<DiagnosticCode> diagnosticCodes(BindingResult result) {
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
