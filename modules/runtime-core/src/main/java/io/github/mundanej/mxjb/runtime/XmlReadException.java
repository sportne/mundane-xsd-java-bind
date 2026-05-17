package io.github.mundanej.mxjb.runtime;

import java.util.Objects;

/** Checked exception raised when generated XML readers cannot continue. */
@SuppressWarnings("serial")
public final class XmlReadException extends Exception {
  private final XmlDiagnostic diagnostic;

  public XmlReadException(XmlDiagnostic diagnostic) {
    this(diagnostic, null);
  }

  public XmlReadException(XmlDiagnostic diagnostic, Throwable cause) {
    super(Objects.requireNonNull(diagnostic, "diagnostic").message(), cause);
    this.diagnostic = diagnostic;
  }

  public XmlDiagnostic diagnostic() {
    return diagnostic;
  }
}
