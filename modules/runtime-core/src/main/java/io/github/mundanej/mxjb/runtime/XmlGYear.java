package io.github.mundanej.mxjb.runtime;

/** XML Schema gYear value retaining timezone-presence semantics. */
public record XmlGYear(String lexicalValue) {
  public XmlGYear {
    lexicalValue = XmlDatatypes.requireGYearLexical(lexicalValue);
  }

  @Override
  public String toString() {
    return lexicalValue;
  }
}
