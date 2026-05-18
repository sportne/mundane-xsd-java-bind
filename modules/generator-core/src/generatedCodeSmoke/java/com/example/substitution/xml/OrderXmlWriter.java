package com.example.substitution.xml;

/** Generated XML writer for {@link com.example.substitution.Order}. */
public final class OrderXmlWriter {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "order");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "id");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "payment");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_4 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "amount");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_5 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "cardPayment");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_6 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "cardLast4");

  private OrderXmlWriter() {}

  public static void write(
      io.github.mundanej.mxjb.runtime.XmlOutput output, com.example.substitution.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(output, "output");
    java.util.Objects.requireNonNull(value, "value");
    writeOrder(output, NAME_1, value);
  }

  private static void writeOrder(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlName elementName,
      com.example.substitution.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    output.startElement(elementName);
    output.startElement(NAME_2);
    output.text(value.id());
    output.endElement(NAME_2);
    if (value.payment().isPresent()) {
      com.example.substitution.PaymentSubstitution paymentValue = value.payment().orElseThrow();
      if (paymentValue instanceof com.example.substitution.PaymentSubstitutionBranch branch) {
        writePayment(output, NAME_3, branch.value());
      } else if (paymentValue
          instanceof com.example.substitution.CardpaymentSubstitutionBranch branch) {
        writeCardpayment(output, NAME_5, branch.value());
      } else {
        throw new io.github.mundanej.mxjb.runtime.XmlWriteException(
            new io.github.mundanej.mxjb.runtime.XmlDiagnostic(
                io.github.mundanej.mxjb.runtime.XmlDiagnosticSeverity.ERROR,
                "MXJB-GW-001",
                "Unsupported XML substitution branch.",
                io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN));
      }
    }
    output.endElement(elementName);
  }

  private static void writePayment(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlName elementName,
      com.example.substitution.Payment value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    output.startElement(elementName);
    output.startElement(NAME_4);
    output.text(String.valueOf(value.amount()));
    output.endElement(NAME_4);
    output.endElement(elementName);
  }

  private static void writeCardpayment(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlName elementName,
      com.example.substitution.Cardpayment value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    output.startElement(elementName);
    output.startElement(NAME_4);
    output.text(String.valueOf(value.amount()));
    output.endElement(NAME_4);
    output.startElement(NAME_6);
    output.text(value.cardlast4());
    output.endElement(NAME_6);
    output.endElement(elementName);
  }
}
