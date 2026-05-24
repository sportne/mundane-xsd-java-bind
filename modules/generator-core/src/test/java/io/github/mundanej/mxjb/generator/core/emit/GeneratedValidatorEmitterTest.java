package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.bind.BindingCardinality;
import io.github.mundanej.mxjb.generator.core.bind.BindingChoice;
import io.github.mundanej.mxjb.generator.core.bind.BindingChoiceBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingContent;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentGroup;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentPosition;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingModel;
import io.github.mundanej.mxjb.generator.core.bind.BindingResult;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingSimpleRestriction;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.BindingValidationPlan;
import io.github.mundanej.mxjb.generator.core.bind.BindingValueSemantics;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityConstraint;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityField;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityPath;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityStep;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import io.github.mundanej.mxjb.runtime.ValidationError;
import io.github.mundanej.mxjb.runtime.ValidationResult;
import io.github.mundanej.mxjb.runtime.XmlAttribute;
import io.github.mundanej.mxjb.runtime.XmlEventKind;
import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.XmlFragment;
import io.github.mundanej.mxjb.runtime.XmlFragmentText;
import io.github.mundanej.mxjb.runtime.XmlLocation;
import io.github.mundanej.mxjb.runtime.XmlName;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GeneratedValidatorEmitterTest {
  @TempDir private Path tempDirectory;

  @Test
  void emitsRootStaticValidatorSourceForSupportedModel() {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field("attribute", "version", scalar("string"), optional(), 0),
                        field("element", "id", scalar("string"), required(), 1)))));

    GeneratedValidatorEmissionResult result = new GeneratedValidatorEmitter().emit(model);

    assertFalse(result.hasErrors());
    assertEquals(
        Path.of("com/example/orders/xml/OrderXmlValidator.java"),
        result.sources().getFirst().relativePath());
    String source = result.sources().getFirst().sourceText();
    assertTrue(source.contains("public final class OrderXmlValidator"));
    assertTrue(source.contains("public static io.github.mundanej.mxjb.runtime.ValidationResult"));
    assertTrue(source.contains("com.example.orders.Order value"));
    assertTrue(source.contains("io.github.mundanej.mxjb.runtime.XmlEventReader input"));
    assertTrue(source.contains("MXJB-GV-001"));
    assertFalse(source.contains("\nimport "));
    assertFalse(source.contains("java.lang.reflect"));
    assertFalse(source.contains("java.lang.invoke"));
    assertFalse(source.contains("ServiceLoader"));
    assertFalse(source.contains("ClassLoader"));
    assertFalse(source.contains("Class.forName"));
    assertFalse(source.contains("Proxy"));
    assertFalse(source.contains("ObjectInputStream"));
    assertFalse(source.contains("ObjectOutputStream"));
    assertFalse(source.contains("Serializable"));
    assertFalse(source.contains("Externalizable"));
    assertFalse(source.contains("ProcessBuilder"));
    assertFalse(source.contains("System.exit"));
    assertFalse(source.contains("javax.xml"));
  }

  @Test
  void generatedValidatorUsesQualifiedSupportTypesToAvoidModelNameCollisions()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("xmlEventReader", model("com.example.collide.XmlEventReader"))),
            List.of(
                type(
                    "com.example.collide",
                    "XmlEventReader",
                    List.of(
                        field(
                            "element",
                            "xmlName",
                            model("com.example.collide.XmlName"),
                            required(),
                            1))),
                type(
                    "com.example.collide",
                    "XmlName",
                    List.of(field("element", "value", scalar("string"), required(), 1)))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);
    GeneratedValidatorEmissionResult validatorResult = new GeneratedValidatorEmitter().emit(model);
    String validatorSource = validatorResult.sources().getFirst().sourceText();

    assertFalse(validatorSource.contains("\nimport "));
    assertTrue(
        validatorSource.contains("com.example.collide.xml.XmlEventReaderXmlReader.read(input)"));
    assertTrue(validatorSource.contains("com.example.collide.XmlEventReader value"));

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> rootClass = compiledSources.load("com.example.collide.XmlEventReader");
      Class<?> nestedClass = compiledSources.load("com.example.collide.XmlName");
      Class<?> validatorClass =
          compiledSources.load("com.example.collide.xml.XmlEventReaderXmlValidator");
      Object nested = nestedClass.getConstructor(String.class).newInstance("value-1");
      Object root = rootClass.getConstructor(nestedClass).newInstance(nested);

      ValidationResult result =
          (ValidationResult) validatorClass.getMethod("validate", rootClass).invoke(null, root);

      assertTrue(result.isValid());
    }
  }

  @Test
  void generatedValidatorCompilesAndValidatesObjectsAndXmlInputs()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model = orderModel();
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> lineClass = compiledSources.load("com.example.lines.Line");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object firstLine = lineClass.getConstructor(String.class).newInstance("SKU-1");
      Object secondLine = lineClass.getConstructor(String.class).newInstance("SKU-2");
      Object order =
          orderClass
              .getConstructor(Optional.class, Integer.class, Optional.class, List.class)
              .newInstance(
                  Optional.of("v1"), 7, Optional.of("gift"), List.of(firstLine, secondLine));

      ValidationResult objectResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, order);
      ValidationResult xmlResult =
          (ValidationResult)
              validatorClass.getMethod("validate", XmlEventReader.class).invoke(null, orderInput());

      assertTrue(objectResult.isValid());
      assertTrue(xmlResult.isValid());
    }
  }

  @Test
  void generatedValidatorReportsObjectCardinalityAndNestedErrors()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field(
                            "element", "detail", model("com.example.orders.Detail"), required(), 1),
                        field("element", "line", scalar("string"), list(1, "2"), 2))),
                type(
                    "com.example.orders",
                    "Detail",
                    List.of(field("element", "tag", scalar("string"), list(1, "unbounded"), 1)))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> detailClass = compiledSources.load("com.example.orders.Detail");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object detail = detailClass.getConstructor(List.class).newInstance((Object) List.of());
      Object tooFewLines =
          orderClass.getConstructor(detailClass, List.class).newInstance(detail, List.of());
      Object tooManyLines =
          orderClass
              .getConstructor(detailClass, List.class)
              .newInstance(detail, List.of("A", "B", "C"));

      ValidationResult tooFewResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, tooFewLines);
      ValidationResult tooManyResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, tooManyLines);

      assertEquals(List.of("MXJB-GV-002", "MXJB-GV-002"), codes(tooFewResult));
      assertEquals(List.of("MXJB-GV-002", "MXJB-GV-003"), codes(tooManyResult));
      assertEquals(
          List.of("Too few values for tag.", "Too many values for line."),
          tooManyResult.errors().stream().map(ValidationError::message).toList());
      assertEquals(XmlLocation.UNKNOWN, tooManyResult.errors().getFirst().location());
    }
  }

  @Test
  void generatedValidatorEnforcesIdentityConstraints()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    SchemaIrIdentityConstraint key =
        new SchemaIrIdentityConstraint(
            "key",
            schemaName("lineSkuKey"),
            null,
            List.of(identityPath(elementStep("line"))),
            List.of(identityField(identityPath(elementStep("sku")))));
    SchemaIrIdentityConstraint keyref =
        new SchemaIrIdentityConstraint(
            "keyref",
            schemaName("referenceSkuKeyref"),
            schemaName("lineSkuKey"),
            List.of(identityPath(elementStep("reference"))),
            List.of(identityField(identityPath(elementStep("sku")))));
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"), List.of(key, keyref))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field("element", "line", model("com.example.orders.Line"), list(), 1),
                        field(
                            "element",
                            "reference",
                            model("com.example.orders.Reference"),
                            list(),
                            2))),
                type(
                    "com.example.orders",
                    "Line",
                    List.of(field("element", "sku", scalar("string"), required(), 1))),
                type(
                    "com.example.orders",
                    "Reference",
                    List.of(field("element", "sku", scalar("string"), required(), 1)))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> lineClass = compiledSources.load("com.example.orders.Line");
      Class<?> referenceClass = compiledSources.load("com.example.orders.Reference");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object firstLine = lineClass.getConstructor(String.class).newInstance("SKU-1");
      Object secondLine = lineClass.getConstructor(String.class).newInstance("SKU-2");
      Object duplicateLine = lineClass.getConstructor(String.class).newInstance("SKU-1");
      Object duplicateReference = referenceClass.getConstructor(String.class).newInstance("SKU-1");
      Object validReference = referenceClass.getConstructor(String.class).newInstance("SKU-2");
      Object danglingReference = referenceClass.getConstructor(String.class).newInstance("SKU-9");
      Object valid =
          orderClass
              .getConstructor(List.class, List.class)
              .newInstance(List.of(firstLine, secondLine), List.of(validReference));
      Object duplicate =
          orderClass
              .getConstructor(List.class, List.class)
              .newInstance(List.of(firstLine, duplicateLine), List.of(duplicateReference));
      Object dangling =
          orderClass
              .getConstructor(List.class, List.class)
              .newInstance(List.of(firstLine), List.of(danglingReference));

      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, valid);
      ValidationResult duplicateResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, duplicate);
      ValidationResult danglingResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, dangling);

      assertTrue(validResult.isValid());
      assertEquals(List.of("MXJB-GV-011"), codes(duplicateResult));
      assertEquals(List.of("MXJB-GV-012"), codes(danglingResult));
    }
  }

  @Test
  void generatedValidatorTreatsFieldUnionAlternativesAsOneTupleColumn()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    SchemaIrIdentityConstraint key =
        new SchemaIrIdentityConstraint(
            "key",
            schemaName("lineCodeKey"),
            null,
            List.of(identityPath(elementStep("line"))),
            List.of(
                identityField(
                    identityPath(attributeStep("sku")), identityPath(attributeStep("code")))));
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"), List.of(key))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(field("element", "line", model("com.example.orders.Line"), list(), 1))),
                type(
                    "com.example.orders",
                    "Line",
                    List.of(
                        field("attribute", "sku", scalar("string"), optional(), 0),
                        field("attribute", "code", scalar("string"), optional(), 0)))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> lineClass = compiledSources.load("com.example.orders.Line");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object skuLine =
          lineClass
              .getConstructor(Optional.class, Optional.class)
              .newInstance(Optional.of("A"), Optional.empty());
      Object codeLine =
          lineClass
              .getConstructor(Optional.class, Optional.class)
              .newInstance(Optional.empty(), Optional.of("B"));
      Object duplicateCodeLine =
          lineClass
              .getConstructor(Optional.class, Optional.class)
              .newInstance(Optional.empty(), Optional.of("A"));
      Object valid = orderClass.getConstructor(List.class).newInstance(List.of(skuLine, codeLine));
      Object duplicate =
          orderClass.getConstructor(List.class).newInstance(List.of(skuLine, duplicateCodeLine));

      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, valid);
      ValidationResult duplicateResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, duplicate);

      assertTrue(validResult.isValid());
      assertEquals(List.of("MXJB-GV-011"), codes(duplicateResult));
    }
  }

  @Test
  void generatedValidatorNormalizesDecimalIdentityValues()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    SchemaIrIdentityConstraint key =
        new SchemaIrIdentityConstraint(
            "key",
            schemaName("lineAmountKey"),
            null,
            List.of(identityPath(elementStep("line"))),
            List.of(identityField(identityPath(elementStep("amount")))));
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"), List.of(key))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(field("element", "line", model("com.example.orders.Line"), list(), 1))),
                type(
                    "com.example.orders",
                    "Line",
                    List.of(field("element", "amount", scalar("decimal"), required(), 1)))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> lineClass = compiledSources.load("com.example.orders.Line");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object firstLine =
          lineClass
              .getConstructor(java.math.BigDecimal.class)
              .newInstance(new java.math.BigDecimal("1.0"));
      Object secondLine =
          lineClass
              .getConstructor(java.math.BigDecimal.class)
              .newInstance(new java.math.BigDecimal("1.00"));
      Object order =
          orderClass.getConstructor(List.class).newInstance(List.of(firstLine, secondLine));

      ValidationResult result =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, order);

      assertEquals(List.of("MXJB-GV-011"), codes(result));
    }
  }

  @Test
  void generatedValidatorReportsFacetErrorsForRestrictedScalars()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field(
                            "element", "code", restrictedString(3, 8, "[A-Z0-9]+"), required(), 1),
                        field("element", "priority", restrictedInt("1", "9"), required(), 2)))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object valid = orderClass.getConstructor(String.class, Integer.class).newInstance("AB12", 3);
      Object invalid = orderClass.getConstructor(String.class, Integer.class).newInstance("ab", 12);

      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, valid);
      ValidationResult invalidResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, invalid);

      assertTrue(validResult.isValid());
      assertEquals(List.of("MXJB-GV-005", "MXJB-GV-007", "MXJB-GV-006"), codes(invalidResult));
    }
  }

  @Test
  void generatedValidatorReportsDatatypeValueSpaceErrorsWithoutExplicitFacets()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("sample", model("com.example.orders.Sample"))),
            List.of(
                type(
                    "com.example.orders",
                    "Sample",
                    List.of(
                        field("element", "name", scalar("NCName"), required(), 1),
                        field("element", "small", scalar("unsignedByte"), required(), 2),
                        field("element", "tokens", scalar("NMTOKENS"), required(), 3)))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> sampleClass = compiledSources.load("com.example.orders.Sample");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.SampleXmlValidator");
      Object valid =
          sampleClass
              .getConstructor(String.class, Short.class, List.class)
              .newInstance("name", (short) 255, List.of("A", "B"));
      Object invalid =
          sampleClass
              .getConstructor(String.class, Short.class, List.class)
              .newInstance("p:name", (short) 300, List.of(""));

      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", sampleClass).invoke(null, valid);
      ValidationResult invalidResult =
          (ValidationResult)
              validatorClass.getMethod("validate", sampleClass).invoke(null, invalid);

      assertTrue(validResult.isValid());
      assertEquals(List.of("MXJB-GV-004", "MXJB-GV-004", "MXJB-GV-004"), codes(invalidResult));
    }
  }

  @Test
  void generatedValidatorRecursesIntoChoiceModelBranches()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model = choiceOrderModel();
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> detailClass = compiledSources.load("com.example.orders.Detail");
      Class<?> domesticChoiceClass = compiledSources.load("com.example.orders.DomesticChoice");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object detail = detailClass.getConstructor(String.class).newInstance("US");
      Object choice = domesticChoiceClass.getConstructor(detailClass).newInstance(detail);
      Object order =
          orderClass
              .getConstructor(String.class, Optional.class)
              .newInstance("A-1", Optional.of(choice));

      ValidationResult result =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, order);

      assertTrue(result.isValid());
    }
  }

  @Test
  void generatedValidatorChecksGroupedSequenceAndAllSemantics()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(groupedSequenceField(), groupedAllField()))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> idContentClass = compiledSources.load("com.example.orders.IdContent");
      Class<?> lineContentClass = compiledSources.load("com.example.orders.LineContent");
      Class<?> allIdContentClass = compiledSources.load("com.example.orders.AllIdContent");
      Class<?> noteContentClass = compiledSources.load("com.example.orders.AllNoteContent");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object valid =
          orderClass
              .getConstructor(List.class, List.class)
              .newInstance(
                  List.of(
                      idContentClass.getConstructor(String.class).newInstance("A-1"),
                      lineContentClass.getConstructor(String.class).newInstance("L-1"),
                      idContentClass.getConstructor(String.class).newInstance("A-2"),
                      lineContentClass.getConstructor(String.class).newInstance("L-2")),
                  List.of());
      Object validAllPresent =
          orderClass
              .getConstructor(List.class, List.class)
              .newInstance(
                  List.of(),
                  List.of(
                      allIdContentClass.getConstructor(String.class).newInstance("A-1"),
                      noteContentClass.getConstructor(String.class).newInstance("gift")));
      Object invalidSequence =
          orderClass
              .getConstructor(List.class, List.class)
              .newInstance(
                  List.of(
                      idContentClass.getConstructor(String.class).newInstance("A-1"),
                      idContentClass.getConstructor(String.class).newInstance("A-2")),
                  List.of());
      Object invalidAll =
          orderClass
              .getConstructor(List.class, List.class)
              .newInstance(
                  List.of(),
                  List.of(noteContentClass.getConstructor(String.class).newInstance("gift")));

      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, valid);
      ValidationResult validAllPresentResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, validAllPresent);
      ValidationResult invalidSequenceResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, invalidSequence);
      ValidationResult invalidAllResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, invalidAll);

      assertTrue(validResult.isValid());
      assertTrue(validAllPresentResult.isValid());
      assertTrue(codes(invalidSequenceResult).contains("MXJB-GV-003"));
      assertEquals(List.of("MXJB-GV-002"), codes(invalidAllResult));
    }
  }

  @Test
  void generatedValidatorUsesAutomataPositionsForNestedChoiceInGroupedSequence()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(
                type("com.example.orders", "Order", List.of(groupedSequenceWithChoiceField()))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> cardContentClass = compiledSources.load("com.example.orders.CardContent");
      Class<?> cashContentClass = compiledSources.load("com.example.orders.CashContent");
      Class<?> lineContentClass = compiledSources.load("com.example.orders.LineContent");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object valid =
          orderClass
              .getConstructor(List.class)
              .newInstance(
                  List.of(
                      cardContentClass.getConstructor(String.class).newInstance("visa"),
                      lineContentClass.getConstructor(String.class).newInstance("L-1"),
                      cashContentClass.getConstructor(String.class).newInstance("cash"),
                      lineContentClass.getConstructor(String.class).newInstance("L-2")));
      Object invalid =
          orderClass
              .getConstructor(List.class)
              .newInstance(
                  List.of(
                      cardContentClass.getConstructor(String.class).newInstance("visa"),
                      cashContentClass.getConstructor(String.class).newInstance("cash"),
                      lineContentClass.getConstructor(String.class).newInstance("L-1")));

      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, valid);
      ValidationResult invalidResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, invalid);

      assertTrue(validResult.isValid());
      assertTrue(codes(invalidResult).contains("MXJB-GV-003"));
    }
  }

  @Test
  void generatedValidatorChecksGroupedChoiceOccurrenceCardinality()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(type("com.example.orders", "Order", List.of(groupedChoiceField()))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> idContentClass = compiledSources.load("com.example.orders.IdContent");
      Class<?> noteContentClass = compiledSources.load("com.example.orders.NoteContent");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object valid =
          orderClass
              .getConstructor(List.class)
              .newInstance(List.of(idContentClass.getConstructor(String.class).newInstance("A-1")));
      Object invalid =
          orderClass
              .getConstructor(List.class)
              .newInstance(
                  List.of(
                      idContentClass.getConstructor(String.class).newInstance("A-1"),
                      noteContentClass.getConstructor(String.class).newInstance("gift")));

      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, valid);
      ValidationResult invalidResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, invalid);

      assertTrue(validResult.isValid());
      assertEquals(List.of("MXJB-GV-003"), codes(invalidResult));
    }
  }

  @Test
  void generatedValidatorConvertsXmlReaderDiagnostics()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model = orderModel();
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");

      assertXmlValidationDiagnostic(validatorClass, wrongRootNamespaceInput(), "MXJB-GR-001");
      assertXmlValidationDiagnostic(validatorClass, missingRequiredElementInput(), "MXJB-GR-004");
      assertXmlValidationDiagnostic(validatorClass, repeatedSingletonInput(), "MXJB-GR-005");
      assertXmlValidationDiagnostic(validatorClass, outOfOrderInput(), "MXJB-GR-002");
      assertXmlValidationDiagnostic(validatorClass, invalidScalarInput(), "MXJB-DT-001");
    }
  }

  @Test
  void generatedValidatorReportsSemanticValidationInDeterministicOrder()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model = semanticValidationOrderModel();
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.SemanticOrder");
      Class<?> validatorClass =
          compiledSources.load("com.example.orders.xml.SemanticOrderXmlValidator");
      Object valid =
          orderClass
              .getConstructor(String.class, String.class, Optional.class)
              .newInstance("NEW", "1", Optional.empty());
      Object invalid =
          orderClass
              .getConstructor(String.class, String.class, Optional.class)
              .newInstance("BAD", "2", Optional.of("A-1"));

      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, valid);
      ValidationResult invalidResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, invalid);

      assertTrue(validResult.isValid());
      assertEquals(List.of("MXJB-GV-004", "MXJB-GV-009"), codes(invalidResult));
      assertEquals(
          List.of(
              "Value does not satisfy datatype facets.", "Value does not match the fixed value."),
          invalidResult.errors().stream().map(ValidationError::message).toList());
      assertEquals(XmlLocation.UNKNOWN, invalidResult.errors().getFirst().location());
    }
  }

  @Test
  void generatedValidatorAppliesStrictWildcardSchemaKnownElementValidation()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(type("com.example.orders", "Order", List.of(strictKnownWildcardField()))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object valid =
          orderClass.getConstructor(List.class).newInstance(List.of(fragment("discount", "10")));
      Object invalidValue =
          orderClass
              .getConstructor(List.class)
              .newInstance(List.of(fragment("discount", "not-int")));
      Object invalidFacet =
          orderClass.getConstructor(List.class).newInstance(List.of(fragment("discount", "99")));
      Object unknown =
          orderClass.getConstructor(List.class).newInstance(List.of(fragment("unknown", "10")));

      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, valid);
      ValidationResult invalidValueResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, invalidValue);
      ValidationResult unknownResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, unknown);
      ValidationResult invalidFacetResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, invalidFacet);

      assertTrue(validResult.isValid());
      assertEquals(List.of("MXJB-GV-004"), codes(invalidValueResult));
      assertEquals(List.of("MXJB-GV-004"), codes(invalidFacetResult));
      assertEquals(List.of("MXJB-GV-009"), codes(unknownResult));
    }
  }

  @Test
  void generatedValidatorAppliesStrictWildcardSchemaKnownAttributeFacets()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(root("order", model("com.example.orders.Order"))),
            List.of(type("com.example.orders", "Order", List.of(strictKnownAnyAttributeField()))));
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.Order");
      Class<?> validatorClass = compiledSources.load("com.example.orders.xml.OrderXmlValidator");
      Object valid =
          orderClass
              .getConstructor(List.class)
              .newInstance(List.of(new XmlAttribute(new XmlName("urn:orders", "rating"), "4")));
      Object invalidFacet =
          orderClass
              .getConstructor(List.class)
              .newInstance(List.of(new XmlAttribute(new XmlName("urn:orders", "rating"), "9")));

      ValidationResult validResult =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, valid);
      ValidationResult invalidFacetResult =
          (ValidationResult)
              validatorClass.getMethod("validate", orderClass).invoke(null, invalidFacet);

      assertTrue(validResult.isValid());
      assertEquals(List.of("MXJB-GV-004"), codes(invalidFacetResult));
    }
  }

  @Test
  void generatedValidatorConvertsSemanticXmlReaderDiagnosticsWithLocations()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model = semanticValidationOrderModel();
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> validatorClass =
          compiledSources.load("com.example.orders.xml.SemanticOrderXmlValidator");

      assertXmlValidationDiagnostic(validatorClass, semanticNilContentInput(), "MXJB-GR-009");
      assertXmlValidationDiagnostic(validatorClass, semanticFixedMismatchInput(), "MXJB-GR-008");
    }
  }

  @Test
  void generatedValidatorRecursesIntoSubstitutionBranchValues()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model = substitutionValidationOrderModel();
    List<GeneratedJavaSource> sources = generatedModelReaderValidatorSources(model);

    try (GeneratedSourceVerifier.CompiledSources compiledSources =
        new GeneratedSourceVerifier(tempDirectory).compile(sources)) {
      Class<?> orderClass = compiledSources.load("com.example.orders.SubstitutionOrder");
      Class<?> cardPaymentClass = compiledSources.load("com.example.orders.CardPayment");
      Class<?> branchClass = compiledSources.load("com.example.orders.CardPaymentBranch");
      Class<?> validatorClass =
          compiledSources.load("com.example.orders.xml.SubstitutionOrderXmlValidator");
      Object branch =
          branchClass
              .getConstructor(cardPaymentClass)
              .newInstance(cardPaymentClass.getConstructor(String.class).newInstance("42"));
      Object order = orderClass.getConstructor(Optional.class).newInstance(Optional.of(branch));

      ValidationResult result =
          (ValidationResult) validatorClass.getMethod("validate", orderClass).invoke(null, order);

      assertEquals(List.of("MXJB-GV-005"), codes(result));
      assertEquals(
          List.of("Value length is outside the accepted range."),
          result.errors().stream().map(ValidationError::message).toList());
    }
  }

  @Test
  void emitsDeterministicSourcesSortedByRootName() {
    BindingModel model =
        new BindingModel(
            List.of(
                root("zeta", model("com.example.orders.Zeta")),
                root("alpha", model("com.example.orders.Alpha"))),
            List.of(
                type("com.example.orders", "Zeta", List.of()),
                type("com.example.orders", "Alpha", List.of())));

    GeneratedValidatorEmissionResult first = new GeneratedValidatorEmitter().emit(model);
    GeneratedValidatorEmissionResult second = new GeneratedValidatorEmitter().emit(model);

    assertFalse(first.hasErrors());
    new GeneratedSourceVerifier(tempDirectory)
        .assertDeterministic(first.sources(), second.sources());
    assertEquals(
        List.of(
            Path.of("com/example/orders/xml/AlphaXmlValidator.java"),
            Path.of("com/example/orders/xml/ZetaXmlValidator.java")),
        first.sources().stream().map(GeneratedJavaSource::relativePath).toList());
  }

  @Test
  void propagatesBindingDiagnosticsWithoutSources() {
    SchemaDiagnostic diagnostic =
        new SchemaDiagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL, "binding", "Invalid binding model.");
    BindingResult bindingResult = BindingResult.empty(List.of(diagnostic));

    GeneratedValidatorEmissionResult result = new GeneratedValidatorEmitter().emit(bindingResult);

    assertEquals(List.of(diagnostic), result.diagnostics());
    assertTrue(result.sources().isEmpty());
  }

  @Test
  void rejectsUnsupportedValidatorModelsWithoutPartialSources() {
    BindingModel model =
        new BindingModel(
            List.of(root("order", scalar("string"))),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field(
                            "attribute", "owner", model("com.example.orders.Owner"), required(), 0),
                        field("attribute", "tag", scalar("string"), list(), 0),
                        field("element", "date", scalar("unsupported"), required(), 1))),
                type("com.example.orders", "Owner", List.of())));

    GeneratedValidatorEmissionResult result = new GeneratedValidatorEmitter().emit(model);

    assertEquals(
        List.of(
            DiagnosticCode.SCHEMA_VALIDATOR_EMISSION_INVALID_MODEL,
            DiagnosticCode.SCHEMA_VALIDATOR_EMISSION_INVALID_MODEL,
            DiagnosticCode.SCHEMA_VALIDATOR_EMISSION_INVALID_MODEL,
            DiagnosticCode.SCHEMA_VALIDATOR_EMISSION_INVALID_MODEL),
        result.diagnostics().stream().map(SchemaDiagnostic::code).toList());
    assertTrue(result.sources().isEmpty());
  }

  private List<GeneratedJavaSource> generatedModelReaderValidatorSources(BindingModel model) {
    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(model);
    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(model);
    GeneratedValidatorEmissionResult validatorResult = new GeneratedValidatorEmitter().emit(model);
    assertFalse(modelResult.hasErrors());
    assertFalse(readerResult.hasErrors());
    assertFalse(validatorResult.hasErrors());
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.addAll(modelResult.sources());
    sources.addAll(readerResult.sources());
    sources.addAll(validatorResult.sources());
    return sources;
  }

  private void assertXmlValidationDiagnostic(
      Class<?> validatorClass, XmlEventReader input, String code)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    ValidationResult result =
        (ValidationResult)
            validatorClass.getMethod("validate", XmlEventReader.class).invoke(null, input);
    assertFalse(result.isValid());
    assertEquals(List.of(code), codes(result));
    assertEquals("test.xml", result.errors().getFirst().location().systemId());
  }

  private List<String> codes(ValidationResult result) {
    return result.errors().stream().map(ValidationError::code).toList();
  }

  private BindingModel orderModel() {
    return new BindingModel(
        List.of(root("order", model("com.example.orders.Order"))),
        List.of(
            type(
                "com.example.orders",
                "Order",
                List.of(
                    field("attribute", "version", scalar("string"), optional(), 0),
                    field("element", "id", scalar("int"), required(), 1),
                    field("element", "note", scalar("string"), optional(), 2),
                    field(
                        "element",
                        new SchemaQName("urn:lines", "line"),
                        "line",
                        model("com.example.lines.Line"),
                        list(),
                        3))),
            type(
                "com.example.lines",
                "Line",
                List.of(
                    field(
                        "element",
                        new SchemaQName("urn:lines", "sku"),
                        "sku",
                        scalar("string"),
                        required(),
                        1)))));
  }

  private BindingModel choiceOrderModel() {
    return new BindingModel(
        List.of(root("order", model("com.example.orders.Order"))),
        List.of(
            type(
                "com.example.orders",
                "Order",
                List.of(field("element", "id", scalar("string"), required(), 1), choiceField())),
            type(
                "com.example.orders",
                "Detail",
                List.of(field("element", "country", scalar("string"), required(), 1)))));
  }

  private BindingModel semanticValidationOrderModel() {
    return new BindingModel(
        List.of(root("semanticOrder", model("com.example.orders.SemanticOrder"))),
        List.of(
            type(
                "com.example.orders",
                "SemanticOrder",
                List.of(
                    field(
                        "attribute",
                        "status",
                        restrictedStatus(),
                        required(),
                        0,
                        new BindingValueSemantics(false, "NEW", null)),
                    field(
                        "attribute",
                        "version",
                        scalar("string"),
                        required(),
                        1,
                        new BindingValueSemantics(false, null, "1")),
                    field(
                        "element",
                        "code",
                        scalar("string"),
                        required(),
                        2,
                        new BindingValueSemantics(true, null, null))))));
  }

  private BindingModel substitutionValidationOrderModel() {
    BindingJavaName choiceName = new BindingJavaName("com.example.orders", "PaymentSubstitution");
    BindingChoice choice =
        new BindingChoice(
            choiceName,
            List.of(
                new BindingChoiceBranch(
                    schemaName("cardPayment"),
                    "cardPayment",
                    model("com.example.orders.CardPayment"),
                    new BindingJavaName("com.example.orders", "CardPaymentBranch"))),
            "substitution");
    return new BindingModel(
        List.of(root("substitutionOrder", model("com.example.orders.SubstitutionOrder"))),
        List.of(
            type(
                "com.example.orders",
                "SubstitutionOrder",
                List.of(
                    new BindingField(
                        "choice",
                        schemaName("payment"),
                        "payment",
                        new BindingTypeReference("choice", choiceName.qualifiedName()),
                        optional(),
                        1,
                        false,
                        choice))),
            type(
                "com.example.orders",
                "CardPayment",
                List.of(field("element", "cardLast4", fixedLengthString(4), required(), 1)))));
  }

  private EventXmlReader orderInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(
            XmlEventKind.START_ELEMENT,
            new XmlName("urn:orders", "order"),
            Map.of(new XmlName("urn:orders", "version"), "v1")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")),
        text("7"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "note")),
        text("gift"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "note")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "sku")),
        text("SKU-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "sku")),
        text("SKU-2"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader wrongRootNamespaceInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:wrong", "order")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:wrong", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader missingRequiredElementInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader repeatedSingletonInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")),
        text("7"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")),
        text("8"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader outOfOrderInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:lines", "sku")),
        text("SKU-1"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "sku")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:lines", "line")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "note")),
        text("late"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "note")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader invalidScalarInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.START_ELEMENT, new XmlName("urn:orders", "id")),
        text("not-an-int"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "id")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "order")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader semanticNilContentInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(
            XmlEventKind.START_ELEMENT,
            new XmlName("urn:orders", "semanticOrder"),
            Map.of(new XmlName("urn:orders", "version"), "1")),
        event(
            XmlEventKind.START_ELEMENT,
            new XmlName("urn:orders", "code"),
            Map.of(new XmlName("http://www.w3.org/2001/XMLSchema-instance", "nil"), "true")),
        text("not-empty"),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "code")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "semanticOrder")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private EventXmlReader semanticFixedMismatchInput() {
    return reader(
        event(XmlEventKind.START_DOCUMENT, null),
        event(
            XmlEventKind.START_ELEMENT,
            new XmlName("urn:orders", "semanticOrder"),
            Map.of(new XmlName("urn:orders", "version"), "2")),
        event(
            XmlEventKind.START_ELEMENT,
            new XmlName("urn:orders", "code"),
            Map.of(new XmlName("http://www.w3.org/2001/XMLSchema-instance", "nil"), "true")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "code")),
        event(XmlEventKind.END_ELEMENT, new XmlName("urn:orders", "semanticOrder")),
        event(XmlEventKind.END_DOCUMENT, null));
  }

  private BindingRootElement root(String localName, BindingTypeReference type) {
    return new BindingRootElement(schemaName(localName), type, required());
  }

  private BindingRootElement root(
      String localName,
      BindingTypeReference type,
      List<SchemaIrIdentityConstraint> identityConstraints) {
    return new BindingRootElement(schemaName(localName), type, required(), identityConstraints);
  }

  private SchemaIrIdentityPath identityPath(SchemaIrIdentityStep... steps) {
    return new SchemaIrIdentityPath(false, false, List.of(steps));
  }

  private SchemaIrIdentityField identityField(SchemaIrIdentityPath... alternatives) {
    return new SchemaIrIdentityField(List.of(alternatives));
  }

  private SchemaIrIdentityStep elementStep(String localName) {
    return new SchemaIrIdentityStep(schemaName(localName), false, false);
  }

  private SchemaIrIdentityStep attributeStep(String localName) {
    return new SchemaIrIdentityStep(schemaName(localName), false, true);
  }

  private BindingType type(String packageName, String simpleName, List<BindingField> fields) {
    return new BindingType(
        new BindingJavaName(packageName, simpleName),
        schemaName(simpleName),
        "record",
        fields,
        new BindingValidationPlan(List.of()));
  }

  private BindingField field(
      String kind,
      String localName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order) {
    return field(kind, schemaName(localName), localName, type, cardinality, order);
  }

  private BindingField field(
      String kind,
      SchemaQName xmlName,
      String javaName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order) {
    return new BindingField(
        kind, xmlName, javaName, type, cardinality, order, cardinality.minOccurs() > 0);
  }

  private BindingField field(
      String kind,
      String localName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order,
      BindingValueSemantics semantics) {
    return new BindingField(
        kind,
        schemaName(localName),
        localName,
        type,
        cardinality,
        order,
        cardinality.minOccurs() > 0,
        semantics);
  }

  private BindingField strictKnownWildcardField() {
    return new BindingField(
        "wildcard",
        new SchemaQName("", "*"),
        "wildcardContent",
        new BindingTypeReference("fragment", "io.github.mundanej.mxjb.runtime.XmlFragment"),
        list(),
        1,
        false,
        new io.github.mundanej.mxjb.generator.core.bind.BindingWildcard(
            new io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace(
                "explicit", List.of("urn:orders")),
            "strict",
            List.of(),
            List.of(
                new io.github.mundanej.mxjb.generator.core.bind.BindingWildcardElement(
                    schemaName("discount"), restrictedInt(null, "50"))),
            List.of()));
  }

  private BindingField strictKnownAnyAttributeField() {
    return new BindingField(
        "anyAttribute",
        new SchemaQName("", "*"),
        "wildcardAttributes",
        new BindingTypeReference("xmlAttribute", "io.github.mundanej.mxjb.runtime.XmlAttribute"),
        list(),
        0,
        false,
        new io.github.mundanej.mxjb.generator.core.bind.BindingWildcard(
            new io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace(
                "explicit", List.of("urn:orders")),
            "strict",
            List.of(),
            List.of(),
            List.of(
                new io.github.mundanej.mxjb.generator.core.bind.BindingWildcardAttribute(
                    schemaName("rating"), restrictedInt(null, "5")))));
  }

  private XmlFragment fragment(String localName, String text) {
    return new XmlFragment(
        new XmlName("urn:orders", localName), List.of(), List.of(new XmlFragmentText(text)));
  }

  private BindingField choiceField() {
    BindingJavaName choiceName = new BindingJavaName("com.example.orders", "OrderChoice");
    BindingChoice choice =
        new BindingChoice(
            choiceName,
            List.of(
                new BindingChoiceBranch(
                    schemaName("domestic"),
                    "domestic",
                    model("com.example.orders.Detail"),
                    new BindingJavaName("com.example.orders", "DomesticChoice")),
                new BindingChoiceBranch(
                    schemaName("international"),
                    "international",
                    scalar("string"),
                    new BindingJavaName("com.example.orders", "InternationalChoice"))));
    return new BindingField(
        "choice",
        schemaName("orderChoice"),
        "orderChoice",
        new BindingTypeReference("choice", choiceName.qualifiedName()),
        optional(),
        2,
        false,
        choice);
  }

  private BindingField groupedSequenceField() {
    BindingJavaName contentName = new BindingJavaName("com.example.orders", "OrderSequenceContent");
    BindingContentBranch id =
        contentBranch("element", "id", scalar("string"), optional(), 1, "IdContent");
    BindingContentBranch line =
        contentBranch("element", "line", scalar("string"), required(), 2, "LineContent");
    BindingContent content =
        new BindingContent(
            contentName,
            List.of(id, line),
            "sequence",
            List.of(new BindingContentGroup("sequence", list(), List.of(id, line))));
    return contentField("orderSequenceContent", contentName, content, list(), 1);
  }

  private BindingField groupedSequenceWithChoiceField() {
    BindingJavaName contentName = new BindingJavaName("com.example.orders", "OrderSequenceContent");
    BindingContentBranch card =
        contentBranch("element", "card", scalar("string"), optional(), 1, "CardContent");
    BindingContentBranch cash =
        contentBranch("element", "cash", scalar("string"), optional(), 1, "CashContent");
    BindingContentBranch line =
        contentBranch("element", "line", scalar("string"), required(), 2, "LineContent");
    BindingContent content =
        new BindingContent(
            contentName,
            List.of(card, cash, line),
            "sequence",
            List.of(
                new BindingContentGroup(
                    "choice",
                    list(),
                    List.of(card, cash),
                    List.of(new BindingContentPosition(list(), List.of(card, cash)))),
                new BindingContentGroup(
                    "sequence",
                    list(),
                    List.of(card, cash, line),
                    List.of(
                        new BindingContentPosition(required(), List.of(card, cash)),
                        new BindingContentPosition(required(), List.of(line))))));
    return contentField("orderSequenceContent", contentName, content, list(), 1);
  }

  private BindingField groupedAllField() {
    BindingJavaName contentName = new BindingJavaName("com.example.orders", "OrderAllContent");
    BindingContentBranch id =
        contentBranch("element", "id", scalar("string"), required(), 1, "AllIdContent");
    BindingContentBranch note =
        contentBranch("element", "note", scalar("string"), required(), 2, "AllNoteContent");
    BindingContent content =
        new BindingContent(
            contentName,
            List.of(id, note),
            "all",
            List.of(new BindingContentGroup("all", list(0, "1"), List.of(id, note))));
    return contentField("orderAllContent", contentName, content, list(0, "1"), 2);
  }

  private BindingField groupedChoiceField() {
    BindingJavaName contentName = new BindingJavaName("com.example.orders", "OrderChoiceContent");
    BindingContentBranch id =
        contentBranch("element", "id", scalar("string"), optional(), 1, "IdContent");
    BindingContentBranch note =
        contentBranch("element", "note", scalar("string"), optional(), 1, "NoteContent");
    BindingContent content =
        new BindingContent(
            contentName,
            List.of(id, note),
            "choice",
            List.of(new BindingContentGroup("choice", list(0, "1"), List.of(id, note))));
    return contentField("orderChoiceContent", contentName, content, list(0, "1"), 1);
  }

  private BindingField contentField(
      String fieldName,
      BindingJavaName contentName,
      BindingContent content,
      BindingCardinality cardinality,
      int order) {
    return new BindingField(
        "content",
        schemaName(fieldName),
        fieldName,
        new BindingTypeReference("choice", contentName.qualifiedName()),
        cardinality,
        order,
        cardinality.minOccurs() > 0,
        content);
  }

  private BindingContentBranch contentBranch(
      String kind,
      String localName,
      BindingTypeReference type,
      BindingCardinality cardinality,
      int order,
      String simpleName) {
    return new BindingContentBranch(
        kind,
        schemaName(localName),
        localName,
        type,
        new BindingJavaName("com.example.orders", simpleName),
        cardinality,
        order,
        null);
  }

  private BindingTypeReference scalar(String name) {
    return new BindingTypeReference("scalar", name);
  }

  private BindingTypeReference restrictedString(int minLength, int maxLength, String pattern) {
    return new BindingTypeReference(
        "scalar",
        "string",
        new BindingSimpleRestriction(
            "string",
            List.of(),
            null,
            minLength,
            maxLength,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(pattern)));
  }

  private BindingTypeReference fixedLengthString(int length) {
    return new BindingTypeReference(
        "scalar",
        "string",
        new BindingSimpleRestriction(
            "string", List.of(), length, null, null, null, null, null, null, null, null, null,
            List.of()));
  }

  private BindingTypeReference restrictedStatus() {
    return new BindingTypeReference(
        "scalar",
        "string",
        new BindingSimpleRestriction(
            "string",
            List.of("NEW", "CLOSED"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
  }

  private BindingTypeReference restrictedInt(String minInclusive, String maxInclusive) {
    return new BindingTypeReference(
        "scalar",
        "int",
        new BindingSimpleRestriction(
            "int",
            List.of(),
            null,
            null,
            null,
            minInclusive,
            maxInclusive,
            null,
            null,
            null,
            null,
            null,
            List.of()));
  }

  private BindingTypeReference model(String name) {
    return new BindingTypeReference("model", name);
  }

  private BindingCardinality required() {
    return new BindingCardinality("required", 1, "1");
  }

  private BindingCardinality optional() {
    return new BindingCardinality("optional", 0, "1");
  }

  private BindingCardinality list() {
    return new BindingCardinality("list", 0, "unbounded");
  }

  private BindingCardinality list(int minOccurs, String maxOccurs) {
    return new BindingCardinality("list", minOccurs, maxOccurs);
  }

  private SchemaQName schemaName(String localName) {
    return new SchemaQName("urn:orders", localName);
  }

  private EventXmlReader reader(Event... events) {
    return new EventXmlReader(List.of(events));
  }

  private Event event(XmlEventKind kind, XmlName name) {
    return new Event(kind, name, "", Map.of());
  }

  private Event event(XmlEventKind kind, XmlName name, Map<XmlName, String> attributes) {
    return new Event(kind, name, "", attributes);
  }

  private Event text(String value) {
    return new Event(XmlEventKind.TEXT, null, value, Map.of());
  }

  private record Event(
      XmlEventKind kind, XmlName name, String text, Map<XmlName, String> attributes) {}

  private static final class EventXmlReader implements XmlEventReader {
    private final List<Event> events;
    private int index;

    private EventXmlReader(List<Event> events) {
      this.events = events;
    }

    @Override
    public XmlEventKind kind() {
      return current().kind();
    }

    @Override
    public XmlName name() {
      return current().name();
    }

    @Override
    public String text() {
      return current().text();
    }

    @Override
    public int attributeCount() {
      return current().attributes().size();
    }

    @Override
    public XmlName attributeName(int indexValue) {
      return current().attributes().keySet().stream().toList().get(indexValue);
    }

    @Override
    public String attributeValue(int indexValue) {
      return current().attributes().values().stream().toList().get(indexValue);
    }

    @Override
    public XmlLocation location() {
      return new XmlLocation("test.xml", index + 1, 1);
    }

    @Override
    public boolean next() {
      if (index + 1 >= events.size()) {
        return false;
      }
      index++;
      return true;
    }

    private Event current() {
      return events.get(index);
    }
  }
}
