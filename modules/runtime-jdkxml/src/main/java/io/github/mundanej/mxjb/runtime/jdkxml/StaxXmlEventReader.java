package io.github.mundanej.mxjb.runtime.jdkxml;

import io.github.mundanej.mxjb.runtime.XmlDiagnostic;
import io.github.mundanej.mxjb.runtime.XmlDiagnosticSeverity;
import io.github.mundanej.mxjb.runtime.XmlEventKind;
import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.XmlLocation;
import io.github.mundanej.mxjb.runtime.XmlName;
import io.github.mundanej.mxjb.runtime.XmlReadException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

final class StaxXmlEventReader implements XmlEventReader {
  private final XMLStreamReader reader;
  private XmlEventKind kind;

  StaxXmlEventReader(XMLStreamReader reader) {
    this.reader = reader;
    this.kind = mapEvent(reader.getEventType());
  }

  @Override
  public XmlEventKind kind() {
    return kind;
  }

  @Override
  public XmlName name() {
    if (kind == XmlEventKind.START_ELEMENT || kind == XmlEventKind.END_ELEMENT) {
      return xmlName(reader.getNamespaceURI(), reader.getLocalName());
    }
    return null;
  }

  @Override
  public String text() {
    if (kind == XmlEventKind.TEXT) {
      return reader.getText();
    }
    return "";
  }

  @Override
  public int attributeCount() {
    if (kind == XmlEventKind.START_ELEMENT) {
      return reader.getAttributeCount();
    }
    return 0;
  }

  @Override
  public XmlName attributeName(int index) {
    return xmlName(reader.getAttributeNamespace(index), reader.getAttributeLocalName(index));
  }

  @Override
  public String attributeValue(int index) {
    return reader.getAttributeValue(index);
  }

  @Override
  public XmlLocation location() {
    return xmlLocation(reader.getLocation());
  }

  @Override
  public boolean next() throws XmlReadException {
    try {
      while (reader.hasNext()) {
        kind = mapEvent(reader.next());
        if (kind != null) {
          return true;
        }
      }
      kind = XmlEventKind.END_DOCUMENT;
      return false;
    } catch (XMLStreamException exception) {
      throw readException("MXJB-JDKXML-R-001", "JDK XML reader failed.", exception);
    }
  }

  private static XmlEventKind mapEvent(int eventType) {
    return switch (eventType) {
      case XMLStreamConstants.START_DOCUMENT -> XmlEventKind.START_DOCUMENT;
      case XMLStreamConstants.START_ELEMENT -> XmlEventKind.START_ELEMENT;
      case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA, XMLStreamConstants.SPACE ->
          XmlEventKind.TEXT;
      case XMLStreamConstants.END_ELEMENT -> XmlEventKind.END_ELEMENT;
      case XMLStreamConstants.END_DOCUMENT -> XmlEventKind.END_DOCUMENT;
      default -> null;
    };
  }

  private XmlReadException readException(String code, String message, XMLStreamException cause) {
    return new XmlReadException(
        new XmlDiagnostic(
            XmlDiagnosticSeverity.ERROR, code, message, xmlLocation(cause.getLocation())),
        cause);
  }

  private static XmlName xmlName(String namespaceUri, String localName) {
    return new XmlName(namespaceUri == null ? "" : namespaceUri, localName);
  }

  private static XmlLocation xmlLocation(Location location) {
    if (location == null) {
      return XmlLocation.UNKNOWN;
    }
    String systemId = location.getSystemId() == null ? "" : location.getSystemId();
    int lineNumber = normalizeCoordinate(location.getLineNumber());
    int columnNumber = normalizeCoordinate(location.getColumnNumber());
    return new XmlLocation(systemId, lineNumber, columnNumber);
  }

  private static int normalizeCoordinate(int value) {
    return value > 0 ? value : -1;
  }
}
