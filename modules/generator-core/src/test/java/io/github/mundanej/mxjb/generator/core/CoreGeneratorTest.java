package io.github.mundanej.mxjb.generator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CoreGeneratorTest {
  @TempDir private Path tempDirectory;

  @Test
  void generatesDeterministicPurchaseOrderSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("purchase-order.xsd", purchaseOrderSchema());
    Path output = tempDirectory.resolve("generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            null,
            "com.acme.generated",
            Map.of("urn:purchase", "com.acme.purchase"),
            List.of(),
            Map.of());

    GeneratorResult first = new CoreGenerator().generate(request);
    Map<Path, String> firstSources = readGeneratedSources(output, first.generatedSources());
    GeneratorResult second = new CoreGenerator().generate(request);
    Map<Path, String> secondSources = readGeneratedSources(output, second.generatedSources());

    assertTrue(first.successful(), first.diagnostics().toString());
    assertEquals(first.generatedSources(), second.generatedSources());
    assertEquals(firstSources, secondSources);
    assertEquals(
        List.of(
            Path.of("com/acme/purchase/Line.java"),
            Path.of("com/acme/purchase/Order.java"),
            Path.of("com/acme/purchase/xml/OrderXmlReader.java"),
            Path.of("com/acme/purchase/xml/OrderXmlValidator.java"),
            Path.of("com/acme/purchase/xml/OrderXmlWriter.java")),
        first.generatedSources());
    compileGeneratedSources(output, first.generatedSources());
  }

  @Test
  void generatesMultiNamespaceSourcesThroughCatalogAndLocalRoot() throws IOException {
    Path primary =
        writeSchema("schemas/order.xsd", orderSchema("https://example.invalid/line.xsd"));
    Path line = writeSchema("catalog/line.xsd", lineSchema());
    Path output = tempDirectory.resolve("generated-multi");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(primary),
            output,
            null,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders", "urn:lines", "com.acme.lines"),
            List.of(tempDirectory.resolve("catalog")),
            Map.of(URI.create("https://example.invalid/line.xsd"), line));

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    assertTrue(result.generatedSources().contains(Path.of("com/acme/orders/Order.java")));
    assertTrue(result.generatedSources().contains(Path.of("com/acme/lines/Line.java")));
    assertTrue(
        Files.readString(output.resolve("com/acme/orders/Order.java"), StandardCharsets.UTF_8)
            .contains("List<com.acme.lines.Line>"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void defaultProfileRejectsChoiceWithoutWritingSources() throws IOException {
    Path schema = writeSchema("choice-order.xsd", choiceOrderSchema());
    Path output = tempDirectory.resolve("choice-default");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    null,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(Files.exists(output));
  }

  @Test
  void plannedFullXsd10ProfileRejectsBeforeGeneration() throws IOException {
    Path schema = writeSchema("purchase-order.xsd", purchaseOrderSchema());
    Path output = tempDirectory.resolve("full-planned");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    GeneratorProfile.XP_XSD10_FULL,
                    "com.acme.generated",
                    Map.of("urn:purchase", "com.acme.purchase"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertEquals(List.of(), result.generatedSources());
    assertEquals("GENERATOR_REQUEST_INVALID", result.diagnostics().getFirst().code());
    assertTrue(result.diagnostics().getFirst().message().contains("Unsupported generator profile"));
    assertFalse(Files.exists(output));
  }

  @Test
  void choiceProfileGeneratesChoiceSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("choice-order.xsd", choiceOrderSchema());
    Path output = tempDirectory.resolve("choice-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_DATA_10_CHOICE,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    assertEquals(
        List.of(
            Path.of("com/acme/orders/DomesticChoice.java"),
            Path.of("com/acme/orders/InternationalChoice.java"),
            Path.of("com/acme/orders/Order.java"),
            Path.of("com/acme/orders/OrderChoice.java"),
            Path.of("com/acme/orders/xml/OrderXmlReader.java"),
            Path.of("com/acme/orders/xml/OrderXmlValidator.java"),
            Path.of("com/acme/orders/xml/OrderXmlWriter.java")),
        result.generatedSources());
    assertTrue(
        Files.readString(output.resolve("com/acme/orders/Order.java"), StandardCharsets.UTF_8)
            .contains("Optional<OrderChoice> orderChoice"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void defaultProfileRejectsRestrictedSimpleTypeWithoutWritingSources() throws IOException {
    Path schema = writeSchema("facet-order.xsd", facetOrderSchema());
    Path output = tempDirectory.resolve("facet-default");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    null,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(Files.exists(output));
  }

  @Test
  void basicValidationProfileGeneratesFacetValidationSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("facet-order.xsd", facetOrderSchema());
    Path output = tempDirectory.resolve("facet-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_VALIDATION_10_BASIC,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    assertTrue(result.generatedSources().contains(Path.of("com/acme/orders/Order.java")));
    String validator =
        Files.readString(output.resolve("com/acme/orders/xml/OrderXmlValidator.java"));
    assertTrue(validator.contains("MXJB-GV-005"));
    assertTrue(validator.contains("MXJB-GV-006"));
    assertTrue(validator.contains("MXJB-GV-007"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void defaultProfileRejectsGroupAndAttributeGroupWithoutWritingSources() throws IOException {
    Path schema = writeSchema("composed-order.xsd", composedOrderSchema());
    Path output = tempDirectory.resolve("composed-default");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    null,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(Files.exists(output));
  }

  @Test
  void composedProfileGeneratesFlattenedGroupSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("composed-order.xsd", composedOrderSchema());
    Path output = tempDirectory.resolve("composed-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_XSD10_COMPOSED,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    assertTrue(result.generatedSources().contains(Path.of("com/acme/orders/Order.java")));
    String order = Files.readString(output.resolve("com/acme/orders/Order.java"));
    assertTrue(order.contains("String id"));
    assertTrue(order.contains("BigDecimal total"));
    assertTrue(order.contains("String version"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void composedProfileGeneratesListAndUnionSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("list-union-order.xsd", listUnionOrderSchema(false));
    Path output = tempDirectory.resolve("list-union-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_XSD10_COMPOSED,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    String order = Files.readString(output.resolve("com/acme/orders/Order.java"));
    String writer = Files.readString(output.resolve("com/acme/orders/xml/OrderXmlWriter.java"));
    String reader = Files.readString(output.resolve("com/acme/orders/xml/OrderXmlReader.java"));
    String validator =
        Files.readString(output.resolve("com/acme/orders/xml/OrderXmlValidator.java"));
    assertTrue(order.contains("List<Integer> quantities"));
    assertTrue(order.contains("String status"));
    assertTrue(order.contains("List<Boolean> flags"));
    assertTrue(writer.contains("Collectors.joining(\" \")"));
    assertTrue(reader.contains("readIntListElement"));
    assertTrue(validator.contains("MXJB-GV-008"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void narrowProfilesRejectListAndUnionWithoutWritingSources() throws IOException {
    Path schema = writeSchema("list-union-order.xsd", listUnionOrderSchema(false));
    Path defaultOutput = tempDirectory.resolve("list-union-default");
    Path validationOutput = tempDirectory.resolve("list-union-validation");

    GeneratorResult defaultResult =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    defaultOutput,
                    null,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));
    GeneratorResult validationResult =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    validationOutput,
                    GeneratorProfile.XP_VALIDATION_10_BASIC,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(defaultResult.successful());
    assertFalse(validationResult.successful());
    assertTrue(
        defaultResult.diagnostics().stream()
            .anyMatch(
                diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
    assertTrue(
        validationResult.diagnostics().stream()
            .anyMatch(
                diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
    assertFalse(Files.exists(defaultOutput));
    assertFalse(Files.exists(validationOutput));
  }

  @Test
  void composedProfileRejectsOptionalListValuedFields() throws IOException {
    Path schema = writeSchema("optional-list-order.xsd", listUnionOrderSchema(true));
    Path output = tempDirectory.resolve("optional-list-generated");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    GeneratorProfile.XP_XSD10_COMPOSED,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    "SCHEMA_BINDING_UNSUPPORTED_TYPE".equals(diagnostic.code())
                        && diagnostic.message().contains("required singleton XML values")));
    assertFalse(Files.exists(output));
  }

  @Test
  void narrowProfilesRejectDerivationWithoutWritingSources() throws IOException {
    Path schema = writeSchema("derivation-order.xsd", derivationOrderSchema(false));
    Path defaultOutput = tempDirectory.resolve("derivation-default");
    Path validationOutput = tempDirectory.resolve("derivation-validation");

    GeneratorResult defaultResult =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    defaultOutput,
                    null,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));
    GeneratorResult validationResult =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    validationOutput,
                    GeneratorProfile.XP_VALIDATION_10_BASIC,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(defaultResult.successful());
    assertFalse(validationResult.successful());
    assertTrue(
        defaultResult.diagnostics().stream()
            .anyMatch(
                diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
    assertTrue(
        validationResult.diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.message().contains("derivation chains require profile")));
    assertFalse(Files.exists(defaultOutput));
    assertFalse(Files.exists(validationOutput));
  }

  @Test
  void composedProfileGeneratesFlattenedDerivationSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("derivation-order.xsd", derivationOrderSchema(false));
    Path output = tempDirectory.resolve("derivation-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_XSD10_COMPOSED,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    String order = Files.readString(output.resolve("com/acme/orders/Order.java"));
    String validator =
        Files.readString(output.resolve("com/acme/orders/xml/OrderXmlValidator.java"));
    assertTrue(order.contains("String id"));
    assertTrue(order.contains("BigDecimal total"));
    assertTrue(order.indexOf("String id") < order.indexOf("BigDecimal total"));
    assertFalse(order.contains("extends"));
    assertTrue(validator.contains("Value length is outside the accepted range."));
    assertTrue(validator.contains("Pattern.matches"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void composedProfileRejectsUnsupportedDerivationWithoutWritingSources() throws IOException {
    Path schema = writeSchema("bad-derivation-order.xsd", derivationOrderSchema(true));
    Path output = tempDirectory.resolve("bad-derivation-generated");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    GeneratorProfile.XP_XSD10_COMPOSED,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    "SCHEMA_IR_INVALID_COMPONENT".equals(diagnostic.code())
                        && diagnostic.message().contains("Duplicate flattened XML element")));
    assertFalse(Files.exists(output));
  }

  @Test
  void narrowProfilesRejectSemanticAttributesWithoutWritingSources() throws IOException {
    Path schema = writeSchema("semantic-order.xsd", semanticOrderSchema(false));
    for (GeneratorProfile profile :
        List.of(
            GeneratorProfile.XP_DATA_10,
            GeneratorProfile.XP_DATA_10_CHOICE,
            GeneratorProfile.XP_VALIDATION_10_BASIC,
            GeneratorProfile.XP_XSD10_COMPOSED)) {
      Path output = tempDirectory.resolve("semantic-" + profile.name());

      GeneratorResult result =
          new CoreGenerator()
              .generate(
                  new GeneratorRequest(
                      List.of(schema),
                      output,
                      profile,
                      "com.acme.generated",
                      Map.of("urn:orders", "com.acme.orders"),
                      List.of(),
                      Map.of()));

      assertFalse(result.successful());
      assertTrue(
          result.diagnostics().stream()
              .anyMatch(
                  diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
      assertFalse(Files.exists(output));
    }
  }

  @Test
  void semanticProfileGeneratesNilDefaultAndFixedSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("semantic-order.xsd", semanticOrderSchema(false));
    Path output = tempDirectory.resolve("semantic-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_XSD10_SEMANTIC,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    String order = Files.readString(output.resolve("com/acme/orders/Order.java"));
    String writer = Files.readString(output.resolve("com/acme/orders/xml/OrderXmlWriter.java"));
    String reader = Files.readString(output.resolve("com/acme/orders/xml/OrderXmlReader.java"));
    String validator =
        Files.readString(output.resolve("com/acme/orders/xml/OrderXmlValidator.java"));
    assertTrue(order.contains("Optional<String> code"));
    assertTrue(order.contains("String status"));
    assertTrue(order.contains("String version"));
    assertTrue(writer.contains("XMLSchema-instance"));
    assertTrue(reader.contains("defaultedText"));
    assertTrue(reader.contains("MXJB-GR-008"));
    assertTrue(validator.contains("MXJB-GV-009"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void semanticProfileRejectsUnsupportedSemanticCombinationsWithoutWritingSources()
      throws IOException {
    Path schema = writeSchema("bad-semantic-order.xsd", semanticOrderSchema(true));
    Path output = tempDirectory.resolve("bad-semantic-generated");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    GeneratorProfile.XP_XSD10_SEMANTIC,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.message().contains("nillable element")
                        || diagnostic.message().contains("cannot declare both default and fixed")));
    assertFalse(Files.exists(output));
  }

  @Test
  void narrowProfilesRejectSubstitutionGroupsWithoutWritingSources() throws IOException {
    Path schema = writeSchema("substitution-order.xsd", substitutionOrderSchema(false));
    for (GeneratorProfile profile :
        List.of(
            GeneratorProfile.XP_DATA_10,
            GeneratorProfile.XP_DATA_10_CHOICE,
            GeneratorProfile.XP_VALIDATION_10_BASIC,
            GeneratorProfile.XP_XSD10_COMPOSED)) {
      Path output = tempDirectory.resolve("substitution-" + profile.name());

      GeneratorResult result =
          new CoreGenerator()
              .generate(
                  new GeneratorRequest(
                      List.of(schema),
                      output,
                      profile,
                      "com.acme.generated",
                      Map.of("urn:orders", "com.acme.orders"),
                      List.of(),
                      Map.of()));

      assertFalse(result.successful());
      assertTrue(
          result.diagnostics().stream()
              .anyMatch(
                  diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
      assertFalse(Files.exists(output));
    }
  }

  @Test
  void semanticProfileGeneratesSubstitutionGroupSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("substitution-order.xsd", substitutionOrderSchema(false));
    Path output = tempDirectory.resolve("substitution-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_XSD10_SEMANTIC,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    assertTrue(
        result.generatedSources().contains(Path.of("com/acme/orders/PaymentSubstitution.java")));
    assertTrue(
        result
            .generatedSources()
            .contains(Path.of("com/acme/orders/CardpaymentSubstitutionBranch.java")));
    String order = Files.readString(output.resolve("com/acme/orders/Order.java"));
    String reader = Files.readString(output.resolve("com/acme/orders/xml/OrderXmlReader.java"));
    assertTrue(order.contains("Optional<PaymentSubstitution> payment"));
    assertTrue(reader.contains("CardpaymentSubstitutionBranch"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void semanticProfileRejectsRepeatedSubstitutionGroupReferencesWithoutWritingSources()
      throws IOException {
    Path schema = writeSchema("bad-substitution-order.xsd", substitutionOrderSchema(true));
    Path output = tempDirectory.resolve("bad-substitution-generated");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    GeneratorProfile.XP_XSD10_SEMANTIC,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic -> diagnostic.message().contains("Repeated substitution group head")));
    assertFalse(Files.exists(output));
  }

  @Test
  void narrowProfilesRejectWildcardsWithoutWritingSources() throws IOException {
    Path schema = writeSchema("document-order.xsd", documentOrderSchema(false));
    for (GeneratorProfile profile :
        List.of(
            GeneratorProfile.XP_DATA_10,
            GeneratorProfile.XP_DATA_10_CHOICE,
            GeneratorProfile.XP_VALIDATION_10_BASIC,
            GeneratorProfile.XP_XSD10_COMPOSED,
            GeneratorProfile.XP_XSD10_SEMANTIC)) {
      Path output = tempDirectory.resolve("wildcard-" + profile.name());

      GeneratorResult result =
          new CoreGenerator()
              .generate(
                  new GeneratorRequest(
                      List.of(schema),
                      output,
                      profile,
                      "com.acme.generated",
                      Map.of("urn:orders", "com.acme.orders"),
                      List.of(),
                      Map.of()));

      assertFalse(result.successful());
      assertTrue(
          result.diagnostics().stream()
              .anyMatch(
                  diagnostic -> "SCHEMA_FRONTEND_UNSUPPORTED_PROFILE".equals(diagnostic.code())));
      assertFalse(Files.exists(output));
    }
  }

  @Test
  void documentProfileGeneratesWildcardSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("document-order.xsd", documentOrderSchema(false));
    Path output = tempDirectory.resolve("document-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_XSD10_DOCUMENT,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    String order = Files.readString(output.resolve("com/acme/orders/Order.java"));
    String reader = Files.readString(output.resolve("com/acme/orders/xml/OrderXmlReader.java"));
    String writer = Files.readString(output.resolve("com/acme/orders/xml/OrderXmlWriter.java"));
    String validator =
        Files.readString(output.resolve("com/acme/orders/xml/OrderXmlValidator.java"));
    assertTrue(order.contains("List<XmlFragment> wildcardContent"));
    assertTrue(reader.contains("readFragment(input)"));
    assertTrue(writer.contains("writeFragment(output"));
    assertTrue(validator.contains("validateFragment(item"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void documentProfileGeneratesMixedContentSourcesAndCompilesThem() throws IOException {
    Path schema = writeSchema("mixed-document-order.xsd", mixedDocumentOrderSchema());
    Path output = tempDirectory.resolve("mixed-document-generated");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            GeneratorProfile.XP_XSD10_DOCUMENT,
            "com.acme.generated",
            Map.of("urn:orders", "com.acme.orders"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertTrue(result.successful(), result.diagnostics().toString());
    String order = Files.readString(output.resolve("com/acme/orders/Order.java"));
    String content = Files.readString(output.resolve("com/acme/orders/OrderContent.java"));
    String textBranch = Files.readString(output.resolve("com/acme/orders/OrderTextContent.java"));
    String reader = Files.readString(output.resolve("com/acme/orders/xml/OrderXmlReader.java"));
    String writer = Files.readString(output.resolve("com/acme/orders/xml/OrderXmlWriter.java"));
    String validator =
        Files.readString(output.resolve("com/acme/orders/xml/OrderXmlValidator.java"));
    assertTrue(order.contains("List<OrderContent> content"));
    assertTrue(content.contains("sealed interface OrderContent"));
    assertTrue(textBranch.contains("record OrderTextContent(String value)"));
    assertTrue(reader.contains("OrderTextContent(input.text())"));
    assertTrue(writer.contains("output.text(branch.value())"));
    assertTrue(validator.contains("Out-of-order mixed content"));
    compileGeneratedSources(output, result.generatedSources());
  }

  @Test
  void documentProfileRejectsUnsupportedWildcardWithoutWritingSources() throws IOException {
    Path schema = writeSchema("bad-document-order.xsd", documentOrderSchema(true));
    Path output = tempDirectory.resolve("bad-document-generated");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    GeneratorProfile.XP_XSD10_DOCUMENT,
                    "com.acme.generated",
                    Map.of("urn:orders", "com.acme.orders"),
                    List.of(),
                    Map.of()));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.message().contains("processContents=\"skip\"")));
    assertFalse(Files.exists(output));
  }

  @Test
  void semanticProfileRejectsUnsupportedValidationCategoriesWithoutWritingSources()
      throws IOException {
    List<String> schemas =
        List.of(
            unsupportedValidationSchema("<xs:any namespace=\"##other\"/>"),
            unsupportedValidationSchema("<xs:element name=\"id\" type=\"xs:string\"/>", true),
            unsupportedValidationSchema(
                """
                <xs:element name="id" type="xs:string"/>
                <xs:assert test="true()"/>
                """),
            """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:tns="urn:orders"
                targetNamespace="urn:orders">
              <xs:element name="order" type="tns:Order">
                <xs:key name="orderId">
                  <xs:selector xpath="tns:id"/>
                  <xs:field xpath="."/>
                </xs:key>
              </xs:element>
              <xs:complexType name="Order">
                <xs:sequence>
                  <xs:element name="id" type="xs:string"/>
                </xs:sequence>
              </xs:complexType>
            </xs:schema>
            """,
            """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:tns="urn:orders"
                targetNamespace="urn:orders">
              <xs:element name="order" type="tns:Order"/>
              <xs:complexType name="Order">
                <xs:sequence>
                  <xs:element name="date" type="xs:date"/>
                </xs:sequence>
              </xs:complexType>
            </xs:schema>
            """,
            """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:tns="urn:orders"
                targetNamespace="urn:orders">
              <xs:element name="order" type="tns:Order"/>
              <xs:complexType name="Order">
                <xs:complexContent>
                  <xs:restriction base="tns:BaseOrder">
                    <xs:sequence>
                      <xs:element name="id" type="xs:string"/>
                    </xs:sequence>
                  </xs:restriction>
                </xs:complexContent>
              </xs:complexType>
              <xs:complexType name="BaseOrder">
                <xs:sequence>
                  <xs:element name="id" type="xs:string"/>
                </xs:sequence>
              </xs:complexType>
            </xs:schema>
            """);

    for (int index = 0; index < schemas.size(); index++) {
      Path schema = writeSchema("bad-semantic-validation-" + index + ".xsd", schemas.get(index));
      Path output = tempDirectory.resolve("bad-semantic-validation-" + index);
      Path repeatOutput = tempDirectory.resolve("bad-semantic-validation-repeat-" + index);

      GeneratorResult result =
          new CoreGenerator()
              .generate(
                  new GeneratorRequest(
                      List.of(schema),
                      output,
                      GeneratorProfile.XP_XSD10_SEMANTIC,
                      "com.acme.generated",
                      Map.of("urn:orders", "com.acme.orders"),
                      List.of(),
                      Map.of()));
      GeneratorResult repeatResult =
          new CoreGenerator()
              .generate(
                  new GeneratorRequest(
                      List.of(schema),
                      repeatOutput,
                      GeneratorProfile.XP_XSD10_SEMANTIC,
                      "com.acme.generated",
                      Map.of("urn:orders", "com.acme.orders"),
                      List.of(),
                      Map.of()));

      assertFalse(result.successful(), "schema " + index + " unexpectedly generated");
      assertFalse(result.diagnostics().isEmpty(), "schema " + index + " had no diagnostics");
      assertEquals(result.diagnostics(), repeatResult.diagnostics(), "schema " + index);
      assertTrue(result.generatedSources().isEmpty(), "schema " + index + " wrote sources");
      assertFalse(Files.exists(output), "schema " + index + " created output");
      assertFalse(Files.exists(repeatOutput), "schema " + index + " created repeat output");
    }
  }

  @Test
  void deniesNetworkResolutionAndWritesNoSources() throws IOException {
    Path schema = writeSchema("order.xsd", orderSchema("https://example.invalid/line.xsd"));
    Path output = tempDirectory.resolve("network-denied");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), output));

    assertFalse(result.successful());
    assertEquals("SCHEMA_RESOURCE_NETWORK_DENIED", result.diagnostics().get(0).code());
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(Files.exists(output));
  }

  @Test
  void invalidBindingConfigurationWritesNoSources() throws IOException {
    Path schema = writeSchema("purchase-order.xsd", purchaseOrderSchema());
    Path output = tempDirectory.resolve("invalid-package");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema), output, null, "not-valid!", Map.of(), List.of(), Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertFalse(result.successful());
    assertEquals("SCHEMA_BINDING_INVALID_CONFIGURATION", result.diagnostics().get(0).code());
    assertFalse(Files.exists(output));
  }

  @Test
  void writeFailureDoesNotLeaveEarlierGeneratedSources() throws IOException {
    Path schema = writeSchema("purchase-order.xsd", purchaseOrderSchema());
    Path output = tempDirectory.resolve("blocked-output");
    Files.createDirectories(output.resolve("com/acme/purchase"));
    Files.writeString(output.resolve("com/acme/purchase/xml"), "not a directory");
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(schema),
            output,
            null,
            "com.acme.generated",
            Map.of("urn:purchase", "com.acme.purchase"),
            List.of(),
            Map.of());

    GeneratorResult result = new CoreGenerator().generate(request);

    assertFalse(result.successful());
    assertEquals(CoreGenerator.WRITE_FAILED, result.diagnostics().get(0).code());
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(Files.exists(output.resolve("com/acme/purchase/Line.java")));
    assertFalse(Files.exists(output.resolve("com/acme/purchase/Order.java")));
  }

  @Test
  void requestValidationReportsMissingInputs() {
    GeneratorRequest request = GeneratorRequest.of(List.of(), null);

    GeneratorResult result = new CoreGenerator().generate(request);

    assertFalse(result.successful());
    assertEquals(
        List.of(CoreGenerator.REQUEST_INVALID, CoreGenerator.REQUEST_INVALID),
        result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
  }

  @Test
  void duplicateRootHelperNamesReturnDiagnosticsWithoutWriting() throws IOException {
    Path schema = writeSchema("duplicate-roots.xsd", duplicateRootSchema());
    Path output = tempDirectory.resolve("duplicate-output");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), output));

    assertFalse(result.successful());
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    "SCHEMA_WRITER_EMISSION_INVALID_MODEL".equals(diagnostic.code())
                        || "SCHEMA_READER_EMISSION_INVALID_MODEL".equals(diagnostic.code())
                        || "SCHEMA_VALIDATOR_EMISSION_INVALID_MODEL".equals(diagnostic.code())));
    assertFalse(Files.exists(output));
  }

  private Path writeSchema(String relativePath, String content) throws IOException {
    Path target = tempDirectory.resolve(relativePath);
    Path parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(target, content, StandardCharsets.UTF_8);
    return target;
  }

  private Map<Path, String> readGeneratedSources(Path output, List<Path> relativePaths)
      throws IOException {
    java.util.LinkedHashMap<Path, String> sources = new java.util.LinkedHashMap<>();
    for (Path relativePath : relativePaths) {
      sources.put(
          relativePath, Files.readString(output.resolve(relativePath), StandardCharsets.UTF_8));
    }
    return Map.copyOf(sources);
  }

  private void compileGeneratedSources(Path output, List<Path> relativePaths) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "A JDK with JavaCompiler is required.");
    Path classes =
        tempDirectory.resolve("classes-" + Math.floorMod(output.hashCode(), Integer.MAX_VALUE));
    try {
      Files.createDirectories(classes);
    } catch (IOException exception) {
      fail(exception);
    }
    List<String> compilerArguments =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.of(
                    "--release",
                    "21",
                    "-Xlint:all",
                    "-Werror",
                    "-classpath",
                    System.getProperty("java.class.path"),
                    "-d",
                    classes.toString()),
                relativePaths.stream().map(path -> output.resolve(path).toString()))
            .toList();
    ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
    int exitCode =
        compiler.run(
            null, compilerOutput, compilerOutput, compilerArguments.toArray(String[]::new));
    if (exitCode != 0) {
      fail(compilerOutput.toString(StandardCharsets.UTF_8));
    }
  }

  private String purchaseOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:purchase"
            xmlns:p="urn:purchase"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:element name="order" type="p:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:element name="line" type="p:Line" maxOccurs="unbounded"/>
            </xs:sequence>
            <xs:attribute name="version" type="xs:string" use="optional"/>
          </xs:complexType>
          <xs:complexType name="Line">
            <xs:sequence>
              <xs:element name="sku" type="xs:string"/>
              <xs:element name="quantity" type="xs:int"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String orderSchema(String lineLocation) {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            xmlns:l="urn:lines"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:import namespace="urn:lines" schemaLocation="LINE_SCHEMA_LOCATION"/>
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:element ref="l:line" maxOccurs="unbounded"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """
        .replace("LINE_SCHEMA_LOCATION", lineLocation);
  }

  private String choiceOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:choice minOccurs="0">
                <xs:element name="domestic" type="xs:string"/>
                <xs:element name="international" type="xs:string"/>
              </xs:choice>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String facetOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:simpleType name="OrderCode">
            <xs:restriction base="xs:string">
              <xs:minLength value="3"/>
              <xs:maxLength value="8"/>
              <xs:pattern value="[A-Z0-9]+"/>
            </xs:restriction>
          </xs:simpleType>
          <xs:simpleType name="Priority">
            <xs:restriction base="xs:int">
              <xs:minInclusive value="1"/>
              <xs:maxInclusive value="9"/>
            </xs:restriction>
          </xs:simpleType>
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="code" type="o:OrderCode"/>
              <xs:element name="priority" type="o:Priority"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String composedOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:group name="OrderFields">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:element name="total" type="xs:decimal"/>
            </xs:sequence>
          </xs:group>
          <xs:attributeGroup name="OrderAttributes">
            <xs:attribute name="version" type="xs:string" use="required"/>
          </xs:attributeGroup>
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:group ref="o:OrderFields"/>
            </xs:sequence>
            <xs:attributeGroup ref="o:OrderAttributes"/>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String listUnionOrderSchema(boolean optionalListElement) {
    String listCardinality = optionalListElement ? " minOccurs=\"0\"" : "";
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:tns="urn:orders"
            targetNamespace="urn:orders">
          <xs:simpleType name="Quantity">
            <xs:restriction base="xs:int">
              <xs:minInclusive value="1"/>
              <xs:maxInclusive value="9"/>
            </xs:restriction>
          </xs:simpleType>
          <xs:simpleType name="QuantityList">
            <xs:list itemType="tns:Quantity"/>
          </xs:simpleType>
          <xs:simpleType name="FlagList">
            <xs:list itemType="xs:boolean"/>
          </xs:simpleType>
          <xs:simpleType name="Status">
            <xs:restriction base="xs:string">
              <xs:enumeration value="NEW"/>
              <xs:enumeration value="CLOSED"/>
            </xs:restriction>
          </xs:simpleType>
          <xs:simpleType name="StatusOrPriority">
            <xs:union memberTypes="tns:Status xs:int"/>
          </xs:simpleType>
          <xs:element name="order" type="tns:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="quantities" type="tns:QuantityList"LIST_CARDINALITY/>
              <xs:element name="status" type="tns:StatusOrPriority"/>
            </xs:sequence>
            <xs:attribute name="flags" type="tns:FlagList" use="required"/>
          </xs:complexType>
        </xs:schema>
        """
        .replace("LIST_CARDINALITY", listCardinality);
  }

  private String derivationOrderSchema(boolean duplicateDerivedField) {
    String derivedElement =
        duplicateDerivedField
            ? "<xs:element name=\"id\" type=\"xs:string\"/>"
            : "<xs:element name=\"total\" type=\"xs:decimal\"/>";
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:tns="urn:orders"
            targetNamespace="urn:orders">
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
                  DERIVED_ELEMENT
                </xs:sequence>
                <xs:attribute name="region" type="xs:string" use="required"/>
              </xs:extension>
            </xs:complexContent>
          </xs:complexType>
        </xs:schema>
        """
        .replace("DERIVED_ELEMENT", derivedElement);
  }

  private String semanticOrderSchema(boolean unsupported) {
    String nillableCardinality = unsupported ? " minOccurs=\"0\"" : "";
    String statusSemantics = unsupported ? " default=\"NEW\" fixed=\"CLOSED\"" : " default=\"NEW\"";
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:tns="urn:orders"
            targetNamespace="urn:orders"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:simpleType name="Status">
            <xs:restriction base="xs:string">
              <xs:enumeration value="NEW"/>
              <xs:enumeration value="CLOSED"/>
            </xs:restriction>
          </xs:simpleType>
          <xs:element name="order" type="tns:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="code" type="xs:string" nillable="true"NILLABLE_CARDINALITY/>
              <xs:element name="note" type="xs:string" minOccurs="0" default="none"/>
              <xs:element name="kind" type="xs:string" fixed="STANDARD"/>
            </xs:sequence>
            <xs:attribute name="status" type="tns:Status"STATUS_SEMANTICS/>
            <xs:attribute name="version" type="xs:string" fixed="1"/>
          </xs:complexType>
        </xs:schema>
        """
        .replace("NILLABLE_CARDINALITY", nillableCardinality)
        .replace("STATUS_SEMANTICS", statusSemantics);
  }

  private String unsupportedValidationSchema(String sequenceContent) {
    return unsupportedValidationSchema(sequenceContent, false);
  }

  private String unsupportedValidationSchema(String sequenceContent, boolean mixed) {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:tns="urn:orders"
            targetNamespace="urn:orders">
          <xs:element name="order" type="tns:Order"/>
          <xs:complexType name="Order"MIXED>
            <xs:sequence>
              SEQUENCE_CONTENT
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """
        .replace("MIXED", mixed ? " mixed=\"true\"" : "")
        .replace("SEQUENCE_CONTENT", sequenceContent);
  }

  private String substitutionOrderSchema(boolean repeatedHeadRef) {
    String paymentCardinality = repeatedHeadRef ? " maxOccurs=\"unbounded\"" : "";
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:tns="urn:orders"
            targetNamespace="urn:orders"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
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
              <xs:element name="id" type="xs:string"/>
              <xs:element ref="tns:payment" minOccurs="0"PAYMENT_CARDINALITY/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """
        .replace("PAYMENT_CARDINALITY", paymentCardinality);
  }

  private String documentOrderSchema(boolean unsupportedProcessContents) {
    String processContents = unsupportedProcessContents ? "strict" : "skip";
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:tns="urn:orders"
            targetNamespace="urn:orders"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:element name="order" type="tns:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:any namespace="##other" processContents="PROCESS_CONTENTS" minOccurs="0" maxOccurs="unbounded"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """
        .replace("PROCESS_CONTENTS", processContents);
  }

  private String mixedDocumentOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:tns="urn:orders"
            targetNamespace="urn:orders"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:element name="order" type="tns:Order"/>
          <xs:complexType name="Order" mixed="true">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:any namespace="##other" processContents="skip" minOccurs="0" maxOccurs="unbounded"/>
              <xs:element name="tail" type="xs:string" minOccurs="0"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String lineSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:lines"
            xmlns:l="urn:lines"
            elementFormDefault="qualified">
          <xs:element name="line" type="l:Line"/>
          <xs:complexType name="Line">
            <xs:sequence>
              <xs:element name="sku" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String duplicateRootSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:duplicate"
            xmlns:d="urn:duplicate"
            elementFormDefault="qualified">
          <xs:element name="first" type="d:Order"/>
          <xs:element name="second" type="d:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }
}
