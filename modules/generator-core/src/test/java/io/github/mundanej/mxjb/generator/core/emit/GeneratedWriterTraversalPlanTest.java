package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.bind.BindingCardinality;
import io.github.mundanej.mxjb.generator.core.bind.BindingContent;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.BindingValidationPlan;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GeneratedWriterTraversalPlanTest {
  @Test
  void plansWriterFieldTraversalByWriterPhaseAndBindingOrder() {
    BindingType type =
        new BindingType(
            new BindingJavaName("com.acme.orders", "Order"),
            q("Order"),
            "record",
            List.of(
                field("element", "line", 30),
                field("attribute", "id", 10),
                field("simpleContent", "text", 20),
                field("anyAttribute", "wildcardAttributes", 15),
                contentField("content", 40)),
            new BindingValidationPlan(List.of()));

    GeneratedWriterTraversalPlan plan = GeneratedWriterTraversalPlan.from(type);

    assertEquals(type.javaName(), plan.ownerName());
    assertEquals(List.of("id"), javaNames(plan.attributeFields()));
    assertEquals(List.of("wildcardAttributes"), javaNames(plan.anyAttributeFields()));
    assertEquals(List.of("text"), javaNames(plan.simpleContentFields()));
    assertEquals(List.of("line", "content"), javaNames(plan.contentFields()));
  }

  @Test
  void plansContentBranchTraversalInBindingOrder() {
    BindingField content = contentField("content", 1);

    GeneratedWriterContentTraversalPlan plan = GeneratedWriterContentTraversalPlan.from(content);

    assertEquals(content, plan.field());
    assertEquals(
        List.of("text", "item"),
        plan.branches().stream().map(BindingContentBranch::javaName).toList());
  }

  @Test
  void nonContentFieldsHaveNoContentBranches() {
    GeneratedWriterContentTraversalPlan plan =
        GeneratedWriterContentTraversalPlan.from(field("element", "line", 1));

    assertTrue(plan.branches().isEmpty());
  }

  private List<String> javaNames(List<BindingField> fields) {
    return fields.stream().map(BindingField::javaName).toList();
  }

  private BindingField field(String kind, String localName, int order) {
    return new BindingField(
        kind, q(localName), localName, scalar("string"), required(), order, true);
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
            List.of(branch("text", "text", 1), branch("element", "item", 2)),
            "mixed content"));
  }

  private BindingContentBranch branch(String kind, String localName, int order) {
    return new BindingContentBranch(
        kind,
        q(localName),
        localName,
        scalar("string"),
        new BindingJavaName("com.acme.orders", localName + "Content"),
        required(),
        order,
        null);
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
