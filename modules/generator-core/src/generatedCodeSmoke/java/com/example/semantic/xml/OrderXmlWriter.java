package com.example.semantic.xml;

/** Generated XML writer for {@link com.example.semantic.Order}. */
public final class OrderXmlWriter {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:semantic", "order");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:semantic", "status");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:semantic", "version");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_4 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:semantic", "code");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_5 =
      new io.github.mundanej.mxjb.runtime.XmlName(
          "http://www.w3.org/2001/XMLSchema-instance", "nil");

  private OrderXmlWriter() {}

  public static void write(
      io.github.mundanej.mxjb.runtime.XmlOutput output, com.example.semantic.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(output, "output");
    java.util.Objects.requireNonNull(value, "value");
    output.startElement(NAME_1);
    output.attribute(NAME_2, value.status());
    output.attribute(NAME_3, value.version());
    if (value.code().isPresent()) {
      output.startElement(NAME_4);
      output.text(value.code().orElseThrow());
      output.endElement(NAME_4);
    } else {
      output.startElement(NAME_4);
      output.attribute(NAME_5, "true");
      output.endElement(NAME_4);
    }
    output.endElement(NAME_1);
  }
}
