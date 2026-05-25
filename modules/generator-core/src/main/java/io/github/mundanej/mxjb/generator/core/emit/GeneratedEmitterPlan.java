package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import io.github.mundanej.mxjb.generator.core.bind.BindingRootElement;
import io.github.mundanej.mxjb.generator.core.bind.BindingType;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

record GeneratedEmitterPlan(
    GeneratedEmitterKind kind,
    BindingRootElement root,
    BindingType rootType,
    BindingJavaName sourceName,
    Path relativePath,
    String rootHelperName,
    List<GeneratedEmitterFieldPlan> fieldPlans) {
  GeneratedEmitterPlan {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(root, "root");
    Objects.requireNonNull(rootType, "rootType");
    Objects.requireNonNull(sourceName, "sourceName");
    Objects.requireNonNull(relativePath, "relativePath");
    Objects.requireNonNull(rootHelperName, "rootHelperName");
    fieldPlans = List.copyOf(fieldPlans);
  }

  static GeneratedEmitterPlan reader(BindingRootElement root, BindingType rootType) {
    return of(GeneratedEmitterKind.READER, root, rootType);
  }

  static GeneratedEmitterPlan writer(BindingRootElement root, BindingType rootType) {
    return of(GeneratedEmitterKind.WRITER, root, rootType);
  }

  static GeneratedEmitterPlan validator(BindingRootElement root, BindingType rootType) {
    return of(GeneratedEmitterKind.VALIDATOR, root, rootType);
  }

  static BindingJavaName sourceName(GeneratedEmitterKind kind, BindingJavaName modelName) {
    return new BindingJavaName(
        modelName.packageName() + ".xml", modelName.simpleName() + kind.sourceSuffix());
  }

  private static GeneratedEmitterPlan of(
      GeneratedEmitterKind kind, BindingRootElement root, BindingType rootType) {
    BindingJavaName sourceName = sourceName(kind, rootType.javaName());
    return new GeneratedEmitterPlan(
        kind,
        root,
        rootType,
        sourceName,
        relativePath(sourceName),
        kind.helperPrefix() + rootType.javaName().simpleName(),
        rootType.fields().stream()
            .map(field -> new GeneratedEmitterFieldPlan(rootType.javaName(), field))
            .toList());
  }

  private static Path relativePath(BindingJavaName sourceName) {
    return Path.of(sourceName.packageName().replace('.', '/'), sourceName.simpleName() + ".java");
  }
}
