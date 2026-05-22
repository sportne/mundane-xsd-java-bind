package io.github.mundanej.mxjb.runtime;

/** XML Schema gMonthDay value retaining timezone-presence semantics. */
public record XmlGMonthDay(String lexicalValue) {
  public XmlGMonthDay {
    lexicalValue = XmlDatatypes.requireGMonthDayLexical(lexicalValue);
  }

  @Override
  public String toString() {
    return lexicalValue;
  }
}
