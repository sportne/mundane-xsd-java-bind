package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.bind.BindingCardinality;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingModel;
import io.github.mundanej.mxjb.generator.core.bind.BindingResult;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.BindingValidationPlan;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GeneratedModelEmitterTest {
  @TempDir private Path tempDirectory;

  @Test
  void emitsDeterministicRecordSourceForSupportedFieldShapes() {
    BindingModel model =
        new BindingModel(
            List.of(),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field("element", "id", scalar("string"), required()),
                        field("element", "note", scalar("string"), optional()),
                        field("element", "line", scalar("int"), list())))));

    GeneratedModelEmissionResult first = new GeneratedModelEmitter().emit(model);
    GeneratedModelEmissionResult second = new GeneratedModelEmitter().emit(model);

    assertFalse(first.hasErrors());
    assertEquals(first.sources(), second.sources());
    assertEquals(
        Path.of("com/example/orders/Order.java"), first.sources().getFirst().relativePath());
    assertEquals(
        """
        package com.example.orders;

        import java.util.List;
        import java.util.Objects;
        import java.util.Optional;

        /** Generated immutable model for XML type Order. */
        public record Order(String id, Optional<String> note, List<Integer> line) {
          public Order {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(note, "note");
            line = List.copyOf(Objects.requireNonNull(line, "line"));
          }
        }
        """,
        first.sources().getFirst().sourceText());
  }

  @Test
  void emitsEmptyRecordWithoutImportsOrConstructor() {
    BindingModel model =
        new BindingModel(List.of(), List.of(type("com.example.orders", "Empty", List.of())));

    GeneratedModelEmissionResult result = new GeneratedModelEmitter().emit(model);

    assertFalse(result.hasErrors());
    assertEquals(
        """
        package com.example.orders;

        /** Generated immutable model for XML type Empty. */
        public record Empty() {}
        """,
        result.sources().getFirst().sourceText());
  }

  @Test
  void emitsModelSourceWithoutRuntimeBindingMechanisms() {
    BindingModel model =
        new BindingModel(
            List.of(),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(field("element", "id", scalar("string"), required())))));

    String source = new GeneratedModelEmitter().emit(model).sources().getFirst().sourceText();

    assertFalse(source.contains("@Generated"));
    assertFalse(source.contains("java.lang.reflect"));
    assertFalse(source.contains("ServiceLoader"));
    assertFalse(source.contains("ClassLoader"));
    assertFalse(source.contains("javax.xml"));
    assertFalse(source.contains("XmlReader"));
    assertFalse(source.contains("XmlWriter"));
    assertFalse(source.contains("validate("));
  }

  @Test
  void usesFullyQualifiedReferencesForCrossPackageModelFields() {
    BindingModel model =
        new BindingModel(
            List.of(),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field(
                            "element",
                            "address",
                            model("com.example.shared.Address"),
                            required()))),
                type("com.example.shared", "Address", List.of())));

    GeneratedModelEmissionResult result = new GeneratedModelEmitter().emit(model);

    assertFalse(result.hasErrors());
    assertTrue(
        result
            .sources()
            .getFirst()
            .sourceText()
            .contains("public record Order(com.example.shared.Address address)"));
  }

  @Test
  void emitsSourcesSortedByQualifiedTypeName() {
    BindingModel model =
        new BindingModel(
            List.of(),
            List.of(
                type("com.example.orders", "Zeta", List.of()),
                type("com.example.orders", "Alpha", List.of())));

    GeneratedModelEmissionResult result = new GeneratedModelEmitter().emit(model);

    assertFalse(result.hasErrors());
    assertEquals(
        List.of(Path.of("com/example/orders/Alpha.java"), Path.of("com/example/orders/Zeta.java")),
        result.sources().stream().map(GeneratedJavaSource::relativePath).toList());
  }

  @Test
  void emitsScalarMappingsAndPreservesBindingFieldOrder() {
    BindingModel model =
        new BindingModel(
            List.of(),
            List.of(
                type(
                    "com.example.orders",
                    "Totals",
                    List.of(
                        field("element", "active", scalar("boolean"), required()),
                        field("element", "quantity", scalar("long"), required()),
                        field("element", "count", scalar("integer"), required()),
                        field("attribute", "amount", scalar("decimal"), required())))));

    GeneratedModelEmissionResult result = new GeneratedModelEmitter().emit(model);

    assertFalse(result.hasErrors());
    assertEquals(
        """
        package com.example.orders;

        import java.math.BigDecimal;
        import java.math.BigInteger;
        import java.util.Objects;

        /** Generated immutable model for XML type Totals. */
        public record Totals(Boolean active, Long quantity, BigInteger count, BigDecimal amount) {
          public Totals {
            Objects.requireNonNull(active, "active");
            Objects.requireNonNull(quantity, "quantity");
            Objects.requireNonNull(count, "count");
            Objects.requireNonNull(amount, "amount");
          }
        }
        """,
        result.sources().getFirst().sourceText());
  }

  @Test
  void emitsBindingModelJavaNamesWithoutResanitizingThem() {
    BindingModel model =
        new BindingModel(
            List.of(),
            List.of(
                type(
                    "com.example.orders",
                    "LineItem2",
                    List.of(
                        field("element", "_class", scalar("string"), required()),
                        field("attribute", "_class2", scalar("string"), required())))));

    GeneratedModelEmissionResult result = new GeneratedModelEmitter().emit(model);

    assertFalse(result.hasErrors());
    assertTrue(
        result
            .sources()
            .getFirst()
            .sourceText()
            .contains("record LineItem2(String _class, String _class2)"));
  }

  @Test
  void compilesGeneratedSourceAndDefensivelyCopiesRepeatedFields()
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException {
    BindingModel model =
        new BindingModel(
            List.of(),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(
                        field("element", "id", scalar("string"), required()),
                        field("element", "note", scalar("string"), optional()),
                        field("element", "line", scalar("int"), list())))));
    GeneratedModelEmissionResult result = new GeneratedModelEmitter().emit(model);

    Class<?> orderClass = compileAndLoad(result.sources(), "com.example.orders.Order");
    Object order =
        orderClass
            .getConstructor(String.class, Optional.class, List.class)
            .newInstance("A-1", Optional.of("ship"), List.of(1, 2));

    Object lines = orderClass.getMethod("line").invoke(order);

    assertInstanceOf(List.class, lines);
    assertEquals(List.of(1, 2), lines);
    assertThrows(UnsupportedOperationException.class, () -> ((List<?>) lines).add(null));
    InvocationTargetException optionalFailure =
        assertThrows(
            InvocationTargetException.class,
            () ->
                orderClass
                    .getConstructor(String.class, Optional.class, List.class)
                    .newInstance("A-1", null, List.of()));
    assertInstanceOf(NullPointerException.class, optionalFailure.getCause());
  }

  @Test
  void propagatesBindingDiagnosticsWithoutSources() {
    SchemaDiagnostic diagnostic =
        new SchemaDiagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL, "binding", "Invalid binding model.");
    BindingResult bindingResult = BindingResult.empty(List.of(diagnostic));

    GeneratedModelEmissionResult result = new GeneratedModelEmitter().emit(bindingResult);

    assertEquals(List.of(diagnostic), result.diagnostics());
    assertTrue(result.sources().isEmpty());
  }

  @Test
  void rejectsUnsupportedBindingModelsWithoutPartialSources() {
    BindingModel model =
        new BindingModel(
            List.of(),
            List.of(
                new BindingType(
                    new BindingJavaName("com.example.orders", "Order"),
                    schemaName("Order"),
                    "class",
                    List.of(),
                    new BindingValidationPlan(List.of()))));

    GeneratedModelEmissionResult result = new GeneratedModelEmitter().emit(model);

    assertEquals(List.of(DiagnosticCode.SCHEMA_EMISSION_INVALID_MODEL), diagnosticCodes(result));
    assertTrue(result.sources().isEmpty());
  }

  @Test
  void rejectsUnsupportedScalarReferencesWithoutPartialSources() {
    BindingModel model =
        new BindingModel(
            List.of(),
            List.of(
                type(
                    "com.example.orders",
                    "Order",
                    List.of(field("element", "when", scalar("date"), required())))));

    GeneratedModelEmissionResult result = new GeneratedModelEmitter().emit(model);

    assertEquals(List.of(DiagnosticCode.SCHEMA_EMISSION_INVALID_MODEL), diagnosticCodes(result));
    assertTrue(result.sources().isEmpty());
  }

  private Class<?> compileAndLoad(List<GeneratedJavaSource> sources, String className)
      throws IOException, ClassNotFoundException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler);
    Path sourceRoot = tempDirectory.resolve("source");
    Path classRoot = tempDirectory.resolve("classes");
    Files.createDirectories(sourceRoot);
    Files.createDirectories(classRoot);
    for (GeneratedJavaSource source : sources) {
      Path sourcePath = sourceRoot.resolve(source.relativePath());
      Path sourceParent = sourcePath.getParent();
      if (sourceParent != null) {
        Files.createDirectories(sourceParent);
      }
      Files.writeString(sourcePath, source.sourceText());
    }
    List<String> arguments =
        sources.stream()
            .map(source -> sourceRoot.resolve(source.relativePath()).toString())
            .toList();
    List<String> compilerArguments =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.of(
                    "--release", "21", "-Xlint:all", "-Werror", "-d", classRoot.toString()),
                arguments.stream())
            .toList();

    assertEquals(0, compiler.run(null, null, null, compilerArguments.toArray(String[]::new)));
    try (URLClassLoader loader = new URLClassLoader(new URL[] {classRoot.toUri().toURL()})) {
      return loader.loadClass(className);
    }
  }

  private List<DiagnosticCode> diagnosticCodes(GeneratedModelEmissionResult result) {
    return result.diagnostics().stream().map(SchemaDiagnostic::code).toList();
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
      String kind, String localName, BindingTypeReference type, BindingCardinality cardinality) {
    return new BindingField(kind, schemaName(localName), localName, type, cardinality, 1, true);
  }

  private BindingTypeReference scalar(String name) {
    return new BindingTypeReference("scalar", name);
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

  private SchemaQName schemaName(String localName) {
    return new SchemaQName("urn:orders", localName);
  }
}
