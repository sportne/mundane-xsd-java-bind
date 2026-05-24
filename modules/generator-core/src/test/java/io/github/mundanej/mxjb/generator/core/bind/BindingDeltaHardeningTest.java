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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BindingDeltaHardeningTest {
  @TempDir private Path tempDirectory;

  @Test
  void anyAttributeUsesXmlAttributeListAndAvoidsNamedAttributeCollision() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Order">
                  <xs:attribute name="wildcard-attributes" type="xs:string"/>
                  <xs:attribute name="blocked" type="xs:string" use="prohibited"/>
                  <xs:anyAttribute namespace="##other" processContents="skip"/>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertFalse(result.hasErrors(), result.diagnostics().toString());
    BindingType order = onlyType(result);
    assertEquals(
        List.of("attribute:wildcardAttributes", "anyAttribute:wildcardAttributes2"),
        order.fields().stream().map(field -> field.kind() + ":" + field.javaName()).toList());

    BindingField anyAttribute = order.fields().get(1);
    assertEquals(
        "xmlAttribute:io.github.mundanej.mxjb.runtime.XmlAttribute", anyAttribute.type().toText());
    assertEquals("list 0..unbounded", anyAttribute.cardinality().toText());
    assertEquals("other:urn:orders", anyAttribute.wildcard().namespaceConstraint().toText());
    assertEquals("skip", anyAttribute.wildcard().processContents());
    assertEquals(
        List.of("blocked"),
        anyAttribute.wildcard().excludedNames().stream().map(name -> name.localName()).toList());
  }

  @Test
  void simpleContentKeepsTextValueAndAttributesAsDistinctFields() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="label" type="tns:Label"/>
                <xs:complexType name="Label">
                  <xs:simpleContent>
                    <xs:extension base="xs:string">
                      <xs:attribute name="value" type="xs:string"/>
                      <xs:attribute name="lang" type="xs:language" use="required"/>
                    </xs:extension>
                  </xs:simpleContent>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertFalse(result.hasErrors(), result.diagnostics().toString());
    BindingType label = onlyType(result);
    assertEquals(
        List.of("simpleContent:value", "attribute:value2", "attribute:lang"),
        label.fields().stream().map(field -> field.kind() + ":" + field.javaName()).toList());
    assertEquals("scalar:string", label.fields().get(0).type().toText());
    assertEquals("#text", label.fields().get(0).xmlName().toText());
    assertEquals("scalar:language", label.fields().get(2).type().toText());
    assertTrue(label.validationPlan().rules().contains("simpleContent value"));
  }

  @Test
  void repeatedSubstitutionHeadReferenceBindsAsRequiredBranchList() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="payment" type="tns:Payment" abstract="true"/>
                <xs:element name="cardPayment"
                    substitutionGroup="tns:payment" type="tns:CardPayment"/>
                <xs:element name="cashPayment" substitutionGroup="tns:payment" type="xs:decimal"/>
                <xs:element name="order" type="tns:Order"/>
                <xs:complexType name="Payment"/>
                <xs:complexType name="CardPayment"/>
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:element ref="tns:payment" minOccurs="1" maxOccurs="2"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertFalse(result.hasErrors(), result.diagnostics().toString());
    BindingField payment = onlyTypeNamed(result, "Order").fields().getFirst();
    assertEquals("choice", payment.kind());
    assertEquals("payment", payment.javaName());
    assertEquals("list 1..2", payment.cardinality().toText());
    assertTrue(payment.required());
    assertEquals("substitution", payment.choice().modelKind());
    assertEquals(
        List.of(
            "cardpayment:model:io.github.mundanej.mxjb.generated.orders.Cardpayment",
            "cashpayment:scalar:decimal"),
        payment.choice().branches().stream()
            .map(branch -> branch.javaName() + ":" + branch.type().toText())
            .toList());
    assertFalse(
        result.model().rootElements().stream()
            .anyMatch(root -> "payment".equals(root.xmlName().localName())));
  }

  @Test
  void invalidQNameSimpleContentRestrictionIsRejectedWithoutPartialModel() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:simpleType name="KnownName">
                  <xs:restriction base="xs:QName">
                    <xs:enumeration value="p:known"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:element name="name" type="tns:NameCarrier"/>
                <xs:complexType name="NameCarrier">
                  <xs:simpleContent>
                    <xs:extension base="tns:KnownName">
                      <xs:attribute name="source" type="xs:string"/>
                    </xs:extension>
                  </xs:simpleContent>
                </xs:complexType>
                """));

    BindingResult result = bind("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertEquals(List.of(DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE), diagnosticCodes(result));
    assertTrue(result.diagnostics().getFirst().message().contains("namespace context"));
    assertEquals("", result.model().toText());
  }

  private BindingResult bind(String primarySchema, GeneratorProfile profile) {
    return new BindingModelBuilder()
        .build(ir(primarySchema, profile), BindingConfiguration.defaults());
  }

  private SchemaIrResult ir(String primarySchema, GeneratorProfile profile) {
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve(primarySchema));
    assertTrue(resolution.diagnostics().isEmpty(), resolution.diagnostics().toString());
    return new SchemaIrBuilder().build(new XsdSyntaxParser().parse(resolution.manifest(), profile));
  }

  private BindingType onlyType(BindingResult result) {
    assertEquals(1, result.model().types().size(), result.model().toText());
    return result.model().types().getFirst();
  }

  private BindingType onlyTypeNamed(BindingResult result, String simpleName) {
    return result.model().types().stream()
        .filter(type -> simpleName.equals(type.javaName().simpleName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError(result.model().toText()));
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
