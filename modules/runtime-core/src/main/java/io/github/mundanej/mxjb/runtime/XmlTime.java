package io.github.mundanej.mxjb.runtime;

/** XML Schema time value retaining timezone-presence semantics. */
public record XmlTime(String lexicalValue) {
  public XmlTime {
    lexicalValue = XmlDatatypes.requireTimeLexical(lexicalValue);
  }

  @Override
  public String toString() {
    return lexicalValue;
  }
}
