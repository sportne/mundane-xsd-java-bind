package io.github.mundanej.mxjb.runtime.jdkxml;

import io.github.mundanej.mxjb.runtime.XmlDiagnostic;
import io.github.mundanej.mxjb.runtime.XmlDiagnosticSeverity;
import io.github.mundanej.mxjb.runtime.XmlLocation;
import io.github.mundanej.mxjb.runtime.XmlName;
import io.github.mundanej.mxjb.runtime.XmlOutput;
import io.github.mundanej.mxjb.runtime.XmlWriteException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

final class StaxXmlOutput implements XmlOutput {
  private final XMLStreamWriter writer;
  private final Map<String, String> prefixesByNamespace = new HashMap<>();
  private final Map<String, String> declaredPrefixesByNamespace = new HashMap<>();
  private int nextPrefixNumber = 1;

  StaxXmlOutput(XMLStreamWriter writer) {
    this.writer = writer;
  }

  @Override
  public void startDocument() throws XmlWriteException {
    try {
      writer.writeStartDocument();
    } catch (XMLStreamException exception) {
      throw writeException(
          "MXJB-JDKXML-W-001", "JDK XML writer failed to start document.", exception);
    }
  }

  @Override
  public void endDocument() throws XmlWriteException {
    try {
      writer.writeEndDocument();
    } catch (XMLStreamException exception) {
      throw writeException(
          "MXJB-JDKXML-W-002", "JDK XML writer failed to end document.", exception);
    }
  }

  @Override
  public void startElement(XmlName name) throws XmlWriteException {
    try {
      if (name.namespaceUri().isEmpty()) {
        writer.writeStartElement(name.localName());
      } else {
        String prefix = prefixFor(name.namespaceUri());
        writer.writeStartElement(prefix, name.localName(), name.namespaceUri());
        declareNamespace(prefix, name.namespaceUri());
      }
    } catch (XMLStreamException exception) {
      throw writeException(
          "MXJB-JDKXML-W-003", "JDK XML writer failed to start element.", exception);
    }
  }

  @Override
  public void attribute(XmlName name, String value) throws XmlWriteException {
    try {
      if (name.namespaceUri().isEmpty()) {
        writer.writeAttribute(name.localName(), value);
      } else {
        String prefix = prefixFor(name.namespaceUri());
        declareNamespace(prefix, name.namespaceUri());
        writer.writeAttribute(prefix, name.namespaceUri(), name.localName(), value);
      }
    } catch (XMLStreamException exception) {
      throw writeException(
          "MXJB-JDKXML-W-004", "JDK XML writer failed to write attribute.", exception);
    }
  }

  @Override
  public void text(String value) throws XmlWriteException {
    try {
      writer.writeCharacters(value);
    } catch (XMLStreamException exception) {
      throw writeException("MXJB-JDKXML-W-005", "JDK XML writer failed to write text.", exception);
    }
  }

  @Override
  public void endElement(XmlName name) throws XmlWriteException {
    try {
      writer.writeEndElement();
    } catch (XMLStreamException exception) {
      throw writeException("MXJB-JDKXML-W-006", "JDK XML writer failed to end element.", exception);
    }
  }

  @Override
  public void flush() throws XmlWriteException {
    try {
      writer.flush();
    } catch (XMLStreamException exception) {
      throw writeException("MXJB-JDKXML-W-007", "JDK XML writer failed to flush.", exception);
    }
  }

  private String prefixFor(String namespaceUri) throws XMLStreamException {
    String existingPrefix = writer.getPrefix(namespaceUri);
    if (existingPrefix != null && !existingPrefix.isBlank()) {
      prefixesByNamespace.putIfAbsent(namespaceUri, existingPrefix);
      return existingPrefix;
    }
    String prefix =
        prefixesByNamespace.computeIfAbsent(namespaceUri, ignored -> "ns" + nextPrefixNumber++);
    writer.setPrefix(prefix, namespaceUri);
    return prefix;
  }

  private void declareNamespace(String prefix, String namespaceUri) throws XMLStreamException {
    if (!prefix.equals(declaredPrefixesByNamespace.get(namespaceUri))) {
      writer.writeNamespace(prefix, namespaceUri);
      declaredPrefixesByNamespace.put(namespaceUri, prefix);
    }
  }

  private static XmlWriteException writeException(
      String code, String message, XMLStreamException cause) {
    return new XmlWriteException(
        new XmlDiagnostic(XmlDiagnosticSeverity.ERROR, code, message, XmlLocation.UNKNOWN), cause);
  }
}
