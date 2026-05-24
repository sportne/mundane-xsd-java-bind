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

final class SchemaIrDeltaHardeningTest {
  @TempDir private Path tempDirectory;

  @Test
  void parsesIdentityConstraintSelectorAndFieldEdgeCases() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="xs:string">
                  <xs:unique name="edgeIdentity">
                    <xs:selector xpath=".//tns:line | tns:adjustment/*"/>
                    <xs:field xpath="@id"/>
                    <xs:field xpath="tns:sku|@*"/>
                    <xs:field xpath="."/>
                  </xs:unique>
                </xs:element>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    SchemaIrIdentityConstraint identity =
        result.model().elements().getFirst().identityConstraints().getFirst();
    assertEquals("unique", identity.kind());
    assertEquals("{urn:orders}edgeIdentity", identity.name().toText());
    assertEquals(
        List.of(".//{urn:orders}line", "{urn:orders}adjustment/*"),
        identity.selectors().stream().map(SchemaIrIdentityPath::toText).toList());
    assertEquals(
        List.of("@id", "{urn:orders}sku|@*", "."),
        identity.fields().stream().map(SchemaIrIdentityField::toText).toList());
  }

  @Test
  void reportsDuplicateIdentityConstraintNamesOnSameElement() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:element name="order" type="xs:string">
                  <xs:unique name="sameName">
                    <xs:selector xpath="."/>
                    <xs:field xpath="."/>
                  </xs:unique>
                  <xs:key name="sameName">
                    <xs:selector xpath="."/>
                    <xs:field xpath="."/>
                  </xs:key>
                </xs:element>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_SEMANTIC);

    assertEquals(List.of(DiagnosticCode.SCHEMA_IR_INVALID_COMPONENT), diagnosticCodes(result));
    assertEquals(
        "SCHEMA_IR_INVALID_COMPONENT | main.xsd | Duplicate identity constraint {urn:orders}sameName.",
        result.diagnostics().getFirst().toManifestLine());
  }

  @Test
  void preservesAnyAttributeMetadataAndDefaults() throws IOException {
    write(
        "main.xsd",
        """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:tns="urn:orders"
                targetNamespace="urn:orders">
              <xs:attributeGroup name="OpenAttributes">
                <xs:anyAttribute namespace="urn:external"/>
              </xs:attributeGroup>
              <xs:complexType name="Order">
                <xs:anyAttribute namespace="##local ##targetNamespace" processContents="lax"/>
              </xs:complexType>
            </xs:schema>
            """);

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    SchemaIrAnyAttribute groupAnyAttribute =
        result.model().attributeGroups().getFirst().anyAttribute();
    assertEquals("explicit", groupAnyAttribute.namespaceConstraint().kind());
    assertEquals(List.of("urn:external"), groupAnyAttribute.namespaceConstraint().namespaces());
    assertEquals("strict", groupAnyAttribute.processContents());

    SchemaIrAnyAttribute typeAnyAttribute = result.model().complexTypes().getFirst().anyAttribute();
    assertEquals("explicit", typeAnyAttribute.namespaceConstraint().kind());
    assertEquals(List.of("", "urn:orders"), typeAnyAttribute.namespaceConstraint().namespaces());
    assertEquals("lax", typeAnyAttribute.processContents());
  }

  @Test
  void preservesAllMetadataAsUnorderedParticle() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:all minOccurs="0">
                    <xs:element name="id" type="xs:string" minOccurs="0"/>
                    <xs:element name="note" type="xs:string" minOccurs="0"/>
                  </xs:all>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_COMPOSED);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    SchemaIrSequence sequence = result.model().complexTypes().getFirst().sequences().getFirst();
    assertEquals(SchemaCardinality.ONE, sequence.cardinality());
    SchemaIrAll all = sequence.allGroups().getFirst();
    assertEquals(new SchemaCardinality(0, "1"), all.cardinality());
    assertEquals(
        List.of("{urn:orders}id", "{urn:orders}note"),
        all.elements().stream().map(element -> element.name().toText()).toList());
    assertEquals(
        List.of("0..1", "0..1"),
        all.elements().stream().map(element -> element.cardinality().toText()).toList());
  }

  @Test
  void defaultsWildcardProcessContentsToStrict() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:complexType name="Order">
                  <xs:sequence>
                    <xs:any namespace="urn:external" minOccurs="0"/>
                  </xs:sequence>
                  <xs:anyAttribute namespace="urn:attrs"/>
                </xs:complexType>
                """));

    SchemaIrResult result = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    SchemaIrComplexType order = result.model().complexTypes().getFirst();
    SchemaIrWildcard wildcard = order.sequences().getFirst().wildcards().getFirst();
    assertEquals(new SchemaCardinality(0, "1"), wildcard.cardinality());
    assertEquals("explicit", wildcard.namespaceConstraint().kind());
    assertEquals(List.of("urn:external"), wildcard.namespaceConstraint().namespaces());
    assertEquals("strict", wildcard.processContents());
    assertEquals("strict", order.anyAttribute().processContents());
  }

  @Test
  void reportsDeterministicDiagnosticsForOneMultiErrorSchema() throws IOException {
    write(
        "main.xsd",
        schema(
            "urn:orders",
            """
                <xs:sequence/>
                <xs:element name="unknownType" type="tns:Missing"/>
                <xs:element name="unknownPrefix" type="missing:Thing"/>
                <xs:complexType name="OpenContent">
                  <xs:sequence>
                    <xs:any namespace="##any urn:external" processContents="maybe"/>
                  </xs:sequence>
                </xs:complexType>
                """));

    SchemaIrResult first = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);
    SchemaIrResult second = build("main.xsd", GeneratorProfile.XP_XSD10_DOCUMENT);

    assertEquals(first.diagnostics(), second.diagnostics());
    assertEquals(
        List.of(
            "SCHEMA_IR_INVALID_COMPONENT | main.xsd | Global xs:sequence is not valid at schema scope.",
            "SCHEMA_IR_INVALID_COMPONENT | main.xsd | xs:any has invalid processContents maybe.",
            "SCHEMA_IR_NAMESPACE_CONFLICT | main.xsd | Cannot resolve namespace prefix in QName missing:Thing.",
            "SCHEMA_IR_UNRESOLVED_REFERENCE | main.xsd | Unresolved type reference {urn:orders}Missing."),
        first.diagnostics().stream().map(SchemaDiagnostic::toManifestLine).toList());
  }

  private SchemaIrResult build(String primarySchema, GeneratorProfile profile) {
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.localRoots(List.of(tempDirectory)));
    SchemaResolutionResult resolution = resolver.resolve(tempDirectory.resolve(primarySchema));
    assertTrue(resolution.diagnostics().isEmpty(), resolution.diagnostics().toString());
    XsdSyntaxResult syntaxResult = new XsdSyntaxParser().parse(resolution.manifest(), profile);
    assertTrue(syntaxResult.diagnostics().isEmpty(), syntaxResult.diagnostics().toString());
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
