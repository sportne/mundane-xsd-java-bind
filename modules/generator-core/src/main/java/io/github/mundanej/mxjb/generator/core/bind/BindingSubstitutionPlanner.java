package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrComplexType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrElement;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSimpleType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSubstitutionGroup;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Package-private planner for substitution-group choice branches. */
final class BindingSubstitutionPlanner {
  private final BindingNameAllocator names;
  private final Map<SchemaQName, SchemaIrComplexType> complexTypes;
  private final Map<SchemaQName, SchemaIrSimpleType> simpleTypes;
  private final BindingBranchTypeBinder typeBinder;
  private final BindingBranchDiagnostics diagnostics;

  BindingSubstitutionPlanner(
      BindingNameAllocator names,
      Map<SchemaQName, SchemaIrComplexType> complexTypes,
      Map<SchemaQName, SchemaIrSimpleType> simpleTypes,
      BindingBranchTypeBinder typeBinder,
      BindingBranchDiagnostics diagnostics) {
    this.names = names;
    this.complexTypes = complexTypes;
    this.simpleTypes = simpleTypes;
    this.typeBinder = typeBinder;
    this.diagnostics = diagnostics;
  }

  BindingField plan(
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
      BindingTypeReference bindingType = typeBinder.bind(branch.type(), branch, branch.name());
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
      diagnostics.report(
          DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
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
      diagnostics.report(
          DiagnosticCode.SCHEMA_BINDING_INVALID_MODEL,
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
    diagnostics.report(
        DiagnosticCode.SCHEMA_BINDING_UNSUPPORTED_TYPE,
        "Unsupported substitution group branch type " + branch.type().name().toText() + ".");
  }
}
