package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.bind.BindingCardinality;
import io.github.mundanej.mxjb.generator.core.bind.BindingChoice;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.BindingValidationPlan;
import io.github.mundanej.mxjb.generator.core.bind.BindingValueSemantics;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GeneratedReaderPlanTest {
  @Test
  void plansReaderStateFeaturesBeforeSourceAssembly() {
    BindingType rootType =
        new BindingType(
            new BindingJavaName("com.acme.orders", "Order"),
            q("Order"),
            "record",
            List.of(
                field(
                    "element",
                    "maybe",
                    scalar("string"),
                    1,
                    new BindingValueSemantics(true, null, null)),
                field(
                    "element",
                    "status",
                    scalar("string"),
                    2,
                    new BindingValueSemantics(false, "NEW", null)),
                field("anyAttribute", "otherAttribute", xmlAttribute(), 3),
                field("element", "tokens", list(scalar("NMTOKEN")), 4),
                new BindingField(
                    "choice",
                    q("dynamic"),
                    "dynamic",
                    new BindingTypeReference("choice", "com.acme.orders.DynamicChoice"),
                    required(),
                    5,
                    true,
                    new BindingChoice(
                        new BindingJavaName("com.acme.orders", "DynamicChoice"),
                        List.of(),
                        "xsiType"))),
            new BindingValidationPlan(List.of()));

    GeneratedReaderStatePlan plan = GeneratedReaderStatePlan.from(rootType, name -> null);

    assertTrue(plan.needsNillableSupport());
    assertTrue(plan.needsXsiTypeSupport());
    assertTrue(plan.needsWildcardSupport());
    assertTrue(plan.needsDefaultedElementSupport());
    assertTrue(plan.needsListSupport());
  }

  @Test
  void plansReaderStateWithoutOptionalHelpersForPlainScalars() {
    BindingType rootType =
        new BindingType(
            new BindingJavaName("com.acme.orders", "Order"),
            q("Order"),
            "record",
            List.of(field("element", "id", scalar("string"), 1)),
            new BindingValidationPlan(List.of()));

    GeneratedReaderStatePlan plan = GeneratedReaderStatePlan.from(rootType, name -> null);

    assertFalse(plan.needsNillableSupport());
    assertFalse(plan.needsXsiTypeSupport());
    assertFalse(plan.needsWildcardSupport());
    assertFalse(plan.needsDefaultedElementSupport());
    assertFalse(plan.needsListSupport());
  }

  @Test
  void plansScalarConversionExpressions() {
    assertEquals("value", GeneratedReaderScalarPlan.of(scalar("string")).parseExpression("value"));
    assertEquals(
        "(Integer) io.github.mundanej.mxjb.runtime.XmlDatatypes.parse(\"int\", value, input, input.location())",
        GeneratedReaderScalarPlan.of(scalar("int")).parseExpression("value"));
    assertEquals("value", GeneratedReaderScalarPlan.of(union()).parseExpression("value"));
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlDatatypes.parseList(\"NMTOKEN\", value, input, "
            + "input.location(), String.class)",
        GeneratedReaderScalarPlan.of(list(scalar("NMTOKEN"))).parseExpression("value"));
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlDatatypes.parseList(\"IDREF\", value, input, "
            + "input.location(), String.class)",
        GeneratedReaderScalarPlan.of(scalar("IDREFS")).parseExpression("value"));
  }

  @Test
  void plansScalarJavaTypesAndDatatypeClassLiterals() {
    assertEquals("String", GeneratedReaderScalarPlan.of(scalar("unknown")).javaType());
    assertEquals(
        "java.util.List<String>", GeneratedReaderScalarPlan.of(scalar("IDREFS")).javaType());
    assertEquals(
        "java.math.BigInteger", GeneratedReaderScalarPlan.of(scalar("integer")).javaType());
    assertEquals(
        "java.math.BigDecimal", GeneratedReaderScalarPlan.of(scalar("decimal")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlDuration",
        GeneratedReaderScalarPlan.of(scalar("duration")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlDateTime",
        GeneratedReaderScalarPlan.of(scalar("dateTime")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlDate",
        GeneratedReaderScalarPlan.of(scalar("date")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlTime",
        GeneratedReaderScalarPlan.of(scalar("time")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlGYear",
        GeneratedReaderScalarPlan.of(scalar("gYear")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlGYearMonth",
        GeneratedReaderScalarPlan.of(scalar("gYearMonth")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlGMonth",
        GeneratedReaderScalarPlan.of(scalar("gMonth")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlGMonthDay",
        GeneratedReaderScalarPlan.of(scalar("gMonthDay")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlGDay",
        GeneratedReaderScalarPlan.of(scalar("gDay")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlBinary",
        GeneratedReaderScalarPlan.of(scalar("hexBinary")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlAnyUri",
        GeneratedReaderScalarPlan.of(scalar("anyURI")).javaType());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlQName",
        GeneratedReaderScalarPlan.of(scalar("QName")).javaType());

    assertEquals(
        "String.class", GeneratedReaderScalarPlan.of(list(scalar("ID"))).datatypeClassLiteral());
    assertEquals(
        "Boolean.class", GeneratedReaderScalarPlan.of(scalar("boolean")).datatypeClassLiteral());
    assertEquals(
        "java.math.BigDecimal.class",
        GeneratedReaderScalarPlan.of(scalar("decimal")).datatypeClassLiteral());
    assertEquals(
        "Float.class", GeneratedReaderScalarPlan.of(scalar("float")).datatypeClassLiteral());
    assertEquals(
        "Double.class", GeneratedReaderScalarPlan.of(scalar("double")).datatypeClassLiteral());
    assertEquals(
        "java.math.BigInteger.class",
        GeneratedReaderScalarPlan.of(scalar("integer")).datatypeClassLiteral());
    assertEquals("Long.class", GeneratedReaderScalarPlan.of(scalar("long")).datatypeClassLiteral());
    assertEquals(
        "Integer.class", GeneratedReaderScalarPlan.of(scalar("int")).datatypeClassLiteral());
    assertEquals(
        "Short.class", GeneratedReaderScalarPlan.of(scalar("short")).datatypeClassLiteral());
    assertEquals("Byte.class", GeneratedReaderScalarPlan.of(scalar("byte")).datatypeClassLiteral());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlDate.class",
        GeneratedReaderScalarPlan.of(scalar("date")).datatypeClassLiteral());
    assertEquals(
        "io.github.mundanej.mxjb.runtime.XmlBinary.class",
        GeneratedReaderScalarPlan.of(scalar("base64Binary")).datatypeClassLiteral());
    assertEquals(
        "Object.class", GeneratedReaderScalarPlan.of(scalar("unknown")).datatypeClassLiteral());
  }

  private BindingField field(String kind, String localName, BindingTypeReference type, int order) {
    return field(kind, localName, type, order, BindingValueSemantics.NONE);
  }

  private BindingField field(
      String kind,
      String localName,
      BindingTypeReference type,
      int order,
      BindingValueSemantics semantics) {
    return new BindingField(
        kind, q(localName), localName, type, required(), order, true, semantics);
  }

  private BindingCardinality required() {
    return new BindingCardinality("required", 1, "1");
  }

  private BindingTypeReference scalar(String name) {
    return new BindingTypeReference("scalar", name);
  }

  private BindingTypeReference list(BindingTypeReference itemType) {
    return new BindingTypeReference("list", itemType.name(), null, itemType, List.of());
  }

  private BindingTypeReference union() {
    return new BindingTypeReference("union", "string", null, null, List.of(scalar("string")));
  }

  private BindingTypeReference xmlAttribute() {
    return new BindingTypeReference("xmlAttribute", "io.github.mundanej.mxjb.runtime.XmlAttribute");
  }

  private SchemaQName q(String localName) {
    return new SchemaQName("urn:orders", localName);
  }
}
