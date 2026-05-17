package com.example.orders.xml;

/** Generated XML writer for {@link com.example.orders.Order}. */
public final class OrderXmlWriter {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "order");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "version");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "id");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_4 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "note");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_5 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:lines", "line");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_6 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:lines", "sku");

  private OrderXmlWriter() {}

  public static void write(
      io.github.mundanej.mxjb.runtime.XmlOutput output, com.example.orders.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(output, "output");
    java.util.Objects.requireNonNull(value, "value");
    writeOrder(output, NAME_1, value);
  }

  private static void writeOrder(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlName elementName,
      com.example.orders.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    output.startElement(elementName);
    if (value.version().isPresent()) {
      String versionValue = value.version().orElseThrow();
      output.attribute(NAME_2, versionValue);
    }
    output.startElement(NAME_3);
    output.text(value.id());
    output.endElement(NAME_3);
    if (value.note().isPresent()) {
      String noteValue = value.note().orElseThrow();
      output.startElement(NAME_4);
      output.text(noteValue);
      output.endElement(NAME_4);
    }
    for (com.example.lines.Line lineValue : value.line()) {
      writeLine(output, NAME_5, lineValue);
    }
    output.endElement(elementName);
  }

  private static void writeLine(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlName elementName,
      com.example.lines.Line value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    output.startElement(elementName);
    output.startElement(NAME_6);
    output.text(value.sku());
    output.endElement(NAME_6);
    output.endElement(elementName);
  }
}
