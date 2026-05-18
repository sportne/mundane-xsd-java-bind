package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingChoiceBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingModel;
import io.github.mundanej.mxjb.generator.core.bind.BindingResult;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingSimpleRestriction;
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
    if ("list".equals(reference.kind())) {
      return reference.itemType() != null && isSupportedTypeReference(reference.itemType(), index);
    }
    if ("union".equals(reference.kind())) {
      return !reference.unionMembers().isEmpty()
          && reference.unionMembers().stream()
              .allMatch(member -> isSupportedTypeReference(member, index));
    }
    if ("fragment".equals(reference.kind())) {
      return "io.github.mundanej.mxjb.runtime.XmlFragment".equals(reference.name());
    }
    return "choice".equals(reference.kind())
        || ("model".equals(reference.kind()) && index.type(reference.name()) != null);
  }

  private boolean isSupportedFieldKind(String kind) {
    return "element".equals(kind)
        || "attribute".equals(kind)
        || "choice".equals(kind)
        || "wildcard".equals(kind);
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
      for (BindingField field :
          type.fields().stream().filter(value -> "choice".equals(value.kind())).toList()) {
        for (BindingChoiceBranch branch : field.choice().branches()) {
          BindingType branchType = modelType(branch.type());
          if (branchType != null && !helperNames.contains(branchType.javaName().qualifiedName())) {
            appendHelper(source, branchType);
          }
        }
      }
    }

    private void appendFieldValidation(StringBuilder source, BindingField field) {
      String accessor = "value." + field.javaName() + "()";
      if ("choice".equals(field.kind())) {
        appendChoiceValidation(source, field, accessor);
        return;
      }
      String shape = field.cardinality().shape();
      if ("list".equals(shape)) {
        if ("wildcard".equals(field.kind())) {
          appendWildcardValidation(source, field, accessor);
        } else {
          appendListValidation(source, field, accessor);
        }
      } else if (field.semantics().nillable()) {
        appendNillableValidation(source, field, accessor);
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
        if (hasValidationRules(field.type())) {
          source.append(" else {\n");
          appendTypeValidation(source, field.type(), accessor, "      ");
          appendFixedValidation(source, field, accessor, "      ");
          source.append("    }\n");
        } else if (field.semantics().hasFixed()) {
          source.append(" else {\n");
          appendFixedValidation(source, field, accessor, "      ");
          source.append("    }\n");
        } else {
          source.append("\n");
        }
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
      if (nestedType == null
          && !hasValidationRules(field.type())
          && !field.semantics().hasFixed()) {
        return;
      }
      source.append("    if (").append(accessor).append(" != null && ").append(accessor);
      source.append(".isPresent()) {\n");
      if (nestedType == null) {
        appendTypeValidation(source, field.type(), accessor + ".get()", "      ");
        appendFixedValidation(source, field, accessor + ".get()", "      ");
      } else {
        source
            .append("      ")
            .append(helperName(nestedType))
            .append("(")
            .append(accessor)
            .append(".get(), location, errors);\n");
      }
      source.append("    }\n");
    }

    private void appendNillableValidation(
        StringBuilder source, BindingField field, String accessor) {
      source.append("    if (").append(accessor).append(" == null) {\n");
      source
          .append("      addError(errors, \"MXJB-GV-001\", \"Missing required value ")
          .append(escape(field.javaName()))
          .append(".\", location);\n");
      source.append("    } else if (").append(accessor).append(".isPresent()) {\n");
      BindingType nestedType = modelType(field);
      if (nestedType == null) {
        appendTypeValidation(source, field.type(), accessor + ".get()", "      ");
      } else {
        source
            .append("      ")
            .append(helperName(nestedType))
            .append("(")
            .append(accessor)
            .append(".get(), location, errors);\n");
      }
      source.append("    }\n");
    }

    private void appendChoiceValidation(StringBuilder source, BindingField field, String accessor) {
      boolean optional = "optional".equals(field.cardinality().shape());
      String valueExpression = optional ? accessor + ".orElse(null)" : accessor;
      if (optional) {
        source.append("    if (").append(accessor).append(" != null && ").append(accessor);
        source.append(".isPresent()) {\n");
      } else {
        source.append("    if (").append(accessor).append(" == null) {\n");
        source
            .append("      addError(errors, \"MXJB-GV-001\", \"Missing required value ")
            .append(escape(field.javaName()))
            .append(".\", location);\n");
        source.append("    } else {\n");
      }
      for (BindingChoiceBranch branch : field.choice().branches()) {
        BindingType nestedType = modelType(branch.type());
        if (nestedType != null) {
          source
              .append("      if (")
              .append(valueExpression)
              .append(" instanceof ")
              .append(branch.branchJavaName().qualifiedName())
              .append(" branch) {\n")
              .append("        ")
              .append(helperName(nestedType))
              .append("(branch.value(), location, errors);\n")
              .append("      }\n");
        } else if (hasValidationRules(branch.type())) {
          source
              .append("      if (")
              .append(valueExpression)
              .append(" instanceof ")
              .append(branch.branchJavaName().qualifiedName())
              .append(" branch) {\n");
          appendTypeValidation(source, branch.type(), "branch.value()", "        ");
          source.append("      }\n");
        }
      }
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
      } else if (hasValidationRules(field.type())) {
        source
            .append("      for (")
            .append(scalarTypeText(field.type()))
            .append(" item : ")
            .append(accessor)
            .append(") {\n");
        source.append("        if (item == null) {\n");
        source
            .append("          addError(errors, \"MXJB-GV-001\", \"Missing required value ")
            .append(escape(field.javaName()))
            .append(".\", location);\n");
        source.append("        } else {\n");
        appendTypeValidation(source, field.type(), "item", "          ");
        source.append("        }\n");
        source.append("      }\n");
      }
      source.append("    }\n");
    }

    private void appendWildcardValidation(
        StringBuilder source, BindingField field, String accessor) {
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
      source
          .append("      for (io.github.mundanej.mxjb.runtime.XmlFragment item : ")
          .append(accessor)
          .append(") {\n")
          .append("        validateFragment(item, \"")
          .append(escape(field.wildcard().namespaceConstraint().kind()))
          .append("\", java.util.Set.of(")
          .append(
              field.wildcard().namespaceConstraint().namespaces().stream()
                  .map(value -> "\"" + escape(value) + "\"")
                  .collect(java.util.stream.Collectors.joining(", ")))
          .append("), location, errors);\n")
          .append("      }\n")
          .append("    }\n");
    }

    private boolean hasFacetRules(BindingTypeReference reference) {
      return reference.restriction() != null && reference.restriction().hasRules();
    }

    private boolean hasValidationRules(BindingTypeReference reference) {
      if (hasFacetRules(reference)) {
        return true;
      }
      if ("list".equals(reference.kind())) {
        return true;
      }
      return "union".equals(reference.kind());
    }

    private void appendTypeValidation(
        StringBuilder source, BindingTypeReference reference, String accessor, String indent) {
      if ("list".equals(reference.kind())) {
        source
            .append(indent)
            .append("for (")
            .append(scalarTypeText(reference.itemType()))
            .append(" item : ")
            .append(accessor)
            .append(") {\n");
        source.append(indent).append("  if (item == null) {\n");
        source
            .append(indent)
            .append(
                "    addError(errors, \"MXJB-GV-001\", \"Missing required value item.\", location);\n");
        source.append(indent).append("  } else {\n");
        appendFacetValidation(source, reference.itemType(), "item", indent + "    ");
        source.append(indent).append("  }\n");
        source.append(indent).append("}\n");
        return;
      }
      if ("union".equals(reference.kind())) {
        appendUnionValidation(source, reference, accessor, indent);
        return;
      }
      appendFacetValidation(source, reference, accessor, indent);
    }

    private void appendUnionValidation(
        StringBuilder source, BindingTypeReference reference, String accessor, String indent) {
      source.append(indent).append("if (!(");
      for (int indexValue = 0; indexValue < reference.unionMembers().size(); indexValue++) {
        if (indexValue > 0) {
          source.append(" || ");
        }
        source.append(unionMemberExpression(reference.unionMembers().get(indexValue), accessor));
      }
      source.append(")) {\n");
      source
          .append(indent)
          .append(
              "  addError(errors, \"MXJB-GV-008\", \"Value does not match any accepted union member.\", location);\n");
      source.append(indent).append("}\n");
    }

    private String unionMemberExpression(BindingTypeReference reference, String accessor) {
      String lexicalMatch = lexicalMatchExpression(reference.name(), accessor);
      if (!hasFacetRules(reference)) {
        return lexicalMatch;
      }
      return "(" + lexicalMatch + " && " + facetMatchExpression(reference, accessor) + ")";
    }

    private String lexicalMatchExpression(String scalar, String accessor) {
      return switch (scalar) {
        case "string" -> "true";
        case "boolean" -> "parseBooleanOrNull(" + accessor + ") != null";
        case "int" -> "parseIntOrNull(" + accessor + ") != null";
        case "integer" -> "parseIntegerOrNull(" + accessor + ") != null";
        case "long" -> "parseLongOrNull(" + accessor + ") != null";
        case "decimal" -> "parseDecimalOrNull(" + accessor + ") != null";
        default -> "false";
      };
    }

    private String facetMatchExpression(BindingTypeReference reference, String accessor) {
      BindingSimpleRestriction restriction = reference.restriction();
      List<String> predicates = new ArrayList<>();
      if (!restriction.enumerations().isEmpty()) {
        List<String> enumerationPredicates = new ArrayList<>();
        for (String value : restriction.enumerations()) {
          enumerationPredicates.add(
              enumerationMatchExpression(restriction.baseScalar(), accessor, value));
        }
        predicates.add("(" + String.join(" || ", enumerationPredicates) + ")");
      }
      if (restriction.length() != null) {
        predicates.add(accessor + ".length() == " + restriction.length());
      }
      if (restriction.minLength() != null) {
        predicates.add(accessor + ".length() >= " + restriction.minLength());
      }
      if (restriction.maxLength() != null) {
        predicates.add(accessor + ".length() <= " + restriction.maxLength());
      }
      if (restriction.minInclusive() != null) {
        predicates.add(
            parsedValueExpression(restriction.baseScalar(), accessor)
                + ".compareTo("
                + numericLiteral(restriction.baseScalar(), restriction.minInclusive())
                + ") >= 0");
      }
      if (restriction.maxInclusive() != null) {
        predicates.add(
            parsedValueExpression(restriction.baseScalar(), accessor)
                + ".compareTo("
                + numericLiteral(restriction.baseScalar(), restriction.maxInclusive())
                + ") <= 0");
      }
      for (String pattern : restriction.patterns()) {
        predicates.add(
            "java.util.regex.Pattern.matches(\"" + escape(pattern) + "\", " + accessor + ")");
      }
      return predicates.isEmpty() ? "true" : String.join(" && ", predicates);
    }

    private String enumerationMatchExpression(String scalar, String accessor, String value) {
      return switch (scalar) {
        case "string" -> "java.util.Objects.equals(" + accessor + ", \"" + escape(value) + "\")";
        case "boolean" ->
            "java.util.Objects.equals(parseBooleanOrNull("
                + accessor
                + "), "
                + booleanLiteral(value)
                + ")";
        case "int" ->
            "java.util.Objects.equals(parseIntOrNull("
                + accessor
                + "), Integer.valueOf("
                + value
                + "))";
        case "long" ->
            "java.util.Objects.equals(parseLongOrNull("
                + accessor
                + "), Long.valueOf("
                + value
                + "L))";
        case "integer", "decimal" ->
            parsedValueExpression(scalar, accessor)
                + ".compareTo("
                + numericLiteral(scalar, value)
                + ") == 0";
        default -> "false";
      };
    }

    private String parsedValueExpression(String scalar, String accessor) {
      return switch (scalar) {
        case "boolean" -> "parseBooleanOrNull(" + accessor + ")";
        case "int" -> "parseIntOrNull(" + accessor + ")";
        case "integer" -> "parseIntegerOrNull(" + accessor + ")";
        case "long" -> "parseLongOrNull(" + accessor + ")";
        case "decimal" -> "parseDecimalOrNull(" + accessor + ")";
        default -> accessor;
      };
    }

    private void appendFacetValidation(
        StringBuilder source, BindingTypeReference reference, String accessor, String indent) {
      BindingSimpleRestriction restriction = reference.restriction();
      if (restriction == null || !restriction.hasRules()) {
        return;
      }
      appendEnumerationValidation(source, restriction, accessor, indent);
      appendLengthValidation(source, restriction, accessor, indent);
      appendRangeValidation(source, restriction, accessor, indent);
      appendPatternValidation(source, restriction, accessor, indent);
    }

    private void appendFixedValidation(
        StringBuilder source, BindingField field, String accessor, String indent) {
      if (!field.semantics().hasFixed()) {
        return;
      }
      source
          .append(indent)
          .append("if (!(")
          .append(fixedComparison(field.type(), accessor, field.semantics().fixedValue()))
          .append(")) {\n");
      source
          .append(indent)
          .append(
              "  addError(errors, \"MXJB-GV-009\", \"Value does not match the fixed value.\", location);\n");
      source.append(indent).append("}\n");
    }

    private String fixedComparison(
        BindingTypeReference reference, String accessor, String fixedValue) {
      return switch (reference.name()) {
        case "string" ->
            "java.util.Objects.equals(" + accessor + ", \"" + escape(fixedValue) + "\")";
        case "boolean" ->
            "java.util.Objects.equals(" + accessor + ", " + booleanLiteral(fixedValue) + ")";
        case "int" ->
            "java.util.Objects.equals(" + accessor + ", Integer.valueOf(" + fixedValue + "))";
        case "long" ->
            "java.util.Objects.equals(" + accessor + ", Long.valueOf(" + fixedValue + "L))";
        case "integer" ->
            accessor + ".compareTo(new java.math.BigInteger(\"" + escape(fixedValue) + "\")) == 0";
        case "decimal" ->
            accessor + ".compareTo(new java.math.BigDecimal(\"" + escape(fixedValue) + "\")) == 0";
        default -> "false";
      };
    }

    private void appendEnumerationValidation(
        StringBuilder source,
        BindingSimpleRestriction restriction,
        String accessor,
        String indent) {
      if (restriction.enumerations().isEmpty()) {
        return;
      }
      source.append(indent).append("if (!(");
      for (int indexValue = 0; indexValue < restriction.enumerations().size(); indexValue++) {
        if (indexValue > 0) {
          source.append(" || ");
        }
        source.append(
            enumerationComparison(
                restriction.baseScalar(), accessor, restriction.enumerations().get(indexValue)));
      }
      source.append(")) {\n");
      source
          .append(indent)
          .append(
              "  addError(errors, \"MXJB-GV-004\", \"Value is not in the accepted enumeration.\", location);\n");
      source.append(indent).append("}\n");
    }

    private String enumerationComparison(String scalar, String accessor, String value) {
      return switch (scalar) {
        case "string" -> "java.util.Objects.equals(" + accessor + ", \"" + escape(value) + "\")";
        case "boolean" ->
            "java.util.Objects.equals(" + accessor + ", " + booleanLiteral(value) + ")";
        case "int" -> "java.util.Objects.equals(" + accessor + ", Integer.valueOf(" + value + "))";
        case "long" -> "java.util.Objects.equals(" + accessor + ", Long.valueOf(" + value + "L))";
        case "integer" ->
            accessor + ".compareTo(new java.math.BigInteger(\"" + escape(value) + "\")) == 0";
        case "decimal" ->
            accessor + ".compareTo(new java.math.BigDecimal(\"" + escape(value) + "\")) == 0";
        default -> "false";
      };
    }

    private String booleanLiteral(String value) {
      return ("true".equals(value) || "1".equals(value)) ? "Boolean.TRUE" : "Boolean.FALSE";
    }

    private void appendLengthValidation(
        StringBuilder source,
        BindingSimpleRestriction restriction,
        String accessor,
        String indent) {
      if (restriction.length() != null) {
        source
            .append(indent)
            .append("if (")
            .append(accessor)
            .append(".length() != ")
            .append(restriction.length())
            .append(") {\n")
            .append(indent)
            .append(
                "  addError(errors, \"MXJB-GV-005\", \"Value length is outside the accepted range.\", location);\n")
            .append(indent)
            .append("}\n");
      }
      if (restriction.minLength() != null) {
        source
            .append(indent)
            .append("if (")
            .append(accessor)
            .append(".length() < ")
            .append(restriction.minLength())
            .append(") {\n")
            .append(indent)
            .append(
                "  addError(errors, \"MXJB-GV-005\", \"Value length is outside the accepted range.\", location);\n")
            .append(indent)
            .append("}\n");
      }
      if (restriction.maxLength() != null) {
        source
            .append(indent)
            .append("if (")
            .append(accessor)
            .append(".length() > ")
            .append(restriction.maxLength())
            .append(") {\n")
            .append(indent)
            .append(
                "  addError(errors, \"MXJB-GV-005\", \"Value length is outside the accepted range.\", location);\n")
            .append(indent)
            .append("}\n");
      }
    }

    private void appendRangeValidation(
        StringBuilder source,
        BindingSimpleRestriction restriction,
        String accessor,
        String indent) {
      if (restriction.minInclusive() != null) {
        source.append(indent).append("if (").append(accessor).append(".compareTo(");
        source.append(numericLiteral(restriction.baseScalar(), restriction.minInclusive()));
        source.append(") < 0) {\n");
        source
            .append(indent)
            .append(
                "  addError(errors, \"MXJB-GV-006\", \"Value is outside the accepted range.\", location);\n");
        source.append(indent).append("}\n");
      }
      if (restriction.maxInclusive() != null) {
        source.append(indent).append("if (").append(accessor).append(".compareTo(");
        source.append(numericLiteral(restriction.baseScalar(), restriction.maxInclusive()));
        source.append(") > 0) {\n");
        source
            .append(indent)
            .append(
                "  addError(errors, \"MXJB-GV-006\", \"Value is outside the accepted range.\", location);\n");
        source.append(indent).append("}\n");
      }
    }

    private String numericLiteral(String scalar, String value) {
      return switch (scalar) {
        case "int" -> "Integer.valueOf(" + value + ")";
        case "long" -> "Long.valueOf(" + value + "L)";
        case "integer" -> "new java.math.BigInteger(\"" + escape(value) + "\")";
        case "decimal" -> "new java.math.BigDecimal(\"" + escape(value) + "\")";
        default -> throw new IllegalArgumentException("Unsupported numeric scalar " + scalar);
      };
    }

    private void appendPatternValidation(
        StringBuilder source,
        BindingSimpleRestriction restriction,
        String accessor,
        String indent) {
      for (String pattern : restriction.patterns()) {
        source
            .append(indent)
            .append("if (!java.util.regex.Pattern.matches(\"")
            .append(escape(pattern))
            .append("\", ")
            .append(accessor)
            .append(")) {\n")
            .append(indent)
            .append(
                "  addError(errors, \"MXJB-GV-007\", \"Value does not match the accepted pattern.\", location);\n")
            .append(indent)
            .append("}\n");
      }
    }

    private String scalarTypeText(BindingTypeReference reference) {
      if ("union".equals(reference.kind())) {
        return "String";
      }
      if ("list".equals(reference.kind())) {
        return "java.util.List<" + scalarTypeText(reference.itemType()) + ">";
      }
      return switch (reference.name()) {
        case "string" -> "String";
        case "boolean" -> "Boolean";
        case "int" -> "Integer";
        case "integer" -> "java.math.BigInteger";
        case "long" -> "Long";
        case "decimal" -> "java.math.BigDecimal";
        default -> throw new IllegalArgumentException("Unsupported scalar " + reference.name());
      };
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
      if (needsWildcardSupport(rootType, new LinkedHashSet<>())) {
        source
            .append('\n')
            .append("  private static void validateFragment(\n")
            .append("      io.github.mundanej.mxjb.runtime.XmlFragment fragment,\n")
            .append("      String wildcardKind,\n")
            .append("      java.util.Set<String> namespaces,\n")
            .append("      io.github.mundanej.mxjb.runtime.XmlLocation location,\n")
            .append(
                "      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {\n")
            .append("    if (fragment == null) {\n")
            .append(
                "      addError(errors, \"MXJB-GV-001\", \"Missing required wildcard fragment.\", location);\n")
            .append("      return;\n")
            .append("    }\n")
            .append("    if (!wildcardMatches(fragment.name(), wildcardKind, namespaces)) {\n")
            .append(
                "      addError(errors, \"MXJB-GV-009\", "
                    + "\"Wildcard fragment namespace is not accepted.\", location);\n")
            .append("    }\n")
            .append(
                "    for (io.github.mundanej.mxjb.runtime.XmlFragmentContent content : fragment.content()) {\n")
            .append(
                "      if (content instanceof io.github.mundanej.mxjb.runtime.XmlFragmentElement element) {\n")
            .append(
                "        validateFragment(element.fragment(), \"any\", java.util.Set.of(), location, errors);\n")
            .append("      } else if (content == null) {\n")
            .append(
                "        addError(errors, \"MXJB-GV-001\", \"Missing required wildcard content.\", location);\n")
            .append("      }\n")
            .append("    }\n")
            .append("  }\n\n")
            .append("  private static boolean wildcardMatches(\n")
            .append("      io.github.mundanej.mxjb.runtime.XmlName name,\n")
            .append("      String kind,\n")
            .append("      java.util.Set<String> namespaces) {\n")
            .append("    if (name == null) {\n")
            .append("      return false;\n")
            .append("    }\n")
            .append("    return switch (kind) {\n")
            .append("      case \"any\" -> true;\n")
            .append("      case \"other\" -> !namespaces.contains(name.namespaceUri());\n")
            .append("      default -> namespaces.contains(name.namespaceUri());\n")
            .append("    };\n")
            .append("  }\n");
      }
      if (!needsUnionSupport()) {
        return;
      }
      source
          .append('\n')
          .append("  private static Boolean parseBooleanOrNull(String value) {\n")
          .append("    return switch (value.trim()) {\n")
          .append("      case \"true\", \"1\" -> Boolean.TRUE;\n")
          .append("      case \"false\", \"0\" -> Boolean.FALSE;\n")
          .append("      default -> null;\n")
          .append("    };\n")
          .append("  }\n\n")
          .append("  private static Integer parseIntOrNull(String value) {\n")
          .append("    try {\n")
          .append("      return Integer.valueOf(value.trim());\n")
          .append("    } catch (NumberFormatException exception) {\n")
          .append("      return null;\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static java.math.BigInteger parseIntegerOrNull(String value) {\n")
          .append("    try {\n")
          .append("      return new java.math.BigInteger(value.trim());\n")
          .append("    } catch (NumberFormatException exception) {\n")
          .append("      return null;\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static Long parseLongOrNull(String value) {\n")
          .append("    try {\n")
          .append("      return Long.valueOf(value.trim());\n")
          .append("    } catch (NumberFormatException exception) {\n")
          .append("      return null;\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static java.math.BigDecimal parseDecimalOrNull(String value) {\n")
          .append("    try {\n")
          .append("      return new java.math.BigDecimal(value.trim());\n")
          .append("    } catch (NumberFormatException exception) {\n")
          .append("      return null;\n")
          .append("    }\n")
          .append("  }\n");
    }

    private boolean needsUnionSupport() {
      return needsUnionSupport(rootType, new LinkedHashSet<>());
    }

    private boolean needsWildcardSupport(BindingType type, Set<String> visited) {
      if (!visited.add(type.javaName().qualifiedName())) {
        return false;
      }
      if (type.fields().stream().anyMatch(field -> "wildcard".equals(field.kind()))) {
        return true;
      }
      for (BindingField field : elements(type)) {
        BindingType nestedType = modelType(field);
        if (nestedType != null && needsWildcardSupport(nestedType, visited)) {
          return true;
        }
      }
      for (BindingField field :
          type.fields().stream().filter(value -> "choice".equals(value.kind())).toList()) {
        for (BindingChoiceBranch branch : field.choice().branches()) {
          BindingType nestedType = modelType(branch.type());
          if (nestedType != null && needsWildcardSupport(nestedType, visited)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean needsUnionSupport(BindingType type, Set<String> visited) {
      if (!visited.add(type.javaName().qualifiedName())) {
        return false;
      }
      for (BindingField field : type.fields()) {
        if (containsUnionType(field.type())) {
          return true;
        }
        BindingType nestedType = modelType(field);
        if (nestedType != null && needsUnionSupport(nestedType, visited)) {
          return true;
        }
        if ("choice".equals(field.kind())) {
          for (BindingChoiceBranch branch : field.choice().branches()) {
            if (containsUnionType(branch.type())) {
              return true;
            }
            BindingType branchType = modelType(branch.type());
            if (branchType != null && needsUnionSupport(branchType, visited)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    private boolean containsUnionType(BindingTypeReference reference) {
      if ("union".equals(reference.kind())) {
        return true;
      }
      if ("list".equals(reference.kind())) {
        return containsUnionType(reference.itemType());
      }
      return false;
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
      return modelType(field.type());
    }

    private BindingType modelType(BindingTypeReference reference) {
      if (!"model".equals(reference.kind())) {
        return null;
      }
      return index.type(reference.name());
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
