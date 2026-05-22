package io.github.mundanej.mxjb.runtime;

/** Pull-style XML event reader consumed by generated readers. */
public interface XmlEventReader {
  XmlEventKind kind();

  XmlName name();

  String text();

  int attributeCount();

  XmlName attributeName(int index);

  String attributeValue(int index);

  XmlLocation location();

  default String namespaceUriForPrefix(String prefix) {
    return null;
  }

  boolean next() throws XmlReadException;
}
