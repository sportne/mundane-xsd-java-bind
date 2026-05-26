package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrComplexType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrElement;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrTypeReference;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Package-private planner for declared-base dynamic xsi:type binding branches. */
final class BindingDynamicTypePlanner {
  private final BindingNameAllocator names;
  private final Map<SchemaQName, SchemaIrComplexType> complexTypes;
  private final Map<SchemaQName, BindingJavaName> complexTypeNames;
  private final BindingBranchDiagnostics diagnostics;

  BindingDynamicTypePlanner(
      BindingNameAllocator names,
      Map<SchemaQName, SchemaIrComplexType> complexTypes,
      Map<SchemaQName, BindingJavaName> complexTypeNames,
      BindingBranchDiagnostics diagnostics) {
    this.names = names;
    this.complexTypes = complexTypes;
    this.complexTypeNames = complexTypeNames;
    this.diagnostics = diagnostics;
  }

  BindingField planIfNeeded(
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
      diagnostics.report(
          DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
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
}
