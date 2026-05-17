package com.example.composed.xml;

/** Generated XML writer for {@link com.example.composed.Order}. */
public final class OrderXmlWriter {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:composed", "order");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:composed", "version");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:composed", "id");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_4 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:composed", "total");

  private OrderXmlWriter() {}

  public static void write(
      io.github.mundanej.mxjb.runtime.XmlOutput output, com.example.composed.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(output, "output");
    java.util.Objects.requireNonNull(value, "value");
    output.startElement(NAME_1);
    output.attribute(NAME_2, value.version());
    output.startElement(NAME_3);
    output.text(value.id());
    output.endElement(NAME_3);
    output.startElement(NAME_4);
    output.text(String.valueOf(value.total()));
    output.endElement(NAME_4);
    output.endElement(NAME_1);
  }
}
