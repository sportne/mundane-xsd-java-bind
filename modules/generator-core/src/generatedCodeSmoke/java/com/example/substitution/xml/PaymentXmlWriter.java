package com.example.substitution.xml;

/** Generated XML writer for {@link com.example.substitution.Payment}. */
public final class PaymentXmlWriter {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "payment");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "amount");

  private PaymentXmlWriter() {}

  public static void write(
      io.github.mundanej.mxjb.runtime.XmlOutput output, com.example.substitution.Payment value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(output, "output");
    java.util.Objects.requireNonNull(value, "value");
    writePayment(output, NAME_1, value);
  }

  private static void writePayment(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlName elementName,
      com.example.substitution.Payment value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    output.startElement(elementName);
    output.startElement(NAME_2);
    output.text(String.valueOf(value.amount()));
    output.endElement(NAME_2);
    output.endElement(elementName);
  }
}
