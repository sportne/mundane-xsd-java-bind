package com.example.semantic.xml;

/** Generated XML reader for {@link com.example.semantic.Order}. */
public final class OrderXmlReader {
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

  private OrderXmlReader() {}

  public static com.example.semantic.Order read(
      io.github.mundanej.mxjb.runtime.XmlEventReader input)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    java.util.Objects.requireNonNull(input, "input");
    if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.START_DOCUMENT) {
      input.next();
    }
    expectStart(input, NAME_1);
    String statusText = attribute(input, NAME_2);
    String versionText = attribute(input, NAME_3);
    String status = statusText == null ? "NEW" : statusText;
    String version = versionText == null ? "1" : versionText;
    if (!"1".equals(version)) {
      throw readException(input, "MXJB-GR-008", "XML value does not match fixed value.");
    }
    input.next();
    java.util.Optional<String> code =
        readNilElement(input, NAME_4)
            ? java.util.Optional.empty()
            : java.util.Optional.of(readStringElement(input, NAME_4));
    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT
        || !NAME_1.equals(input.name())) {
      throw readException(input, "MXJB-GR-007", "Mismatched end element.");
    }
    input.next();
    return new com.example.semantic.Order(status, version, code);
  }

  private static boolean readNilElement(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    expectStart(input, name);
    String nil = attribute(input, NAME_5);
    if (!"true".equals(nil) && !"1".equals(nil)) {
      return false;
    }
    if (!input.next()) {
      throw readException(input, "MXJB-GR-007", "Unexpected end of XML input.");
    }
    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT
        || !name.equals(input.name())) {
      throw readException(input, "MXJB-GR-009", "xsi:nil element must be empty.");
    }
    input.next();
    return true;
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

  private static String attribute(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name) {
    for (int index = 0; index < input.attributeCount(); index++) {
      if (name.equals(input.attributeName(index))) {
        return input.attributeValue(index);
      }
    }
    return null;
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
