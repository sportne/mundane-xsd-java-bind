package io.github.mundanej.mxjb.runtime;

/** XML output abstraction consumed by generated writers. */
public interface XmlOutput {
  void startDocument() throws XmlWriteException;

  void endDocument() throws XmlWriteException;

  void startElement(XmlName name) throws XmlWriteException;

  void attribute(XmlName name, String value) throws XmlWriteException;

  void text(String value) throws XmlWriteException;

  default String qNameText(XmlQName value) throws XmlWriteException {
    return value.lexicalName();
  }

  void endElement(XmlName name) throws XmlWriteException;

  void flush() throws XmlWriteException;
}
