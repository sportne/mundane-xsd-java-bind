package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;

@FunctionalInterface
interface BindingBranchDiagnostics {
  void report(DiagnosticCode code, String message);
}
