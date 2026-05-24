package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingChoiceBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingModel;
import io.github.mundanej.mxjb.generator.core.bind.BindingResult;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingSimpleRestriction;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.XmlSchemaBuiltIns;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityConstraint;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityField;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityPath;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityStep;
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
        if ("content".equals(field.kind()) && field.content() == null) {
          diagnostics.add(invalidModel("Validator content field is missing content metadata."));
        }
        if ("content".equals(field.kind()) && !"list".equals(field.cardinality().shape())) {
          diagnostics.add(invalidModel("Validator content fields require list cardinality."));
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
      return XmlSchemaBuiltIns.isSupported(reference.name());
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
    if ("xmlAttribute".equals(reference.kind())) {
      return "io.github.mundanej.mxjb.runtime.XmlAttribute".equals(reference.name());
    }
    return "choice".equals(reference.kind())
        || ("model".equals(reference.kind()) && index.type(reference.name()) != null);
  }

  private boolean isSupportedFieldKind(String kind) {
    return "element".equals(kind)
        || "attribute".equals(kind)
        || "anyAttribute".equals(kind)
        || "simpleContent".equals(kind)
        || "choice".equals(kind)
        || "wildcard".equals(kind)
        || "content".equals(kind);
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
    SourceState sourceState = new SourceState(root, rootType, validatorName, index);
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
    private final BindingRootElement root;
    private final BindingType rootType;
    private final BindingJavaName validatorName;
    private final ModelIndex index;
    private final Set<String> helperNames = new LinkedHashSet<>();
    private final Set<String> identityHelperNames = new LinkedHashSet<>();

    private SourceState(
        BindingRootElement root,
        BindingType rootType,
        BindingJavaName validatorName,
        ModelIndex index) {
      this.root = root;
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
      if (hasIdentityConstraints()) {
        appendIdentityHelper(source, rootType);
      }
      appendSharedHelpers(source);
      source.append("}\n");
      return source.toString();
    }

    private boolean hasIdentityConstraints() {
      return !root.identityConstraints().isEmpty();
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
          .append(
              hasIdentityConstraints()
                  ? "    validateIdentityConstraints(identityNode"
                      + rootType.javaName().simpleName()
                      + "(\""
                      + escape(root.xmlName().toText())
                      + "\", value), errors);\n"
                  : "")
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
      for (BindingField field :
          type.fields().stream().filter(value -> "content".equals(value.kind())).toList()) {
        for (BindingContentBranch branch : field.content().branches()) {
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
      if ("content".equals(field.kind())) {
        appendContentValidation(source, field, accessor);
        return;
      }
      if ("anyAttribute".equals(field.kind())) {
        appendAnyAttributeValidation(source, field, accessor);
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
      if ("list".equals(field.cardinality().shape())) {
        appendChoiceListValidation(source, field, accessor);
        return;
      }
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

    private void appendChoiceListValidation(
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
          .append("      for (")
          .append(field.type().name())
          .append(" item : ")
          .append(accessor)
          .append(") {\n");
      source.append("        if (item == null) {\n");
      source
          .append("          addError(errors, \"MXJB-GV-001\", \"Missing required value ")
          .append(escape(field.javaName()))
          .append(".\", location);\n");
      source.append("        }");
      for (BindingChoiceBranch branch : field.choice().branches()) {
        source
            .append(" else if (item instanceof ")
            .append(branch.branchJavaName().qualifiedName())
            .append(" branch) {\n");
        BindingType nestedType = modelType(branch.type());
        if (nestedType != null) {
          source
              .append("          ")
              .append(helperName(nestedType))
              .append("(branch.value(), location, errors);\n");
        } else if (hasValidationRules(branch.type())) {
          appendTypeValidation(source, branch.type(), "branch.value()", "          ");
        }
        source.append("        }");
      }
      source.append(" else {\n");
      source.append(
          "          addError(errors, \"MXJB-GV-009\", \"Unsupported choice branch.\", location);\n");
      source.append("        }\n");
      source.append("      }\n");
      source.append("    }\n");
    }

    private void appendContentValidation(
        StringBuilder source, BindingField field, String accessor) {
      source.append("    if (").append(accessor).append(" == null) {\n");
      source
          .append("      addError(errors, \"MXJB-GV-002\", \"Too few values for ")
          .append(escape(field.javaName()))
          .append(".\", location);\n");
      source.append("    } else {\n");
      if (!field.content().groups().isEmpty()
          && !"mixed content".equals(field.content().modelKind())) {
        appendGroupedContentValidation(source, field, accessor);
        source.append("    }\n");
        return;
      }
      source.append("      int lastContentOrder = -1;\n");
      for (BindingContentBranch branch : field.content().branches()) {
        if ("text".equals(branch.kind())) {
          continue;
        }
        source.append("      int ").append(branch.javaName()).append("Count = 0;\n");
      }
      source
          .append("      for (")
          .append(field.type().name())
          .append(" item : ")
          .append(accessor)
          .append(") {\n");
      source.append("        if (item == null) {\n");
      source
          .append("          addError(errors, \"MXJB-GV-001\", \"Missing required value ")
          .append(escape(field.javaName()))
          .append(".\", location);\n");
      source.append("        }");
      for (BindingContentBranch branch : field.content().branches()) {
        source
            .append(" else if (item instanceof ")
            .append(branch.branchJavaName().qualifiedName())
            .append(" branch) {\n");
        appendContentBranchValidation(source, branch);
        source.append("        }");
      }
      source.append(" else {\n");
      source.append(
          "          addError(errors, \"MXJB-GV-009\", \"Unsupported mixed content branch.\", location);\n");
      source.append("        }\n");
      source.append("      }\n");
      for (BindingContentBranch branch : field.content().branches()) {
        if ("text".equals(branch.kind())) {
          continue;
        }
        if (branch.cardinality().minOccurs() > 0) {
          source.append("      if (").append(branch.javaName()).append("Count < ");
          source.append(branch.cardinality().minOccurs()).append(") {\n");
          source
              .append("        addError(errors, \"MXJB-GV-002\", \"Too few values for ")
              .append(escape(branch.javaName()))
              .append(".\", location);\n");
          source.append("      }\n");
        }
        if (!"unbounded".equals(branch.cardinality().maxOccurs())) {
          source.append("      if (").append(branch.javaName()).append("Count > ");
          source.append(Integer.parseInt(branch.cardinality().maxOccurs())).append(") {\n");
          source
              .append("        addError(errors, \"MXJB-GV-003\", \"Too many values for ")
              .append(escape(branch.javaName()))
              .append(".\", location);\n");
          source.append("      }\n");
        }
      }
      if (!field.content().groups().isEmpty()) {
        appendGroupedContentValidation(source, field, accessor);
      }
      source.append("    }\n");
    }

    private void appendGroupedContentValidation(
        StringBuilder source, BindingField field, String accessor) {
      source
          .append("      for (")
          .append(field.type().name())
          .append(" item : ")
          .append(accessor)
          .append(") {\n");
      source.append("        if (item == null) {\n");
      source
          .append("          addError(errors, \"MXJB-GV-001\", \"Missing required value ")
          .append(escape(field.javaName()))
          .append(".\", location);\n");
      source.append("        }");
      for (BindingContentBranch branch : field.content().branches()) {
        source
            .append(" else if (item instanceof ")
            .append(branch.branchJavaName().qualifiedName())
            .append(" branch) {\n");
        appendGroupedContentBranchValueValidation(source, branch);
        source.append("        }");
      }
      source.append(" else {\n");
      source.append(
          "          addError(errors, \"MXJB-GV-009\", \"Unsupported grouped content branch.\", location);\n");
      source.append("        }\n");
      source.append("      }\n");
      for (int groupIndex = 0; groupIndex < field.content().groups().size(); groupIndex++) {
        io.github.mundanej.mxjb.generator.core.bind.BindingContentGroup group =
            field.content().groups().get(groupIndex);
        if ("choice".equals(group.modelKind())) {
          appendChoiceGroupValidation(source, field, accessor, group, groupIndex);
        } else if ("all".equals(group.modelKind())) {
          appendAllGroupValidation(source, field, accessor, group, groupIndex);
        } else {
          appendSequenceGroupValidation(source, field, accessor, group, groupIndex);
        }
      }
    }

    private void appendGroupedContentBranchValueValidation(
        StringBuilder source, BindingContentBranch branch) {
      source.append("          if (branch.value() == null) {\n");
      source
          .append("            addError(errors, \"MXJB-GV-001\", \"Missing required value ")
          .append(escape(branch.javaName()))
          .append(".\", location);\n");
      source.append("          } else {\n");
      if ("wildcard".equals(branch.kind())) {
        source
            .append("            validateFragment(branch.value(), \"")
            .append(escape(branch.wildcard().namespaceConstraint().kind()))
            .append("\", java.util.Set.of(")
            .append(
                branch.wildcard().namespaceConstraint().namespaces().stream()
                    .map(value -> "\"" + escape(value) + "\"")
                    .collect(java.util.stream.Collectors.joining(", ")))
            .append("), location, errors);\n");
      } else {
        BindingType nestedType = modelType(branch.type());
        if (nestedType != null) {
          source
              .append("            ")
              .append(helperName(nestedType))
              .append("(branch.value(), location, errors);\n");
        } else if (hasValidationRules(branch.type())) {
          appendTypeValidation(source, branch.type(), "branch.value()", "            ");
        }
      }
      source.append("          }\n");
    }

    private void appendChoiceGroupValidation(
        StringBuilder source,
        BindingField field,
        String accessor,
        io.github.mundanej.mxjb.generator.core.bind.BindingContentGroup group,
        int groupIndex) {
      String countName = field.javaName() + "Choice" + groupIndex + "Count";
      source.append("      int ").append(countName).append(" = 0;\n");
      source.append("      for (").append(field.type().name()).append(" item : ");
      source.append(accessor).append(") {\n");
      source.append("        if (");
      appendInstanceOfAny(source, "item", group.branches());
      source.append(") {\n");
      source.append("          ").append(countName).append("++;\n");
      source.append("        }\n");
      source.append("      }\n");
      appendGroupedCountValidation(source, group, countName);
    }

    private void appendAllGroupValidation(
        StringBuilder source,
        BindingField field,
        String accessor,
        io.github.mundanej.mxjb.generator.core.bind.BindingContentGroup group,
        int groupIndex) {
      String totalName = field.javaName() + "All" + groupIndex + "Count";
      source.append("      int ").append(totalName).append(" = 0;\n");
      for (BindingContentBranch branch : group.branches()) {
        source
            .append("      int ")
            .append(branch.javaName())
            .append(groupIndex)
            .append("Count = 0;\n");
      }
      source.append("      for (").append(field.type().name()).append(" item : ");
      source.append(accessor).append(") {\n");
      for (int indexValue = 0; indexValue < group.branches().size(); indexValue++) {
        BindingContentBranch branch = group.branches().get(indexValue);
        source.append(indexValue == 0 ? "        if (" : "        } else if (");
        source.append("item instanceof ").append(branch.branchJavaName().qualifiedName());
        source.append(") {\n");
        source
            .append("          ")
            .append(branch.javaName())
            .append(groupIndex)
            .append("Count++;\n");
        source.append("          ").append(totalName).append("++;\n");
      }
      source.append("        }\n");
      source.append("      }\n");
      source.append("      if (").append(totalName).append(" > 0) {\n");
      for (BindingContentBranch branch : group.branches()) {
        if (branch.cardinality().minOccurs() > 0) {
          source
              .append("        if (")
              .append(branch.javaName())
              .append(groupIndex)
              .append("Count < ");
          source.append(branch.cardinality().minOccurs()).append(") {\n");
          source
              .append("          addError(errors, \"MXJB-GV-002\", \"Too few values for ")
              .append(escape(branch.javaName()))
              .append(".\", location);\n");
          source.append("        }\n");
        }
        source
            .append("        if (")
            .append(branch.javaName())
            .append(groupIndex)
            .append("Count > 1) {\n");
        source
            .append("          addError(errors, \"MXJB-GV-003\", \"Too many values for ")
            .append(escape(branch.javaName()))
            .append(".\", location);\n");
        source.append("        }\n");
      }
      source.append("      }\n");
      appendGroupedCountValidation(source, group, totalName);
    }

    private void appendSequenceGroupValidation(
        StringBuilder source,
        BindingField field,
        String accessor,
        io.github.mundanej.mxjb.generator.core.bind.BindingContentGroup group,
        int groupIndex) {
      String expectedName = field.javaName() + "ExpectedOrder" + groupIndex;
      String groupCountName = field.javaName() + "Group" + groupIndex + "Count";
      source.append("      int ").append(expectedName).append(" = 1;\n");
      source.append("      int ").append(groupCountName).append(" = 0;\n");
      source.append("      for (").append(field.type().name()).append(" item : ");
      source.append(accessor).append(") {\n");
      for (int indexValue = 0; indexValue < group.branches().size(); indexValue++) {
        BindingContentBranch branch = group.branches().get(indexValue);
        source.append(indexValue == 0 ? "        if (" : "        } else if (");
        source.append("item instanceof ").append(branch.branchJavaName().qualifiedName());
        source.append(") {\n");
        source.append("          if (").append(expectedName).append(" != ");
        source.append(indexValue + 1).append(") {\n");
        source.append(
            "            addError(errors, \"MXJB-GV-009\", \"Out-of-order grouped content.\", location);\n");
        source.append("          }\n");
        if (indexValue == group.branches().size() - 1) {
          source.append("          ").append(groupCountName).append("++;\n");
          source.append("          ").append(expectedName).append(" = 1;\n");
        } else {
          source.append("          ").append(expectedName).append("++;\n");
        }
      }
      source.append("        }\n");
      source.append("      }\n");
      source.append("      if (").append(expectedName).append(" != 1) {\n");
      source.append(
          "        addError(errors, \"MXJB-GV-002\", \"Incomplete grouped content.\", location);\n");
      source.append("      }\n");
      appendGroupedCountValidation(source, group, groupCountName);
    }

    private void appendGroupedCountValidation(
        StringBuilder source,
        io.github.mundanej.mxjb.generator.core.bind.BindingContentGroup group,
        String countName) {
      if (group.cardinality().minOccurs() > 0) {
        source.append("      if (").append(countName).append(" < ");
        source.append(group.cardinality().minOccurs()).append(") {\n");
        source.append(
            "        addError(errors, \"MXJB-GV-002\", \"Too few grouped content values.\", location);\n");
        source.append("      }\n");
      }
      if (!"unbounded".equals(group.cardinality().maxOccurs())) {
        source.append("      if (").append(countName).append(" > ");
        source.append(Integer.parseInt(group.cardinality().maxOccurs())).append(") {\n");
        source.append(
            "        addError(errors, \"MXJB-GV-003\", \"Too many grouped content values.\", location);\n");
        source.append("      }\n");
      }
    }

    private void appendInstanceOfAny(
        StringBuilder source, String valueName, List<BindingContentBranch> branches) {
      for (int indexValue = 0; indexValue < branches.size(); indexValue++) {
        if (indexValue > 0) {
          source.append(" || ");
        }
        source
            .append(valueName)
            .append(" instanceof ")
            .append(branches.get(indexValue).branchJavaName().qualifiedName());
      }
    }

    private void appendContentBranchValidation(StringBuilder source, BindingContentBranch branch) {
      if ("text".equals(branch.kind())) {
        source.append("          if (branch.value() == null) {\n");
        source.append(
            "            addError(errors, \"MXJB-GV-001\", \"Missing required value text.\", location);\n");
        source.append("          }\n");
        return;
      }
      source.append("          if (").append(branch.order()).append(" < lastContentOrder) {\n");
      source.append(
          "            addError(errors, \"MXJB-GV-009\", \"Out-of-order mixed content.\", location);\n");
      source.append("          }\n");
      source
          .append("          lastContentOrder = Math.max(lastContentOrder, ")
          .append(branch.order())
          .append(");\n");
      source.append("          ").append(branch.javaName()).append("Count++;\n");
      source.append("          if (branch.value() == null) {\n");
      source
          .append("            addError(errors, \"MXJB-GV-001\", \"Missing required value ")
          .append(escape(branch.javaName()))
          .append(".\", location);\n");
      source.append("          } else {\n");
      if ("wildcard".equals(branch.kind())) {
        source
            .append("            validateFragment(branch.value(), \"")
            .append(escape(branch.wildcard().namespaceConstraint().kind()))
            .append("\", java.util.Set.of(")
            .append(
                branch.wildcard().namespaceConstraint().namespaces().stream()
                    .map(value -> "\"" + escape(value) + "\"")
                    .collect(java.util.stream.Collectors.joining(", ")))
            .append("), location, errors);\n");
      } else {
        BindingType nestedType = modelType(branch.type());
        if (nestedType != null) {
          source
              .append("            ")
              .append(helperName(nestedType))
              .append("(branch.value(), location, errors);\n");
        } else if (hasValidationRules(branch.type())) {
          appendTypeValidation(source, branch.type(), "branch.value()", "            ");
        }
      }
      source.append("          }\n");
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

    private void appendAnyAttributeValidation(
        StringBuilder source, BindingField field, String accessor) {
      source.append("    if (").append(accessor).append(" == null) {\n");
      source
          .append("      addError(errors, \"MXJB-GV-002\", \"Too few values for ")
          .append(escape(field.javaName()))
          .append(".\", location);\n");
      source.append("    } else {\n");
      source
          .append("      for (io.github.mundanej.mxjb.runtime.XmlAttribute item : ")
          .append(accessor)
          .append(") {\n");
      source
          .append("        validateWildcardAttribute(item, \"")
          .append(escape(field.wildcard().namespaceConstraint().kind()))
          .append("\", java.util.Set.of(")
          .append(
              field.wildcard().namespaceConstraint().namespaces().stream()
                  .map(value -> "\"" + escape(value) + "\"")
                  .collect(java.util.stream.Collectors.joining(", ")))
          .append("), java.util.Set.of(")
          .append(
              field.wildcard().excludedNames().stream()
                  .map(
                      name ->
                          "new io.github.mundanej.mxjb.runtime.XmlName(\""
                              + escape(name.namespace())
                              + "\", \""
                              + escape(name.localName())
                              + "\")")
                  .collect(java.util.stream.Collectors.joining(", ")))
          .append("), location, errors);\n");
      source.append("      }\n");
      source.append("    }\n");
    }

    private void appendIdentityHelper(StringBuilder source, BindingType type) {
      if (!identityHelperNames.add(type.javaName().qualifiedName())) {
        return;
      }
      source
          .append("  private static IdentityNode identityNode")
          .append(type.javaName().simpleName())
          .append("(String name, ")
          .append(typeText(type))
          .append(" value) {\n")
          .append("    java.util.LinkedHashMap<String, Object> attributes =\n")
          .append("        new java.util.LinkedHashMap<>();\n")
          .append("    java.util.ArrayList<IdentityNode> children = new java.util.ArrayList<>();\n")
          .append("    Object text = null;\n");
      for (BindingField field : type.fields().stream().sorted(fieldComparator()).toList()) {
        appendIdentityField(source, field);
      }
      source
          .append("    return new IdentityNode(name, text, attributes, children);\n")
          .append("  }\n\n");
      for (BindingField field : elements(type)) {
        BindingType nestedType = modelType(field);
        if (nestedType != null
            && !identityHelperNames.contains(nestedType.javaName().qualifiedName())) {
          appendIdentityHelper(source, nestedType);
        }
      }
      for (BindingField field :
          type.fields().stream().filter(value -> "choice".equals(value.kind())).toList()) {
        for (BindingChoiceBranch branch : field.choice().branches()) {
          BindingType branchType = modelType(branch.type());
          if (branchType != null
              && !identityHelperNames.contains(branchType.javaName().qualifiedName())) {
            appendIdentityHelper(source, branchType);
          }
        }
      }
      for (BindingField field :
          type.fields().stream().filter(value -> "content".equals(value.kind())).toList()) {
        for (BindingContentBranch branch : field.content().branches()) {
          BindingType branchType = modelType(branch.type());
          if (branchType != null
              && !identityHelperNames.contains(branchType.javaName().qualifiedName())) {
            appendIdentityHelper(source, branchType);
          }
        }
      }
    }

    private void appendIdentityField(StringBuilder source, BindingField field) {
      String accessor = "value." + field.javaName() + "()";
      if ("attribute".equals(field.kind())) {
        appendIdentityAttribute(source, field, accessor);
      } else if ("anyAttribute".equals(field.kind())) {
        appendIdentityAnyAttribute(source, accessor);
      } else if ("simpleContent".equals(field.kind())) {
        appendIdentitySimpleContent(source, field, accessor);
      } else if ("element".equals(field.kind())) {
        appendIdentityElement(source, field, accessor);
      } else if ("choice".equals(field.kind())) {
        appendIdentityChoice(source, field, accessor);
      } else if ("content".equals(field.kind())) {
        appendIdentityContent(source, field, accessor);
      }
    }

    private void appendIdentityAttribute(
        StringBuilder source, BindingField field, String accessor) {
      String name = escape(field.xmlName().toText());
      if ("optional".equals(field.cardinality().shape())) {
        source
            .append("    if (")
            .append(accessor)
            .append(" != null && ")
            .append(accessor)
            .append(".isPresent()) {\n")
            .append("      attributes.put(\"")
            .append(name)
            .append("\", identityScalar(")
            .append(accessor)
            .append(".get()));\n")
            .append("    }\n");
        return;
      }
      source
          .append("    if (")
          .append(accessor)
          .append(" != null) {\n")
          .append("      attributes.put(\"")
          .append(name)
          .append("\", identityScalar(")
          .append(accessor)
          .append("));\n")
          .append("    }\n");
    }

    private void appendIdentityAnyAttribute(StringBuilder source, String accessor) {
      source
          .append("    if (")
          .append(accessor)
          .append(" != null) {\n")
          .append(
              "      for (io.github.mundanej.mxjb.runtime.XmlAttribute attribute : "
                  + accessor
                  + ") {\n")
          .append("        if (attribute != null && attribute.name() != null) {\n")
          .append("          attributes.put(identityName(attribute.name()), attribute.value());\n")
          .append("        }\n")
          .append("      }\n")
          .append("    }\n");
    }

    private void appendIdentitySimpleContent(
        StringBuilder source, BindingField field, String accessor) {
      if ("optional".equals(field.cardinality().shape())) {
        source
            .append("    if (")
            .append(accessor)
            .append(" != null && ")
            .append(accessor)
            .append(".isPresent()) {\n")
            .append("      text = identityScalar(")
            .append(accessor)
            .append(".get());\n")
            .append("    }\n");
        return;
      }
      source
          .append("    if (")
          .append(accessor)
          .append(" != null) {\n")
          .append("      text = identityScalar(")
          .append(accessor)
          .append(");\n")
          .append("    }\n");
    }

    private void appendIdentityElement(StringBuilder source, BindingField field, String accessor) {
      BindingType nestedType = modelType(field);
      String name = escape(field.xmlName().toText());
      String shape = field.cardinality().shape();
      if ("list".equals(shape)) {
        source
            .append("    if (")
            .append(accessor)
            .append(" != null) {\n")
            .append("      for (")
            .append(nestedType == null ? scalarTypeText(field.type()) : typeText(nestedType))
            .append(" item : ")
            .append(accessor)
            .append(") {\n")
            .append("        if (item != null) {\n");
        appendIdentityElementItem(source, nestedType, name, "item", "          ");
        source.append("        }\n").append("      }\n").append("    }\n");
        return;
      }
      String item = "optional".equals(shape) ? accessor + ".get()" : accessor;
      if ("optional".equals(shape)) {
        source
            .append("    if (")
            .append(accessor)
            .append(" != null && ")
            .append(accessor)
            .append(".isPresent()) {\n");
        appendIdentityElementItem(source, nestedType, name, item, "      ");
        source.append("    }\n");
      } else {
        source.append("    if (").append(accessor).append(" != null) {\n");
        appendIdentityElementItem(source, nestedType, name, item, "      ");
        source.append("    }\n");
      }
    }

    private void appendIdentityElementItem(
        StringBuilder source, BindingType nestedType, String name, String value, String indent) {
      source.append(indent).append("children.add(");
      if (nestedType == null) {
        source
            .append("new IdentityNode(\"")
            .append(name)
            .append("\", identityScalar(")
            .append(value)
            .append("), java.util.Map.of(), java.util.List.of())");
      } else {
        source
            .append("identityNode")
            .append(nestedType.javaName().simpleName())
            .append("(\"")
            .append(name)
            .append("\", ")
            .append(value)
            .append(")");
      }
      source.append(");\n");
    }

    private void appendIdentityChoice(StringBuilder source, BindingField field, String accessor) {
      if ("list".equals(field.cardinality().shape())) {
        source.append("    if (").append(accessor).append(" != null) {\n");
        source
            .append("      for (")
            .append(field.type().name())
            .append(" item : ")
            .append(accessor)
            .append(") {\n");
        appendIdentityChoiceBranches(source, field.choice().branches(), "item", "        ");
        source.append("      }\n").append("    }\n");
        return;
      }
      String valueExpression = field.javaName() + "IdentityChoice";
      source.append("    Object ").append(valueExpression).append(" = ");
      if ("optional".equals(field.cardinality().shape())) {
        source
            .append(accessor)
            .append(" == null ? null : ")
            .append(accessor)
            .append(".orElse(null)");
      } else {
        source.append(accessor);
      }
      source.append(";\n");
      source.append("    if (").append(valueExpression).append(" != null) {\n");
      appendIdentityChoiceBranches(source, field.choice().branches(), valueExpression, "      ");
      source.append("    }\n");
    }

    private void appendIdentityChoiceBranches(
        StringBuilder source,
        List<BindingChoiceBranch> branches,
        String valueExpression,
        String indent) {
      for (BindingChoiceBranch branch : branches) {
        if ("fragment".equals(branch.type().kind())) {
          continue;
        }
        BindingType nestedType = modelType(branch.type());
        source
            .append(indent)
            .append("if (")
            .append(valueExpression)
            .append(" instanceof ")
            .append(branch.branchJavaName().qualifiedName())
            .append(" branch && branch.value() != null) {\n");
        appendIdentityElementItem(
            source, nestedType, escape(branch.xmlName().toText()), "branch.value()", indent + "  ");
        source.append(indent).append("}\n");
      }
    }

    private void appendIdentityContent(StringBuilder source, BindingField field, String accessor) {
      source.append("    if (").append(accessor).append(" != null) {\n");
      source
          .append("      for (")
          .append(field.type().name())
          .append(" item : ")
          .append(accessor)
          .append(") {\n");
      for (BindingContentBranch branch : field.content().branches()) {
        if ("text".equals(branch.kind())
            || "wildcard".equals(branch.kind())
            || "fragment".equals(branch.type().kind())) {
          continue;
        }
        BindingType nestedType = modelType(branch.type());
        source
            .append("        if (item instanceof ")
            .append(branch.branchJavaName().qualifiedName())
            .append(" branch && branch.value() != null) {\n");
        appendIdentityElementItem(
            source, nestedType, escape(branch.xmlName().toText()), "branch.value()", "          ");
        source.append("        }\n");
      }
      source.append("      }\n").append("    }\n");
    }

    private boolean hasFacetRules(BindingTypeReference reference) {
      return reference.restriction() != null && reference.restriction().hasRules();
    }

    private boolean hasValidationRules(BindingTypeReference reference) {
      if (hasFacetRules(reference)) {
        return true;
      }
      if ("scalar".equals(reference.kind()) && !"string".equals(reference.name())) {
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
        appendDatatypeValueValidation(source, reference.itemType(), "item", indent + "    ");
        appendFacetValidation(source, reference.itemType(), "item", indent + "    ");
        source.append(indent).append("  }\n");
        source.append(indent).append("}\n");
        return;
      }
      if ("union".equals(reference.kind())) {
        appendUnionValidation(source, reference, accessor, indent);
        return;
      }
      appendDatatypeValueValidation(source, reference, accessor, indent);
      appendFacetValidation(source, reference, accessor, indent);
    }

    private void appendDatatypeValueValidation(
        StringBuilder source, BindingTypeReference reference, String accessor, String indent) {
      if (!"scalar".equals(reference.kind()) || "string".equals(reference.name())) {
        return;
      }
      appendFacetCheck(
          source,
          datatypeFacetExpression(
              reference.name(),
              accessor,
              List.of(),
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              List.of()),
          "MXJB-GV-004",
          "Value does not match datatype value space.",
          indent);
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
      return "io.github.mundanej.mxjb.runtime.XmlDatatypes.isLexicallyValid(\""
          + escape(scalar)
          + "\", "
          + accessor
          + ")";
    }

    private String facetMatchExpression(BindingTypeReference reference, String accessor) {
      BindingSimpleRestriction restriction = reference.restriction();
      return facetHelperExpression(restriction, accessor);
    }

    private void appendFacetValidation(
        StringBuilder source, BindingTypeReference reference, String accessor, String indent) {
      BindingSimpleRestriction restriction = reference.restriction();
      if (restriction == null || !restriction.hasRules()) {
        return;
      }
      if (restriction.length() != null
          || restriction.minLength() != null
          || restriction.maxLength() != null) {
        appendFacetCheck(
            source,
            datatypeFacetExpression(
                restriction.baseScalar(),
                accessor,
                List.of(),
                restriction.length(),
                restriction.minLength(),
                restriction.maxLength(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()),
            "MXJB-GV-005",
            "Value length is outside the accepted range.",
            indent);
      }
      if (!restriction.patterns().isEmpty()) {
        appendFacetCheck(
            source,
            datatypeFacetExpression(
                restriction.baseScalar(),
                accessor,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                restriction.patterns()),
            "MXJB-GV-007",
            "Value does not match required pattern.",
            indent);
      }
      if (restriction.minInclusive() != null
          || restriction.maxInclusive() != null
          || restriction.minExclusive() != null
          || restriction.maxExclusive() != null) {
        appendFacetCheck(
            source,
            datatypeFacetExpression(
                restriction.baseScalar(),
                accessor,
                List.of(),
                null,
                null,
                null,
                restriction.minInclusive(),
                restriction.maxInclusive(),
                restriction.minExclusive(),
                restriction.maxExclusive(),
                null,
                null,
                List.of()),
            "MXJB-GV-006",
            "Value is outside the accepted range.",
            indent);
      }
      if (!restriction.enumerations().isEmpty()
          || restriction.totalDigits() != null
          || restriction.fractionDigits() != null) {
        appendFacetCheck(
            source,
            datatypeFacetExpression(
                restriction.baseScalar(),
                accessor,
                restriction.enumerations(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                restriction.totalDigits(),
                restriction.fractionDigits(),
                List.of()),
            "MXJB-GV-004",
            "Value does not satisfy datatype facets.",
            indent);
      }
    }

    private void appendFacetCheck(
        StringBuilder source, String expression, String code, String message, String indent) {
      source.append(indent).append("if (!").append(expression).append(") {\n");
      source
          .append(indent)
          .append("  addError(errors, \"")
          .append(code)
          .append("\", \"")
          .append(message)
          .append("\", location);\n");
      source.append(indent).append("}\n");
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
      return "io.github.mundanej.mxjb.runtime.XmlDatatypes.matchesFacets(\""
          + escape(reference.name())
          + "\", "
          + accessor
          + ", java.util.List.of(\""
          + escape(fixedValue)
          + "\"), null, null, null, null, null, null, null, null, null, java.util.List.of())";
    }

    private String scalarTypeText(BindingTypeReference reference) {
      if ("union".equals(reference.kind())) {
        return "String";
      }
      if ("list".equals(reference.kind())) {
        return "java.util.List<" + scalarTypeText(reference.itemType()) + ">";
      }
      return qualifiedScalarType(reference.name());
    }

    private String facetHelperExpression(BindingSimpleRestriction restriction, String accessor) {
      return datatypeFacetExpression(
          restriction.baseScalar(),
          accessor,
          restriction.enumerations(),
          restriction.length(),
          restriction.minLength(),
          restriction.maxLength(),
          restriction.minInclusive(),
          restriction.maxInclusive(),
          restriction.minExclusive(),
          restriction.maxExclusive(),
          restriction.totalDigits(),
          restriction.fractionDigits(),
          restriction.patterns());
    }

    private String datatypeFacetExpression(
        String baseScalar,
        String accessor,
        List<String> enumerations,
        Integer length,
        Integer minLength,
        Integer maxLength,
        String minInclusive,
        String maxInclusive,
        String minExclusive,
        String maxExclusive,
        Integer totalDigits,
        Integer fractionDigits,
        List<String> patterns) {
      return "io.github.mundanej.mxjb.runtime.XmlDatatypes.matchesFacets(\""
          + escape(baseScalar)
          + "\", "
          + accessor
          + ", "
          + stringListExpression(enumerations)
          + ", "
          + integerLiteral(length)
          + ", "
          + integerLiteral(minLength)
          + ", "
          + integerLiteral(maxLength)
          + ", "
          + stringLiteral(minInclusive)
          + ", "
          + stringLiteral(maxInclusive)
          + ", "
          + stringLiteral(minExclusive)
          + ", "
          + stringLiteral(maxExclusive)
          + ", "
          + integerLiteral(totalDigits)
          + ", "
          + integerLiteral(fractionDigits)
          + ", "
          + stringListExpression(patterns)
          + ")";
    }

    private String qualifiedScalarType(String scalar) {
      String javaType = XmlSchemaBuiltIns.javaType(scalar);
      if (javaType == null) {
        return "String";
      }
      return switch (javaType) {
        case "List<String>" -> "java.util.List<String>";
        case "BigInteger" -> "java.math.BigInteger";
        case "BigDecimal" -> "java.math.BigDecimal";
        case "XmlDuration",
            "XmlDateTime",
            "XmlDate",
            "XmlTime",
            "XmlGYear",
            "XmlGYearMonth",
            "XmlGMonth",
            "XmlGMonthDay",
            "XmlGDay",
            "XmlBinary",
            "XmlAnyUri",
            "XmlQName" ->
            "io.github.mundanej.mxjb.runtime." + javaType;
        default -> javaType;
      };
    }

    private String stringListExpression(List<String> values) {
      if (values.isEmpty()) {
        return "java.util.List.of()";
      }
      return values.stream()
          .map(this::stringLiteral)
          .collect(java.util.stream.Collectors.joining(", ", "java.util.List.of(", ")"));
    }

    private String stringLiteral(String value) {
      return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private String integerLiteral(Integer value) {
      return value == null ? "null" : value.toString();
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
            .append("  private static void validateWildcardAttribute(\n")
            .append("      io.github.mundanej.mxjb.runtime.XmlAttribute attribute,\n")
            .append("      String wildcardKind,\n")
            .append("      java.util.Set<String> namespaces,\n")
            .append("      java.util.Set<io.github.mundanej.mxjb.runtime.XmlName> excludedNames,\n")
            .append("      io.github.mundanej.mxjb.runtime.XmlLocation location,\n")
            .append(
                "      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {\n")
            .append("    if (attribute == null || attribute.name() == null) {\n")
            .append(
                "      addError(errors, \"MXJB-GV-001\", \"Missing required wildcard attribute.\", location);\n")
            .append("      return;\n")
            .append("    }\n")
            .append("    if (excludedNames.contains(attribute.name())) {\n")
            .append(
                "      addError(errors, \"MXJB-GV-009\", \"Prohibited XML attribute.\", location);\n")
            .append("    }\n")
            .append("    if (!wildcardMatches(attribute.name(), wildcardKind, namespaces)) {\n")
            .append(
                "      addError(errors, \"MXJB-GV-009\", "
                    + "\"Wildcard attribute namespace is not accepted.\", location);\n")
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
      if (hasIdentityConstraints()) {
        appendIdentitySharedHelpers(source);
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

    private void appendIdentitySharedHelpers(StringBuilder source) {
      source
          .append('\n')
          .append("  private record IdentityNode(\n")
          .append("      String name,\n")
          .append("      Object text,\n")
          .append("      java.util.Map<String, Object> attributes,\n")
          .append("      java.util.List<IdentityNode> children) {}\n\n")
          .append("  private record IdentitySelectorPath(\n")
          .append("      boolean descendant, java.util.List<String> steps) {}\n\n")
          .append("  private record IdentityField(\n")
          .append("      java.util.List<IdentityFieldPath> alternatives) {}\n\n")
          .append("  private record IdentityFieldPath(\n")
          .append("      boolean self,\n")
          .append("      boolean attribute,\n")
          .append("      java.util.List<String> steps,\n")
          .append("      String terminal) {}\n\n")
          .append("  private record IdentityReference(\n")
          .append("      String name, String refer, java.util.List<Object> tuple) {}\n\n");
      appendValidateIdentityConstraints(source);
      source
          .append("  private static void validateIdentityConstraint(\n")
          .append("      IdentityNode root,\n")
          .append("      String kind,\n")
          .append("      String name,\n")
          .append("      String refer,\n")
          .append("      java.util.List<IdentitySelectorPath> selectors,\n")
          .append("      java.util.List<IdentityField> fields,\n")
          .append("      java.util.Map<String, java.util.Set<java.util.List<Object>>> tables,\n")
          .append("      java.util.ArrayList<IdentityReference> references,\n")
          .append(
              "      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {\n")
          .append("    java.util.ArrayList<IdentityNode> selected = new java.util.ArrayList<>();\n")
          .append("    for (IdentitySelectorPath selector : selectors) {\n")
          .append("      selected.addAll(selectIdentityNodes(root, selector));\n")
          .append("    }\n")
          .append("    java.util.Set<java.util.List<Object>> table = null;\n")
          .append("    if (!\"keyref\".equals(kind)) {\n")
          .append(
              "      table = tables.computeIfAbsent(name, unused -> new java.util.LinkedHashSet<>());\n")
          .append("    }\n")
          .append("    for (IdentityNode node : selected) {\n")
          .append(
              "      java.util.List<Object> tuple = identityTuple(node, kind, name, fields, errors);\n")
          .append("      if (tuple == null) {\n")
          .append("        continue;\n")
          .append("      }\n")
          .append("      if (\"keyref\".equals(kind)) {\n")
          .append("        references.add(new IdentityReference(name, refer, tuple));\n")
          .append("      } else if (!table.add(tuple)) {\n")
          .append(
              "        addError(errors, \"MXJB-GV-011\", "
                  + "\"Duplicate identity value for \" + name + \".\", "
                  + "io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN);\n")
          .append("      }\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static java.util.List<Object> identityTuple(\n")
          .append("      IdentityNode node,\n")
          .append("      String kind,\n")
          .append("      String name,\n")
          .append("      java.util.List<IdentityField> fields,\n")
          .append(
              "      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {\n")
          .append("    java.util.ArrayList<Object> tuple = new java.util.ArrayList<>();\n")
          .append("    for (IdentityField field : fields) {\n")
          .append("      java.util.List<Object> values = identityFieldValues(node, field);\n")
          .append("      if (values.size() != 1) {\n")
          .append("        if (\"key\".equals(kind)) {\n")
          .append(
              "          addError(errors, \"MXJB-GV-010\", "
                  + "\"Missing key field for \" + name + \".\", "
                  + "io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN);\n")
          .append("        }\n")
          .append("        return null;\n")
          .append("      }\n")
          .append("      tuple.add(values.getFirst());\n")
          .append("    }\n")
          .append("    return java.util.List.copyOf(tuple);\n")
          .append("  }\n\n")
          .append("  private static java.util.List<IdentityNode> selectIdentityNodes(\n")
          .append("      IdentityNode root, IdentitySelectorPath path) {\n")
          .append("    java.util.ArrayList<IdentityNode> selected = new java.util.ArrayList<>();\n")
          .append("    if (path.steps().isEmpty()) {\n")
          .append("      selected.add(root);\n")
          .append("      return selected;\n")
          .append("    }\n")
          .append("    if (path.descendant()) {\n")
          .append("      collectDescendantMatches(root, path.steps().getFirst(), selected);\n")
          .append(
              "      return traverseIdentityPath(selected, path.steps().subList(1, path.steps().size()));\n")
          .append("    }\n")
          .append("    selected.add(root);\n")
          .append("    return traverseIdentityPath(selected, path.steps());\n")
          .append("  }\n\n")
          .append("  private static void collectDescendantMatches(\n")
          .append(
              "      IdentityNode node, String name, java.util.ArrayList<IdentityNode> selected) {\n")
          .append("    for (IdentityNode child : node.children()) {\n")
          .append("      if (identityNameMatches(child.name(), name)) {\n")
          .append("        selected.add(child);\n")
          .append("      }\n")
          .append("      collectDescendantMatches(child, name, selected);\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static java.util.List<IdentityNode> traverseIdentityPath(\n")
          .append("      java.util.List<IdentityNode> nodes, java.util.List<String> steps) {\n")
          .append("    java.util.List<IdentityNode> current = nodes;\n")
          .append("    for (String step : steps) {\n")
          .append("      java.util.ArrayList<IdentityNode> next = new java.util.ArrayList<>();\n")
          .append("      for (IdentityNode node : current) {\n")
          .append("        for (IdentityNode child : node.children()) {\n")
          .append("          if (identityNameMatches(child.name(), step)) {\n")
          .append("            next.add(child);\n")
          .append("          }\n")
          .append("        }\n")
          .append("      }\n")
          .append("      current = next;\n")
          .append("    }\n")
          .append("    return current;\n")
          .append("  }\n\n")
          .append("  private static java.util.List<Object> identityFieldValues(\n")
          .append("      IdentityNode node, IdentityField field) {\n")
          .append("    java.util.ArrayList<Object> values = new java.util.ArrayList<>();\n")
          .append("    for (IdentityFieldPath alternative : field.alternatives()) {\n")
          .append("      values.addAll(identityFieldPathValues(node, alternative));\n")
          .append("    }\n")
          .append("    return values;\n")
          .append("  }\n\n")
          .append("  private static java.util.List<Object> identityFieldPathValues(\n")
          .append("      IdentityNode node, IdentityFieldPath field) {\n")
          .append("    if (field.self()) {\n")
          .append(
              "      return node.text() == null ? java.util.List.of() : java.util.List.of(node.text());\n")
          .append("    }\n")
          .append("    java.util.List<IdentityNode> targets = traverseIdentityPath(\n")
          .append("        java.util.List.of(node), field.steps());\n")
          .append("    java.util.ArrayList<Object> values = new java.util.ArrayList<>();\n")
          .append("    for (IdentityNode target : targets) {\n")
          .append("      if (field.attribute()) {\n")
          .append("        if (\"*\".equals(field.terminal())) {\n")
          .append("          values.addAll(target.attributes().values());\n")
          .append("        } else if (target.attributes().containsKey(field.terminal())) {\n")
          .append("          values.add(target.attributes().get(field.terminal()));\n")
          .append("        }\n")
          .append("      } else {\n")
          .append("        for (IdentityNode child : target.children()) {\n")
          .append(
              "          if (identityNameMatches(child.name(), field.terminal()) && child.text() != null) {\n")
          .append("            values.add(child.text());\n")
          .append("          }\n")
          .append("        }\n")
          .append("      }\n")
          .append("    }\n")
          .append("    return values;\n")
          .append("  }\n\n")
          .append(
              "  private static boolean identityNameMatches(String actual, String expected) {\n")
          .append(
              "    return \"*\".equals(expected) || java.util.Objects.equals(actual, expected);\n")
          .append("  }\n\n")
          .append("  private static Object identityScalar(Object value) {\n")
          .append("    if (value instanceof java.math.BigDecimal decimal) {\n")
          .append("      return decimal.stripTrailingZeros();\n")
          .append("    }\n")
          .append(
              "    if (value instanceof Float floatValue && floatValue.floatValue() == 0.0f) {\n")
          .append("      return Float.valueOf(0.0f);\n")
          .append("    }\n")
          .append(
              "    if (value instanceof Double doubleValue && doubleValue.doubleValue() == 0.0d) {\n")
          .append("      return Double.valueOf(0.0d);\n")
          .append("    }\n")
          .append("    return value;\n")
          .append("  }\n\n")
          .append(
              "  private static String identityName(io.github.mundanej.mxjb.runtime.XmlName name) {\n")
          .append("    return name.namespaceUri().isEmpty()\n")
          .append("        ? name.localName()\n")
          .append("        : \"{\" + name.namespaceUri() + \"}\" + name.localName();\n")
          .append("  }\n\n");
    }

    private void appendValidateIdentityConstraints(StringBuilder source) {
      source
          .append("  private static void validateIdentityConstraints(\n")
          .append("      IdentityNode root,\n")
          .append(
              "      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {\n")
          .append("    java.util.Map<String, java.util.Set<java.util.List<Object>>> tables =\n")
          .append("        new java.util.LinkedHashMap<>();\n")
          .append(
              "    java.util.ArrayList<IdentityReference> references = new java.util.ArrayList<>();\n");
      for (SchemaIrIdentityConstraint constraint : root.identityConstraints()) {
        if (!"keyref".equals(constraint.kind())) {
          appendIdentityConstraintCall(source, constraint);
        }
      }
      for (SchemaIrIdentityConstraint constraint : root.identityConstraints()) {
        if ("keyref".equals(constraint.kind())) {
          appendIdentityConstraintCall(source, constraint);
        }
      }
      source
          .append("    for (IdentityReference reference : references) {\n")
          .append(
              "      java.util.Set<java.util.List<Object>> table = tables.get(reference.refer());\n")
          .append("      if (table == null || !table.contains(reference.tuple())) {\n")
          .append(
              "        addError(errors, \"MXJB-GV-012\", "
                  + "\"Unresolved key reference for \" + reference.name() + \".\", "
                  + "io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN);\n")
          .append("      }\n")
          .append("    }\n")
          .append("  }\n\n");
    }

    private void appendIdentityConstraintCall(
        StringBuilder source, SchemaIrIdentityConstraint constraint) {
      source
          .append("    validateIdentityConstraint(root, \"")
          .append(escape(constraint.kind()))
          .append("\", \"")
          .append(escape(constraint.name().toText()))
          .append("\", ")
          .append(
              constraint.refer() == null
                  ? "null"
                  : "\"" + escape(constraint.refer().toText()) + "\"")
          .append(", ")
          .append(selectorListExpression(constraint.selectors()))
          .append(", ")
          .append(fieldListExpression(constraint.fields()))
          .append(", tables, references, errors);\n");
    }

    private String selectorListExpression(List<SchemaIrIdentityPath> paths) {
      return paths.stream()
          .map(
              path ->
                  "new IdentitySelectorPath("
                      + path.descendant()
                      + ", "
                      + identityStepListExpression(path.steps())
                      + ")")
          .collect(java.util.stream.Collectors.joining(", ", "java.util.List.of(", ")"));
    }

    private String fieldListExpression(List<SchemaIrIdentityField> fields) {
      return fields.stream()
          .map(this::fieldExpression)
          .collect(java.util.stream.Collectors.joining(", ", "java.util.List.of(", ")"));
    }

    private String fieldExpression(SchemaIrIdentityField field) {
      return field.alternatives().stream()
          .map(this::fieldPathExpression)
          .collect(
              java.util.stream.Collectors.joining(
                  ", ", "new IdentityField(java.util.List.of(", "))"));
    }

    private String fieldPathExpression(SchemaIrIdentityPath path) {
      if (path.self()) {
        return "new IdentityFieldPath(true, false, java.util.List.of(), null)";
      }
      List<SchemaIrIdentityStep> steps = path.steps();
      SchemaIrIdentityStep terminal = steps.getLast();
      return "new IdentityFieldPath(false, "
          + terminal.attribute()
          + ", "
          + identityStepListExpression(steps.subList(0, steps.size() - 1))
          + ", \""
          + escape(identityStepName(terminal))
          + "\")";
    }

    private String identityStepListExpression(List<SchemaIrIdentityStep> steps) {
      if (steps.isEmpty()) {
        return "java.util.List.of()";
      }
      return steps.stream()
          .map(step -> "\"" + escape(identityStepName(step)) + "\"")
          .collect(java.util.stream.Collectors.joining(", ", "java.util.List.of(", ")"));
    }

    private String identityStepName(SchemaIrIdentityStep step) {
      return step.wildcard() ? "*" : step.name().toText();
    }

    private boolean needsUnionSupport() {
      return needsUnionSupport(rootType, new LinkedHashSet<>());
    }

    private boolean needsWildcardSupport(BindingType type, Set<String> visited) {
      if (!visited.add(type.javaName().qualifiedName())) {
        return false;
      }
      if (type.fields().stream()
          .anyMatch(
              field -> "wildcard".equals(field.kind()) || "anyAttribute".equals(field.kind()))) {
        return true;
      }
      for (BindingField field :
          type.fields().stream().filter(value -> "content".equals(value.kind())).toList()) {
        for (BindingContentBranch branch : field.content().branches()) {
          if ("wildcard".equals(branch.kind())) {
            return true;
          }
          BindingType nestedType = modelType(branch.type());
          if (nestedType != null && needsWildcardSupport(nestedType, visited)) {
            return true;
          }
        }
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
        if ("content".equals(field.kind())) {
          for (BindingContentBranch branch : field.content().branches()) {
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
