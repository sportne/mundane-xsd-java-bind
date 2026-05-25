package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.schema.SchemaCardinality;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrAll;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrAnyAttribute;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrAttribute;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrChoice;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrComplexType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrElement;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrGroup;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrModel;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrParticle;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrResult;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSequence;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSimpleContent;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSimpleRestriction;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSimpleType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSubstitutionGroup;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrTypeReference;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrValueSemantics;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcard;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds the deterministic binding model from normalized schema IR. */
public final class BindingModelBuilder {
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
    private final Map<SchemaQName, SchemaIrElement> globalElements = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrAttribute> globalAttributes = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrComplexType> complexTypes = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrSimpleType> simpleTypes = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrSubstitutionGroup> substitutionGroups =
        new LinkedHashMap<>();
    private final Map<SchemaQName, BindingJavaName> complexTypeNames = new LinkedHashMap<>();
    private final IdentityHashMap<SchemaIrComplexType, BindingJavaName> inlineComplexTypeNames =
        new IdentityHashMap<>();
    private final List<SchemaDiagnostic> diagnostics = new ArrayList<>();
    private final BindingNameAllocator names;
    private final BindingContentPlanner contentPlanner;

    private BuildState(BindingConfiguration configuration) {
      this.names = new BindingNameAllocator(configuration);
      this.contentPlanner =
          new BindingContentPlanner(
              names,
              element -> globalElements.get(element.name()),
              this::bindTypeReference,
              this::knownWildcardElements,
              this::unsupportedGroupedContent);
    }

    private void validateConfiguration() {
      diagnostics.addAll(names.validateConfiguration());
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
      for (SchemaIrSubstitutionGroup substitutionGroup : model.substitutionGroups()) {
        substitutionGroups.put(substitutionGroup.head(), substitutionGroup);
      }
      for (SchemaIrComplexType complexType : model.complexTypes()) {
        complexTypeNames.put(complexType.name(), names.javaName(complexType.name()));
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
        inlineComplexTypeNames.put(inlineComplexType, names.javaName(element.name()));
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
      if (complexType.simpleContent() != null) {
        BindingField field = bindSimpleContentField(complexType.simpleContent(), usedFieldNames);
        fields.add(field);
        validationRules.add("simpleContent " + field.javaName());
      } else if (complexType.mixed()) {
        BindingField field =
            contentPlanner.mixedContentField(complexType, javaName, usedFieldNames, order);
        fields.add(field);
        validationRules.add("content " + field.javaName() + " " + field.cardinality().toText());
      } else {
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
                  choice.wildcardBranches().isEmpty()
                      ? bindChoiceField(complexType, javaName, choice, usedFieldNames, order)
                      : contentPlanner.groupedContentField(
                          complexType,
                          javaName,
                          "choice",
                          choice.cardinality(),
                          choice.branches(),
                          usedFieldNames,
                          order);
              fields.add(field);
              validationRules.add(
                  field.kind() + " " + field.javaName() + " " + field.cardinality().toText());
            } else if (particle instanceof SchemaIrWildcard wildcard) {
              BindingField field = bindWildcardField(wildcard, usedFieldNames, order);
              fields.add(field);
              validationRules.add(
                  "wildcard " + field.javaName() + " " + field.cardinality().toText());
            } else if (particle instanceof SchemaIrAll all) {
              if (requiresGroupedAll(all)) {
                BindingField field =
                    contentPlanner.groupedContentField(
                        complexType,
                        javaName,
                        "all",
                        all.cardinality(),
                        all.elements().stream().map(SchemaIrParticle.class::cast).toList(),
                        usedFieldNames,
                        order);
                fields.add(field);
                validationRules.add(
                    "content " + field.javaName() + " " + field.cardinality().toText());
              } else {
                for (SchemaIrElement element : all.elements()) {
                  fields.add(bindElementField(element, usedFieldNames, order));
                  validationRules.add(
                      "all-element "
                          + fields.get(fields.size() - 1).javaName()
                          + " "
                          + fields.get(fields.size() - 1).cardinality().toText());
                }
              }
            } else if (particle instanceof SchemaIrGroup group) {
              BindingField field =
                  contentPlanner.groupedContentField(
                      complexType,
                      javaName,
                      group.modelKind(),
                      group.cardinality(),
                      group.particles(),
                      usedFieldNames,
                      order);
              fields.add(field);
              validationRules.add(
                  "content " + field.javaName() + " " + field.cardinality().toText());
            } else {
              diagnostic(
                  DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
                  "binding",
                  "Unsupported schema particle in binding model.");
            }
            order++;
          }
        }
      }
      List<SchemaQName> prohibitedAttributes = new ArrayList<>();
      for (SchemaIrAttribute attribute : complexType.attributes()) {
        if ("prohibited".equals(attribute.use())) {
          prohibitedAttributes.add(attribute.name());
          validationRules.add("prohibited-attribute " + attribute.name().toText());
          continue;
        }
        SchemaIrAttribute declaration =
            attribute.reference() ? globalAttributes.get(attribute.name()) : attribute;
        SchemaIrTypeReference type = declaration == null ? attribute.type() : declaration.type();
        BindingValueSemantics semantics =
            bindingSemantics(declaration == null ? attribute.semantics() : declaration.semantics());
        BindingTypeReference bindingType = bindTypeReference(type, null, attribute.name());
        BindingCardinality cardinality = attributeCardinality(attribute);
        cardinality = effectiveAttributeCardinality(cardinality, semantics);
        validateListSimpleTypeCardinality(bindingType, cardinality, attribute.name());
        validateValueSemantics("attribute", attribute.name(), bindingType, cardinality, semantics);
        String fieldName = names.uniqueFieldName(attribute.name(), usedFieldNames);
        boolean required = "required".equals(attribute.use());
        fields.add(
            new BindingField(
                "attribute",
                attribute.name(),
                fieldName,
                bindingType,
                cardinality,
                0,
                required,
                semantics));
        validationRules.add("attribute " + fieldName + " use=" + attribute.use());
      }
      if (complexType.anyAttribute() != null) {
        BindingField field =
            bindAnyAttributeField(complexType.anyAttribute(), prohibitedAttributes, usedFieldNames);
        fields.add(field);
        validationRules.add("anyAttribute " + field.javaName());
      }
      return new BindingType(
          javaName,
          complexType.name(),
          "record",
          fields,
          new BindingValidationPlan(validationRules));
    }

    private boolean requiresGroupedAll(SchemaIrAll all) {
      return all.cardinality().minOccurs() == 0
          && all.elements().stream().anyMatch(element -> element.cardinality().minOccurs() > 0);
    }

    private BindingField bindSimpleContentField(
        SchemaIrSimpleContent simpleContent, Set<String> usedFieldNames) {
      BindingTypeReference type =
          simpleContent.restriction() == null
              ? bindTypeReference(simpleContent.valueType(), null, new SchemaQName("", "value"))
              : bindRestrictedScalar(simpleContent.restriction());
      return new BindingField(
          "simpleContent",
          new SchemaQName("", "#text"),
          names.unique("value", usedFieldNames),
          type,
          new BindingCardinality("required", 1, "1"),
          0,
          true);
    }

    private BindingField bindAnyAttributeField(
        SchemaIrAnyAttribute anyAttribute,
        List<SchemaQName> prohibitedAttributes,
        Set<String> usedFieldNames) {
      String fieldName = names.unique("wildcardAttributes", usedFieldNames);
      return new BindingField(
          "anyAttribute",
          new SchemaQName("", "@*"),
          fieldName,
          BindingTypeReference.xmlAttribute(),
          new BindingCardinality("list", 0, "unbounded"),
          0,
          false,
          new BindingWildcard(
              anyAttribute.namespaceConstraint(),
              anyAttribute.processContents(),
              prohibitedAttributes,
              List.of(),
              knownWildcardAttributes(anyAttribute.namespaceConstraint(), prohibitedAttributes)));
    }

    private BindingField bindElementField(
        SchemaIrElement element, Set<String> usedFieldNames, int order) {
      if (element.reference() && substitutionGroups.containsKey(element.name())) {
        return bindSubstitutionField(
            element, substitutionGroups.get(element.name()), usedFieldNames, order);
      }
      SchemaIrElement declaration =
          element.reference() ? globalElements.get(element.name()) : element;
      SchemaIrTypeReference type = declaration == null ? element.type() : declaration.type();
      BindingValueSemantics semantics =
          bindingSemantics(declaration == null ? element.semantics() : declaration.semantics());
      BindingField dynamicField =
          bindXsiTypeFieldIfNeeded(element, declaration, type, usedFieldNames, order);
      if (dynamicField != null) {
        return dynamicField;
      }
      BindingTypeReference bindingType = bindTypeReference(type, declaration, element.name());
      BindingCardinality cardinality = BindingCardinality.from(element.cardinality());
      validateListSimpleTypeCardinality(bindingType, cardinality, element.name());
      validateValueSemantics("element", element.name(), bindingType, cardinality, semantics);
      String fieldName = names.uniqueFieldName(element.name(), usedFieldNames);
      boolean required = cardinality.minOccurs() > 0;
      return new BindingField(
          "element",
          element.name(),
          fieldName,
          bindingType,
          cardinality,
          order,
          required,
          semantics);
    }

    private BindingField bindXsiTypeFieldIfNeeded(
        SchemaIrElement element,
        SchemaIrElement declaration,
        SchemaIrTypeReference type,
        Set<String> usedFieldNames,
        int order) {
      if (type.anonymous() || type.name().isXmlSchemaBuiltIn()) {
        return null;
      }
      SchemaIrComplexType declaredType = complexTypes.get(type.name());
      if (declaredType == null) {
        return null;
      }
      List<String> blockControls = dynamicBlockControls(element, declaration, declaredType);
      List<SchemaIrComplexType> candidates =
          dynamicTypeCandidates(type.name()).stream()
              .filter(candidate -> !candidate.abstractType())
              .filter(candidate -> !isBlockedDynamicType(candidate, type.name(), blockControls))
              .toList();
      if (candidates.isEmpty() && !declaredType.abstractType()) {
        return null;
      }
      BindingCardinality cardinality = BindingCardinality.from(element.cardinality());
      String packageName = names.javaName(element.name()).packageName();
      String dynamicSimpleName =
          names.uniqueTypeName(packageName, names.typeName(element.name()) + "DynamicType");
      BindingJavaName dynamicName = new BindingJavaName(packageName, dynamicSimpleName);
      Set<String> branchNames = new HashSet<>();
      List<BindingChoiceBranch> branches = new ArrayList<>();
      if (!declaredType.abstractType()) {
        branches.add(dynamicTypeBranch(element, declaredType, packageName, branchNames, true));
      }
      for (SchemaIrComplexType candidate : candidates) {
        branches.add(dynamicTypeBranch(element, candidate, packageName, branchNames, false));
      }
      if (branches.isEmpty()) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
            "binding",
            "Element "
                + element.name().toText()
                + " declares abstract type "
                + type.name().toText()
                + " without legal concrete xsi:type candidates.");
      }
      BindingChoice choice = new BindingChoice(dynamicName, branches, "xsiType");
      String fieldName = names.unique(names.fieldName(element.name()), usedFieldNames);
      return new BindingField(
          "choice",
          element.name(),
          fieldName,
          BindingTypeReference.choice(dynamicName),
          cardinality,
          order,
          cardinality.minOccurs() > 0,
          choice);
    }

    private BindingChoiceBranch dynamicTypeBranch(
        SchemaIrElement element,
        SchemaIrComplexType type,
        String packageName,
        Set<String> branchNames,
        boolean defaultDynamicType) {
      BindingJavaName modelName = complexTypeNames.get(type.name());
      BindingTypeReference bindingType = BindingTypeReference.model(modelName);
      String branchFieldName = names.uniqueFieldName(type.name(), branchNames);
      String branchSimpleName =
          names.uniqueTypeName(packageName, names.typeName(type.name()) + "DynamicTypeBranch");
      return new BindingChoiceBranch(
          element.name(),
          branchFieldName,
          bindingType,
          new BindingJavaName(packageName, branchSimpleName),
          type.name(),
          defaultDynamicType);
    }

    private List<SchemaIrComplexType> dynamicTypeCandidates(SchemaQName baseName) {
      return complexTypes.values().stream()
          .filter(type -> !baseName.equals(type.name()))
          .filter(type -> derivesFrom(type, baseName))
          .sorted(Comparator.comparing(type -> type.name().toText()))
          .toList();
    }

    private boolean derivesFrom(SchemaIrComplexType type, SchemaQName baseName) {
      SchemaQName current = type.derivationBase();
      while (current != null) {
        if (baseName.equals(current)) {
          return true;
        }
        SchemaIrComplexType currentType = complexTypes.get(current);
        current = currentType == null ? null : currentType.derivationBase();
      }
      return false;
    }

    private List<String> dynamicBlockControls(
        SchemaIrElement element, SchemaIrElement declaration, SchemaIrComplexType declaredType) {
      Set<String> controls = new LinkedHashSet<>();
      controls.addAll(declaration == null ? element.blockControls() : declaration.blockControls());
      controls.addAll(declaredType.blockControls());
      return List.copyOf(controls);
    }

    private boolean isBlockedDynamicType(
        SchemaIrComplexType candidate, SchemaQName declaredBase, List<String> blockControls) {
      SchemaIrComplexType current = candidate;
      while (current != null && current.derivationBase() != null) {
        if (blockControls.contains(current.derivationKind())) {
          return true;
        }
        if (declaredBase.equals(current.derivationBase())) {
          return false;
        }
        current = complexTypes.get(current.derivationBase());
      }
      return false;
    }

    private void unsupportedGroupedContent() {
      diagnostic(
          DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
          "binding",
          "Unsupported schema particle in grouped content binding.");
    }

    private BindingField bindWildcardField(
        SchemaIrWildcard wildcard, Set<String> usedFieldNames, int order) {
      String fieldName = names.unique("wildcardContent", usedFieldNames);
      BindingCardinality cardinality =
          new BindingCardinality(
              "list", wildcard.cardinality().minOccurs(), wildcard.cardinality().maxOccurs());
      return new BindingField(
          "wildcard",
          new SchemaQName("", "*"),
          fieldName,
          BindingTypeReference.fragment(),
          cardinality,
          order,
          wildcard.cardinality().minOccurs() > 0,
          new BindingWildcard(
              wildcard.namespaceConstraint(),
              wildcard.processContents(),
              List.of(),
              knownWildcardElements(wildcard.namespaceConstraint()),
              List.of()));
    }

    private List<BindingWildcardElement> knownWildcardElements(
        io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace namespace) {
      return globalElements.values().stream()
          .filter(element -> !element.abstractElement())
          .filter(element -> wildcardMatches(element.name(), namespace))
          .map(
              element ->
                  new BindingWildcardElement(
                      element.name(), bindTypeReference(element.type(), element, element.name())))
          .toList();
    }

    private List<BindingWildcardAttribute> knownWildcardAttributes(
        io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace namespace,
        List<SchemaQName> excludedNames) {
      return globalAttributes.values().stream()
          .filter(attribute -> !excludedNames.contains(attribute.name()))
          .filter(attribute -> wildcardMatches(attribute.name(), namespace))
          .map(
              attribute ->
                  new BindingWildcardAttribute(
                      attribute.name(),
                      bindTypeReference(attribute.type(), null, attribute.name())))
          .toList();
    }

    private boolean wildcardMatches(
        SchemaQName name,
        io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace namespace) {
      return switch (namespace.kind()) {
        case "any" -> true;
        case "other" -> !namespace.namespaces().contains(name.namespace());
        default -> namespace.namespaces().contains(name.namespace());
      };
    }

    private BindingField bindSubstitutionField(
        SchemaIrElement element,
        SchemaIrSubstitutionGroup substitutionGroup,
        Set<String> usedFieldNames,
        int order) {
      BindingCardinality cardinality = BindingCardinality.from(element.cardinality());
      String packageName = names.javaName(element.name()).packageName();
      String substitutionSimpleName =
          names.uniqueTypeName(packageName, names.typeName(element.name()) + "Substitution");
      BindingJavaName substitutionName = new BindingJavaName(packageName, substitutionSimpleName);
      Set<String> branchNames = new HashSet<>();
      List<BindingChoiceBranch> branches = new ArrayList<>();
      for (SchemaIrElement branch : substitutionGroup.branches()) {
        validateSubstitutionBranch(branch);
        BindingTypeReference bindingType = bindTypeReference(branch.type(), branch, branch.name());
        String branchFieldName = names.uniqueFieldName(branch.name(), branchNames);
        String branchSimpleName =
            names.uniqueTypeName(packageName, names.typeName(branch.name()) + "SubstitutionBranch");
        branches.add(
            new BindingChoiceBranch(
                branch.name(),
                branchFieldName,
                bindingType,
                new BindingJavaName(packageName, branchSimpleName)));
      }
      if (branches.isEmpty()) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
            "binding",
            "Substitution group head " + element.name().toText() + " has no concrete branches.");
      }
      BindingChoice bindingChoice = new BindingChoice(substitutionName, branches, "substitution");
      String fieldName = names.unique(names.fieldName(element.name()), usedFieldNames);
      boolean required = cardinality.minOccurs() > 0;
      return new BindingField(
          "choice",
          element.name(),
          fieldName,
          BindingTypeReference.choice(substitutionName),
          cardinality,
          order,
          required,
          bindingChoice);
    }

    private void validateSubstitutionBranch(SchemaIrElement branch) {
      if (branch.semantics().hasAny()) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
            "binding",
            "Substitution group branch "
                + branch.name().toText()
                + " cannot carry nillable/default/fixed semantics in TASK-0033.");
      }
      if (branch.type().anonymous()) {
        return;
      }
      if (branch.type().name().isXmlSchemaBuiltIn()
          || complexTypes.containsKey(branch.type().name())
          || simpleTypes.containsKey(branch.type().name())) {
        return;
      }
      diagnostic(
          DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
          "binding",
          "Unsupported substitution group branch type " + branch.type().name().toText() + ".");
    }

    private BindingField bindChoiceField(
        SchemaIrComplexType complexType,
        BindingJavaName ownerName,
        SchemaIrChoice choice,
        Set<String> usedFieldNames,
        int order) {
      String packageName = ownerName.packageName();
      String choiceSimpleName =
          names.uniqueTypeName(packageName, ownerName.simpleName() + "Choice");
      BindingJavaName choiceName = new BindingJavaName(packageName, choiceSimpleName);
      Set<String> branchNames = new HashSet<>();
      List<BindingChoiceBranch> branches = new ArrayList<>();
      for (SchemaIrElement branch : choice.elementBranches()) {
        SchemaIrElement declaration =
            branch.reference() ? globalElements.get(branch.name()) : branch;
        SchemaIrTypeReference type = declaration == null ? branch.type() : declaration.type();
        BindingTypeReference bindingType = bindTypeReference(type, declaration, branch.name());
        String branchFieldName = names.uniqueFieldName(branch.name(), branchNames);
        String branchSimpleName =
            names.uniqueTypeName(packageName, names.typeName(branch.name()) + "Choice");
        branches.add(
            new BindingChoiceBranch(
                branch.name(),
                branchFieldName,
                bindingType,
                new BindingJavaName(packageName, branchSimpleName)));
      }
      BindingChoice bindingChoice = new BindingChoice(choiceName, branches);
      BindingCardinality cardinality = BindingCardinality.from(choice.cardinality());
      String baseFieldName = names.fieldNameFromTypeName(choiceSimpleName);
      String fieldName = names.unique(baseFieldName, usedFieldNames);
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
          element.name(),
          type,
          BindingCardinality.from(element.cardinality()),
          element.identityConstraints());
    }

    private List<BindingRootElement> bindRootElements(List<SchemaIrElement> elements) {
      List<BindingRootElement> roots = new ArrayList<>();
      for (SchemaIrElement element : elements) {
        if (!element.abstractElement()) {
          roots.add(bindRootElement(element));
        }
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
        if (!XmlSchemaBuiltIns.isSupported(name.localName())) {
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
        return bindRestrictedScalar(simpleType.restriction());
      }
      if (simpleType != null && simpleType.list() != null) {
        BindingTypeReference itemType =
            simpleType.list().itemRestriction() == null
                ? bindSimpleCompositionMember(simpleType.list().itemType())
                : bindRestrictedScalar(simpleType.list().itemRestriction());
        if (itemType == null) {
          return BindingTypeReference.scalar("unsupported");
        }
        return BindingTypeReference.list(itemType);
      }
      if (simpleType != null && simpleType.union() != null) {
        List<BindingTypeReference> members = new ArrayList<>();
        for (SchemaQName memberType : simpleType.union().memberTypes()) {
          BindingTypeReference member = bindSimpleCompositionMember(memberType);
          if (member != null) {
            members.add(member);
          }
        }
        for (SchemaIrSimpleRestriction restriction :
            simpleType.union().anonymousMemberRestrictions()) {
          members.add(bindRestrictedScalar(restriction));
        }
        if (members.isEmpty()) {
          return BindingTypeReference.scalar("unsupported");
        }
        return BindingTypeReference.union(members);
      }
      diagnostic(
          DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
          "binding",
          "Unsupported schema type " + name.toText() + ".");
      return BindingTypeReference.scalar("unsupported");
    }

    private BindingTypeReference bindSimpleCompositionMember(SchemaQName name) {
      if (name.isXmlSchemaBuiltIn()) {
        if (!XmlSchemaBuiltIns.isSupported(name.localName())) {
          diagnostic(
              DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
              "binding",
              "Unsupported XML Schema built-in type " + name.toText() + ".");
          return null;
        }
        return BindingTypeReference.scalar(name.localName());
      }
      SchemaIrSimpleType simpleType = simpleTypes.get(name);
      if (simpleType != null && simpleType.restriction() != null) {
        return bindRestrictedScalar(simpleType.restriction());
      }
      diagnostic(
          DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
          "binding",
          "Unsupported simple type composition member " + name.toText() + ".");
      return null;
    }

    private BindingTypeReference bindRestrictedScalar(SchemaIrSimpleRestriction restriction) {
      for (String enumeration : restriction.enumerations()) {
        if (XmlSchemaBuiltIns.hasPrefixedQNameLexical(
            restriction.base().localName(), enumeration)) {
          diagnostic(
              DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
              "binding",
              "Prefixed QName enumeration facets require schema namespace context preservation.");
        } else if (!XmlSchemaBuiltIns.isLexicallyValid(
            restriction.base().localName(), enumeration)) {
          diagnostic(
              DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
              "binding",
              "Invalid enumeration value "
                  + enumeration
                  + " for base "
                  + restriction.base().toText()
                  + ".");
        }
      }
      validateRestrictionBound(restriction.base(), "minInclusive", restriction.minInclusive());
      validateRestrictionBound(restriction.base(), "maxInclusive", restriction.maxInclusive());
      validateRestrictionBound(restriction.base(), "minExclusive", restriction.minExclusive());
      validateRestrictionBound(restriction.base(), "maxExclusive", restriction.maxExclusive());
      BindingSimpleRestriction bindingRestriction =
          new BindingSimpleRestriction(
              restriction.base().localName(),
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
              restriction.whiteSpace(),
              restriction.patterns());
      return BindingTypeReference.scalar(restriction.base().localName(), bindingRestriction);
    }

    private void validateRestrictionBound(SchemaQName base, String facetName, String value) {
      if (value == null) {
        return;
      }
      if (!XmlSchemaBuiltIns.isLexicallyValid(base.localName(), value)) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
            "binding",
            "Invalid " + facetName + " value " + value + " for base " + base.toText() + ".");
      }
    }

    private void validateListSimpleTypeCardinality(
        BindingTypeReference bindingType, BindingCardinality cardinality, SchemaQName name) {
      if (!"list".equals(bindingType.kind())) {
        return;
      }
      if (!"required".equals(cardinality.shape())
          || cardinality.minOccurs() != 1
          || !"1".equals(cardinality.maxOccurs())) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
            "binding",
            "xs:list-valued field "
                + name.toText()
                + " supports only required singleton XML values in profile XP-XSD10-COMPOSED.");
      }
    }

    private BindingValueSemantics bindingSemantics(SchemaIrValueSemantics semantics) {
      if (semantics == null) {
        return BindingValueSemantics.NONE;
      }
      return new BindingValueSemantics(
          semantics.nillable(), semantics.defaultValue(), semantics.fixedValue());
    }

    private BindingCardinality effectiveAttributeCardinality(
        BindingCardinality cardinality, BindingValueSemantics semantics) {
      if (semantics.hasDefault() || semantics.hasFixed()) {
        return BindingCardinality.from(SchemaCardinality.ONE);
      }
      return cardinality;
    }

    private void validateValueSemantics(
        String kind,
        SchemaQName name,
        BindingTypeReference bindingType,
        BindingCardinality cardinality,
        BindingValueSemantics semantics) {
      if (!semantics.hasAny()) {
        return;
      }
      if (semantics.hasDefault() && semantics.hasFixed()) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
            "binding",
            "Field " + name.toText() + " cannot declare both default and fixed values.");
      }
      if ("attribute".equals(kind) && semantics.nillable()) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
            "binding",
            "Attribute " + name.toText() + " cannot be nillable.");
      }
      if (semantics.nillable()) {
        if (!"element".equals(kind)
            || !"required".equals(cardinality.shape())
            || cardinality.minOccurs() != 1
            || !"1".equals(cardinality.maxOccurs())
            || "list".equals(bindingType.kind())) {
          diagnostic(
              DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
              "binding",
              "nillable element "
                  + name.toText()
                  + " supports only required singleton non-list values in profile XP-XSD10-SEMANTIC.");
        }
        if (semantics.hasDefault() || semantics.hasFixed()) {
          diagnostic(
              DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
              "binding",
              "nillable element "
                  + name.toText()
                  + " cannot combine nil semantics with default or fixed values.");
        }
      }
      validateDefaultOrFixed(kind, name, bindingType, "default", semantics.defaultValue());
      validateDefaultOrFixed(kind, name, bindingType, "fixed", semantics.fixedValue());
    }

    private void validateDefaultOrFixed(
        String kind,
        SchemaQName name,
        BindingTypeReference bindingType,
        String label,
        String value) {
      if (value == null) {
        return;
      }
      if (!"scalar".equals(bindingType.kind())) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
            "binding",
            kind
                + " "
                + name.toText()
                + " "
                + label
                + " values support only scalar built-ins or restricted scalar aliases.");
        return;
      }
      if (XmlSchemaBuiltIns.hasPrefixedQNameLexical(bindingType.name(), value)) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
            "binding",
            kind
                + " "
                + name.toText()
                + " has namespace-prefixed QName "
                + label
                + " value "
                + value
                + ", which requires schema namespace context preservation.");
        return;
      }
      if (!XmlSchemaBuiltIns.isLexicallyValid(bindingType.name(), value)
          || !XmlSchemaBuiltIns.matchesRestriction(
              bindingType.name(), value, bindingType.restriction())) {
        diagnostic(
            DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
            "binding",
            kind
                + " "
                + name.toText()
                + " has unsupported lexical "
                + label
                + " value "
                + value
                + ".");
      }
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
}
