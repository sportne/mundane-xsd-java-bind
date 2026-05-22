package io.github.mundanej.mxjb.runtime;

/** XML Schema gDay value retaining timezone-presence semantics. */
public record XmlGDay(String lexicalValue) {
  public XmlGDay {
    lexicalValue = XmlDatatypes.requireGDayLexical(lexicalValue);
  }

  @Override
  public String toString() {
    return lexicalValue;
  }
}
