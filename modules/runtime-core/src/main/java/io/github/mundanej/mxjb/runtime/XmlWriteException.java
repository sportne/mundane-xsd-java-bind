package io.github.mundanej.mxjb.runtime;

import java.util.Objects;

/** Checked exception raised when generated XML writers cannot continue. */
@SuppressWarnings("serial")
public final class XmlWriteException extends Exception {
  private final XmlDiagnostic diagnostic;

  public XmlWriteException(XmlDiagnostic diagnostic) {
    this(diagnostic, null);
  }

  public XmlWriteException(XmlDiagnostic diagnostic, Throwable cause) {
    super(Objects.requireNonNull(diagnostic, "diagnostic").message(), cause);
    this.diagnostic = diagnostic;
  }

  public XmlDiagnostic diagnostic() {
    return diagnostic;
  }
}
