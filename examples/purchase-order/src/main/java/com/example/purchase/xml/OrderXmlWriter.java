package com.example.purchase.xml;

/** Generated XML writer for {@link com.example.purchase.Order}. */
public final class OrderXmlWriter {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:purchase", "order");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:purchase", "version");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:purchase", "id");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_4 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:purchase", "note");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_5 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:purchase", "line");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_6 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:purchase", "sku");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_7 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:purchase", "quantity");

  private OrderXmlWriter() {}

  public static void write(
      io.github.mundanej.mxjb.runtime.XmlOutput output, com.example.purchase.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(output, "output");
    java.util.Objects.requireNonNull(value, "value");
    writeOrder(output, NAME_1, value);
  }

  private static void writeOrder(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlName elementName,
      com.example.purchase.Order value)
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
    for (com.example.purchase.Line lineValue : value.line()) {
      writeLine(output, NAME_5, lineValue);
    }
    output.endElement(elementName);
  }

  private static void writeLine(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlName elementName,
      com.example.purchase.Line value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    output.startElement(elementName);
    output.startElement(NAME_6);
    output.text(value.sku());
    output.endElement(NAME_6);
    output.startElement(NAME_7);
    output.text(Integer.toString(value.quantity()));
    output.endElement(NAME_7);
    output.endElement(elementName);
  }
}
