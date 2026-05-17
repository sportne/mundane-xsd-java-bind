package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import java.nio.file.Path;
import java.util.Objects;

/** Deterministic generated Java source unit. */
public record GeneratedJavaSource(BindingJavaName typeName, Path relativePath, String sourceText) {
  public GeneratedJavaSource {
    Objects.requireNonNull(typeName, "typeName");
    Objects.requireNonNull(relativePath, "relativePath");
    Objects.requireNonNull(sourceText, "sourceText");
  }
}
