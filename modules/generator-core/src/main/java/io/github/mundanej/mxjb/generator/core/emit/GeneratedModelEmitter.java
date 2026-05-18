package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingChoice;
import io.github.mundanej.mxjb.generator.core.bind.BindingChoiceBranch;
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

    List<GeneratedJavaSource> sources = new ArrayList<>();
    for (BindingType type :
        model.types().stream()
            .sorted(Comparator.comparing(value -> value.javaName().qualifiedName()))
            .toList()) {
      sources.add(emitType(type));
      for (BindingField field : type.fields()) {
        if ("choice".equals(field.kind()) && field.choice() != null) {
          sources.addAll(emitChoice(field.choice()));
        }
      }
    }
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
        if ("choice".equals(field.kind()) && field.choice() == null) {
          diagnostics.add(invalidModel("Choice field is missing generated choice metadata."));
        }
        if ("choice".equals(field.kind()) && "list".equals(field.cardinality().shape())) {
          diagnostics.add(invalidModel("Choice fields do not support list cardinality."));
        }
        if (field.choice() != null) {
          for (BindingChoiceBranch branch : field.choice().branches()) {
            if (!isSupportedTypeReference(branch.type())) {
              diagnostics.add(
                  invalidModel(
                      "Unsupported generated choice branch type " + branch.type().toText() + "."));
            }
          }
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
    if ("list".equals(reference.kind())) {
      return reference.itemType() != null && isSupportedTypeReference(reference.itemType());
    }
    if ("union".equals(reference.kind())) {
      return !reference.unionMembers().isEmpty()
          && reference.unionMembers().stream().allMatch(this::isSupportedTypeReference);
    }
    return "model".equals(reference.kind()) || "choice".equals(reference.kind());
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

  private List<GeneratedJavaSource> emitChoice(BindingChoice choice) {
    List<GeneratedJavaSource> sources = new ArrayList<>();
    sources.add(emitChoiceInterface(choice));
    for (BindingChoiceBranch branch : choice.branches()) {
      sources.add(emitChoiceBranch(choice, branch));
    }
    return sources;
  }

  private GeneratedJavaSource emitChoiceInterface(BindingChoice choice) {
    BindingJavaName javaName = choice.javaName();
    String permits =
        choice.branches().stream()
            .map(branch -> branch.branchJavaName().simpleName())
            .collect(Collectors.joining(", "));
    String sourceText =
        "package "
            + javaName.packageName()
            + ";\n\n"
            + "/** Generated sealed model for XML choice "
            + javaName.simpleName()
            + ". */\n"
            + "public sealed interface "
            + javaName.simpleName()
            + " permits "
            + permits
            + " {}\n";
    return new GeneratedJavaSource(javaName, relativePath(javaName), sourceText);
  }

  private GeneratedJavaSource emitChoiceBranch(BindingChoice choice, BindingChoiceBranch branch) {
    BindingJavaName javaName = branch.branchJavaName();
    StringBuilder source = new StringBuilder();
    source.append("package ").append(javaName.packageName()).append(";\n\n");
    String imports = choiceBranchImports(branch);
    if (!imports.isEmpty()) {
      source.append(imports).append('\n');
    }
    source
        .append("/** Generated branch for XML choice ")
        .append(choice.javaName().simpleName())
        .append(". */\n");
    source
        .append("public record ")
        .append(javaName.simpleName())
        .append('(')
        .append(baseType(javaName.packageName(), branch.type()))
        .append(" value) implements ")
        .append(choice.javaName().simpleName())
        .append(" {\n")
        .append("  public ")
        .append(javaName.simpleName())
        .append(" {\n")
        .append(choiceBranchConstructorLine(branch))
        .append("  }\n")
        .append("}\n");
    return new GeneratedJavaSource(javaName, relativePath(javaName), source.toString());
  }

  private String choiceBranchImports(BindingChoiceBranch branch) {
    Set<String> imports = new LinkedHashSet<>();
    if ("list".equals(branch.type().kind())) {
      imports.add("java.util.List");
    }
    addScalarImports(imports, branch.type());
    imports.add("java.util.Objects");
    return imports.stream()
        .sorted()
        .map(value -> "import " + value + ";\n")
        .collect(Collectors.joining());
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
      if ("list".equals(cardinality) || "list".equals(field.type().kind())) {
        imports.add("java.util.List");
      }
      if ("optional".equals(cardinality) || field.semantics().nillable()) {
        imports.add("java.util.Optional");
      }
      addScalarImports(imports, field.type());
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
    if (field.semantics().nillable()) {
      return "Optional<" + base + ">";
    }
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
    if ("list".equals(reference.kind())) {
      return "List<" + baseType(currentPackage, reference.itemType()) + ">";
    }
    if ("union".equals(reference.kind())) {
      return "String";
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
    if ("list".equals(field.type().kind())) {
      return "    "
          + fieldName
          + " = List.copyOf(Objects.requireNonNull("
          + fieldName
          + ", \""
          + fieldName
          + "\"));\n";
    }
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

  private String choiceBranchConstructorLine(BindingChoiceBranch branch) {
    if ("list".equals(branch.type().kind())) {
      return "    value = List.copyOf(Objects.requireNonNull(value, \"value\"));\n";
    }
    return "    Objects.requireNonNull(value, \"value\");\n";
  }

  private void addScalarImports(Set<String> imports, BindingTypeReference reference) {
    if ("scalar".equals(reference.kind()) && "integer".equals(reference.name())) {
      imports.add("java.math.BigInteger");
    }
    if ("scalar".equals(reference.kind()) && "decimal".equals(reference.name())) {
      imports.add("java.math.BigDecimal");
    }
    if ("list".equals(reference.kind())) {
      addScalarImports(imports, reference.itemType());
    }
  }
}
