package com.example.substitution.xml;

/** Generated XML writer for {@link com.example.substitution.Cardpayment}. */
public final class CardpaymentXmlWriter {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "cardPayment");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "amount");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "cardLast4");

  private CardpaymentXmlWriter() {}

  public static void write(
      io.github.mundanej.mxjb.runtime.XmlOutput output, com.example.substitution.Cardpayment value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(output, "output");
    java.util.Objects.requireNonNull(value, "value");
    writeCardpayment(output, NAME_1, value);
  }

  private static void writeCardpayment(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlName elementName,
      com.example.substitution.Cardpayment value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    output.startElement(elementName);
    output.startElement(NAME_2);
    output.text(String.valueOf(value.amount()));
    output.endElement(NAME_2);
    output.startElement(NAME_3);
    output.text(value.cardlast4());
    output.endElement(NAME_3);
    output.endElement(elementName);
  }
}
