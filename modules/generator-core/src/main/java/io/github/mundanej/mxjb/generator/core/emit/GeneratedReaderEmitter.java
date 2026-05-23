package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingChoiceBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingModel;
import io.github.mundanej.mxjb.generator.core.bind.BindingResult;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.XmlSchemaBuiltIns;
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
        if ("attribute".equals(field.kind())
            && !Set.of("scalar", "list", "union").contains(field.type().kind())) {
          diagnostics.add(
              invalidModel("Reader attributes require scalar type " + field.type().toText() + "."));
        }
        if ("attribute".equals(field.kind()) && "list".equals(field.cardinality().shape())) {
          diagnostics.add(invalidModel("Reader attributes do not support list cardinality."));
        }
        if ("content".equals(field.kind()) && field.content() == null) {
          diagnostics.add(invalidModel("Reader content field is missing content metadata."));
        }
        if ("content".equals(field.kind()) && !"list".equals(field.cardinality().shape())) {
          diagnostics.add(invalidModel("Reader content fields require list cardinality."));
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
        || "choice".equals(kind)
        || "wildcard".equals(kind)
        || "content".equals(kind);
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
    private static final SchemaQName XSI_NIL =
        new SchemaQName("http://www.w3.org/2001/XMLSchema-instance", "nil");
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
      if (needsNillableSupport(rootType, new LinkedHashSet<>())) {
        nameConstant(XSI_NIL);
      }
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
      BindingField mixedContent = mixedContentField(type);
      if (mixedContent == null) {
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
      } else {
        appendMixedContentLoop(source, mixedContent);
      }
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
        if ("content".equals(field.kind())) {
          for (BindingContentBranch branch : field.content().branches()) {
            BindingType branchType = modelType(branch.type());
            if (branchType != null
                && !helperNames.contains(branchType.javaName().qualifiedName())) {
              appendHelper(source, branchType);
            }
          }
        }
      }
    }

    private void appendMixedContentLoop(StringBuilder source, BindingField field) {
      source.append("    while (true) {\n");
      source.append(
          "      if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.TEXT) {\n");
      source.append("        if (!input.text().isBlank()) {\n");
      source
          .append("          ")
          .append(field.javaName())
          .append("Values.add(new ")
          .append(textContentBranch(field).branchJavaName().qualifiedName())
          .append("(input.text()));\n");
      source.append("        }\n");
      source.append("        if (!input.next()) {\n");
      source.append(
          "          throw readException(input, \"MXJB-GR-007\", \"Unexpected end of XML input.\");\n");
      source.append("        }\n");
      source.append(
          "      } else if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT) {\n");
      source.append("        break;\n");
      source.append(
          "      } else if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.START_ELEMENT) {\n");
      appendMixedElementDispatch(source, field);
      source.append("      } else {\n");
      source.append("        if (!input.next()) {\n");
      source.append(
          "          throw readException(input, \"MXJB-GR-007\", \"Unexpected end of XML input.\");\n");
      source.append("        }\n");
      source.append("      }\n");
      source.append("    }\n");
    }

    private void appendUnexpectedAttributeCheck(StringBuilder source, BindingType type) {
      for (BindingField anyAttribute : anyAttributes(type)) {
        source
            .append("    java.util.ArrayList<io.github.mundanej.mxjb.runtime.XmlAttribute> ")
            .append(anyAttribute.javaName())
            .append("Values = new java.util.ArrayList<>();\n");
      }
      source.append("    for (int index = 0; index < input.attributeCount(); index++) {\n");
      source.append(
          "      io.github.mundanej.mxjb.runtime.XmlName attributeName = input.attributeName(index);\n");
      List<BindingField> attributes = attributes(type);
      List<BindingField> anyAttributes = anyAttributes(type);
      List<io.github.mundanej.mxjb.generator.core.schema.SchemaQName> prohibitedNames =
          anyAttributes.stream()
              .flatMap(field -> field.wildcard().excludedNames().stream())
              .toList();
      for (io.github.mundanej.mxjb.generator.core.schema.SchemaQName prohibited : prohibitedNames) {
        source
            .append("      if (")
            .append(nameConstant(prohibited))
            .append(".equals(attributeName)) {\n");
        source.append(
            "        throw readException(input, \"MXJB-GR-003\", \"Prohibited XML attribute.\");\n");
        source.append("      }\n");
      }
      if (!attributes.isEmpty()) {
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
      }
      if (anyAttributes.isEmpty()) {
        source.append(
            attributes.isEmpty()
                ? "      throw readException(input, \"MXJB-GR-003\", \"Unexpected XML attribute.\");\n"
                : "        throw readException(input, \"MXJB-GR-003\", \"Unexpected XML attribute.\");\n");
        if (!attributes.isEmpty()) {
          source.append("      }\n");
        }
      } else {
        if (attributes.isEmpty()) {
          source.append("      if (");
        } else {
          source.append("        if (");
        }
        for (int indexValue = 0; indexValue < anyAttributes.size(); indexValue++) {
          if (indexValue > 0) {
            source.append("\n            || ");
          }
          source.append(wildcardMatchExpression(anyAttributes.get(indexValue), "attributeName"));
        }
        source.append(") {\n");
        source
            .append(attributes.isEmpty() ? "        " : "          ")
            .append(anyAttributes.getFirst().javaName())
            .append("Values.add(new io.github.mundanej.mxjb.runtime.XmlAttribute(attributeName, ")
            .append("input.attributeValue(index)));\n");
        source.append(attributes.isEmpty() ? "      } else {\n" : "        } else {\n");
        source.append(
            attributes.isEmpty()
                ? "        throw readException(input, \"MXJB-GR-003\", \"Unexpected XML attribute.\");\n"
                : "          throw readException(input, \"MXJB-GR-003\", \"Unexpected XML attribute.\");\n");
        source.append(attributes.isEmpty() ? "      }\n" : "        }\n");
        if (!attributes.isEmpty()) {
          source.append("      }\n");
        }
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
      if (field.semantics().hasDefault() || field.semantics().hasFixed()) {
        String fallback =
            field.semantics().hasDefault()
                ? field.semantics().defaultValue()
                : field.semantics().fixedValue();
        source
            .append("    ")
            .append(localType(field))
            .append(' ')
            .append(field.javaName())
            .append(" = ")
            .append(
                parseExpression(
                    field.type(),
                    textName + " == null ? \"" + escape(fallback) + "\" : " + textName))
            .append(";\n");
        if (field.semantics().hasFixed()) {
          appendFixedReadCheck(source, field, "    ");
        }
      } else if ("optional".equals(field.cardinality().shape())) {
        source
            .append("    ")
            .append(localType(field))
            .append(' ')
            .append(field.javaName())
            .append("Value = ")
            .append(textName)
            .append(" == null ? null : ")
            .append(parseExpression(field.type(), textName))
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
            .append(parseExpression(field.type(), textName))
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
      } else if (field.semantics().nillable()) {
        source
            .append("    java.util.Optional<")
            .append(localType(field))
            .append("> ")
            .append(field.javaName())
            .append(" = null;\n");
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
        } else if ("wildcard".equals(field.kind())) {
          appendDispatchPrefix(source, firstBranch);
          source.append(wildcardMatchExpression(field)).append(") {\n");
          appendWildcardRead(source, field);
          firstBranch = false;
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

    private void appendMixedElementDispatch(StringBuilder source, BindingField field) {
      boolean firstBranch = true;
      for (BindingContentBranch branch : field.content().branches()) {
        if ("text".equals(branch.kind())) {
          continue;
        }
        appendDispatchPrefix(source, firstBranch);
        if ("wildcard".equals(branch.kind())) {
          source.append(wildcardMatchExpression(branch)).append(") {\n");
        } else {
          source.append(nameConstant(branch.xmlName())).append(".equals(input.name())) {\n");
        }
        appendContentBranchRead(source, field, branch);
        firstBranch = false;
      }
      if (firstBranch) {
        source.append(
            "        throw readException(input, \"MXJB-GR-002\", \"Unexpected XML element.\");\n");
      } else {
        source.append("        } else {\n");
        source.append(
            "          throw readException(input, \"MXJB-GR-002\", \"Unexpected XML element.\");\n");
        source.append("        }\n");
      }
    }

    private void appendDispatchPrefix(StringBuilder source, boolean firstBranch) {
      source.append(firstBranch ? "      if (" : "      } else if (");
    }

    private void appendWildcardRead(StringBuilder source, BindingField field) {
      source.append("        if (").append(field.order()).append(" < lastElementOrder) {\n");
      source.append(
          "          throw readException(input, \"MXJB-GR-002\", \"Out-of-order XML wildcard content.\");\n");
      source.append("        }\n");
      source
          .append("        lastElementOrder = Math.max(lastElementOrder, ")
          .append(field.order())
          .append(");\n");
      appendMaxOccursCheck(source, field);
      source
          .append("        ")
          .append(field.javaName())
          .append("Values.add(readFragment(input));\n");
    }

    private void appendContentBranchRead(
        StringBuilder source, BindingField field, BindingContentBranch branch) {
      source.append("          if (").append(branch.order()).append(" < lastElementOrder) {\n");
      source.append(
          "            throw readException(input, \"MXJB-GR-002\", \"Out-of-order XML mixed content.\");\n");
      source.append("          }\n");
      source
          .append("          lastElementOrder = Math.max(lastElementOrder, ")
          .append(branch.order())
          .append(");\n");
      appendContentMaxOccursCheck(source, field, branch);
      source
          .append("          ")
          .append(field.javaName())
          .append("Values.add(new ")
          .append(branch.branchJavaName().qualifiedName())
          .append("(")
          .append(readContentBranchValueExpression(branch))
          .append("));\n");
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
      if (field.semantics().nillable()) {
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
            .append(readNillableValueExpression(field))
            .append(";\n");
      } else if ("list".equals(shape)) {
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
        if (field.semantics().hasFixed()) {
          appendFixedOptionalReadCheck(source, field, "        ");
        }
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
        if (field.semantics().hasFixed()) {
          appendFixedReadCheck(source, field, "        ");
        }
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
      if ("list".equals(field.cardinality().shape())) {
        appendMaxOccursCheck(source, field);
      } else if ("optional".equals(field.cardinality().shape())) {
        source.append("        if (").append(field.javaName()).append(".isPresent()) {\n");
      } else {
        source.append("        if (").append(field.javaName()).append(" != null) {\n");
      }
      if (!"list".equals(field.cardinality().shape())) {
        source
            .append("          throw readException(input, \"MXJB-GR-005\", \"Repeated XML ")
            .append(escape(field.choice().modelKind()))
            .append(" ")
            .append(escape(field.javaName()))
            .append(".\");\n");
        source.append("        }\n");
      }
      String valueExpression = readBranchValueExpression(branch);
      if ("list".equals(field.cardinality().shape())) {
        source
            .append("        ")
            .append(field.javaName())
            .append("Values.add(new ")
            .append(branch.branchJavaName().qualifiedName())
            .append("(")
            .append(valueExpression)
            .append("));\n");
      } else if ("optional".equals(field.cardinality().shape())) {
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
          .append("          throw readException(input, \"MXJB-GR-005\", \"Too many XML ")
          .append("wildcard".equals(field.kind()) ? "wildcard content" : "elements ")
          .append("wildcard".equals(field.kind()) ? "" : escape(field.xmlName().toText()))
          .append(".\");\n");
      source.append("        }\n");
    }

    private void appendContentMaxOccursCheck(
        StringBuilder source, BindingField field, BindingContentBranch branch) {
      if ("unbounded".equals(branch.cardinality().maxOccurs())) {
        return;
      }
      source
          .append("          int current")
          .append(branch.branchJavaName().simpleName())
          .append("Count = 0;\n");
      source
          .append("          for (")
          .append(localType(field))
          .append(" item : ")
          .append(field.javaName())
          .append("Values) {\n");
      source
          .append("            if (item instanceof ")
          .append(branch.branchJavaName().qualifiedName())
          .append(") {\n");
      source
          .append("              current")
          .append(branch.branchJavaName().simpleName())
          .append("Count++;\n");
      source.append("            }\n");
      source.append("          }\n");
      source
          .append("          if (current")
          .append(branch.branchJavaName().simpleName())
          .append("Count >= ")
          .append(Integer.parseInt(branch.cardinality().maxOccurs()))
          .append(") {\n");
      source.append(
          "            throw readException(input, \"MXJB-GR-005\", \"Too many XML mixed content values.\");\n");
      source.append("          }\n");
    }

    private String readValueExpression(BindingField field) {
      BindingType nestedType = modelType(field);
      if (nestedType != null) {
        return helperName(nestedType) + "(input, " + nameConstant(field.xmlName()) + ")";
      }
      if (field.semantics().hasDefault()) {
        return parseExpression(
            field.type(),
            "defaultedText(readTextElement(input, "
                + nameConstant(field.xmlName())
                + "), \""
                + escape(field.semantics().defaultValue())
                + "\")");
      }
      return readElementValueExpression(field.type(), nameConstant(field.xmlName()));
    }

    private String readNillableValueExpression(BindingField field) {
      BindingType nestedType = modelType(field);
      if (nestedType != null) {
        return "readNilElement(input, "
            + nameConstant(field.xmlName())
            + ") ? java.util.Optional.empty() : java.util.Optional.of("
            + helperName(nestedType)
            + "(input, "
            + nameConstant(field.xmlName())
            + "))";
      }
      return "readNilElement(input, "
          + nameConstant(field.xmlName())
          + ") ? java.util.Optional.empty() : java.util.Optional.of("
          + readValueExpression(field)
          + ")";
    }

    private String readBranchValueExpression(BindingChoiceBranch branch) {
      BindingType nestedType = modelType(branch.type());
      if (nestedType != null) {
        return helperName(nestedType) + "(input, " + nameConstant(branch.xmlName()) + ")";
      }
      return readElementValueExpression(branch.type(), nameConstant(branch.xmlName()));
    }

    private String readContentBranchValueExpression(BindingContentBranch branch) {
      if ("wildcard".equals(branch.kind())) {
        return "readFragment(input)";
      }
      BindingType nestedType = modelType(branch.type());
      if (nestedType != null) {
        return helperName(nestedType) + "(input, " + nameConstant(branch.xmlName()) + ")";
      }
      return readElementValueExpression(branch.type(), nameConstant(branch.xmlName()));
    }

    private String readElementValueExpression(BindingTypeReference reference, String nameConstant) {
      if ("union".equals(reference.kind()) || "string".equals(reference.name())) {
        return parseExpression(reference, "readTextElement(input, " + nameConstant + ")");
      }
      if ("list".equals(reference.kind())) {
        return "readDatatypeListElement(input, "
            + nameConstant
            + ", \""
            + escape(reference.itemType().name())
            + "\", "
            + scalarClassLiteral(reference.itemType())
            + ")";
      }
      if (XmlSchemaBuiltIns.isListValued(reference.name())) {
        return "readDatatypeListElement(input, "
            + nameConstant
            + ", \""
            + escape(listBuiltInItemType(reference.name()))
            + "\", String.class)";
      }
      return "readDatatypeElement(input, "
          + nameConstant
          + ", \""
          + escape(reference.name())
          + "\", "
          + scalarClassLiteral(reference)
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
          .append("      throw readException(input, \"MXJB-GR-004\", \"Missing required XML ")
          .append("wildcard".equals(field.kind()) ? "wildcard content" : "element ")
          .append("wildcard".equals(field.kind()) ? "" : escape(field.xmlName().toText()))
          .append(".\");\n")
          .append("    }\n");
    }

    private void appendFixedReadCheck(StringBuilder source, BindingField field, String indent) {
      String fixedExpression =
          parseExpression(field.type(), "\"" + escape(field.semantics().fixedValue()) + "\"");
      source
          .append(indent)
          .append("if (!java.util.Objects.equals(")
          .append(field.javaName())
          .append(", ")
          .append(fixedExpression)
          .append(")) {\n")
          .append(indent)
          .append(
              "  throw readException(input, \"MXJB-GR-008\", \"XML value does not match fixed value ")
          .append(escape(field.xmlName().toText()))
          .append(".\");\n")
          .append(indent)
          .append("}\n");
    }

    private void appendFixedOptionalReadCheck(
        StringBuilder source, BindingField field, String indent) {
      String fixedExpression =
          parseExpression(field.type(), "\"" + escape(field.semantics().fixedValue()) + "\"");
      source
          .append(indent)
          .append("if (!java.util.Objects.equals(")
          .append(field.javaName())
          .append(".orElseThrow(), ")
          .append(fixedExpression)
          .append(")) {\n")
          .append(indent)
          .append(
              "  throw readException(input, \"MXJB-GR-008\", \"XML value does not match fixed value ")
          .append(escape(field.xmlName().toText()))
          .append(".\");\n")
          .append(indent)
          .append("}\n");
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
          .append("  }\n\n");
      if (needsWildcardSupport(rootType, new LinkedHashSet<>())) {
        source
            .append("  private static boolean wildcardMatches(\n")
            .append("      io.github.mundanej.mxjb.runtime.XmlName name,\n")
            .append("      String kind,\n")
            .append("      java.util.Set<String> namespaces) {\n")
            .append("    return switch (kind) {\n")
            .append("      case \"any\" -> true;\n")
            .append("      case \"other\" -> !namespaces.contains(name.namespaceUri());\n")
            .append("      default -> namespaces.contains(name.namespaceUri());\n")
            .append("    };\n")
            .append("  }\n\n")
            .append("  private static io.github.mundanej.mxjb.runtime.XmlFragment readFragment(\n")
            .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input)\n")
            .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
            .append(
                "    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.START_ELEMENT) {\n")
            .append(
                "      throw readException(input, \"MXJB-GR-007\", \"Expected wildcard XML element.\");\n")
            .append("    }\n")
            .append("    io.github.mundanej.mxjb.runtime.XmlName name = input.name();\n")
            .append(
                "    java.util.ArrayList<io.github.mundanej.mxjb.runtime.XmlAttribute> attributes =\n")
            .append("        new java.util.ArrayList<>();\n")
            .append("    for (int index = 0; index < input.attributeCount(); index++) {\n")
            .append("      attributes.add(new io.github.mundanej.mxjb.runtime.XmlAttribute(\n")
            .append("          input.attributeName(index), input.attributeValue(index)));\n")
            .append("    }\n")
            .append(
                "    java.util.ArrayList<io.github.mundanej.mxjb.runtime.XmlFragmentContent> content =\n")
            .append("        new java.util.ArrayList<>();\n")
            .append("    if (!input.next()) {\n")
            .append(
                "      throw readException(input, \"MXJB-GR-007\", \"Unclosed wildcard XML fragment.\");\n")
            .append("    }\n")
            .append("    while (true) {\n")
            .append(
                "      if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.TEXT) {\n")
            .append(
                "        content.add(new io.github.mundanej.mxjb.runtime.XmlFragmentText(input.text()));\n")
            .append("        if (!input.next()) {\n")
            .append(
                "          throw readException(input, \"MXJB-GR-007\", \"Unclosed wildcard XML fragment.\");\n")
            .append("        }\n")
            .append(
                "      } else if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.START_ELEMENT) {\n")
            .append(
                "        content.add(new io.github.mundanej.mxjb.runtime.XmlFragmentElement(readFragment(input)));\n")
            .append(
                "      } else if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT) {\n")
            .append("        if (!name.equals(input.name())) {\n")
            .append(
                "          throw readException(input, \"MXJB-GR-007\", \"Mismatched wildcard fragment end.\");\n")
            .append("        }\n")
            .append("        input.next();\n")
            .append(
                "        return new io.github.mundanej.mxjb.runtime.XmlFragment(name, attributes, content);\n")
            .append("      } else {\n")
            .append("        if (!input.next()) {\n")
            .append(
                "          throw readException(input, \"MXJB-GR-007\", \"Unclosed wildcard XML fragment.\");\n")
            .append("        }\n")
            .append("      }\n")
            .append("    }\n")
            .append("  }\n\n");
      }
      source
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
          .append("  }\n\n");
      if (needsDefaultedElementSupport(rootType, new LinkedHashSet<>())) {
        source
            .append("  private static String defaultedText(String value, String defaultValue) {\n")
            .append("    return value.isEmpty() ? defaultValue : value;\n")
            .append("  }\n\n");
      }
      source
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
      if (needsNillableSupport(rootType, new LinkedHashSet<>())) {
        source
            .append("  private static boolean readNilElement(\n")
            .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
            .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
            .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
            .append("    expectStart(input, name);\n")
            .append("    String nil = attribute(input, ")
            .append(nameConstant(XSI_NIL))
            .append(");\n")
            .append("    if (!\"true\".equals(nil) && !\"1\".equals(nil)) {\n")
            .append("      return false;\n")
            .append("    }\n")
            .append("    if (!input.next()) {\n")
            .append(
                "      throw readException(input, \"MXJB-GR-007\", \"Unexpected end of XML input.\");\n")
            .append("    }\n")
            .append("    movePastWhitespace(input);\n")
            .append(
                "    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT\n")
            .append("        || !name.equals(input.name())) {\n")
            .append(
                "      throw readException(input, \"MXJB-GR-009\", \"xsi:nil element must be empty.\");\n")
            .append("    }\n")
            .append("    input.next();\n")
            .append("    return true;\n")
            .append("  }\n\n");
      }
    }

    private void appendScalarElementHelpers(StringBuilder source) {
      source
          .append("  private static String readStringElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return readTextElement(input, name);\n")
          .append("  }\n\n")
          .append("  private static <T> T readDatatypeElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name,\n")
          .append("      String datatype,\n")
          .append("      Class<T> javaType)\n")
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
          .append("        T value =\n")
          .append("            javaType.cast(\n")
          .append("                io.github.mundanej.mxjb.runtime.XmlDatatypes.parse(\n")
          .append("                    datatype, text.toString(), input, input.location()));\n")
          .append("        input.next();\n")
          .append("        return value;\n")
          .append("      } else {\n")
          .append(
              "        throw readException(input, \"MXJB-GR-007\", \"Expected XML text content.\");\n")
          .append("      }\n")
          .append("    }\n")
          .append(
              "    throw readException(input, \"MXJB-GR-007\", \"Unclosed XML text element.\");\n")
          .append("  }\n\n")
          .append("  private static <T> java.util.List<T> readDatatypeListElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name,\n")
          .append("      String itemDatatype,\n")
          .append("      Class<T> javaType)\n")
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
          .append("        java.util.List<T> value =\n")
          .append("            io.github.mundanej.mxjb.runtime.XmlDatatypes.parseList(\n")
          .append(
              "                itemDatatype, text.toString(), input, input.location(), javaType);\n")
          .append("        input.next();\n")
          .append("        return value;\n")
          .append("      } else {\n")
          .append(
              "        throw readException(input, \"MXJB-GR-007\", \"Expected XML text content.\");\n")
          .append("      }\n")
          .append("    }\n")
          .append(
              "    throw readException(input, \"MXJB-GR-007\", \"Unclosed XML text element.\");\n")
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
      if (!needsListSupport()) {
        return;
      }
      source
          .append("  private static java.util.List<String> readStringListElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseStringList(readTextElement(input, name), input.location());\n")
          .append("  }\n\n")
          .append("  private static java.util.List<Boolean> readBooleanListElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseBooleanList(readTextElement(input, name), input.location());\n")
          .append("  }\n\n")
          .append("  private static java.util.List<Integer> readIntListElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseIntList(readTextElement(input, name), input.location());\n")
          .append("  }\n\n")
          .append("  private static java.util.List<java.math.BigInteger> readIntegerListElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseIntegerList(readTextElement(input, name), input.location());\n")
          .append("  }\n\n")
          .append("  private static java.util.List<Long> readLongListElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseLongList(readTextElement(input, name), input.location());\n")
          .append("  }\n\n")
          .append("  private static java.util.List<java.math.BigDecimal> readDecimalListElement(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlEventReader input,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName name)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    return parseDecimalList(readTextElement(input, name), input.location());\n")
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
      if (!needsListSupport()) {
        return;
      }
      source
          .append("  private static java.util.List<String> parseStringList(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location) {\n")
          .append("    java.util.ArrayList<String> values = new java.util.ArrayList<>();\n")
          .append("    for (String token : listTokens(value)) {\n")
          .append("      values.add(token);\n")
          .append("    }\n")
          .append("    return java.util.List.copyOf(values);\n")
          .append("  }\n\n")
          .append("  private static java.util.List<Boolean> parseBooleanList(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    java.util.ArrayList<Boolean> values = new java.util.ArrayList<>();\n")
          .append("    for (String token : listTokens(value)) {\n")
          .append("      values.add(parseBoolean(token, location));\n")
          .append("    }\n")
          .append("    return java.util.List.copyOf(values);\n")
          .append("  }\n\n")
          .append("  private static java.util.List<Integer> parseIntList(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    java.util.ArrayList<Integer> values = new java.util.ArrayList<>();\n")
          .append("    for (String token : listTokens(value)) {\n")
          .append("      values.add(parseInt(token, location));\n")
          .append("    }\n")
          .append("    return java.util.List.copyOf(values);\n")
          .append("  }\n\n")
          .append("  private static java.util.List<java.math.BigInteger> parseIntegerList(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append(
              "    java.util.ArrayList<java.math.BigInteger> values = new java.util.ArrayList<>();\n")
          .append("    for (String token : listTokens(value)) {\n")
          .append("      values.add(parseInteger(token, location));\n")
          .append("    }\n")
          .append("    return java.util.List.copyOf(values);\n")
          .append("  }\n\n")
          .append("  private static java.util.List<Long> parseLongList(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append("    java.util.ArrayList<Long> values = new java.util.ArrayList<>();\n")
          .append("    for (String token : listTokens(value)) {\n")
          .append("      values.add(parseLong(token, location));\n")
          .append("    }\n")
          .append("    return java.util.List.copyOf(values);\n")
          .append("  }\n\n")
          .append("  private static java.util.List<java.math.BigDecimal> parseDecimalList(\n")
          .append("      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlReadException {\n")
          .append(
              "    java.util.ArrayList<java.math.BigDecimal> values = new java.util.ArrayList<>();\n")
          .append("    for (String token : listTokens(value)) {\n")
          .append("      values.add(parseDecimal(token, location));\n")
          .append("    }\n")
          .append("    return java.util.List.copyOf(values);\n")
          .append("  }\n\n")
          .append("  private static java.util.List<String> listTokens(String value) {\n")
          .append("    String trimmed = value.trim();\n")
          .append("    if (trimmed.isEmpty()) {\n")
          .append("      return java.util.List.of();\n")
          .append("    }\n")
          .append("    return java.util.List.of(trimmed.split(\"\\\\s+\"));\n")
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

    private String parseExpression(BindingTypeReference reference, String valueExpression) {
      if ("list".equals(reference.kind())) {
        return "io.github.mundanej.mxjb.runtime.XmlDatatypes.parseList(\""
            + escape(reference.itemType().name())
            + "\", "
            + valueExpression
            + ", input, input.location(), "
            + scalarClassLiteral(reference.itemType())
            + ")";
      }
      if ("union".equals(reference.kind())) {
        return valueExpression;
      }
      if (XmlSchemaBuiltIns.isListValued(reference.name())) {
        return "io.github.mundanej.mxjb.runtime.XmlDatatypes.parseList(\""
            + escape(listBuiltInItemType(reference.name()))
            + "\", "
            + valueExpression
            + ", input, input.location(), String.class)";
      }
      if ("string".equals(reference.name())) {
        return valueExpression;
      }
      return "("
          + scalarType(reference)
          + ") io.github.mundanej.mxjb.runtime.XmlDatatypes.parse(\""
          + escape(reference.name())
          + "\", "
          + valueExpression
          + ", input, input.location())";
    }

    private String listBuiltInItemType(String name) {
      return switch (name) {
        case "NMTOKENS" -> "NMTOKEN";
        case "IDREFS" -> "IDREF";
        case "ENTITIES" -> "ENTITY";
        default -> "string";
      };
    }

    private String scalarClassLiteral(BindingTypeReference reference) {
      return switch (reference.name()) {
        case "string",
            "normalizedString",
            "token",
            "language",
            "Name",
            "NCName",
            "NMTOKEN",
            "ID",
            "IDREF",
            "ENTITY" ->
            "String.class";
        case "boolean" -> "Boolean.class";
        case "decimal" -> "java.math.BigDecimal.class";
        case "float" -> "Float.class";
        case "double" -> "Double.class";
        case "integer",
            "nonPositiveInteger",
            "negativeInteger",
            "nonNegativeInteger",
            "positiveInteger",
            "unsignedLong" ->
            "java.math.BigInteger.class";
        case "long", "unsignedInt" -> "Long.class";
        case "int", "unsignedShort" -> "Integer.class";
        case "short", "unsignedByte" -> "Short.class";
        case "byte" -> "Byte.class";
        case "duration" -> "io.github.mundanej.mxjb.runtime.XmlDuration.class";
        case "dateTime" -> "io.github.mundanej.mxjb.runtime.XmlDateTime.class";
        case "date" -> "io.github.mundanej.mxjb.runtime.XmlDate.class";
        case "time" -> "io.github.mundanej.mxjb.runtime.XmlTime.class";
        case "gYear" -> "io.github.mundanej.mxjb.runtime.XmlGYear.class";
        case "gYearMonth" -> "io.github.mundanej.mxjb.runtime.XmlGYearMonth.class";
        case "gMonth" -> "io.github.mundanej.mxjb.runtime.XmlGMonth.class";
        case "gMonthDay" -> "io.github.mundanej.mxjb.runtime.XmlGMonthDay.class";
        case "gDay" -> "io.github.mundanej.mxjb.runtime.XmlGDay.class";
        case "hexBinary", "base64Binary" -> "io.github.mundanej.mxjb.runtime.XmlBinary.class";
        case "anyURI" -> "io.github.mundanej.mxjb.runtime.XmlAnyUri.class";
        case "QName", "NOTATION" -> "io.github.mundanej.mxjb.runtime.XmlQName.class";
        default -> "Object.class";
      };
    }

    private boolean needsListSupport() {
      return needsListSupport(rootType, new LinkedHashSet<>());
    }

    private boolean needsListSupport(BindingType type, Set<String> visited) {
      if (!visited.add(type.javaName().qualifiedName())) {
        return false;
      }
      for (BindingField field : type.fields()) {
        if (containsListType(field.type())) {
          return true;
        }
        BindingType nestedType = modelType(field);
        if (nestedType != null && needsListSupport(nestedType, visited)) {
          return true;
        }
        if ("choice".equals(field.kind())) {
          for (BindingChoiceBranch branch : field.choice().branches()) {
            if (containsListType(branch.type())) {
              return true;
            }
            BindingType branchType = modelType(branch.type());
            if (branchType != null && needsListSupport(branchType, visited)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    private boolean needsNillableSupport(BindingType type, Set<String> visited) {
      if (!visited.add(type.javaName().qualifiedName())) {
        return false;
      }
      for (BindingField field : type.fields()) {
        if (field.semantics().nillable()) {
          return true;
        }
        BindingType nestedType = modelType(field);
        if (nestedType != null && needsNillableSupport(nestedType, visited)) {
          return true;
        }
      }
      return false;
    }

    private boolean needsDefaultedElementSupport(BindingType type, Set<String> visited) {
      if (!visited.add(type.javaName().qualifiedName())) {
        return false;
      }
      for (BindingField field : type.fields()) {
        if ("element".equals(field.kind()) && field.semantics().hasDefault()) {
          return true;
        }
        BindingType nestedType = modelType(field);
        if (nestedType != null && needsDefaultedElementSupport(nestedType, visited)) {
          return true;
        }
      }
      return false;
    }

    private boolean needsWildcardSupport(BindingType type, Set<String> visited) {
      if (!visited.add(type.javaName().qualifiedName())) {
        return false;
      }
      for (BindingField field : type.fields()) {
        if ("wildcard".equals(field.kind()) || "anyAttribute".equals(field.kind())) {
          return true;
        }
        if ("content".equals(field.kind())
            && field.content().branches().stream()
                .anyMatch(branch -> "wildcard".equals(branch.kind()))) {
          return true;
        }
        if ("content".equals(field.kind())) {
          for (BindingContentBranch branch : field.content().branches()) {
            BindingType branchType = modelType(branch.type());
            if (branchType != null && needsWildcardSupport(branchType, visited)) {
              return true;
            }
          }
        }
        BindingType nestedType = modelType(field);
        if (nestedType != null && needsWildcardSupport(nestedType, visited)) {
          return true;
        }
      }
      return false;
    }

    private boolean containsListType(BindingTypeReference reference) {
      if ("list".equals(reference.kind())) {
        return true;
      }
      if ("union".equals(reference.kind())) {
        return reference.unionMembers().stream().anyMatch(this::containsListType);
      }
      return false;
    }

    private String localType(BindingField field) {
      if ("choice".equals(field.type().kind())) {
        return field.type().name();
      }
      if ("model".equals(field.type().kind())) {
        BindingType type = Objects.requireNonNull(index.type(field.type().name()));
        return typeText(type);
      }
      if ("list".equals(field.type().kind())) {
        return "java.util.List<" + scalarType(field.type().itemType()) + ">";
      }
      if ("union".equals(field.type().kind())) {
        return "String";
      }
      if ("fragment".equals(field.type().kind())) {
        return "io.github.mundanej.mxjb.runtime.XmlFragment";
      }
      if ("xmlAttribute".equals(field.type().kind())) {
        return "io.github.mundanej.mxjb.runtime.XmlAttribute";
      }
      return scalarType(field.type());
    }

    private String scalarType(BindingTypeReference reference) {
      return qualifiedScalarType(reference.name());
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

    private List<BindingField> anyAttributes(BindingType type) {
      return type.fields().stream()
          .filter(field -> "anyAttribute".equals(field.kind()))
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
          .filter(
              field ->
                  "element".equals(field.kind())
                      || "choice".equals(field.kind())
                      || "wildcard".equals(field.kind())
                      || "content".equals(field.kind()))
          .sorted(Comparator.comparingInt(BindingField::order))
          .toList();
    }

    private BindingField mixedContentField(BindingType type) {
      return type.fields().stream()
          .filter(field -> "content".equals(field.kind()))
          .findFirst()
          .orElse(null);
    }

    private BindingContentBranch textContentBranch(BindingField field) {
      return field.content().branches().stream()
          .filter(branch -> "text".equals(branch.kind()))
          .findFirst()
          .orElseThrow();
    }

    private String wildcardMatchExpression(BindingField field) {
      return wildcardMatchExpression(field, "input.name()");
    }

    private String wildcardMatchExpression(BindingField field, String nameExpression) {
      return "wildcardMatches("
          + nameExpression
          + ", \""
          + escape(field.wildcard().namespaceConstraint().kind())
          + "\", java.util.Set.of("
          + field.wildcard().namespaceConstraint().namespaces().stream()
              .map(value -> "\"" + escape(value) + "\"")
              .collect(java.util.stream.Collectors.joining(", "))
          + "))";
    }

    private String wildcardMatchExpression(BindingContentBranch branch) {
      return "wildcardMatches(input.name(), \""
          + escape(branch.wildcard().namespaceConstraint().kind())
          + "\", java.util.Set.of("
          + branch.wildcard().namespaceConstraint().namespaces().stream()
              .map(value -> "\"" + escape(value) + "\"")
              .collect(java.util.stream.Collectors.joining(", "))
          + "))";
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
      for (BindingField field : anyAttributes(type)) {
        for (SchemaQName excludedName : field.wildcard().excludedNames()) {
          nameConstant(excludedName);
        }
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
      for (BindingField field :
          type.fields().stream().filter(value -> "content".equals(value.kind())).toList()) {
        for (BindingContentBranch branch : field.content().branches()) {
          if ("text".equals(branch.kind())) {
            continue;
          }
          if (!"wildcard".equals(branch.kind())) {
            nameConstant(branch.xmlName());
          }
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
