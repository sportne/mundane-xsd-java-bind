package com.example.composed.xml;

/** Generated XML reader for {@link com.example.composed.Order}. */
public final class OrderXmlReader {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:composed", "order");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:composed", "version");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:composed", "id");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_4 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:composed", "total");

  private OrderXmlReader() {}

  public static com.example.composed.Order read(
      io.github.mundanej.mxjb.runtime.XmlEventReader input)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    java.util.Objects.requireNonNull(input, "input");
    if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.START_DOCUMENT) {
      input.next();
    }
    expectStart(input, NAME_1);
    String version = requiredAttribute(input, NAME_2);
    input.next();
    String id = readStringElement(input, NAME_3);
    java.math.BigDecimal total = new java.math.BigDecimal(readStringElement(input, NAME_4));
    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT
        || !NAME_1.equals(input.name())) {
      throw readException(input, "MXJB-GR-007", "Mismatched end element.");
    }
    input.next();
    return new com.example.composed.Order(version, id, total);
  }

  private static String requiredAttribute(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    for (int index = 0; index < input.attributeCount(); index++) {
      if (name.equals(input.attributeName(index))) {
        return input.attributeValue(index);
      }
    }
    throw readException(input, "MXJB-GR-004", "Missing required attribute.");
  }

  private static String readStringElement(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    expectStart(input, name);
    StringBuilder text = new StringBuilder();
    while (input.next()) {
      if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.TEXT) {
        text.append(input.text());
      } else if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT) {
        input.next();
        return text.toString();
      } else {
        throw readException(input, "MXJB-GR-007", "Expected XML text content.");
      }
    }
    throw readException(input, "MXJB-GR-007", "Unclosed XML text element.");
  }

  private static void expectStart(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.START_ELEMENT
        || !name.equals(input.name())) {
      throw readException(input, "MXJB-GR-002", "Expected XML element.");
    }
  }

  private static io.github.mundanej.mxjb.runtime.XmlReadException readException(
      io.github.mundanej.mxjb.runtime.XmlEventReader input, String code, String message) {
    return new io.github.mundanej.mxjb.runtime.XmlReadException(
        new io.github.mundanej.mxjb.runtime.XmlDiagnostic(
            io.github.mundanej.mxjb.runtime.XmlDiagnosticSeverity.ERROR,
            code,
            message,
            input.location()));
  }
}
