package io.github.mundanej.mxjb.runtime;

/** XML Schema date value retaining timezone-presence semantics. */
public record XmlDate(String lexicalValue) {
  public XmlDate {
    lexicalValue = XmlDatatypes.requireDateLexical(lexicalValue);
  }

  @Override
  public String toString() {
    return lexicalValue;
  }
}
