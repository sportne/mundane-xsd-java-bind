package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaCardinality;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrAll;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrChoice;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrComplexType;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrElement;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrGroup;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrParticle;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrSequence;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrTypeReference;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcard;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrWildcardNamespace;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class BindingContentPlanner {
  private final BindingNameAllocator names;
  private final ElementResolver elementResolver;
  private final TypeBinder typeBinder;
  private final WildcardElementResolver wildcardElementResolver;
  private final DiagnosticReporter diagnosticReporter;

  BindingContentPlanner(
      BindingNameAllocator names,
      ElementResolver elementResolver,
      TypeBinder typeBinder,
      WildcardElementResolver wildcardElementResolver,
      DiagnosticReporter diagnosticReporter) {
    this.names = names;
    this.elementResolver = elementResolver;
    this.typeBinder = typeBinder;
    this.wildcardElementResolver = wildcardElementResolver;
    this.diagnosticReporter = diagnosticReporter;
  }

  BindingField mixedContentField(
      SchemaIrComplexType complexType,
      BindingJavaName ownerName,
      Set<String> usedFieldNames,
      int order) {
    String packageName = ownerName.packageName();
    String contentSimpleName =
        names.uniqueTypeName(packageName, ownerName.simpleName() + "Content");
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
                packageName,
                names.uniqueTypeName(packageName, ownerName.simpleName() + "TextContent")),
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
    String fieldName = names.unique("content", usedFieldNames);
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

  BindingField groupedContentField(
      SchemaIrComplexType complexType,
      BindingJavaName ownerName,
      String modelKind,
      SchemaCardinality cardinality,
      List<SchemaIrParticle> particles,
      Set<String> usedFieldNames,
      int order) {
    String packageName = ownerName.packageName();
    String contentSimpleName =
        names.uniqueTypeName(
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
    String fieldName = names.unique(names.fieldNameFromTypeName(contentSimpleName), usedFieldNames);
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
      return addChoiceGroup(
          choice, packageName, ownerSimpleName, branchNames, branches, groups, branchOrder);
    }
    if (particle instanceof SchemaIrAll all) {
      return addAllGroup(all, packageName, branchNames, branches, groups, branchOrder);
    }
    if (particle instanceof SchemaIrGroup group) {
      return addNestedGroup(
          group, packageName, ownerSimpleName, branchNames, branches, groups, branchOrder);
    }
    diagnosticReporter.unsupportedGroupedContent();
    return branchOrder;
  }

  private int addChoiceGroup(
      SchemaIrChoice choice,
      String packageName,
      String ownerSimpleName,
      Set<String> branchNames,
      List<BindingContentBranch> branches,
      List<BindingContentGroup> groups,
      int branchOrder) {
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
    BindingCardinality cardinality = BindingCardinality.from(choice.cardinality());
    groups.add(
        new BindingContentGroup(
            "choice",
            cardinality,
            groupBranches,
            List.of(new BindingContentPosition(cardinality, groupBranches))));
    return branchOrder;
  }

  private int addAllGroup(
      SchemaIrAll all,
      String packageName,
      Set<String> branchNames,
      List<BindingContentBranch> branches,
      List<BindingContentGroup> groups,
      int branchOrder) {
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
            "all", BindingCardinality.from(all.cardinality()), groupBranches, groupPositions));
    return branchOrder - 1;
  }

  private int addNestedGroup(
      SchemaIrGroup group,
      String packageName,
      String ownerSimpleName,
      Set<String> branchNames,
      List<BindingContentBranch> branches,
      List<BindingContentGroup> groups,
      int branchOrder) {
    List<BindingContentBranch> groupBranches = new ArrayList<>();
    List<BindingContentPosition> groupPositions = new ArrayList<>();
    for (SchemaIrParticle nested : group.particles()) {
      if (nested instanceof SchemaIrChoice choice) {
        branchOrder =
            addNestedChoicePosition(
                group,
                choice,
                packageName,
                ownerSimpleName,
                branchNames,
                branches,
                groups,
                groupBranches,
                groupPositions,
                branchOrder);
        continue;
      }
      int beforeSize = branches.size();
      branchOrder =
          addContentBranches(
              nested, packageName, ownerSimpleName, branchNames, branches, groups, branchOrder);
      List<BindingContentBranch> positionBranches =
          new ArrayList<>(branches.subList(beforeSize, branches.size()));
      groupBranches.addAll(positionBranches);
      groupPositions.add(new BindingContentPosition(positionCardinality(nested), positionBranches));
      branchOrder++;
    }
    groups.add(
        new BindingContentGroup(
            group.modelKind(),
            BindingCardinality.from(group.cardinality()),
            groupBranches,
            groupPositions));
    return branchOrder - 1;
  }

  private int addNestedChoicePosition(
      SchemaIrGroup group,
      SchemaIrChoice choice,
      String packageName,
      String ownerSimpleName,
      Set<String> branchNames,
      List<BindingContentBranch> branches,
      List<BindingContentGroup> groups,
      List<BindingContentBranch> groupBranches,
      List<BindingContentPosition> groupPositions,
      int branchOrder) {
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
    return branchOrder + 1;
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
    SchemaIrElement declaration = element.reference() ? elementResolver.resolve(element) : element;
    SchemaIrTypeReference type = declaration == null ? element.type() : declaration.type();
    BindingTypeReference bindingType = typeBinder.bind(type, declaration, element.name());
    String branchFieldName = names.uniqueFieldName(element.name(), branchNames);
    String branchSimpleName =
        names.uniqueTypeName(packageName, names.typeName(element.name()) + "Content");
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
    String branchFieldName = names.unique("wildcardContent", branchNames);
    String branchSimpleName =
        names.uniqueTypeName(packageName, ownerSimpleName + "WildcardContent");
    return new BindingContentBranch(
        "wildcard",
        new SchemaQName("", "*"),
        branchFieldName,
        BindingTypeReference.fragment(),
        new BindingJavaName(packageName, branchSimpleName),
        BindingCardinality.from(wildcard.cardinality()),
        order,
        new BindingWildcard(
            wildcard.namespaceConstraint(),
            wildcard.processContents(),
            List.of(),
            wildcardElementResolver.knownElements(wildcard.namespaceConstraint()),
            List.of()));
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

  interface ElementResolver {
    SchemaIrElement resolve(SchemaIrElement element);
  }

  interface TypeBinder {
    BindingTypeReference bind(
        SchemaIrTypeReference reference, SchemaIrElement owner, SchemaQName fallbackName);
  }

  interface WildcardElementResolver {
    List<BindingWildcardElement> knownElements(SchemaIrWildcardNamespace namespace);
  }

  interface DiagnosticReporter {
    void unsupportedGroupedContent();
  }
}
