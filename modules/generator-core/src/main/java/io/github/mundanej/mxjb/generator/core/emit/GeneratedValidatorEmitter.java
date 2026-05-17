package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingModel;
import io.github.mundanej.mxjb.generator.core.bind.BindingResult;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Emits deterministic XML validator source from the internal binding model. */
public final class GeneratedValidatorEmitter {
  public GeneratedValidatorEmissionResult emit(BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return GeneratedValidatorEmissionResult.empty(bindingResult.diagnostics());
    }
    return emit(bindingResult.model());
  }

  public GeneratedValidatorEmissionResult emit(BindingModel model) {
    ModelIndex index = new ModelIndex(model);
    List<SchemaDiagnostic> diagnostics = validate(model, index);
    if (!diagnostics.isEmpty()) {
      return GeneratedValidatorEmissionResult.empty(diagnostics);
    }

    List<GeneratedJavaSource> sources =
        model.rootElements().stream()
            .sorted(Comparator.comparing(root -> root.xmlName().toText()))
            .map(root -> emitRootValidator(root, index))
            .toList();
    return new GeneratedValidatorEmissionResult(sources, List.of());
  }

  private List<SchemaDiagnostic> validate(BindingModel model, ModelIndex index) {
    List<SchemaDiagnostic> diagnostics = new ArrayList<>();
    Set<String> validatorNames = new LinkedHashSet<>();
    for (BindingRootElement root : model.rootElements()) {
      if (!"model".equals(root.type().kind())) {
        diagnostics.add(
            invalidModel("Root validator requires model type " + root.xmlName().toText() + "."));
        continue;
      }
      BindingType rootType = index.type(root.type().name());
      if (rootType == null) {
        diagnostics.add(
            invalidModel("Missing root validator model type " + root.type().name() + "."));
        continue;
      }
      BindingJavaName validatorName = validatorName(rootType.javaName());
      if (!validatorNames.add(validatorName.qualifiedName())) {
        diagnostics.add(
            invalidModel("Duplicate root validator " + validatorName.qualifiedName() + "."));
      }
    }
    for (BindingType type : model.types()) {
      if (!"record".equals(type.shape())) {
        diagnostics.add(invalidModel("Unsupported validator model shape " + type.shape() + "."));
      }
      for (BindingField field : type.fields()) {
        if (!isSupportedTypeReference(field.type(), index)) {
          diagnostics.add(
              invalidModel("Unsupported validator field type " + field.type().toText() + "."));
        }
        if (!isSupportedFieldKind(field.kind())) {
          diagnostics.add(invalidModel("Unsupported validator field kind " + field.kind() + "."));
        }
        if ("attribute".equals(field.kind()) && "model".equals(field.type().kind())) {
          diagnostics.add(
              invalidModel(
                  "Validator attributes require scalar type " + field.type().toText() + "."));
        }
        if ("attribute".equals(field.kind()) && "list".equals(field.cardinality().shape())) {
          diagnostics.add(invalidModel("Validator attributes do not support list cardinality."));
        }
        if (!isSupportedCardinality(field.cardinality().shape())) {
          diagnostics.add(
              invalidModel(
                  "Unsupported validator cardinality " + field.cardinality().toText() + "."));
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

  private boolean isSupportedTypeReference(BindingTypeReference reference, ModelIndex index) {
    if ("scalar".equals(reference.kind())) {
      return switch (reference.name()) {
        case "string", "boolean", "int", "integer", "long", "decimal" -> true;
        default -> false;
      };
    }
    return "model".equals(reference.kind()) && index.type(reference.name()) != null;
  }

  private boolean isSupportedFieldKind(String kind) {
    return "element".equals(kind) || "attribute".equals(kind);
  }

  private boolean isSupportedCardinality(String shape) {
    return "required".equals(shape) || "optional".equals(shape) || "list".equals(shape);
  }

  private SchemaDiagnostic invalidModel(String message) {
    return new SchemaDiagnostic(
        DiagnosticCode.SCHEMA_VALIDATOR_EMISSION_INVALID_MODEL, "validator-emission", message);
  }

  private GeneratedJavaSource emitRootValidator(BindingRootElement root, ModelIndex index) {
    BindingType rootType = Objects.requireNonNull(index.type(root.type().name()));
    BindingJavaName validatorName = validatorName(rootType.javaName());
    SourceState sourceState = new SourceState(rootType, validatorName, index);
    return new GeneratedJavaSource(
        validatorName, relativePath(validatorName), sourceState.sourceText());
  }

  private BindingJavaName validatorName(BindingJavaName modelName) {
    return new BindingJavaName(
        modelName.packageName() + ".xml", modelName.simpleName() + "XmlValidator");
  }

  private Path relativePath(BindingJavaName validatorName) {
    return Path.of(
        validatorName.packageName().replace('.', '/'), validatorName.simpleName() + ".java");
  }

  private static final class SourceState {
    private final BindingType rootType;
    private final BindingJavaName validatorName;
    private final ModelIndex index;
    private final Set<String> helperNames = new LinkedHashSet<>();

    private SourceState(BindingType rootType, BindingJavaName validatorName, ModelIndex index) {
      this.rootType = rootType;
      this.validatorName = validatorName;
      this.index = index;
    }

    private String sourceText() {
      StringBuilder source = new StringBuilder();
      source.append("package ").append(validatorName.packageName()).append(";\n\n");
      source
          .append("/** Generated XML validator for {@link ")
          .append(rootType.javaName().qualifiedName())
          .append("}. */\n");
      source.append("public final class ").append(validatorName.simpleName()).append(" {\n");
      source.append("  private ").append(validatorName.simpleName()).append("() {}\n\n");
      appendPublicValidate(source);
      appendHelper(source, rootType);
      appendSharedHelpers(source);
      source.append("}\n");
      return source.toString();
    }

    private void appendPublicValidate(StringBuilder source) {
      source
          .append("  public static io.github.mundanej.mxjb.runtime.ValidationResult validate(\n")
          .append("      ")
          .append(typeText(rootType))
          .append(" value) {\n")
          .append("    java.util.Objects.requireNonNull(value, \"value\");\n")
          .append(
              "    java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors =\n")
          .append("        new java.util.ArrayList<>();\n")
          .append("    ")
          .append(helperName(rootType))
          .append("(value, io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN, errors);\n")
          .append("    return validationResult(errors);\n")
          .append("  }\n\n")
          .append("  public static io.github.mundanej.mxjb.runtime.ValidationResult validate(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input) {\n")
          .append("    java.util.Objects.requireNonNull(input, \"input\");\n")
          .append("    try {\n")
          .append("      return validate(")
          .append(readerName(rootType.javaName()).qualifiedName())
          .append(".read(input));\n")
          .append("    } catch (io.github.mundanej.mxjb.runtime.XmlReadException exception) {\n")
          .append(
              "      io.github.mundanej.mxjb.runtime.XmlDiagnostic diagnostic = exception.diagnostic();\n")
          .append("      return io.github.mundanej.mxjb.runtime.ValidationResult.invalid(\n")
          .append("          new io.github.mundanej.mxjb.runtime.ValidationError(\n")
          .append(
              "              diagnostic.code(), diagnostic.message(), diagnostic.location()));\n")
          .append("    }\n")
          .append("  }\n\n");
    }

    private void appendHelper(StringBuilder source, BindingType type) {
      if (!helperNames.add(type.javaName().qualifiedName())) {
        return;
      }
      source
          .append("  private static void ")
          .append(helperName(type))
          .append("(\n")
          .append("      ")
          .append(typeText(type))
          .append(" value,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlLocation location,\n")
          .append(
              "      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {\n");
      for (BindingField field : type.fields().stream().sorted(fieldComparator()).toList()) {
        appendFieldValidation(source, field);
      }
      source.append("  }\n\n");
      for (BindingField field : elements(type)) {
        BindingType nestedType = modelType(field);
        if (nestedType != null && !helperNames.contains(nestedType.javaName().qualifiedName())) {
          appendHelper(source, nestedType);
        }
      }
    }

    private void appendFieldValidation(StringBuilder source, BindingField field) {
      String accessor = "value." + field.javaName() + "()";
      String shape = field.cardinality().shape();
      if ("list".equals(shape)) {
        appendListValidation(source, field, accessor);
      } else if ("optional".equals(shape)) {
        appendOptionalValidation(source, field, accessor);
      } else {
        appendRequiredValidation(source, field, accessor);
      }
    }

    private void appendRequiredValidation(
        StringBuilder source, BindingField field, String accessor) {
      source.append("    if (").append(accessor).append(" == null) {\n");
      source
          .append("      addError(errors, \"MXJB-GV-001\", \"Missing required value ")
          .append(escape(field.javaName()))
          .append(".\", location);\n");
      source.append("    }");
      BindingType nestedType = modelType(field);
      if (nestedType == null) {
        source.append("\n");
      } else {
        source.append(" else {\n");
        source
            .append("      ")
            .append(helperName(nestedType))
            .append("(")
            .append(accessor)
            .append(", location, errors);\n");
        source.append("    }\n");
      }
    }

    private void appendOptionalValidation(
        StringBuilder source, BindingField field, String accessor) {
      BindingType nestedType = modelType(field);
      if (nestedType == null) {
        return;
      }
      source.append("    if (").append(accessor).append(" != null && ").append(accessor);
      source.append(".isPresent()) {\n");
      source
          .append("      ")
          .append(helperName(nestedType))
          .append("(")
          .append(accessor)
          .append(".get(), location, errors);\n");
      source.append("    }\n");
    }

    private void appendListValidation(StringBuilder source, BindingField field, String accessor) {
      source.append("    if (").append(accessor).append(" == null) {\n");
      source
          .append("      addError(errors, \"MXJB-GV-002\", \"Too few values for ")
          .append(escape(field.javaName()))
          .append(".\", location);\n");
      source.append("    } else {\n");
      if (field.cardinality().minOccurs() > 0) {
        source.append("      if (").append(accessor).append(".size() < ");
        source.append(field.cardinality().minOccurs()).append(") {\n");
        source
            .append("        addError(errors, \"MXJB-GV-002\", \"Too few values for ")
            .append(escape(field.javaName()))
            .append(".\", location);\n");
        source.append("      }\n");
      }
      if (!"unbounded".equals(field.cardinality().maxOccurs())) {
        source.append("      if (").append(accessor).append(".size() > ");
        source.append(Integer.parseInt(field.cardinality().maxOccurs())).append(") {\n");
        source
            .append("        addError(errors, \"MXJB-GV-003\", \"Too many values for ")
            .append(escape(field.javaName()))
            .append(".\", location);\n");
        source.append("      }\n");
      }
      BindingType nestedType = modelType(field);
      if (nestedType != null) {
        source
            .append("      for (")
            .append(typeText(nestedType))
            .append(" item : ")
            .append(accessor)
            .append(") {\n");
        source.append("        if (item == null) {\n");
        source
            .append("          addError(errors, \"MXJB-GV-001\", \"Missing required value ")
            .append(escape(field.javaName()))
            .append(".\", location);\n");
        source.append("        } else {\n");
        source
            .append("          ")
            .append(helperName(nestedType))
            .append("(item, location, errors);\n");
        source.append("        }\n");
        source.append("      }\n");
      }
      source.append("    }\n");
    }

    private void appendSharedHelpers(StringBuilder source) {
      source
          .append(
              "  private static io.github.mundanej.mxjb.runtime.ValidationResult validationResult(\n")
          .append(
              "      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {\n")
          .append("    if (errors.isEmpty()) {\n")
          .append("      return io.github.mundanej.mxjb.runtime.ValidationResult.valid();\n")
          .append("    }\n")
          .append("    return io.github.mundanej.mxjb.runtime.ValidationResult.invalid(errors);\n")
          .append("  }\n\n")
          .append("  private static void addError(\n")
          .append(
              "      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors,\n")
          .append("      String code,\n")
          .append("      String message,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlLocation location) {\n")
          .append(
              "    errors.add(new io.github.mundanej.mxjb.runtime.ValidationError(code, message, location));\n")
          .append("  }\n");
    }

    private List<BindingField> elements(BindingType type) {
      return type.fields().stream()
          .filter(field -> "element".equals(field.kind()))
          .sorted(Comparator.comparingInt(BindingField::order))
          .toList();
    }

    private Comparator<BindingField> fieldComparator() {
      return Comparator.comparingInt(
              (BindingField field) -> "attribute".equals(field.kind()) ? 0 : 1)
          .thenComparingInt(BindingField::order)
          .thenComparing(BindingField::javaName);
    }

    private BindingType modelType(BindingField field) {
      if (!"model".equals(field.type().kind())) {
        return null;
      }
      return index.type(field.type().name());
    }

    private String helperName(BindingType type) {
      return "validate" + type.javaName().simpleName();
    }

    private String typeText(BindingType type) {
      return type.javaName().qualifiedName();
    }

    private BindingJavaName readerName(BindingJavaName modelName) {
      return new BindingJavaName(
          modelName.packageName() + ".xml", modelName.simpleName() + "XmlReader");
    }

    private String escape(String value) {
      StringBuilder escaped = new StringBuilder();
      for (int indexValue = 0; indexValue < value.length(); indexValue++) {
        char character = value.charAt(indexValue);
        switch (character) {
          case '\\' -> escaped.append("\\\\");
          case '"' -> escaped.append("\\\"");
          case '\n' -> escaped.append("\\n");
          case '\r' -> escaped.append("\\r");
          case '\t' -> escaped.append("\\t");
          default -> {
            if (character < 0x20) {
              escaped.append(String.format("\\u%04x", (int) character));
            } else {
              escaped.append(character);
            }
          }
        }
      }
      return escaped.toString();
    }
  }

  private static final class ModelIndex {
    private final Map<String, BindingType> types = new LinkedHashMap<>();

    private ModelIndex(BindingModel model) {
      for (BindingType type : model.types()) {
        types.put(type.javaName().qualifiedName(), type);
      }
    }

    private BindingType type(String qualifiedName) {
      return types.get(qualifiedName);
    }
  }
}
