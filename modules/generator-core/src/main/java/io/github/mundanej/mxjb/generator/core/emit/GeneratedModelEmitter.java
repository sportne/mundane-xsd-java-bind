package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingModel;
import io.github.mundanej.mxjb.generator.core.bind.BindingResult;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Emits deterministic Java 21 model source from the internal binding model. */
public final class GeneratedModelEmitter {
  private static final Map<String, String> SCALAR_TYPES =
      Map.of(
          "string", "String",
          "boolean", "Boolean",
          "int", "Integer",
          "integer", "BigInteger",
          "long", "Long",
          "decimal", "BigDecimal");

  public GeneratedModelEmissionResult emit(BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return GeneratedModelEmissionResult.empty(bindingResult.diagnostics());
    }
    return emit(bindingResult.model());
  }

  public GeneratedModelEmissionResult emit(BindingModel model) {
    List<SchemaDiagnostic> diagnostics = validate(model);
    if (!diagnostics.isEmpty()) {
      return GeneratedModelEmissionResult.empty(diagnostics);
    }

    List<GeneratedJavaSource> sources =
        model.types().stream()
            .sorted(Comparator.comparing(type -> type.javaName().qualifiedName()))
            .map(this::emitType)
            .toList();
    return new GeneratedModelEmissionResult(sources, List.of());
  }

  private List<SchemaDiagnostic> validate(BindingModel model) {
    List<SchemaDiagnostic> diagnostics = new ArrayList<>();
    for (BindingType type : model.types()) {
      if (!"record".equals(type.shape())) {
        diagnostics.add(invalidModel("Unsupported generated model shape " + type.shape() + "."));
      }
      for (BindingField field : type.fields()) {
        if (!isSupportedTypeReference(field.type())) {
          diagnostics.add(
              invalidModel(
                  "Unsupported generated model type reference " + field.type().toText() + "."));
        }
        if (!isSupportedCardinality(field.cardinality().shape())) {
          diagnostics.add(
              invalidModel(
                  "Unsupported generated model cardinality " + field.cardinality().toText() + "."));
        }
      }
    }
    return diagnostics.stream()
        .sorted(
            Comparator.comparing(SchemaDiagnostic::resource)
                .thenComparing(diagnostic -> diagnostic.code().name())
                .thenComparing(SchemaDiagnostic::message))
        .toList();
  }

  private boolean isSupportedTypeReference(BindingTypeReference reference) {
    if ("scalar".equals(reference.kind())) {
      return SCALAR_TYPES.containsKey(reference.name());
    }
    return "model".equals(reference.kind());
  }

  private boolean isSupportedCardinality(String shape) {
    return "required".equals(shape) || "optional".equals(shape) || "list".equals(shape);
  }

  private SchemaDiagnostic invalidModel(String message) {
    return new SchemaDiagnostic(DiagnosticCode.SCHEMA_EMISSION_INVALID_MODEL, "emission", message);
  }

  private GeneratedJavaSource emitType(BindingType type) {
    BindingJavaName javaName = type.javaName();
    String sourceText = sourceText(type);
    return new GeneratedJavaSource(javaName, relativePath(javaName), sourceText);
  }

  private Path relativePath(BindingJavaName javaName) {
    return Path.of(javaName.packageName().replace('.', '/'), javaName.simpleName() + ".java");
  }

  private String sourceText(BindingType type) {
    StringBuilder source = new StringBuilder();
    source.append("package ").append(type.javaName().packageName()).append(";\n\n");
    String imports = imports(type);
    if (!imports.isEmpty()) {
      source.append(imports).append('\n');
    }
    source
        .append("/** Generated immutable model for XML type ")
        .append(type.javaName().simpleName())
        .append(". */\n");
    source
        .append("public record ")
        .append(type.javaName().simpleName())
        .append('(')
        .append(recordComponents(type))
        .append(")");
    if (type.fields().isEmpty()) {
      source.append(" {}\n");
      return source.toString();
    }
    source.append(" {\n");
    source.append("  public ").append(type.javaName().simpleName()).append(" {\n");
    for (BindingField field : type.fields()) {
      source.append(constructorLine(field));
    }
    source.append("  }\n");
    source.append("}\n");
    return source.toString();
  }

  private String imports(BindingType type) {
    Set<String> imports = new LinkedHashSet<>();
    for (BindingField field : type.fields()) {
      String cardinality = field.cardinality().shape();
      if ("list".equals(cardinality)) {
        imports.add("java.util.List");
      }
      if ("optional".equals(cardinality)) {
        imports.add("java.util.Optional");
      }
      if ("scalar".equals(field.type().kind()) && "integer".equals(field.type().name())) {
        imports.add("java.math.BigInteger");
      }
      if ("scalar".equals(field.type().kind()) && "decimal".equals(field.type().name())) {
        imports.add("java.math.BigDecimal");
      }
    }
    if (!type.fields().isEmpty()) {
      imports.add("java.util.Objects");
    }
    return imports.stream()
        .sorted()
        .map(value -> "import " + value + ";\n")
        .collect(Collectors.joining());
  }

  private String recordComponents(BindingType type) {
    return type.fields().stream()
        .map(field -> fieldType(type.javaName().packageName(), field) + " " + field.javaName())
        .collect(Collectors.joining(", "));
  }

  private String fieldType(String currentPackage, BindingField field) {
    String base = baseType(currentPackage, field.type());
    String cardinality = field.cardinality().shape();
    return switch (cardinality) {
      case "optional" -> "Optional<" + base + ">";
      case "list" -> "List<" + base + ">";
      default -> base;
    };
  }

  private String baseType(String currentPackage, BindingTypeReference reference) {
    if ("scalar".equals(reference.kind())) {
      return SCALAR_TYPES.get(reference.name());
    }
    BindingJavaName name = javaName(reference.name());
    if (currentPackage.equals(name.packageName())) {
      return name.simpleName();
    }
    return name.qualifiedName();
  }

  private BindingJavaName javaName(String qualifiedName) {
    int separator = qualifiedName.lastIndexOf('.');
    if (separator < 0) {
      return new BindingJavaName("", qualifiedName);
    }
    return new BindingJavaName(
        qualifiedName.substring(0, separator), qualifiedName.substring(separator + 1));
  }

  private String constructorLine(BindingField field) {
    String fieldName = field.javaName();
    return switch (field.cardinality().shape()) {
      case "list" ->
          "    "
              + fieldName
              + " = List.copyOf(Objects.requireNonNull("
              + fieldName
              + ", \""
              + fieldName
              + "\"));\n";
      default -> "    Objects.requireNonNull(" + fieldName + ", \"" + fieldName + "\");\n";
    };
  }
}
