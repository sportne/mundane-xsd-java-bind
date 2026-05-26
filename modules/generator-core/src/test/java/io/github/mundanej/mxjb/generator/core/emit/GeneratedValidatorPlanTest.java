package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.bind.BindingCardinality;
import io.github.mundanej.mxjb.generator.core.bind.BindingChoice;
import io.github.mundanej.mxjb.generator.core.bind.BindingChoiceBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingContent;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.BindingValidationPlan;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityConstraint;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GeneratedValidatorPlanTest {
  @Test
  void plansValidatorFieldAndBranchTraversal() {
    BindingType type =
        new BindingType(
            new BindingJavaName("com.acme.orders", "Order"),
            q("Order"),
            "record",
            List.of(
                field("element", "line", 30),
                field("attribute", "id", 10),
                contentField("content", 40),
                choiceField("choice", 20),
                field("element", "sameOrderLine", 30)),
            new BindingValidationPlan(List.of()));

    GeneratedValidatorTraversalPlan plan = GeneratedValidatorTraversalPlan.from(type);

    assertEquals(type.javaName(), plan.ownerName());
    assertEquals(
        List.of("id", "choice", "line", "sameOrderLine", "content"), javaNames(plan.fields()));
    assertEquals(List.of("line", "sameOrderLine"), javaNames(plan.elementFields()));
    assertEquals(List.of("choice"), javaNames(plan.choiceFields()));
    assertEquals(List.of("content"), javaNames(plan.contentFields()));
    assertEquals(
        List.of("branch"),
        plan.choiceBranches(choiceField("choice", 1)).stream()
            .map(BindingChoiceBranch::javaName)
            .toList());
    assertEquals(
        List.of("text"),
        plan.contentBranches(contentField("content", 1)).stream()
            .map(BindingContentBranch::javaName)
            .toList());
  }

  @Test
  void plansIdentityConstraintActivation() {
    BindingType type =
        new BindingType(
            new BindingJavaName("com.acme.orders", "Order"),
            q("Order"),
            "record",
            List.of(),
            new BindingValidationPlan(List.of()));
    BindingRootElement root =
        new BindingRootElement(
            q("order"),
            new BindingTypeReference("model", type.javaName().qualifiedName()),
            required(),
            List.of(
                new SchemaIrIdentityConstraint(
                    "unique", q("orderId"), null, List.of(), List.of())));

    GeneratedValidatorIdentityPlan plan = GeneratedValidatorIdentityPlan.from(root, type);

    assertTrue(plan.hasConstraints());
    assertEquals(root, plan.root());
    assertEquals(type, plan.rootType());
    assertEquals(
        List.of("unique"),
        plan.constraints().stream().map(SchemaIrIdentityConstraint::kind).toList());
    assertFalse(
        GeneratedValidatorIdentityPlan.from(
                new BindingRootElement(q("empty"), root.type(), required()), type)
            .hasConstraints());
  }

  private List<String> javaNames(List<BindingField> fields) {
    return fields.stream().map(BindingField::javaName).toList();
  }

  private BindingField field(String kind, String localName, int order) {
    return new BindingField(
        kind, q(localName), localName, scalar("string"), required(), order, true);
  }

  private BindingField choiceField(String localName, int order) {
    return new BindingField(
        "choice",
        q(localName),
        localName,
        new BindingTypeReference("choice", "com.acme.orders.OrderChoice"),
        required(),
        order,
        true,
        new BindingChoice(
            new BindingJavaName("com.acme.orders", "OrderChoice"),
            List.of(
                new BindingChoiceBranch(
                    q("branch"),
                    "branch",
                    scalar("string"),
                    new BindingJavaName("com.acme.orders", "BranchChoice")))));
  }

  private BindingField contentField(String localName, int order) {
    return new BindingField(
        "content",
        q(localName),
        localName,
        new BindingTypeReference("choice", "com.acme.orders.OrderContent"),
        new BindingCardinality("list", 0, "unbounded"),
        order,
        false,
        new BindingContent(
            new BindingJavaName("com.acme.orders", "OrderContent"),
            List.of(
                new BindingContentBranch(
                    "text",
                    q("text"),
                    "text",
                    scalar("string"),
                    new BindingJavaName("com.acme.orders", "TextContent"),
                    required(),
                    1,
                    null)),
            "mixed content"));
  }

  private BindingCardinality required() {
    return new BindingCardinality("required", 1, "1");
  }

  private BindingTypeReference scalar(String name) {
    return new BindingTypeReference("scalar", name);
  }

  private SchemaQName q(String localName) {
    return new SchemaQName("urn:orders", localName);
  }
}
