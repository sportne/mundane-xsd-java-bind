package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingChoiceBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingContentBranch;
import io.github.mundanej.mxjb.generator.core.bind.BindingField;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

record GeneratedReaderStatePlan(
    boolean needsNillableSupport,
    boolean needsXsiTypeSupport,
    boolean needsWildcardSupport,
    boolean needsDefaultedElementSupport,
    boolean needsListSupport) {
  static GeneratedReaderStatePlan from(
      BindingType rootType, Function<String, BindingType> typeLookup) {
    Objects.requireNonNull(rootType, "rootType");
    Objects.requireNonNull(typeLookup, "typeLookup");
    return new GeneratedReaderStatePlan(
        needsNillableSupport(rootType, typeLookup, new LinkedHashSet<>()),
        needsXsiTypeSupport(rootType, typeLookup, new LinkedHashSet<>()),
        needsWildcardSupport(rootType, typeLookup, new LinkedHashSet<>()),
        needsDefaultedElementSupport(rootType, typeLookup, new LinkedHashSet<>()),
        needsListSupport(rootType, typeLookup, new LinkedHashSet<>()));
  }

  private static boolean needsListSupport(
      BindingType type, Function<String, BindingType> typeLookup, Set<String> visited) {
    if (!visited.add(type.javaName().qualifiedName())) {
      return false;
    }
    for (BindingField field : type.fields()) {
      if (containsListType(field.type())) {
        return true;
      }
      BindingType nestedType = modelType(field.type(), typeLookup);
      if (nestedType != null && needsListSupport(nestedType, typeLookup, visited)) {
        return true;
      }
      if ("choice".equals(field.kind())) {
        for (BindingChoiceBranch branch : field.choice().branches()) {
          if (containsListType(branch.type())) {
            return true;
          }
          BindingType branchType = modelType(branch.type(), typeLookup);
          if (branchType != null && needsListSupport(branchType, typeLookup, visited)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean needsNillableSupport(
      BindingType type, Function<String, BindingType> typeLookup, Set<String> visited) {
    if (!visited.add(type.javaName().qualifiedName())) {
      return false;
    }
    for (BindingField field : type.fields()) {
      if (field.semantics().nillable()) {
        return true;
      }
      BindingType nestedType = modelType(field.type(), typeLookup);
      if (nestedType != null && needsNillableSupport(nestedType, typeLookup, visited)) {
        return true;
      }
    }
    return false;
  }

  private static boolean needsXsiTypeSupport(
      BindingType type, Function<String, BindingType> typeLookup, Set<String> visited) {
    if (!visited.add(type.javaName().qualifiedName())) {
      return false;
    }
    for (BindingField field : type.fields()) {
      if ("choice".equals(field.kind())
          && field.choice() != null
          && "xsiType".equals(field.choice().modelKind())) {
        return true;
      }
      BindingType nestedType = modelType(field.type(), typeLookup);
      if (nestedType != null && needsXsiTypeSupport(nestedType, typeLookup, visited)) {
        return true;
      }
      if ("choice".equals(field.kind()) && field.choice() != null) {
        for (BindingChoiceBranch branch : field.choice().branches()) {
          BindingType branchType = modelType(branch.type(), typeLookup);
          if (branchType != null && needsXsiTypeSupport(branchType, typeLookup, visited)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean needsDefaultedElementSupport(
      BindingType type, Function<String, BindingType> typeLookup, Set<String> visited) {
    if (!visited.add(type.javaName().qualifiedName())) {
      return false;
    }
    for (BindingField field : type.fields()) {
      if ("element".equals(field.kind()) && field.semantics().hasDefault()) {
        return true;
      }
      BindingType nestedType = modelType(field.type(), typeLookup);
      if (nestedType != null && needsDefaultedElementSupport(nestedType, typeLookup, visited)) {
        return true;
      }
    }
    return false;
  }

  private static boolean needsWildcardSupport(
      BindingType type, Function<String, BindingType> typeLookup, Set<String> visited) {
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
          BindingType branchType = modelType(branch.type(), typeLookup);
          if (branchType != null && needsWildcardSupport(branchType, typeLookup, visited)) {
            return true;
          }
        }
      }
      BindingType nestedType = modelType(field.type(), typeLookup);
      if (nestedType != null && needsWildcardSupport(nestedType, typeLookup, visited)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsListType(BindingTypeReference reference) {
    if ("list".equals(reference.kind())) {
      return true;
    }
    if ("union".equals(reference.kind())) {
      return reference.unionMembers().stream().anyMatch(GeneratedReaderStatePlan::containsListType);
    }
    return false;
  }

  private static BindingType modelType(
      BindingTypeReference reference, Function<String, BindingType> typeLookup) {
    if (!"model".equals(reference.kind())) {
      return null;
    }
    return typeLookup.apply(reference.name());
  }
}
