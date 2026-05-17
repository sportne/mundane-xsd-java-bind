package com.example.facet.xml;

/** Generated XML writer for {@link com.example.facet.Order}. */
public final class OrderXmlWriter {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:facet", "order");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:facet", "code");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:facet", "priority");

  private OrderXmlWriter() {}

  public static void write(
      io.github.mundanej.mxjb.runtime.XmlOutput output, com.example.facet.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(output, "output");
    java.util.Objects.requireNonNull(value, "value");
    output.startElement(NAME_1);
    output.startElement(NAME_2);
    output.text(value.code());
    output.endElement(NAME_2);
    output.startElement(NAME_3);
    output.text(String.valueOf(value.priority()));
    output.endElement(NAME_3);
    output.endElement(NAME_1);
  }
}
