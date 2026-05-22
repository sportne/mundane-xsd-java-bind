package io.github.mundanej.mxjb.runtime;

/** XML Schema gMonth value retaining timezone-presence semantics. */
public record XmlGMonth(String lexicalValue) {
  public XmlGMonth {
    lexicalValue = XmlDatatypes.requireGMonthLexical(lexicalValue);
  }

  @Override
  public String toString() {
    return lexicalValue;
  }
}
