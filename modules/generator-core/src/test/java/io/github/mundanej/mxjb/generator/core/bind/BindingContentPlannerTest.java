package io.github.mundanej.mxjb.generator.core.bind;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mundanej.mxjb.generator.core.schema.SchemaCardinality;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrChoice;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrComplexType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrElement;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrGroup;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSequence;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrTypeReference;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcard;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BindingContentPlannerTest {
  @Test
  void plansGroupedContentBranchesPositionsAndWildcardMetadata() {
    BindingContentPlanner planner = planner();
    SchemaIrWildcard wildcard =
        new SchemaIrWildcard(
            new SchemaCardinality(0, "unbounded"),
            new SchemaIrWildcardNamespace("other", List.of("urn:orders")),
            "skip");
    SchemaIrGroup group =
        new SchemaIrGroup(
            "sequence",
            SchemaCardinality.ONE,
            List.of(
                element("id"),
                new SchemaIrChoice(
                    new SchemaCardinality(0, "1"), List.of(element("card"), wildcard))));

    BindingField field =
        planner.groupedContentField(
            complexType("Order"),
            new BindingJavaName("com.acme.orders", "Order"),
            "sequence",
            SchemaCardinality.ONE,
            List.of(group),
            new HashSet<>(),
            1);

    BindingContent content = field.content();
    assertEquals("orderSequenceContent", field.javaName());
    assertEquals("com.acme.orders.OrderSequenceContent", content.javaName().qualifiedName());
    assertEquals(List.of("id", "card", "wildcardContent"), branchNames(content));
    assertEquals(1, content.groups().size());
    assertEquals(2, content.groups().getFirst().positions().size());
    assertEquals(
        List.of(q("known")),
        content.branches().get(2).wildcard().knownElements().stream()
            .map(BindingWildcardElement::xmlName)
            .toList());
  }

  @Test
  void plansMixedContentTextAndElementBranches() {
    BindingContentPlanner planner = planner();
    SchemaIrComplexType complexType =
        new SchemaIrComplexType(
            q("Order"),
            List.of(),
            List.of(new SchemaIrSequence(SchemaCardinality.ONE, List.of(element("line")))),
            true,
            false);

    BindingField field =
        planner.mixedContentField(
            complexType, new BindingJavaName("com.acme.orders", "Order"), new HashSet<>(), 1);

    assertEquals("content", field.javaName());
    assertEquals(List.of("text", "line"), branchNames(field.content()));
    assertEquals("mixed content", field.content().modelKind());
  }

  @Test
  void preservesNestedGroupMetadataInsideChoiceBranches() {
    BindingContentPlanner planner = planner();
    SchemaIrGroup nestedGroup =
        new SchemaIrGroup(
            "sequence", SchemaCardinality.ONE, List.of(element("sku"), element("qty")));
    SchemaIrGroup outerGroup =
        new SchemaIrGroup(
            "sequence",
            SchemaCardinality.ONE,
            List.of(new SchemaIrChoice(new SchemaCardinality(0, "1"), List.of(nestedGroup))));

    BindingField field =
        planner.groupedContentField(
            complexType("Order"),
            new BindingJavaName("com.acme.orders", "Order"),
            "sequence",
            SchemaCardinality.ONE,
            List.of(outerGroup),
            new HashSet<>(),
            1);

    BindingContent content = field.content();
    assertEquals(List.of("sku", "qty"), branchNames(content));
    assertEquals(2, content.groups().size());
    assertEquals(2, content.groups().getFirst().positions().size());
    assertEquals(1, content.groups().get(1).positions().size());
    assertEquals(List.of("sku", "qty"), branchNames(content.groups().get(1)));
  }

  private BindingContentPlanner planner() {
    BindingNameAllocator names = new BindingNameAllocator(BindingConfiguration.defaults());
    return new BindingContentPlanner(
        names,
        ignored -> null,
        (reference, owner, fallbackName) ->
            BindingTypeReference.scalar(reference.name().localName()),
        namespace ->
            List.of(new BindingWildcardElement(q("known"), BindingTypeReference.scalar("string"))),
        () -> {
          throw new AssertionError("unexpected unsupported particle");
        });
  }

  private SchemaIrComplexType complexType(String localName) {
    return new SchemaIrComplexType(q(localName), List.of(), List.of(), false, false);
  }

  private SchemaIrElement element(String localName) {
    return new SchemaIrElement(
        q(localName),
        SchemaIrTypeReference.named(new SchemaQName(SchemaQName.XSD_NAMESPACE, "string")),
        SchemaCardinality.ONE,
        null,
        false);
  }

  private List<String> branchNames(BindingContent content) {
    return content.branches().stream().map(BindingContentBranch::javaName).toList();
  }

  private List<String> branchNames(BindingContentGroup group) {
    return group.branches().stream().map(BindingContentBranch::javaName).toList();
  }

  private SchemaQName q(String localName) {
    return new SchemaQName("urn:orders", localName);
  }
}
