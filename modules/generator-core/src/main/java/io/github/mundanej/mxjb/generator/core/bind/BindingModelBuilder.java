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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
    private final BindingConfiguration configuration;
    private final Map<SchemaQName, SchemaIrElement> globalElements = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrAttribute> globalAttributes = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrComplexType> complexTypes = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrSimpleType> simpleTypes = new LinkedHashMap<>();
    private final Map<SchemaQName, SchemaIrSubstitutionGroup> substitutionGroups =
        new LinkedHashMap<>();
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
      for (SchemaIrSubstitutionGroup substitutionGroup : model.substitutionGroups()) {
        substitutionGroups.put(substitutionGroup.head(), substitutionGroup);
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
      if (complexType.simpleContent() != null) {
        BindingField field = bindSimpleContentField(complexType.simpleContent(), usedFieldNames);
        fields.add(field);
        validationRules.add("simpleContent " + field.javaName());
      } else if (complexType.mixed()) {
        BindingField field = bindContentField(complexType, javaName, usedFieldNames, order);
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
                      : bindGroupedContentField(
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
                    bindGroupedContentField(
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
                  bindGroupedContentField(
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
        String fieldName = JavaNames.uniqueFieldName(attribute.name(), usedFieldNames);
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
          JavaNames.unique("value", usedFieldNames),
          type,
          new BindingCardinality("required", 1, "1"),
          0,
          true);
    }

    private BindingField bindAnyAttributeField(
        SchemaIrAnyAttribute anyAttribute,
        List<SchemaQName> prohibitedAttributes,
        Set<String> usedFieldNames) {
      String fieldName = JavaNames.unique("wildcardAttributes", usedFieldNames);
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
              prohibitedAttributes));
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
      String fieldName = JavaNames.uniqueFieldName(element.name(), usedFieldNames);
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
      String packageName = javaName(element.name()).packageName();
      String dynamicSimpleName =
          uniqueTypeName(packageName, JavaNames.typeName(element.name()) + "DynamicType");
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
      String fieldName = JavaNames.unique(JavaNames.fieldName(element.name()), usedFieldNames);
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
      String branchFieldName = JavaNames.uniqueFieldName(type.name(), branchNames);
      String branchSimpleName =
          uniqueTypeName(packageName, JavaNames.typeName(type.name()) + "DynamicTypeBranch");
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

    private BindingField bindContentField(
        SchemaIrComplexType complexType,
        BindingJavaName ownerName,
        Set<String> usedFieldNames,
        int order) {
      String packageName = ownerName.packageName();
      String contentSimpleName = uniqueTypeName(packageName, ownerName.simpleName() + "Content");
      BindingJavaName contentName = new BindingJavaName(packageName, contentSimpleName);
      Set<String> branchNames = new HashSet<>();
      List<BindingContentBranch> branches = new ArrayList<>();
      List<BindingContentGroup> groups = new ArrayList<>();
      branches.add(
          new BindingContentBranch(
              "text",
              new SchemaQName("", "#text"),
              "text",
              BindingTypeReference.scalar("string"),
              new BindingJavaName(
                  packageName, uniqueTypeName(packageName, ownerName.simpleName() + "TextContent")),
              new BindingCardinality("list", 0, "unbounded"),
              0,
              null));
      int branchOrder = 1;
      for (SchemaIrSequence sequence : complexType.sequences()) {
        for (SchemaIrParticle particle : sequence.particles()) {
          branchOrder =
              addContentBranches(
                  particle,
                  packageName,
                  ownerName.simpleName(),
                  branchNames,
                  branches,
                  groups,
                  branchOrder);
          branchOrder++;
        }
      }
      String fieldName = JavaNames.unique("content", usedFieldNames);
      return new BindingField(
          "content",
          complexType.name() == null ? new SchemaQName("", fieldName) : complexType.name(),
          fieldName,
          BindingTypeReference.choice(contentName),
          new BindingCardinality("list", 0, "unbounded"),
          order,
          false,
          new BindingContent(contentName, branches, "mixed content", groups));
    }

    private BindingField bindGroupedContentField(
        SchemaIrComplexType complexType,
        BindingJavaName ownerName,
        String modelKind,
        SchemaCardinality cardinality,
        List<SchemaIrParticle> particles,
        Set<String> usedFieldNames,
        int order) {
      String packageName = ownerName.packageName();
      String contentSimpleName =
          uniqueTypeName(
              packageName, ownerName.simpleName() + JavaNames.capitalize(modelKind) + "Content");
      BindingJavaName contentName = new BindingJavaName(packageName, contentSimpleName);
      Set<String> branchNames = new HashSet<>();
      List<BindingContentBranch> branches = new ArrayList<>();
      List<BindingContentGroup> groups = new ArrayList<>();
      int branchOrder = 1;
      for (SchemaIrParticle particle : particles) {
        branchOrder =
            addContentBranches(
                particle,
                packageName,
                ownerName.simpleName(),
                branchNames,
                branches,
                groups,
                branchOrder);
        branchOrder++;
      }
      String fieldName =
          JavaNames.unique(JavaNames.fieldNameFromTypeName(contentSimpleName), usedFieldNames);
      return new BindingField(
          "content",
          complexType.name() == null ? new SchemaQName("", fieldName) : complexType.name(),
          fieldName,
          BindingTypeReference.choice(contentName),
          new BindingCardinality("list", cardinality.minOccurs(), cardinality.maxOccurs()),
          order,
          cardinality.minOccurs() > 0,
          new BindingContent(
              contentName,
              branches,
              modelKind,
              groups.isEmpty()
                  ? List.of(
                      new BindingContentGroup(
                          modelKind,
                          new BindingCardinality(
                              "list", cardinality.minOccurs(), cardinality.maxOccurs()),
                          branches))
                  : groups));
    }

    private int addContentBranches(
        SchemaIrParticle particle,
        String packageName,
        String ownerSimpleName,
        Set<String> branchNames,
        List<BindingContentBranch> branches,
        List<BindingContentGroup> groups,
        int branchOrder) {
      if (particle instanceof SchemaIrElement element) {
        branches.add(bindElementContentBranch(element, packageName, branchNames, branchOrder));
        return branchOrder;
      }
      if (particle instanceof SchemaIrWildcard wildcard) {
        branches.add(
            bindWildcardContentBranch(
                wildcard, packageName, ownerSimpleName, branchNames, branchOrder));
        return branchOrder;
      }
      if (particle instanceof SchemaIrChoice choice) {
        List<BindingContentBranch> groupBranches = new ArrayList<>();
        for (SchemaIrParticle branch : choice.branches()) {
          int beforeSize = branches.size();
          addContentBranches(
              withChoiceBranchCardinality(branch, choice.cardinality()),
              packageName,
              ownerSimpleName,
              branchNames,
              branches,
              groups,
              branchOrder);
          groupBranches.addAll(branches.subList(beforeSize, branches.size()));
        }
        groups.add(
            new BindingContentGroup(
                "choice",
                new BindingCardinality(
                    "list", choice.cardinality().minOccurs(), choice.cardinality().maxOccurs()),
                groupBranches,
                List.of(
                    new BindingContentPosition(
                        new BindingCardinality(
                            "list",
                            choice.cardinality().minOccurs(),
                            choice.cardinality().maxOccurs()),
                        groupBranches))));
        return branchOrder;
      }
      if (particle instanceof SchemaIrAll all) {
        List<BindingContentBranch> groupBranches = new ArrayList<>();
        List<BindingContentPosition> groupPositions = new ArrayList<>();
        for (SchemaIrElement element : all.elements()) {
          BindingContentBranch branch =
              bindElementContentBranch(
                  withElementCardinality(element, SchemaCardinality.ONE),
                  packageName,
                  branchNames,
                  branchOrder);
          branches.add(branch);
          groupBranches.add(branch);
          groupPositions.add(new BindingContentPosition(branch.cardinality(), List.of(branch)));
          branchOrder++;
        }
        groups.add(
            new BindingContentGroup(
                "all",
                new BindingCardinality(
                    "list", all.cardinality().minOccurs(), all.cardinality().maxOccurs()),
                groupBranches,
                groupPositions));
        return branchOrder - 1;
      }
      if (particle instanceof SchemaIrGroup group) {
        List<BindingContentBranch> groupBranches = new ArrayList<>();
        List<BindingContentPosition> groupPositions = new ArrayList<>();
        for (SchemaIrParticle nested : group.particles()) {
          if (nested instanceof SchemaIrChoice choice) {
            List<BindingContentBranch> positionBranches = new ArrayList<>();
            BindingCardinality positionCardinality = BindingCardinality.from(choice.cardinality());
            SchemaCardinality effectiveChoiceCardinality =
                composeCardinality(group.cardinality(), choice.cardinality());
            for (SchemaIrParticle choiceBranch : choice.branches()) {
              int beforeSize = branches.size();
              addContentBranches(
                  withChoiceBranchCardinality(choiceBranch, effectiveChoiceCardinality),
                  packageName,
                  ownerSimpleName,
                  branchNames,
                  branches,
                  groups,
                  branchOrder);
              positionBranches.addAll(branches.subList(beforeSize, branches.size()));
            }
            groupBranches.addAll(positionBranches);
            groupPositions.add(new BindingContentPosition(positionCardinality, positionBranches));
            branchOrder++;
            continue;
          }
          int beforeSize = branches.size();
          branchOrder =
              addContentBranches(
                  nested, packageName, ownerSimpleName, branchNames, branches, groups, branchOrder);
          List<BindingContentBranch> positionBranches =
              new ArrayList<>(branches.subList(beforeSize, branches.size()));
          groupBranches.addAll(positionBranches);
          groupPositions.add(
              new BindingContentPosition(positionCardinality(nested), positionBranches));
          branchOrder++;
        }
        groups.add(
            new BindingContentGroup(
                group.modelKind(),
                new BindingCardinality(
                    "list", group.cardinality().minOccurs(), group.cardinality().maxOccurs()),
                groupBranches,
                groupPositions));
        return branchOrder - 1;
      }
      diagnostic(
          DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
          "binding",
          "Unsupported schema particle in grouped content binding.");
      return branchOrder;
    }

    private SchemaIrParticle withChoiceBranchCardinality(
        SchemaIrParticle particle, SchemaCardinality cardinality) {
      SchemaCardinality effective = new SchemaCardinality(0, cardinality.maxOccurs());
      return withBranchCardinality(particle, effective);
    }

    private BindingCardinality positionCardinality(SchemaIrParticle particle) {
      if (particle instanceof SchemaIrElement element) {
        return BindingCardinality.from(element.cardinality());
      }
      if (particle instanceof SchemaIrWildcard wildcard) {
        return BindingCardinality.from(wildcard.cardinality());
      }
      if (particle instanceof SchemaIrChoice choice) {
        return BindingCardinality.from(choice.cardinality());
      }
      if (particle instanceof SchemaIrAll all) {
        return BindingCardinality.from(all.cardinality());
      }
      if (particle instanceof SchemaIrGroup group) {
        return BindingCardinality.from(group.cardinality());
      }
      return new BindingCardinality("required", 1, "1");
    }

    private SchemaIrParticle withBranchCardinality(
        SchemaIrParticle particle, SchemaCardinality cardinality) {
      if (particle instanceof SchemaIrElement element) {
        return withElementCardinality(element, cardinality);
      }
      if (particle instanceof SchemaIrWildcard wildcard) {
        return new SchemaIrWildcard(
            composeCardinality(cardinality, wildcard.cardinality()),
            wildcard.namespaceConstraint(),
            wildcard.processContents());
      }
      if (particle instanceof SchemaIrChoice choice) {
        return new SchemaIrChoice(
            composeCardinality(cardinality, choice.cardinality()), choice.branches());
      }
      return particle;
    }

    private SchemaIrElement withElementCardinality(
        SchemaIrElement element, SchemaCardinality cardinality) {
      return new SchemaIrElement(
          element.name(),
          element.type(),
          composeCardinality(cardinality, element.cardinality()),
          element.inlineComplexType(),
          element.semantics(),
          element.substitutionGroup(),
          element.abstractElement(),
          element.blockControls(),
          element.identityConstraints(),
          element.reference());
    }

    private BindingContentBranch bindElementContentBranch(
        SchemaIrElement element, String packageName, Set<String> branchNames, int order) {
      SchemaIrElement declaration =
          element.reference() ? globalElements.get(element.name()) : element;
      SchemaIrTypeReference type = declaration == null ? element.type() : declaration.type();
      BindingTypeReference bindingType = bindTypeReference(type, declaration, element.name());
      String branchFieldName = JavaNames.uniqueFieldName(element.name(), branchNames);
      String branchSimpleName =
          uniqueTypeName(packageName, JavaNames.typeName(element.name()) + "Content");
      return new BindingContentBranch(
          "element",
          element.name(),
          branchFieldName,
          bindingType,
          new BindingJavaName(packageName, branchSimpleName),
          BindingCardinality.from(element.cardinality()),
          order,
          null);
    }

    private BindingContentBranch bindWildcardContentBranch(
        SchemaIrWildcard wildcard,
        String packageName,
        String ownerSimpleName,
        Set<String> branchNames,
        int order) {
      String branchFieldName = JavaNames.unique("wildcardContent", branchNames);
      String branchSimpleName = uniqueTypeName(packageName, ownerSimpleName + "WildcardContent");
      return new BindingContentBranch(
          "wildcard",
          new SchemaQName("", "*"),
          branchFieldName,
          BindingTypeReference.fragment(),
          new BindingJavaName(packageName, branchSimpleName),
          new BindingCardinality(
              "list", wildcard.cardinality().minOccurs(), wildcard.cardinality().maxOccurs()),
          order,
          new BindingWildcard(wildcard.namespaceConstraint(), wildcard.processContents()));
    }

    private BindingField bindWildcardField(
        SchemaIrWildcard wildcard, Set<String> usedFieldNames, int order) {
      String fieldName = JavaNames.unique("wildcardContent", usedFieldNames);
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
          new BindingWildcard(wildcard.namespaceConstraint(), wildcard.processContents()));
    }

    private BindingField bindSubstitutionField(
        SchemaIrElement element,
        SchemaIrSubstitutionGroup substitutionGroup,
        Set<String> usedFieldNames,
        int order) {
      BindingCardinality cardinality = BindingCardinality.from(element.cardinality());
      String packageName = javaName(element.name()).packageName();
      String substitutionSimpleName =
          uniqueTypeName(packageName, JavaNames.typeName(element.name()) + "Substitution");
      BindingJavaName substitutionName = new BindingJavaName(packageName, substitutionSimpleName);
      Set<String> branchNames = new HashSet<>();
      List<BindingChoiceBranch> branches = new ArrayList<>();
      for (SchemaIrElement branch : substitutionGroup.branches()) {
        validateSubstitutionBranch(branch);
        BindingTypeReference bindingType = bindTypeReference(branch.type(), branch, branch.name());
        String branchFieldName = JavaNames.uniqueFieldName(branch.name(), branchNames);
        String branchSimpleName =
            uniqueTypeName(packageName, JavaNames.typeName(branch.name()) + "SubstitutionBranch");
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
      String fieldName = JavaNames.unique(JavaNames.fieldName(element.name()), usedFieldNames);
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
      String choiceSimpleName = uniqueTypeName(packageName, ownerName.simpleName() + "Choice");
      BindingJavaName choiceName = new BindingJavaName(packageName, choiceSimpleName);
      Set<String> branchNames = new HashSet<>();
      List<BindingChoiceBranch> branches = new ArrayList<>();
      for (SchemaIrElement branch : choice.elementBranches()) {
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
        BindingTypeReference itemType = bindSimpleCompositionMember(simpleType.list().itemType());
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

    private SchemaCardinality composeCardinality(SchemaCardinality outer, SchemaCardinality inner) {
      return new SchemaCardinality(
          outer.minOccurs() * inner.minOccurs(), multiplyMax(outer.maxOccurs(), inner.maxOccurs()));
    }

    private String multiplyMax(String left, String right) {
      if ("unbounded".equals(left) || "unbounded".equals(right)) {
        return "unbounded";
      }
      return Integer.toString(Integer.parseInt(left) * Integer.parseInt(right));
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
