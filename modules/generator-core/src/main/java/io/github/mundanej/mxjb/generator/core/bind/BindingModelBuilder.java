package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.schema.SchemaCardinality;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrAttribute;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrChoice;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrComplexType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrElement;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrModel;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrParticle;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrResult;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSequence;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSimpleRestriction;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSimpleType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrTypeReference;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Builds the deterministic binding model from normalized schema IR. */
public final class BindingModelBuilder {
  private static final Set<String> SUPPORTED_BUILT_INS =
      Set.of("string", "boolean", "int", "integer", "long", "decimal");

  public BindingResult build(SchemaIrResult irResult) {
    return build(irResult, BindingConfiguration.defaults());
  }

  public BindingResult build(SchemaIrResult irResult, BindingConfiguration configuration) {
    if (irResult.hasErrors()) {
      return BindingResult.empty(irResult.diagnostics());
    }

    BuildState state = new BuildState(configuration);
    state.validateConfiguration();
    if (!state.diagnostics.isEmpty()) {
      return BindingResult.empty(state.sortedDiagnostics());
    }

    BindingModel model = state.buildModel(irResult.model());
    if (!state.diagnostics.isEmpty()) {
      return BindingResult.empty(state.sortedDiagnostics());
    }
    return new BindingResult(model, List.of());
  }

  private static final class BuildState {
    private final BindingConfiguration configuration;
    private final Map<SchemaQName, SchemaIrElement> globalElements = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrAttribute> globalAttributes = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrComplexType> complexTypes = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrSimpleType> simpleTypes = new LinkedHashMap<>();
    private final Map<SchemaQName, BindingJavaName> complexTypeNames = new LinkedHashMap<>();
    private final IdentityHashMap<SchemaIrComplexType, BindingJavaName> inlineComplexTypeNames =
        new IdentityHashMap<>();
    private final Map<String, Set<String>> usedTypeNamesByPackage = new HashMap<>();
    private final List<SchemaDiagnostic> diagnostics = new ArrayList<>();

    private BuildState(BindingConfiguration configuration) {
      this.configuration = configuration;
    }

    private void validateConfiguration() {
      if (!JavaNames.isPackageName(configuration.defaultPackage())) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_CONFIGURATION,
            "binding",
            "Invalid default package " + configuration.defaultPackage() + ".");
      }
      for (String packageName : configuration.namespacePackages().values()) {
        if (!JavaNames.isPackageName(packageName)) {
          diagnostic(
              DiagnosticCode.SCHEMA_BINDING_INVALID_CONFIGURATION,
              "binding",
              "Invalid namespace package " + packageName + ".");
        }
      }
    }

    private BindingModel buildModel(SchemaIrModel model) {
      index(model);
      List<BindingType> boundTypes = bindComplexTypes(model);
      List<BindingRootElement> roots = bindRootElements(model.elements());
      return new BindingModel(roots, boundTypes);
    }

    private void index(SchemaIrModel model) {
      for (SchemaIrElement element : model.elements()) {
        globalElements.put(element.name(), element);
      }
      for (SchemaIrAttribute attribute : model.attributes()) {
        globalAttributes.put(attribute.name(), attribute);
      }
      for (SchemaIrComplexType complexType : model.complexTypes()) {
        complexTypes.put(complexType.name(), complexType);
      }
      for (SchemaIrSimpleType simpleType : model.simpleTypes()) {
        simpleTypes.put(simpleType.name(), simpleType);
      }
      for (SchemaIrComplexType complexType : model.complexTypes()) {
        complexTypeNames.put(complexType.name(), javaName(complexType.name()));
      }
      for (SchemaIrComplexType complexType : model.complexTypes()) {
        allocateInlineTypeNames(complexType);
      }
      allocateInlineTypeNames(model.elements());
    }

    private void allocateInlineTypeNames(SchemaIrComplexType complexType) {
      for (SchemaIrSequence sequence : complexType.sequences()) {
        allocateInlineTypeNames(sequence.elements());
      }
    }

    private void allocateInlineTypeNames(List<SchemaIrElement> elements) {
      for (SchemaIrElement element : elements) {
        SchemaIrComplexType inlineComplexType = element.inlineComplexType();
        if (inlineComplexType == null) {
          continue;
        }
        inlineComplexTypeNames.put(inlineComplexType, javaName(element.name()));
        for (SchemaIrSequence sequence : inlineComplexType.sequences()) {
          allocateInlineTypeNames(sequence.elements());
        }
      }
    }

    private List<BindingType> bindComplexTypes(SchemaIrModel model) {
      List<BindingType> boundTypes = new ArrayList<>();
      for (SchemaIrComplexType complexType : model.complexTypes()) {
        boundTypes.add(bindType(complexType, complexTypeNames.get(complexType.name())));
      }
      for (SchemaIrComplexType complexType : model.complexTypes()) {
        addInlineTypes(boundTypes, complexType);
      }
      addInlineTypes(boundTypes, model.elements());
      return boundTypes;
    }

    private void addInlineTypes(List<BindingType> boundTypes, SchemaIrComplexType complexType) {
      for (SchemaIrSequence sequence : complexType.sequences()) {
        addInlineTypes(boundTypes, sequence.elements());
      }
    }

    private void addInlineTypes(List<BindingType> boundTypes, List<SchemaIrElement> elements) {
      for (SchemaIrElement element : elements) {
        SchemaIrComplexType inlineComplexType = element.inlineComplexType();
        if (inlineComplexType == null) {
          continue;
        }
        boundTypes.add(bindType(inlineComplexType, inlineComplexTypeNames.get(inlineComplexType)));
        for (SchemaIrSequence sequence : inlineComplexType.sequences()) {
          addInlineTypes(boundTypes, sequence.elements());
        }
      }
    }

    private BindingType bindType(SchemaIrComplexType complexType, BindingJavaName javaName) {
      List<BindingField> fields = new ArrayList<>();
      List<String> validationRules = new ArrayList<>();
      Set<String> usedFieldNames = new HashSet<>();
      int order = 1;
      for (SchemaIrSequence sequence : complexType.sequences()) {
        for (SchemaIrParticle particle : sequence.particles()) {
          if (particle instanceof SchemaIrElement element) {
            fields.add(bindElementField(element, usedFieldNames, order));
            validationRules.add(
                "element "
                    + fields.get(fields.size() - 1).javaName()
                    + " "
                    + fields.get(fields.size() - 1).cardinality().toText());
          } else if (particle instanceof SchemaIrChoice choice) {
            BindingField field =
                bindChoiceField(complexType, javaName, choice, usedFieldNames, order);
            fields.add(field);
            validationRules.add("choice " + field.javaName() + " " + field.cardinality().toText());
          } else {
            diagnostic(
                DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
                "binding",
                "Unsupported schema particle in binding model.");
          }
          order++;
        }
      }
      for (SchemaIrAttribute attribute : complexType.attributes()) {
        SchemaIrAttribute declaration =
            attribute.reference() ? globalAttributes.get(attribute.name()) : attribute;
        SchemaIrTypeReference type = declaration == null ? attribute.type() : declaration.type();
        BindingTypeReference bindingType = bindTypeReference(type, null, attribute.name());
        BindingCardinality cardinality = attributeCardinality(attribute);
        String fieldName = JavaNames.uniqueFieldName(attribute.name(), usedFieldNames);
        boolean required = "required".equals(attribute.use());
        fields.add(
            new BindingField(
                "attribute", attribute.name(), fieldName, bindingType, cardinality, 0, required));
        validationRules.add("attribute " + fieldName + " use=" + attribute.use());
      }
      return new BindingType(
          javaName,
          complexType.name(),
          "record",
          fields,
          new BindingValidationPlan(validationRules));
    }

    private BindingField bindElementField(
        SchemaIrElement element, Set<String> usedFieldNames, int order) {
      SchemaIrElement declaration =
          element.reference() ? globalElements.get(element.name()) : element;
      SchemaIrTypeReference type = declaration == null ? element.type() : declaration.type();
      BindingTypeReference bindingType = bindTypeReference(type, declaration, element.name());
      BindingCardinality cardinality = BindingCardinality.from(element.cardinality());
      String fieldName = JavaNames.uniqueFieldName(element.name(), usedFieldNames);
      boolean required = cardinality.minOccurs() > 0;
      return new BindingField(
          "element", element.name(), fieldName, bindingType, cardinality, order, required);
    }

    private BindingField bindChoiceField(
        SchemaIrComplexType complexType,
        BindingJavaName ownerName,
        SchemaIrChoice choice,
        Set<String> usedFieldNames,
        int order) {
      String packageName = ownerName.packageName();
      String choiceSimpleName = uniqueTypeName(packageName, ownerName.simpleName() + "Choice");
      BindingJavaName choiceName = new BindingJavaName(packageName, choiceSimpleName);
      Set<String> branchNames = new HashSet<>();
      List<BindingChoiceBranch> branches = new ArrayList<>();
      for (SchemaIrElement branch : choice.branches()) {
        SchemaIrElement declaration =
            branch.reference() ? globalElements.get(branch.name()) : branch;
        SchemaIrTypeReference type = declaration == null ? branch.type() : declaration.type();
        BindingTypeReference bindingType = bindTypeReference(type, declaration, branch.name());
        String branchFieldName = JavaNames.uniqueFieldName(branch.name(), branchNames);
        String branchSimpleName =
            uniqueTypeName(packageName, JavaNames.typeName(branch.name()) + "Choice");
        branches.add(
            new BindingChoiceBranch(
                branch.name(),
                branchFieldName,
                bindingType,
                new BindingJavaName(packageName, branchSimpleName)));
      }
      BindingChoice bindingChoice = new BindingChoice(choiceName, branches);
      BindingCardinality cardinality = BindingCardinality.from(choice.cardinality());
      String baseFieldName = JavaNames.fieldNameFromTypeName(choiceSimpleName);
      String fieldName = JavaNames.unique(baseFieldName, usedFieldNames);
      boolean required = cardinality.minOccurs() > 0;
      return new BindingField(
          "choice",
          new SchemaQName(
              complexType.name() == null ? "" : complexType.name().namespace(), fieldName),
          fieldName,
          BindingTypeReference.choice(choiceName),
          cardinality,
          order,
          required,
          bindingChoice);
    }

    private BindingRootElement bindRootElement(SchemaIrElement element) {
      BindingTypeReference type = bindTypeReference(element.type(), element, element.name());
      return new BindingRootElement(
          element.name(), type, BindingCardinality.from(element.cardinality()));
    }

    private List<BindingRootElement> bindRootElements(List<SchemaIrElement> elements) {
      List<BindingRootElement> roots = new ArrayList<>();
      for (SchemaIrElement element : elements) {
        roots.add(bindRootElement(element));
      }
      return roots;
    }

    private BindingTypeReference bindTypeReference(
        SchemaIrTypeReference reference, SchemaIrElement owner, SchemaQName fallbackName) {
      if (reference.anonymous()) {
        if (owner != null && owner.inlineComplexType() != null) {
          BindingJavaName inlineName = inlineComplexTypeNames.get(owner.inlineComplexType());
          if (inlineName != null) {
            return BindingTypeReference.model(inlineName);
          }
        }
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
            "binding",
            "Anonymous type for " + fallbackName.toText() + " is missing binding metadata.");
        return BindingTypeReference.scalar("unsupported");
      }
      SchemaQName name = reference.name();
      if (name.isXmlSchemaBuiltIn()) {
        if (!SUPPORTED_BUILT_INS.contains(name.localName())) {
          diagnostic(
              DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
              "binding",
              "Unsupported XML Schema built-in type " + name.toText() + ".");
          return BindingTypeReference.scalar(name.localName());
        }
        return BindingTypeReference.scalar(name.localName());
      }
      BindingJavaName complexTypeName = complexTypeNames.get(name);
      if (complexTypeName != null) {
        return BindingTypeReference.model(complexTypeName);
      }
      SchemaIrSimpleType simpleType = simpleTypes.get(name);
      if (simpleType != null && simpleType.restriction() != null) {
        SchemaIrSimpleRestriction restriction = simpleType.restriction();
        BindingSimpleRestriction bindingRestriction =
            new BindingSimpleRestriction(
                restriction.base().localName(),
                restriction.enumerations(),
                restriction.length(),
                restriction.minLength(),
                restriction.maxLength(),
                restriction.minInclusive(),
                restriction.maxInclusive(),
                restriction.patterns());
        return BindingTypeReference.scalar(restriction.base().localName(), bindingRestriction);
      }
      diagnostic(
          DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
          "binding",
          "Unsupported schema type " + name.toText() + ".");
      return BindingTypeReference.scalar("unsupported");
    }

    private BindingCardinality attributeCardinality(SchemaIrAttribute attribute) {
      if ("required".equals(attribute.use())) {
        return BindingCardinality.from(SchemaCardinality.ONE);
      }
      if (!"optional".equals(attribute.use())) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
            "binding",
            "Unsupported attribute use " + attribute.use() + ".");
      }
      return new BindingCardinality("optional", 0, "1");
    }

    private BindingJavaName javaName(SchemaQName schemaName) {
      String packageName = packageName(schemaName.namespace());
      Set<String> usedTypeNames =
          usedTypeNamesByPackage.computeIfAbsent(packageName, ignored -> new HashSet<>());
      String simpleName = JavaNames.uniqueTypeName(schemaName, usedTypeNames);
      return new BindingJavaName(packageName, simpleName);
    }

    private String uniqueTypeName(String packageName, String baseName) {
      Set<String> usedTypeNames =
          usedTypeNamesByPackage.computeIfAbsent(packageName, ignored -> new HashSet<>());
      return JavaNames.unique(JavaNames.sanitizeIdentifier(baseName, true), usedTypeNames);
    }

    private String packageName(String namespace) {
      String override = configuration.namespacePackages().get(namespace);
      if (override != null) {
        return override;
      }
      if (namespace == null || namespace.isBlank()) {
        return configuration.defaultPackage();
      }
      if (namespace.startsWith("urn:")) {
        List<String> tokens = JavaNames.packageTokens(namespace.substring("urn:".length()));
        return tokens.isEmpty()
            ? configuration.defaultPackage()
            : configuration.defaultPackage() + "." + String.join(".", tokens);
      }
      try {
        URI uri = new URI(namespace);
        if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
          List<String> tokens = new ArrayList<>();
          String host = uri.getHost();
          if (host != null) {
            List<String> parts = JavaNames.splitOnDot(host);
            for (int index = parts.size() - 1; index >= 0; index--) {
              tokens.addAll(JavaNames.packageTokens(parts.get(index)));
            }
          }
          tokens.addAll(JavaNames.packageTokens(uri.getPath()));
          return tokens.isEmpty() ? configuration.defaultPackage() : String.join(".", tokens);
        }
      } catch (URISyntaxException ignored) {
        return configuration.defaultPackage();
      }
      return configuration.defaultPackage();
    }

    private void diagnostic(DiagnosticCode code, String resource, String message) {
      diagnostics.add(new SchemaDiagnostic(code, resource, message));
    }

    private List<SchemaDiagnostic> sortedDiagnostics() {
      return diagnostics.stream()
          .sorted(
              Comparator.comparing(SchemaDiagnostic::resource)
                  .thenComparing(diagnostic -> diagnostic.code().name())
                  .thenComparing(SchemaDiagnostic::message))
          .toList();
    }
  }

  private static final class JavaNames {
    private static final Set<String> KEYWORDS =
        Set.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "record",
            "return",
            "sealed",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "var",
            "void",
            "volatile",
            "while",
            "yield");

    private static String uniqueTypeName(SchemaQName schemaName, Set<String> used) {
      return unique(typeName(schemaName), used);
    }

    private static String uniqueFieldName(SchemaQName schemaName, Set<String> used) {
      return unique(fieldName(schemaName), used);
    }

    private static String typeName(SchemaQName schemaName) {
      return sanitizeIdentifier(upperCamel(tokens(schemaName.localName())), true);
    }

    private static String fieldName(SchemaQName schemaName) {
      return sanitizeIdentifier(lowerCamel(tokens(schemaName.localName())), false);
    }

    private static String fieldNameFromTypeName(String typeName) {
      if (typeName == null || typeName.isBlank()) {
        return "value";
      }
      return sanitizeIdentifier(
          typeName.substring(0, 1).toLowerCase(Locale.ROOT) + typeName.substring(1), false);
    }

    private static String unique(String base, Set<String> used) {
      String candidate = base;
      int suffix = 2;
      while (!used.add(candidate)) {
        candidate = base + suffix;
        suffix++;
      }
      return candidate;
    }

    private static boolean isPackageName(String value) {
      if (value == null || value.isBlank()) {
        return false;
      }
      for (String part : splitOnDot(value)) {
        if (part.isEmpty() || !part.equals(sanitizeIdentifier(part, false))) {
          return false;
        }
      }
      return true;
    }

    private static List<String> packageTokens(String value) {
      return tokens(value).stream()
          .map(token -> sanitizeIdentifier(token.toLowerCase(Locale.ROOT), false))
          .filter(token -> !token.isBlank())
          .toList();
    }

    private static List<String> tokens(String value) {
      List<String> result = new ArrayList<>();
      StringBuilder token = new StringBuilder();
      for (int index = 0; index < value.length(); index++) {
        char character = value.charAt(index);
        if (Character.isLetterOrDigit(character)) {
          token.append(character);
        } else if (!token.isEmpty()) {
          result.add(token.toString());
          token.setLength(0);
        }
      }
      if (!token.isEmpty()) {
        result.add(token.toString());
      }
      return result;
    }

    private static List<String> splitOnDot(String value) {
      List<String> result = new ArrayList<>();
      int start = 0;
      for (int index = 0; index < value.length(); index++) {
        if (value.charAt(index) == '.') {
          result.add(value.substring(start, index));
          start = index + 1;
        }
      }
      result.add(value.substring(start));
      return result;
    }

    private static String upperCamel(List<String> tokens) {
      if (tokens.isEmpty()) {
        return "Value";
      }
      return tokens.stream()
          .map(JavaNames::capitalize)
          .collect(java.util.stream.Collectors.joining());
    }

    private static String lowerCamel(List<String> tokens) {
      String upper = upperCamel(tokens);
      return Character.toLowerCase(upper.charAt(0)) + upper.substring(1);
    }

    private static String capitalize(String value) {
      String lower = value.toLowerCase(Locale.ROOT);
      return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String sanitizeIdentifier(String value, boolean typeName) {
      if (value == null || value.isBlank()) {
        value = typeName ? "Value" : "value";
      }
      StringBuilder builder = new StringBuilder();
      for (int index = 0; index < value.length(); index++) {
        char character = value.charAt(index);
        builder.append(Character.isLetterOrDigit(character) || character == '_' ? character : '_');
      }
      String sanitized = builder.toString();
      if (!Character.isJavaIdentifierStart(sanitized.charAt(0))) {
        sanitized = "_" + sanitized;
      }
      if (KEYWORDS.contains(sanitized)) {
        sanitized = "_" + sanitized;
      }
      return sanitized;
    }
  }
}
