package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingChoiceBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingModel;
import io.github.mundanej.mxjb.generator.core.bind.BindingResult;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Emits deterministic XML reader source from the internal binding model. */
public final class GeneratedReaderEmitter {
  public GeneratedReaderEmissionResult emit(BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return GeneratedReaderEmissionResult.empty(bindingResult.diagnostics());
    }
    return emit(bindingResult.model());
  }

  public GeneratedReaderEmissionResult emit(BindingModel model) {
    ModelIndex index = new ModelIndex(model);
    List<SchemaDiagnostic> diagnostics = validate(model, index);
    if (!diagnostics.isEmpty()) {
      return GeneratedReaderEmissionResult.empty(diagnostics);
    }

    List<GeneratedJavaSource> sources =
        model.rootElements().stream()
            .sorted(Comparator.comparing(root -> root.xmlName().toText()))
            .map(root -> emitRootReader(root, index))
            .toList();
    return new GeneratedReaderEmissionResult(sources, List.of());
  }

  private List<SchemaDiagnostic> validate(BindingModel model, ModelIndex index) {
    List<SchemaDiagnostic> diagnostics = new ArrayList<>();
    Set<String> readerNames = new LinkedHashSet<>();
    for (BindingRootElement root : model.rootElements()) {
      if (!"model".equals(root.type().kind())) {
        diagnostics.add(
            invalidModel("Root reader requires model type " + root.xmlName().toText() + "."));
        continue;
      }
      BindingType rootType = index.type(root.type().name());
      if (rootType == null) {
        diagnostics.add(invalidModel("Missing root reader model type " + root.type().name() + "."));
        continue;
      }
      BindingJavaName readerName = readerName(rootType.javaName());
      if (!readerNames.add(readerName.qualifiedName())) {
        diagnostics.add(invalidModel("Duplicate root reader " + readerName.qualifiedName() + "."));
      }
    }
    for (BindingType type : model.types()) {
      if (!"record".equals(type.shape())) {
        diagnostics.add(invalidModel("Unsupported reader model shape " + type.shape() + "."));
      }
      for (BindingField field : type.fields()) {
        if (!isSupportedTypeReference(field.type(), index)) {
          diagnostics.add(
              invalidModel("Unsupported reader field type " + field.type().toText() + "."));
        }
        if (!isSupportedFieldKind(field.kind())) {
          diagnostics.add(invalidModel("Unsupported reader field kind " + field.kind() + "."));
        }
        if ("attribute".equals(field.kind()) && !"scalar".equals(field.type().kind())) {
          diagnostics.add(
              invalidModel("Reader attributes require scalar type " + field.type().toText() + "."));
        }
        if ("attribute".equals(field.kind()) && "list".equals(field.cardinality().shape())) {
          diagnostics.add(invalidModel("Reader attributes do not support list cardinality."));
        }
        if (!isSupportedCardinality(field.cardinality().shape())) {
          diagnostics.add(
              invalidModel("Unsupported reader cardinality " + field.cardinality().toText() + "."));
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
    return "choice".equals(reference.kind())
        || ("model".equals(reference.kind()) && index.type(reference.name()) != null);
  }

  private boolean isSupportedFieldKind(String kind) {
    return "element".equals(kind) || "attribute".equals(kind) || "choice".equals(kind);
  }

  private boolean isSupportedCardinality(String shape) {
    return "required".equals(shape) || "optional".equals(shape) || "list".equals(shape);
  }

  private SchemaDiagnostic invalidModel(String message) {
    return new SchemaDiagnostic(
        DiagnosticCode.SCHEMA_READER_EMISSION_INVALID_MODEL, "reader-emission", message);
  }

  private GeneratedJavaSource emitRootReader(BindingRootElement root, ModelIndex index) {
    BindingType rootType = Objects.requireNonNull(index.type(root.type().name()));
    BindingJavaName readerName = readerName(rootType.javaName());
    SourceState sourceState = new SourceState(root, rootType, readerName, index);
    return new GeneratedJavaSource(readerName, relativePath(readerName), sourceState.sourceText());
  }

  private BindingJavaName readerName(BindingJavaName modelName) {
    return new BindingJavaName(
        modelName.packageName() + ".xml", modelName.simpleName() + "XmlReader");
  }

  private Path relativePath(BindingJavaName readerName) {
    return Path.of(readerName.packageName().replace('.', '/'), readerName.simpleName() + ".java");
  }

  private static final class SourceState {
    private final BindingRootElement root;
    private final BindingType rootType;
    private final BindingJavaName readerName;
    private final ModelIndex index;
    private final LinkedHashMap<SchemaQName, String> nameConstants = new LinkedHashMap<>();
    private final Set<String> helperNames = new LinkedHashSet<>();

    private SourceState(
        BindingRootElement root,
        BindingType rootType,
        BindingJavaName readerName,
        ModelIndex index) {
      this.root = root;
      this.rootType = rootType;
      this.readerName = readerName;
      this.index = index;
    }

    private String sourceText() {
      collectNames(root.xmlName(), rootType, new LinkedHashSet<>());
      StringBuilder source = new StringBuilder();
      source.append("package ").append(readerName.packageName()).append(";\n\n");
      source
          .append("/** Generated XML reader for {@link ")
          .append(rootType.javaName().qualifiedName())
          .append("}. */\n");
      source.append("public final class ").append(readerName.simpleName()).append(" {\n");
      appendNameConstants(source);
      source.append('\n');
      source.append("  private ").append(readerName.simpleName()).append("() {}\n\n");
      appendPublicRead(source);
      appendHelper(source, rootType);
      appendSharedHelpers(source);
      source.append("}\n");
      return source.toString();
    }

    private void appendNameConstants(StringBuilder source) {
      for (Map.Entry<SchemaQName, String> entry : nameConstants.entrySet()) {
        source
            .append("  private static final io.github.mundanej.mxjb.runtime.XmlName ")
            .append(entry.getValue())
            .append(" =\n")
            .append("      new io.github.mundanej.mxjb.runtime.XmlName(\"")
            .append(escape(entry.getKey().namespace()))
            .append("\", \"")
            .append(escape(entry.getKey().localName()))
            .append("\");\n");
      }
    }

    private void appendPublicRead(StringBuilder source) {
      source
          .append("  public static ")
          .append(typeText(rootType))
          .append(" read(io.github.mundanej.mxjb.runtime.XmlEventReader input)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    java.util.Objects.requireNonNull(input, \"input\");\n")
          .append("    moveToDocumentContent(input);\n")
          .append(
              "    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.START_ELEMENT\n")
          .append("        || !")
          .append(nameConstant(root.xmlName()))
          .append(".equals(input.name())) {\n")
          .append("      throw readException(input, \"MXJB-GR-001\", \"Expected root element ")
          .append(escape(root.xmlName().toText()))
          .append(".\");\n")
          .append("    }\n")
          .append("    ")
          .append(typeText(rootType))
          .append(" value = ")
          .append(helperName(rootType))
          .append("(input, ")
          .append(nameConstant(root.xmlName()))
          .append(");\n")
          .append("    movePastWhitespace(input);\n")
          .append(
              "    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.END_DOCUMENT) {\n")
          .append(
              "      throw readException(input, \"MXJB-GR-007\", \"Unexpected content after root element.\");\n")
          .append("    }\n")
          .append("    return value;\n")
          .append("  }\n\n");
    }

    private void appendHelper(StringBuilder source, BindingType type) {
      if (!helperNames.add(type.javaName().qualifiedName())) {
        return;
      }
      source
          .append("  private static ")
          .append(typeText(type))
          .append(' ')
          .append(helperName(type))
          .append("(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName elementName)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    expectStart(input, elementName);\n");
      appendUnexpectedAttributeCheck(source, type);
      for (BindingField field : attributes(type)) {
        appendAttributeRead(source, field);
      }
      for (BindingField field : contentFields(type)) {
        appendElementVariable(source, field);
      }
      source.append("    int lastElementOrder = -1;\n");
      source.append("    if (!input.next()) {\n");
      source.append(
          "      throw readException(input, \"MXJB-GR-007\", \"Unexpected end of XML input.\");\n");
      source.append("    }\n");
      source.append("    while (true) {\n");
      source.append("      movePastWhitespace(input);\n");
      source.append(
          "      if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT) {\n");
      source.append("        break;\n");
      source.append("      }\n");
      source.append(
          "      if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.START_ELEMENT) {\n");
      source.append(
          "        throw readException(input, \"MXJB-GR-007\", \"Expected XML element content.\");\n");
      source.append("      }\n");
      appendElementDispatch(source, type);
      source.append("    }\n");
      source.append("    if (!elementName.equals(input.name())) {\n");
      source.append(
          "      throw readException(input, \"MXJB-GR-007\", \"Mismatched end element.\");\n");
      source.append("    }\n");
      for (BindingField field : requiredFields(type)) {
        appendRequiredCheck(source, field);
      }
      for (BindingField field : requiredListFields(type)) {
        appendRequiredListCheck(source, field);
      }
      source.append("    input.next();\n");
      source.append("    return new ").append(typeText(type)).append("(");
      source.append(constructorArguments(type));
      source.append(");\n");
      source.append("  }\n\n");
      for (BindingField field : contentFields(type)) {
        BindingType nestedType = modelType(field);
        if (nestedType != null && !helperNames.contains(nestedType.javaName().qualifiedName())) {
          appendHelper(source, nestedType);
        }
        if ("choice".equals(field.kind())) {
          for (BindingChoiceBranch branch : field.choice().branches()) {
            BindingType branchType = modelType(branch.type());
            if (branchType != null
                && !helperNames.contains(branchType.javaName().qualifiedName())) {
              appendHelper(source, branchType);
            }
          }
        }
      }
    }

    private void appendUnexpectedAttributeCheck(StringBuilder source, BindingType type) {
      source.append("    for (int index = 0; index < input.attributeCount(); index++) {\n");
      source.append(
          "      io.github.mundanej.mxjb.runtime.XmlName attributeName = input.attributeName(index);\n");
      List<BindingField> attributes = attributes(type);
      if (attributes.isEmpty()) {
        source.append(
            "      throw readException(input, \"MXJB-GR-003\", \"Unexpected XML attribute.\");\n");
      } else {
        source.append("      if (");
        for (int indexValue = 0; indexValue < attributes.size(); indexValue++) {
          if (indexValue > 0) {
            source.append("\n          && ");
          }
          source
              .append('!')
              .append(nameConstant(attributes.get(indexValue).xmlName()))
              .append(".equals(attributeName)");
        }
        source.append(") {\n");
        source.append(
            "        throw readException(input, \"MXJB-GR-003\", \"Unexpected XML attribute.\");\n");
        source.append("      }\n");
      }
      source.append("    }\n");
    }

    private void appendAttributeRead(StringBuilder source, BindingField field) {
      String textName = field.javaName() + "Text";
      String constant = nameConstant(field.xmlName());
      source
          .append("    String ")
          .append(textName)
          .append(" = attribute(input, ")
          .append(constant)
          .append(");\n");
      if ("optional".equals(field.cardinality().shape())) {
        source
            .append("    ")
            .append(localType(field))
            .append(' ')
            .append(field.javaName())
            .append("Value = ")
            .append(textName)
            .append(" == null ? null : ")
            .append(parseScalarExpression(field, textName))
            .append(";\n");
        source
            .append("    java.util.Optional<")
            .append(localType(field))
            .append("> ")
            .append(field.javaName())
            .append(" = ")
            .append(field.javaName())
            .append("Value == null\n")
            .append("        ? java.util.Optional.empty()\n")
            .append("        : java.util.Optional.of(")
            .append(field.javaName())
            .append("Value);\n");
      } else {
        source.append("    if (").append(textName).append(" == null) {\n");
        source
            .append(
                "      throw readException(input, \"MXJB-GR-004\", \"Missing required XML attribute ")
            .append(escape(field.xmlName().toText()))
            .append(".\");\n");
        source.append("    }\n");
        source
            .append("    ")
            .append(localType(field))
            .append(' ')
            .append(field.javaName())
            .append(" = ")
            .append(parseScalarExpression(field, textName))
            .append(";\n");
      }
    }

    private void appendElementVariable(StringBuilder source, BindingField field) {
      String shape = field.cardinality().shape();
      if ("list".equals(shape)) {
        source
            .append("    java.util.ArrayList<")
            .append(localType(field))
            .append("> ")
            .append(field.javaName())
            .append("Values = new java.util.ArrayList<>();\n");
      } else if ("optional".equals(shape)) {
        source
            .append("    java.util.Optional<")
            .append(localType(field))
            .append("> ")
            .append(field.javaName())
            .append(" = java.util.Optional.empty();\n");
      } else {
        source
            .append("    ")
            .append(localType(field))
            .append(' ')
            .append(field.javaName())
            .append(" = null;\n");
      }
    }

    private void appendElementDispatch(StringBuilder source, BindingType type) {
      List<BindingField> fields = contentFields(type);
      boolean firstBranch = true;
      for (BindingField field : fields) {
        if ("choice".equals(field.kind())) {
          for (BindingChoiceBranch branch : field.choice().branches()) {
            appendDispatchPrefix(source, firstBranch);
            source.append(nameConstant(branch.xmlName())).append(".equals(input.name())) {\n");
            appendChoiceBranchRead(source, field, branch);
            firstBranch = false;
          }
        } else {
          appendDispatchPrefix(source, firstBranch);
          source.append(nameConstant(field.xmlName())).append(".equals(input.name())) {\n");
          appendSingleElementRead(source, field);
          firstBranch = false;
        }
      }
      if (fields.isEmpty()) {
        source.append(
            "      throw readException(input, \"MXJB-GR-002\", \"Unexpected XML element.\");\n");
      } else {
        source.append("      } else {\n");
        source.append(
            "        throw readException(input, \"MXJB-GR-002\", \"Unexpected XML element.\");\n");
        source.append("      }\n");
      }
    }

    private void appendDispatchPrefix(StringBuilder source, boolean firstBranch) {
      source.append(firstBranch ? "      if (" : "      } else if (");
    }

    private void appendSingleElementRead(StringBuilder source, BindingField field) {
      String shape = field.cardinality().shape();
      source.append("        if (").append(field.order()).append(" < lastElementOrder) {\n");
      source
          .append(
              "          throw readException(input, \"MXJB-GR-002\", \"Out-of-order XML element ")
          .append(escape(field.xmlName().toText()))
          .append(".\");\n");
      source.append("        }\n");
      source
          .append("        lastElementOrder = Math.max(lastElementOrder, ")
          .append(field.order())
          .append(");\n");
      if ("list".equals(shape)) {
        appendMaxOccursCheck(source, field);
        source
            .append("        ")
            .append(field.javaName())
            .append("Values.add(")
            .append(readValueExpression(field))
            .append(");\n");
      } else if ("optional".equals(shape)) {
        source.append("        if (").append(field.javaName()).append(".isPresent()) {\n");
        source
            .append("          throw readException(input, \"MXJB-GR-005\", \"Repeated XML element ")
            .append(escape(field.xmlName().toText()))
            .append(".\");\n");
        source.append("        }\n");
        source
            .append("        ")
            .append(field.javaName())
            .append(" = java.util.Optional.of(")
            .append(readValueExpression(field))
            .append(");\n");
      } else {
        source.append("        if (").append(field.javaName()).append(" != null) {\n");
        source
            .append("          throw readException(input, \"MXJB-GR-005\", \"Repeated XML element ")
            .append(escape(field.xmlName().toText()))
            .append(".\");\n");
        source.append("        }\n");
        source
            .append("        ")
            .append(field.javaName())
            .append(" = ")
            .append(readValueExpression(field))
            .append(";\n");
      }
    }

    private void appendChoiceBranchRead(
        StringBuilder source, BindingField field, BindingChoiceBranch branch) {
      source.append("        if (").append(field.order()).append(" < lastElementOrder) {\n");
      source
          .append(
              "          throw readException(input, \"MXJB-GR-002\", \"Out-of-order XML element ")
          .append(escape(branch.xmlName().toText()))
          .append(".\");\n");
      source.append("        }\n");
      source
          .append("        lastElementOrder = Math.max(lastElementOrder, ")
          .append(field.order())
          .append(");\n");
      if ("optional".equals(field.cardinality().shape())) {
        source.append("        if (").append(field.javaName()).append(".isPresent()) {\n");
      } else {
        source.append("        if (").append(field.javaName()).append(" != null) {\n");
      }
      source
          .append("          throw readException(input, \"MXJB-GR-005\", \"Repeated XML choice ")
          .append(escape(field.javaName()))
          .append(".\");\n");
      source.append("        }\n");
      String valueExpression = readBranchValueExpression(branch);
      if ("optional".equals(field.cardinality().shape())) {
        source
            .append("        ")
            .append(field.javaName())
            .append(" = java.util.Optional.of(new ")
            .append(branch.branchJavaName().qualifiedName())
            .append("(")
            .append(valueExpression)
            .append("));\n");
      } else {
        source
            .append("        ")
            .append(field.javaName())
            .append(" = new ")
            .append(branch.branchJavaName().qualifiedName())
            .append("(")
            .append(valueExpression)
            .append(");\n");
      }
    }

    private void appendMaxOccursCheck(StringBuilder source, BindingField field) {
      if ("unbounded".equals(field.cardinality().maxOccurs())) {
        return;
      }
      source.append("        if (").append(field.javaName()).append("Values.size() >= ");
      source.append(Integer.parseInt(field.cardinality().maxOccurs()));
      source.append(") {\n");
      source
          .append("          throw readException(input, \"MXJB-GR-005\", \"Too many XML elements ")
          .append(escape(field.xmlName().toText()))
          .append(".\");\n");
      source.append("        }\n");
    }

    private String readValueExpression(BindingField field) {
      BindingType nestedType = modelType(field);
      if (nestedType != null) {
        return helperName(nestedType) + "(input, " + nameConstant(field.xmlName()) + ")";
      }
      return "read"
          + scalarMethodSuffix(field.type().name())
          + "Element(input, "
          + nameConstant(field.xmlName())
          + ")";
    }

    private String readBranchValueExpression(BindingChoiceBranch branch) {
      BindingType nestedType = modelType(branch.type());
      if (nestedType != null) {
        return helperName(nestedType) + "(input, " + nameConstant(branch.xmlName()) + ")";
      }
      return "read"
          + scalarMethodSuffix(branch.type().name())
          + "Element(input, "
          + nameConstant(branch.xmlName())
          + ")";
    }

    private void appendRequiredCheck(StringBuilder source, BindingField field) {
      if ("optional".equals(field.cardinality().shape())
          || "list".equals(field.cardinality().shape())) {
        return;
      }
      source.append("    if (").append(field.javaName()).append(" == null) {\n");
      source
          .append("      throw readException(input, \"MXJB-GR-004\", \"Missing required XML ")
          .append("attribute".equals(field.kind()) ? "attribute " : "element ")
          .append(escape(field.xmlName().toText()))
          .append(".\");\n");
      source.append("    }\n");
    }

    private void appendRequiredListCheck(StringBuilder source, BindingField field) {
      source
          .append("    if (")
          .append(field.javaName())
          .append("Values.size() < ")
          .append(field.cardinality().minOccurs())
          .append(") {\n")
          .append(
              "      throw readException(input, \"MXJB-GR-004\", \"Missing required XML element ")
          .append(escape(field.xmlName().toText()))
          .append(".\");\n")
          .append("    }\n");
    }

    private String constructorArguments(BindingType type) {
      return type.fields().stream()
          .map(this::constructorArgument)
          .collect(java.util.stream.Collectors.joining(", "));
    }

    private String constructorArgument(BindingField field) {
      if ("list".equals(field.cardinality().shape())) {
        return "java.util.List.copyOf(" + field.javaName() + "Values)";
      }
      return field.javaName();
    }

    private void appendSharedHelpers(StringBuilder source) {
      appendContentHelpers(source);
      appendScalarElementHelpers(source);
      appendScalarParseHelpers(source);
      appendDiagnosticHelpers(source);
    }

    private void appendContentHelpers(StringBuilder source) {
      source
          .append("  private static void moveToDocumentContent(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append(
              "    while (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.START_DOCUMENT\n")
          .append("        || isWhitespace(input)) {\n")
          .append("      if (!input.next()) {\n")
          .append("        return;\n")
          .append("      }\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static void movePastWhitespace(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    while (isWhitespace(input)) {\n")
          .append("      if (!input.next()) {\n")
          .append("        return;\n")
          .append("      }\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static boolean isWhitespace(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input) {\n")
          .append("    return input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.TEXT\n")
          .append("        && input.text().isBlank();\n")
          .append("  }\n\n")
          .append("  private static void expectStart(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append(
              "    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.START_ELEMENT\n")
          .append("        || !name.equals(input.name())) {\n")
          .append("      throw readException(input, \"MXJB-GR-002\", \"Expected XML element.\");\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static String attribute(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name) {\n")
          .append("    for (int index = 0; index < input.attributeCount(); index++) {\n")
          .append("      if (name.equals(input.attributeName(index))) {\n")
          .append("        return input.attributeValue(index);\n")
          .append("      }\n")
          .append("    }\n")
          .append("    return null;\n")
          .append("  }\n\n")
          .append("  private static String readTextElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    expectStart(input, name);\n")
          .append("    StringBuilder text = new StringBuilder();\n")
          .append("    while (input.next()) {\n")
          .append(
              "      if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.TEXT) {\n")
          .append("        text.append(input.text());\n")
          .append(
              "      } else if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT) {\n")
          .append("        if (!name.equals(input.name())) {\n")
          .append(
              "          throw readException(input, \"MXJB-GR-007\", \"Mismatched text element end.\");\n")
          .append("        }\n")
          .append("        input.next();\n")
          .append("        return text.toString();\n")
          .append("      } else {\n")
          .append(
              "        throw readException(input, \"MXJB-GR-007\", \"Expected XML text content.\");\n")
          .append("      }\n")
          .append("    }\n")
          .append(
              "    throw readException(input, \"MXJB-GR-007\", \"Unclosed XML text element.\");\n")
          .append("  }\n\n");
    }

    private void appendScalarElementHelpers(StringBuilder source) {
      source
          .append("  private static String readStringElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return readTextElement(input, name);\n")
          .append("  }\n\n")
          .append("  private static Boolean readBooleanElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseBoolean(readTextElement(input, name), input.location());\n")
          .append("  }\n\n")
          .append("  private static Integer readIntElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseInt(readTextElement(input, name), input.location());\n")
          .append("  }\n\n")
          .append("  private static java.math.BigInteger readIntegerElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseInteger(readTextElement(input, name), input.location());\n")
          .append("  }\n\n")
          .append("  private static Long readLongElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseLong(readTextElement(input, name), input.location());\n")
          .append("  }\n\n")
          .append("  private static java.math.BigDecimal readDecimalElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseDecimal(readTextElement(input, name), input.location());\n")
          .append("  }\n\n");
    }

    private void appendScalarParseHelpers(StringBuilder source) {
      source
          .append("  private static Boolean parseBoolean(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return switch (value.trim()) {\n")
          .append("      case \"true\", \"1\" -> Boolean.TRUE;\n")
          .append("      case \"false\", \"0\" -> Boolean.FALSE;\n")
          .append(
              "      default -> throw readException(location, \"MXJB-GR-006\", \"Invalid boolean value.\");\n")
          .append("    };\n")
          .append("  }\n\n")
          .append("  private static Integer parseInt(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    try {\n")
          .append("      return Integer.valueOf(value.trim());\n")
          .append("    } catch (NumberFormatException exception) {\n")
          .append(
              "      throw readException(location, \"MXJB-GR-006\", \"Invalid int value.\", exception);\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static java.math.BigInteger parseInteger(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    try {\n")
          .append("      return new java.math.BigInteger(value.trim());\n")
          .append("    } catch (NumberFormatException exception) {\n")
          .append(
              "      throw readException(location, \"MXJB-GR-006\", \"Invalid integer value.\", exception);\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static Long parseLong(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    try {\n")
          .append("      return Long.valueOf(value.trim());\n")
          .append("    } catch (NumberFormatException exception) {\n")
          .append(
              "      throw readException(location, \"MXJB-GR-006\", \"Invalid long value.\", exception);\n")
          .append("    }\n")
          .append("  }\n\n")
          .append("  private static java.math.BigDecimal parseDecimal(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    try {\n")
          .append("      return new java.math.BigDecimal(value.trim());\n")
          .append("    } catch (NumberFormatException exception) {\n")
          .append(
              "      throw readException(location, \"MXJB-GR-006\", \"Invalid decimal value.\", exception);\n")
          .append("    }\n")
          .append("  }\n\n");
    }

    private void appendDiagnosticHelpers(StringBuilder source) {
      source
          .append(
              "  private static io.github.mundanej.mxjb.runtime.XmlReadException readException(\n")
          .append(
              "      io.github.mundanej.mxjb.runtime.XmlEventReader input, String code, String message) {\n")
          .append("    return readException(input.location(), code, message);\n")
          .append("  }\n\n")
          .append(
              "  private static io.github.mundanej.mxjb.runtime.XmlReadException readException(\n")
          .append(
              "      io.github.mundanej.mxjb.runtime.XmlLocation location, String code, String message) {\n")
          .append("    return new io.github.mundanej.mxjb.runtime.XmlReadException(\n")
          .append("        new io.github.mundanej.mxjb.runtime.XmlDiagnostic(\n")
          .append("            io.github.mundanej.mxjb.runtime.XmlDiagnosticSeverity.ERROR,\n")
          .append("            code,\n")
          .append("            message,\n")
          .append("            location));\n")
          .append("  }\n\n")
          .append(
              "  private static io.github.mundanej.mxjb.runtime.XmlReadException readException(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlLocation location,\n")
          .append("      String code,\n")
          .append("      String message,\n")
          .append("      Throwable cause) {\n")
          .append("    return new io.github.mundanej.mxjb.runtime.XmlReadException(\n")
          .append("        new io.github.mundanej.mxjb.runtime.XmlDiagnostic(\n")
          .append("            io.github.mundanej.mxjb.runtime.XmlDiagnosticSeverity.ERROR,\n")
          .append("            code,\n")
          .append("            message,\n")
          .append("            location),\n")
          .append("        cause);\n")
          .append("  }\n");
    }

    private String parseScalarExpression(BindingField field, String valueExpression) {
      return switch (field.type().name()) {
        case "boolean" -> "parseBoolean(" + valueExpression + ", input.location())";
        case "int" -> "parseInt(" + valueExpression + ", input.location())";
        case "integer" -> "parseInteger(" + valueExpression + ", input.location())";
        case "long" -> "parseLong(" + valueExpression + ", input.location())";
        case "decimal" -> "parseDecimal(" + valueExpression + ", input.location())";
        default -> valueExpression;
      };
    }

    private String scalarMethodSuffix(String scalarName) {
      return switch (scalarName) {
        case "boolean" -> "Boolean";
        case "int" -> "Int";
        case "integer" -> "Integer";
        case "long" -> "Long";
        case "decimal" -> "Decimal";
        default -> "String";
      };
    }

    private String localType(BindingField field) {
      if ("choice".equals(field.type().kind())) {
        return field.type().name();
      }
      if ("model".equals(field.type().kind())) {
        BindingType type = Objects.requireNonNull(index.type(field.type().name()));
        return typeText(type);
      }
      return switch (field.type().name()) {
        case "boolean" -> "Boolean";
        case "int" -> "Integer";
        case "integer" -> "java.math.BigInteger";
        case "long" -> "Long";
        case "decimal" -> "java.math.BigDecimal";
        default -> "String";
      };
    }

    private List<BindingField> requiredFields(BindingType type) {
      return type.fields().stream()
          .filter(field -> !"list".equals(field.cardinality().shape()))
          .filter(field -> "required".equals(field.cardinality().shape()))
          .sorted(Comparator.comparingInt(BindingField::order))
          .toList();
    }

    private List<BindingField> requiredListFields(BindingType type) {
      return type.fields().stream()
          .filter(field -> "list".equals(field.cardinality().shape()))
          .filter(field -> field.cardinality().minOccurs() > 0)
          .sorted(Comparator.comparingInt(BindingField::order))
          .toList();
    }

    private List<BindingField> attributes(BindingType type) {
      return type.fields().stream()
          .filter(field -> "attribute".equals(field.kind()))
          .sorted(Comparator.comparingInt(BindingField::order))
          .toList();
    }

    private List<BindingField> elements(BindingType type) {
      return type.fields().stream()
          .filter(field -> "element".equals(field.kind()))
          .sorted(Comparator.comparingInt(BindingField::order))
          .toList();
    }

    private List<BindingField> contentFields(BindingType type) {
      return type.fields().stream()
          .filter(field -> "element".equals(field.kind()) || "choice".equals(field.kind()))
          .sorted(Comparator.comparingInt(BindingField::order))
          .toList();
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

    private void collectNames(SchemaQName elementName, BindingType type, Set<String> visited) {
      nameConstant(elementName);
      if (!visited.add(type.javaName().qualifiedName())) {
        return;
      }
      for (BindingField field : attributes(type)) {
        nameConstant(field.xmlName());
      }
      for (BindingField field : elements(type)) {
        nameConstant(field.xmlName());
        BindingType nestedType = modelType(field);
        if (nestedType != null) {
          collectNames(field.xmlName(), nestedType, visited);
        }
      }
      for (BindingField field :
          type.fields().stream().filter(value -> "choice".equals(value.kind())).toList()) {
        for (BindingChoiceBranch branch : field.choice().branches()) {
          nameConstant(branch.xmlName());
          BindingType nestedType = modelType(branch.type());
          if (nestedType != null) {
            collectNames(branch.xmlName(), nestedType, visited);
          }
        }
      }
    }

    private String nameConstant(SchemaQName name) {
      return nameConstants.computeIfAbsent(name, ignored -> "NAME_" + (nameConstants.size() + 1));
    }

    private String helperName(BindingType type) {
      return "read" + type.javaName().simpleName();
    }

    private String typeText(BindingType type) {
      return type.javaName().qualifiedName();
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
