package io.github.mundanej.mxjb.runtime;

/** XML Schema duration value retained as a validated lexical duration. */
public record XmlDuration(String lexicalValue) {
  public XmlDuration {
    lexicalValue = XmlDatatypes.requireDurationLexical(lexicalValue);
  }

  @Override
  public String toString() {
    return lexicalValue;
  }
}
