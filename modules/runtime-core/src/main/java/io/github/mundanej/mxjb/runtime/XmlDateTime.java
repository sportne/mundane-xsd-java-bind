package io.github.mundanej.mxjb.runtime;

/** XML Schema dateTime value retaining timezone-presence semantics. */
public record XmlDateTime(String lexicalValue) {
  public XmlDateTime {
    lexicalValue = XmlDatatypes.requireDateTimeLexical(lexicalValue);
  }

  @Override
  public String toString() {
    return lexicalValue;
  }
}
