package io.github.mundanej.mxjb.generator.core.bind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.schema.SchemaCardinality;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrComplexType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrElement;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSubstitutionGroup;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrTypeReference;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrValueSemantics;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class BindingBranchPlannerTest {
  @Test
  void plansSubstitutionBranchesInSchemaOrder() {
    BindingNameAllocator names = new BindingNameAllocator(BindingConfiguration.defaults());
    Map<SchemaQName, SchemaIrComplexType> complexTypes =
        Map.of(
            q("CardPayment"), complexType(q("CardPayment")),
            q("WirePayment"), complexType(q("WirePayment")));
    List<String> diagnostics = new ArrayList<>();
    BindingSubstitutionPlanner planner =
        new BindingSubstitutionPlanner(
            names,
            complexTypes,
            Map.of(),
            (type, declaration, contextName) ->
                BindingTypeReference.model(
                    new BindingJavaName("test.generated", type.name().localName())),
            (code, message) -> diagnostics.add(code + " | " + message));

    BindingField field =
        planner.plan(
            element("payment", q("Payment"), true),
            new SchemaIrSubstitutionGroup(
                q("payment"),
                List.of(
                    element("cardPayment", q("CardPayment"), false),
                    element("wirePayment", q("WirePayment"), false))),
            new java.util.HashSet<>(),
            7);

    assertEquals("choice", field.kind());
    assertEquals("payment", field.javaName());
    assertEquals(new BindingCardinality("required", 1, "1"), field.cardinality());
    assertEquals(7, field.order());
    assertEquals("substitution", field.choice().modelKind());
    assertEquals(
        List.of("cardpayment", "wirepayment"),
        field.choice().branches().stream().map(BindingChoiceBranch::javaName).toList());
    assertEquals(List.of(), diagnostics);
  }

  @Test
  void reportsUnsupportedSubstitutionBranchTypes() {
    List<String> diagnostics = new ArrayList<>();
    BindingSubstitutionPlanner diagnosticPlanner =
        new BindingSubstitutionPlanner(
            new BindingNameAllocator(BindingConfiguration.defaults()),
            Map.of(),
            Map.of(),
            (type, declaration, contextName) -> BindingTypeReference.scalar("string"),
            (code, message) -> diagnostics.add(code + " | " + message));

    diagnosticPlanner.plan(
        element("payment", q("Payment"), true),
        new SchemaIrSubstitutionGroup(
            q("payment"), List.of(element("missingPayment", q("MissingPayment"), false))),
        new java.util.HashSet<>(),
        1);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE
                + " | Unsupported substitution group branch type {urn:orders}MissingPayment."),
        diagnostics);
  }

  @Test
  void plansDynamicTypeBranchesWithDeclaredDefaultFirstThenSortedCandidates() {
    BindingNameAllocator names = new BindingNameAllocator(BindingConfiguration.defaults());
    SchemaIrComplexType base = complexType(q("Payment"));
    SchemaIrComplexType card = derivedType(q("CardPayment"), q("Payment"), "extension", false);
    SchemaIrComplexType wire = derivedType(q("WirePayment"), q("Payment"), "extension", false);
    Map<SchemaQName, SchemaIrComplexType> complexTypes = orderedTypes(base, wire, card);
    Map<SchemaQName, BindingJavaName> typeNames =
        Map.of(
            q("Payment"), new BindingJavaName("test.generated", "Payment"),
            q("CardPayment"), new BindingJavaName("test.generated", "CardPayment"),
            q("WirePayment"), new BindingJavaName("test.generated", "WirePayment"));
    List<String> diagnostics = new ArrayList<>();
    BindingDynamicTypePlanner planner =
        new BindingDynamicTypePlanner(
            names, complexTypes, typeNames, (code, message) -> diagnostics.add(message));

    BindingField field =
        planner.planIfNeeded(
            element("payment", q("Payment"), false),
            null,
            SchemaIrTypeReference.named(q("Payment")),
            new java.util.HashSet<>(),
            3);

    assertNotNull(field);
    assertEquals("xsiType", field.choice().modelKind());
    assertEquals(
        List.of(q("Payment"), q("CardPayment"), q("WirePayment")),
        field.choice().branches().stream().map(BindingChoiceBranch::dynamicTypeName).toList());
    assertEquals(
        List.of(true, false, false),
        field.choice().branches().stream().map(BindingChoiceBranch::defaultDynamicType).toList());
    assertEquals(List.of(), diagnostics);
  }

  @Test
  void skipsDynamicTypePlanWhenConcreteDeclaredTypeHasOnlyBlockedCandidates() {
    BindingNameAllocator names = new BindingNameAllocator(BindingConfiguration.defaults());
    SchemaIrComplexType base =
        new SchemaIrComplexType(
            q("Payment"),
            null,
            List.of(),
            null,
            List.of(),
            false,
            false,
            false,
            null,
            "",
            List.of("extension"),
            List.of());
    SchemaIrComplexType card = derivedType(q("CardPayment"), q("Payment"), "extension", false);
    BindingDynamicTypePlanner planner =
        new BindingDynamicTypePlanner(
            names,
            orderedTypes(base, card),
            Map.of(q("Payment"), new BindingJavaName("test.generated", "Payment")),
            this::recordDiagnostic);

    assertNull(
        planner.planIfNeeded(
            element("payment", q("Payment"), false),
            null,
            SchemaIrTypeReference.named(q("Payment")),
            new java.util.HashSet<>(),
            1));
  }

  private void recordDiagnostic(DiagnosticCode code, String message) {}

  private Map<SchemaQName, SchemaIrComplexType> orderedTypes(SchemaIrComplexType... types) {
    Map<SchemaQName, SchemaIrComplexType> ordered = new LinkedHashMap<>();
    for (SchemaIrComplexType type : types) {
      ordered.put(type.name(), type);
    }
    return ordered;
  }

  private SchemaIrElement element(String localName, SchemaQName typeName, boolean reference) {
    return new SchemaIrElement(
        q(localName),
        SchemaIrTypeReference.named(typeName),
        SchemaCardinality.ONE,
        null,
        SchemaIrValueSemantics.NONE,
        null,
        false,
        List.of(),
        List.of(),
        reference);
  }

  private SchemaIrComplexType complexType(SchemaQName name) {
    return derivedType(name, null, "", false);
  }

  private SchemaIrComplexType derivedType(
      SchemaQName name, SchemaQName baseName, String derivationKind, boolean abstractType) {
    return new SchemaIrComplexType(
        name,
        null,
        List.of(),
        null,
        List.of(),
        false,
        false,
        abstractType,
        baseName,
        derivationKind,
        List.of(),
        List.of());
  }

  private SchemaQName q(String localName) {
    return new SchemaQName("urn:orders", localName);
  }
}
