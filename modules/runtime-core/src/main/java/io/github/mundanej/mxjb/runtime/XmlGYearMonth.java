package io.github.mundanej.mxjb.runtime;

/** XML Schema gYearMonth value retaining timezone-presence semantics. */
public record XmlGYearMonth(String lexicalValue) {
  public XmlGYearMonth {
    lexicalValue = XmlDatatypes.requireGYearMonthLexical(lexicalValue);
  }

  @Override
  public String toString() {
    return lexicalValue;
  }
}
