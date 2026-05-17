package com.example.choice.xml;

/** Generated XML writer for {@link com.example.choice.Order}. */
public final class OrderXmlWriter {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:choice", "order");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:choice", "id");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:choice", "domestic");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_4 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:choice", "international");

  private OrderXmlWriter() {}

  public static void write(
      io.github.mundanej.mxjb.runtime.XmlOutput output, com.example.choice.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(output, "output");
    java.util.Objects.requireNonNull(value, "value");
    output.startElement(NAME_1);
    output.startElement(NAME_2);
    output.text(value.id());
    output.endElement(NAME_2);
    if (value.orderChoice().isPresent()) {
      com.example.choice.OrderChoice orderChoiceValue = value.orderChoice().orElseThrow();
      if (orderChoiceValue instanceof com.example.choice.DomesticChoice branch) {
        output.startElement(NAME_3);
        output.text(branch.value());
        output.endElement(NAME_3);
      } else if (orderChoiceValue instanceof com.example.choice.InternationalChoice branch) {
        output.startElement(NAME_4);
        output.text(branch.value());
        output.endElement(NAME_4);
      } else {
        throw new io.github.mundanej.mxjb.runtime.XmlWriteException(
            new io.github.mundanej.mxjb.runtime.XmlDiagnostic(
                io.github.mundanej.mxjb.runtime.XmlDiagnosticSeverity.ERROR,
                "MXJB-GW-001",
                "Unsupported XML choice branch.",
                io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN));
      }
    }
    output.endElement(NAME_1);
  }
}
