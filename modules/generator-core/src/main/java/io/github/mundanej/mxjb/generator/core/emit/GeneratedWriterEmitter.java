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

/** Emits deterministic XML writer source from the internal binding model. */
public final class GeneratedWriterEmitter {
  public GeneratedWriterEmissionResult emit(BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return GeneratedWriterEmissionResult.empty(bindingResult.diagnostics());
    }
    return emit(bindingResult.model());
  }

  public GeneratedWriterEmissionResult emit(BindingModel model) {
    ModelIndex index = new ModelIndex(model);
    List<SchemaDiagnostic> diagnostics = validate(model, index);
    if (!diagnostics.isEmpty()) {
      return GeneratedWriterEmissionResult.empty(diagnostics);
    }

    List<GeneratedJavaSource> sources =
        model.rootElements().stream()
            .sorted(Comparator.comparing(root -> root.xmlName().toText()))
            .map(root -> emitRootWriter(root, index))
            .toList();
    return new GeneratedWriterEmissionResult(sources, List.of());
  }

  private List<SchemaDiagnostic> validate(BindingModel model, ModelIndex index) {
    List<SchemaDiagnostic> diagnostics = new ArrayList<>();
    Set<String> writerNames = new LinkedHashSet<>();
    for (BindingRootElement root : model.rootElements()) {
      if (!"model".equals(root.type().kind())) {
        diagnostics.add(
            invalidModel("Root writer requires model type " + root.xmlName().toText() + "."));
        continue;
      }
      BindingType rootType = index.type(root.type().name());
      if (rootType == null) {
        diagnostics.add(invalidModel("Missing root writer model type " + root.type().name() + "."));
        continue;
      }
      BindingJavaName writerName = writerName(rootType.javaName());
      if (!writerNames.add(writerName.qualifiedName())) {
        diagnostics.add(invalidModel("Duplicate root writer " + writerName.qualifiedName() + "."));
      }
    }
    for (BindingType type : model.types()) {
      if (!"record".equals(type.shape())) {
        diagnostics.add(invalidModel("Unsupported writer model shape " + type.shape() + "."));
      }
      for (BindingField field : type.fields()) {
        if (!isSupportedTypeReference(field.type(), index)) {
          diagnostics.add(
              invalidModel("Unsupported writer field type " + field.type().toText() + "."));
        }
        if (!isSupportedFieldKind(field.kind())) {
          diagnostics.add(invalidModel("Unsupported writer field kind " + field.kind() + "."));
        }
        if ("content".equals(field.kind()) && field.content() == null) {
          diagnostics.add(invalidModel("Writer content field is missing content metadata."));
        }
        if ("content".equals(field.kind()) && !"list".equals(field.cardinality().shape())) {
          diagnostics.add(invalidModel("Writer content fields require list cardinality."));
        }
        if ("attribute".equals(field.kind())
            && !Set.of("scalar", "list", "union").contains(field.type().kind())) {
          diagnostics.add(
              invalidModel("Writer attributes require scalar type " + field.type().toText() + "."));
        }
        if (!isSupportedCardinality(field.cardinality().shape())) {
          diagnostics.add(
              invalidModel("Unsupported writer cardinality " + field.cardinality().toText() + "."));
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
        DiagnosticCode.SCHEMA_WRITER_EMISSION_INVALID_MODEL, "writer-emission", message);
  }

  private GeneratedJavaSource emitRootWriter(BindingRootElement root, ModelIndex index) {
    BindingType rootType = Objects.requireNonNull(index.type(root.type().name()));
    BindingJavaName writerName = writerName(rootType.javaName());
    SourceState sourceState = new SourceState(root, rootType, writerName, index);
    return new GeneratedJavaSource(writerName, relativePath(writerName), sourceState.sourceText());
  }

  private BindingJavaName writerName(BindingJavaName modelName) {
    return new BindingJavaName(
        modelName.packageName() + ".xml", modelName.simpleName() + "XmlWriter");
  }

  private Path relativePath(BindingJavaName writerName) {
    return Path.of(writerName.packageName().replace('.', '/'), writerName.simpleName() + ".java");
  }

  private static final class SourceState {
    private static final SchemaQName XSI_NIL =
        new SchemaQName("http://www.w3.org/2001/XMLSchema-instance", "nil");
    private final BindingRootElement root;
    private final BindingType rootType;
    private final BindingJavaName writerName;
    private final ModelIndex index;
    private final LinkedHashMap<SchemaQName, String> nameConstants = new LinkedHashMap<>();
    private final Set<String> helperNames = new LinkedHashSet<>();

    private SourceState(
        BindingRootElement root,
        BindingType rootType,
        BindingJavaName writerName,
        ModelIndex index) {
      this.root = root;
      this.rootType = rootType;
      this.writerName = writerName;
      this.index = index;
    }

    private String sourceText() {
      collectNames(root.xmlName(), rootType, new LinkedHashSet<>());
      if (needsNillableSupport(rootType, new LinkedHashSet<>())) {
        nameConstant(XSI_NIL);
      }
      StringBuilder source = new StringBuilder();
      source.append("package ").append(writerName.packageName()).append(";\n\n");
      source
          .append("/** Generated XML writer for {@link ")
          .append(rootType.javaName().qualifiedName())
          .append("}. */\n");
      source.append("public final class ").append(writerName.simpleName()).append(" {\n");
      appendNameConstants(source);
      source.append('\n');
      source.append("  private ").append(writerName.simpleName()).append("() {}\n\n");
      appendPublicWrite(source);
      appendHelper(source, rootType);
      if (hasWildcard(rootType, new LinkedHashSet<>())) {
        appendFragmentHelpers(source);
      }
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

    private void appendPublicWrite(StringBuilder source) {
      source
          .append("  public static void write(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlOutput output,\n")
          .append("      ")
          .append(typeText(rootType))
          .append(" value)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlWriteException {\n")
          .append("    java.util.Objects.requireNonNull(output, \"output\");\n")
          .append("    java.util.Objects.requireNonNull(value, \"value\");\n")
          .append("    ")
          .append(helperName(rootType))
          .append("(output, ")
          .append(nameConstant(root.xmlName()))
          .append(", value);\n")
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
          .append("      io.github.mundanej.mxjb.runtime.XmlOutput output,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlName elementName,\n")
          .append("      ")
          .append(typeText(type))
          .append(" value)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlWriteException {\n")
          .append("    output.startElement(elementName);\n");
      for (BindingField field : attributes(type)) {
        appendFieldWrite(source, field);
      }
      for (BindingField field : anyAttributes(type)) {
        appendFieldWrite(source, field);
      }
      for (BindingField field : contentFields(type)) {
        appendFieldWrite(source, field);
      }
      source.append("    output.endElement(elementName);\n");
      source.append("  }\n");
      for (BindingField field : contentFields(type)) {
        BindingType nestedType = modelType(field);
        if (nestedType != null && !helperNames.contains(nestedType.javaName().qualifiedName())) {
          source.append('\n');
          appendHelper(source, nestedType);
        }
        if ("choice".equals(field.kind())) {
          for (BindingChoiceBranch branch : field.choice().branches()) {
            BindingType branchType = modelType(branch.type());
            if (branchType != null
                && !helperNames.contains(branchType.javaName().qualifiedName())) {
              source.append('\n');
              appendHelper(source, branchType);
            }
          }
        }
        if ("content".equals(field.kind())) {
          for (BindingContentBranch branch : field.content().branches()) {
            BindingType branchType = modelType(branch.type());
            if (branchType != null
                && !helperNames.contains(branchType.javaName().qualifiedName())) {
              source.append('\n');
              appendHelper(source, branchType);
            }
          }
        }
      }
    }

    private void appendFieldWrite(StringBuilder source, BindingField field) {
      String valueExpression = "value." + field.javaName() + "()";
      String shape = field.cardinality().shape();
      if (field.semantics().nillable()) {
        source.append("    if (").append(valueExpression).append(".isPresent()) {\n");
        appendSingleValueWrite(source, field, valueExpression + ".orElseThrow()", "      ");
        source.append("    } else {\n");
        appendNilElementWrite(source, field, "      ");
        source.append("    }\n");
      } else if ("optional".equals(shape)) {
        source.append("    if (").append(valueExpression).append(".isPresent()) {\n");
        source
            .append("      ")
            .append(localType(field))
            .append(' ')
            .append(field.javaName())
            .append("Value = ")
            .append(valueExpression)
            .append(".orElseThrow();\n");
        appendSingleValueWrite(source, field, field.javaName() + "Value", "      ");
        source.append("    }\n");
      } else if ("list".equals(shape)) {
        source
            .append("    for (")
            .append(localType(field))
            .append(' ')
            .append(field.javaName())
            .append("Value : ")
            .append(valueExpression)
            .append(") {\n");
        appendSingleValueWrite(source, field, field.javaName() + "Value", "      ");
        source.append("    }\n");
      } else {
        appendSingleValueWrite(source, field, valueExpression, "    ");
      }
    }

    private void appendNilElementWrite(StringBuilder source, BindingField field, String indent) {
      String name = nameConstant(field.xmlName());
      source.append(indent).append("output.startElement(").append(name).append(");\n");
      source
          .append(indent)
          .append("output.attribute(")
          .append(nameConstant(XSI_NIL))
          .append(", \"true\");\n");
      source.append(indent).append("output.endElement(").append(name).append(");\n");
    }

    private void appendSingleValueWrite(
        StringBuilder source, BindingField field, String valueExpression, String indent) {
      if ("choice".equals(field.kind())) {
        appendChoiceValueWrite(source, field, valueExpression, indent);
        return;
      }
      if ("content".equals(field.kind())) {
        appendContentValueWrite(source, field, valueExpression, indent);
        return;
      }
      if ("wildcard".equals(field.kind())) {
        source
            .append(indent)
            .append("writeFragment(output, ")
            .append(valueExpression)
            .append(");\n");
        return;
      }
      if ("anyAttribute".equals(field.kind())) {
        source
            .append(indent)
            .append("output.attribute(")
            .append(valueExpression)
            .append(".name(), ")
            .append(valueExpression)
            .append(".value());\n");
        return;
      }
      String name = nameConstant(field.xmlName());
      if ("attribute".equals(field.kind())) {
        source
            .append(indent)
            .append("output.attribute(")
            .append(name)
            .append(", ")
            .append(scalarText(field, valueExpression))
            .append(");\n");
        return;
      }
      BindingType nestedType = modelType(field);
      if (nestedType != null) {
        source
            .append(indent)
            .append(helperName(nestedType))
            .append("(output, ")
            .append(name)
            .append(", ")
            .append(valueExpression)
            .append(");\n");
        return;
      }
      source.append(indent).append("output.startElement(").append(name).append(");\n");
      source
          .append(indent)
          .append("output.text(")
          .append(scalarText(field, valueExpression))
          .append(");\n");
      source.append(indent).append("output.endElement(").append(name).append(");\n");
    }

    private void appendContentValueWrite(
        StringBuilder source, BindingField field, String valueExpression, String indent) {
      boolean first = true;
      for (BindingContentBranch branch : field.content().branches()) {
        source
            .append(indent)
            .append(first ? "if" : "} else if")
            .append(" (")
            .append(valueExpression)
            .append(" instanceof ")
            .append(branch.branchJavaName().qualifiedName())
            .append(" branch) {\n");
        if ("text".equals(branch.kind())) {
          source.append(indent).append("  output.text(branch.value());\n");
        } else if ("wildcard".equals(branch.kind())) {
          source.append(indent).append("  writeFragment(output, branch.value());\n");
        } else {
          appendContentElementWrite(source, branch, "branch.value()", indent + "  ");
        }
        first = false;
      }
      source.append(indent).append("}\n");
    }

    private void appendContentElementWrite(
        StringBuilder source, BindingContentBranch branch, String valueExpression, String indent) {
      String name = nameConstant(branch.xmlName());
      BindingType nestedType = modelType(branch.type());
      if (nestedType != null) {
        source
            .append(indent)
            .append(helperName(nestedType))
            .append("(output, ")
            .append(name)
            .append(", ")
            .append(valueExpression)
            .append(");\n");
        return;
      }
      source.append(indent).append("output.startElement(").append(name).append(");\n");
      source
          .append(indent)
          .append("output.text(")
          .append(scalarText(branch.type(), valueExpression))
          .append(");\n");
      source.append(indent).append("output.endElement(").append(name).append(");\n");
    }

    private String scalarText(BindingField field, String valueExpression) {
      return scalarText(field.type(), valueExpression);
    }

    private String scalarText(BindingTypeReference reference, String valueExpression) {
      if ("scalar".equals(reference.kind()) && "string".equals(reference.name())) {
        return valueExpression;
      }
      if ("list".equals(reference.kind())) {
        return "io.github.mundanej.mxjb.runtime.XmlDatatypes.formatList(\""
            + escape(reference.itemType().name())
            + "\", "
            + valueExpression
            + ", output)";
      }
      if ("union".equals(reference.kind())) {
        return valueExpression;
      }
      return "io.github.mundanej.mxjb.runtime.XmlDatatypes.format(\""
          + escape(reference.name())
          + "\", "
          + valueExpression
          + ", output)";
    }

    private void appendFragmentHelpers(StringBuilder source) {
      source
          .append("  private static void writeFragment(\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlOutput output,\n")
          .append("      io.github.mundanej.mxjb.runtime.XmlFragment fragment)\n")
          .append("      throws io.github.mundanej.mxjb.runtime.XmlWriteException {\n")
          .append("    java.util.Objects.requireNonNull(fragment, \"fragment\");\n")
          .append("    output.startElement(fragment.name());\n")
          .append(
              "    for (io.github.mundanej.mxjb.runtime.XmlAttribute attribute : fragment.attributes()) {\n")
          .append("      output.attribute(attribute.name(), attribute.value());\n")
          .append("    }\n")
          .append(
              "    for (io.github.mundanej.mxjb.runtime.XmlFragmentContent content : fragment.content()) {\n")
          .append(
              "      if (content instanceof io.github.mundanej.mxjb.runtime.XmlFragmentText text) {\n")
          .append("        output.text(text.text());\n")
          .append(
              "      } else if (content instanceof io.github.mundanej.mxjb.runtime.XmlFragmentElement element) {\n")
          .append("        writeFragment(output, element.fragment());\n")
          .append("      }\n")
          .append("    }\n")
          .append("    output.endElement(fragment.name());\n")
          .append("  }\n\n");
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

    private boolean hasWildcard(BindingType type, Set<String> visited) {
      if (!visited.add(type.javaName().qualifiedName())) {
        return false;
      }
      if (type.fields().stream().anyMatch(field -> "wildcard".equals(field.kind()))) {
        return true;
      }
      for (BindingField field :
          type.fields().stream().filter(value -> "content".equals(value.kind())).toList()) {
        for (BindingContentBranch branch : field.content().branches()) {
          if ("wildcard".equals(branch.kind())) {
            return true;
          }
          BindingType nestedType = modelType(branch.type());
          if (nestedType != null && hasWildcard(nestedType, visited)) {
            return true;
          }
        }
      }
      for (BindingField field : elements(type)) {
        BindingType nestedType = modelType(field);
        if (nestedType != null && hasWildcard(nestedType, visited)) {
          return true;
        }
      }
      for (BindingField field :
          type.fields().stream().filter(value -> "choice".equals(value.kind())).toList()) {
        for (BindingChoiceBranch branch : field.choice().branches()) {
          BindingType nestedType = modelType(branch.type());
          if (nestedType != null && hasWildcard(nestedType, visited)) {
            return true;
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

    private void appendChoiceValueWrite(
        StringBuilder source, BindingField field, String valueExpression, String indent) {
      for (int indexValue = 0; indexValue < field.choice().branches().size(); indexValue++) {
        BindingChoiceBranch branch = field.choice().branches().get(indexValue);
        source
            .append(indent)
            .append(indexValue == 0 ? "if (" : "} else if (")
            .append(valueExpression)
            .append(" instanceof ")
            .append(branch.branchJavaName().qualifiedName())
            .append(" branch) {\n");
        appendBranchSingleValueWrite(source, branch, "branch.value()", indent + "  ");
      }
      source
          .append(indent)
          .append("} else {\n")
          .append(indent)
          .append("  throw new io.github.mundanej.mxjb.runtime.XmlWriteException(\n")
          .append(indent)
          .append("      new io.github.mundanej.mxjb.runtime.XmlDiagnostic(\n")
          .append(indent)
          .append("          io.github.mundanej.mxjb.runtime.XmlDiagnosticSeverity.ERROR,\n")
          .append(indent)
          .append("          \"MXJB-GW-001\",\n")
          .append(indent)
          .append("          \"Unsupported XML ")
          .append(escape(field.choice().modelKind()))
          .append(" branch.\",\n")
          .append(indent)
          .append("          io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN));\n")
          .append(indent)
          .append("}\n");
    }

    private void appendBranchSingleValueWrite(
        StringBuilder source, BindingChoiceBranch branch, String valueExpression, String indent) {
      String name = nameConstant(branch.xmlName());
      BindingType nestedType = modelType(branch.type());
      if (nestedType != null) {
        source
            .append(indent)
            .append(helperName(nestedType))
            .append("(output, ")
            .append(name)
            .append(", ")
            .append(valueExpression)
            .append(");\n");
        return;
      }
      source.append(indent).append("output.startElement(").append(name).append(");\n");
      source
          .append(indent)
          .append("output.text(")
          .append(scalarText(branch.type(), valueExpression))
          .append(");\n");
      source.append(indent).append("output.endElement(").append(name).append(");\n");
    }

    private String nameConstant(SchemaQName name) {
      return nameConstants.computeIfAbsent(name, ignored -> "NAME_" + (nameConstants.size() + 1));
    }

    private String helperName(BindingType type) {
      return "write" + type.javaName().simpleName();
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
