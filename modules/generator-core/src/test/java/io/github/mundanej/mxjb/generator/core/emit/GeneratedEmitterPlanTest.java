package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.mundanej.mxjb.generator.core.bind.BindingCardinality;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.BindingValidationPlan;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GeneratedEmitterPlanTest {
  @Test
  void plansReaderWriterAndValidatorRootSourceNamesAndHelpers() {
    BindingType rootType = rootType();
    BindingRootElement root = root(rootType);

    GeneratedEmitterPlan reader = GeneratedEmitterPlan.reader(root, rootType);
    GeneratedEmitterPlan writer = GeneratedEmitterPlan.writer(root, rootType);
    GeneratedEmitterPlan validator = GeneratedEmitterPlan.validator(root, rootType);

    assertEquals("com.acme.orders.xml.OrderXmlReader", reader.sourceName().qualifiedName());
    assertEquals("com.acme.orders.xml.OrderXmlWriter", writer.sourceName().qualifiedName());
    assertEquals("com.acme.orders.xml.OrderXmlValidator", validator.sourceName().qualifiedName());
    assertEquals(Path.of("com/acme/orders/xml/OrderXmlReader.java"), reader.relativePath());
    assertEquals("readOrder", reader.rootHelperName());
    assertEquals("writeOrder", writer.rootHelperName());
    assertEquals("validateOrder", validator.rootHelperName());
    assertSame(root, reader.root());
    assertSame(rootType, reader.rootType());
  }

  @Test
  void plansFieldTraversalInputsInBindingOrder() {
    BindingType rootType = rootType();
    BindingRootElement root = root(rootType);

    GeneratedEmitterPlan plan = GeneratedEmitterPlan.reader(root, rootType);

    assertEquals(
        List.of("id", "quantity"),
        plan.fieldPlans().stream().map(field -> field.field().javaName()).toList());
    assertEquals(
        List.of(rootType.javaName(), rootType.javaName()),
        plan.fieldPlans().stream().map(GeneratedEmitterFieldPlan::ownerName).toList());
  }

  private BindingType rootType() {
    BindingJavaName modelName = new BindingJavaName("com.acme.orders", "Order");
    return new BindingType(
        modelName,
        q("Order"),
        "record",
        List.of(
            field("id", new BindingTypeReference("scalar", "string"), 1),
            field("quantity", new BindingTypeReference("scalar", "int"), 2)),
        new BindingValidationPlan(List.of()));
  }

  private BindingRootElement root(BindingType rootType) {
    return new BindingRootElement(
        q("order"),
        new BindingTypeReference("model", rootType.javaName().qualifiedName()),
        new BindingCardinality("required", 1, "1"));
  }

  private BindingField field(String localName, BindingTypeReference type, int order) {
    return new BindingField(
        "element",
        q(localName),
        localName,
        type,
        new BindingCardinality("required", 1, "1"),
        order,
        true);
  }

  private SchemaQName q(String localName) {
    return new SchemaQName("urn:orders", localName);
  }
}
