package com.example.substitution.xml;

/** Generated XML reader for {@link com.example.substitution.Cardpayment}. */
public final class CardpaymentXmlReader {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "cardPayment");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "amount");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:substitution", "cardLast4");

  private CardpaymentXmlReader() {}

  public static com.example.substitution.Cardpayment read(
      io.github.mundanej.mxjb.runtime.XmlEventReader input)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    java.util.Objects.requireNonNull(input, "input");
    moveToDocumentContent(input);
    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.START_ELEMENT
        || !NAME_1.equals(input.name())) {
      throw readException(
          input, "MXJB-GR-001", "Expected root element {urn:substitution}cardPayment.");
    }
    com.example.substitution.Cardpayment value = readCardpayment(input, NAME_1);
    movePastWhitespace(input);
    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.END_DOCUMENT) {
      throw readException(input, "MXJB-GR-007", "Unexpected content after root element.");
    }
    return value;
  }

  private static com.example.substitution.Cardpayment readCardpayment(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName elementName)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    expectStart(input, elementName);
    for (int index = 0; index < input.attributeCount(); index++) {
      io.github.mundanej.mxjb.runtime.XmlName attributeName = input.attributeName(index);
      throw readException(input, "MXJB-GR-003", "Unexpected XML attribute.");
    }
    java.math.BigDecimal amount = null;
    String cardlast4 = null;
    int lastElementOrder = -1;
    if (!input.next()) {
      throw readException(input, "MXJB-GR-007", "Unexpected end of XML input.");
    }
    while (true) {
      movePastWhitespace(input);
      if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT) {
        break;
      }
      if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.START_ELEMENT) {
        throw readException(input, "MXJB-GR-007", "Expected XML element content.");
      }
      if (NAME_2.equals(input.name())) {
        if (1 < lastElementOrder) {
          throw readException(
              input, "MXJB-GR-002", "Out-of-order XML element {urn:substitution}amount.");
        }
        lastElementOrder = Math.max(lastElementOrder, 1);
        if (amount != null) {
          throw readException(
              input, "MXJB-GR-005", "Repeated XML element {urn:substitution}amount.");
        }
        amount = readDecimalElement(input, NAME_2);
      } else if (NAME_3.equals(input.name())) {
        if (2 < lastElementOrder) {
          throw readException(
              input, "MXJB-GR-002", "Out-of-order XML element {urn:substitution}cardLast4.");
        }
        lastElementOrder = Math.max(lastElementOrder, 2);
        if (cardlast4 != null) {
          throw readException(
              input, "MXJB-GR-005", "Repeated XML element {urn:substitution}cardLast4.");
        }
        cardlast4 = readStringElement(input, NAME_3);
      } else {
        throw readException(input, "MXJB-GR-002", "Unexpected XML element.");
      }
    }
    if (!elementName.equals(input.name())) {
      throw readException(input, "MXJB-GR-007", "Mismatched end element.");
    }
    if (amount == null) {
      throw readException(
          input, "MXJB-GR-004", "Missing required XML element {urn:substitution}amount.");
    }
    if (cardlast4 == null) {
      throw readException(
          input, "MXJB-GR-004", "Missing required XML element {urn:substitution}cardLast4.");
    }
    input.next();
    return new com.example.substitution.Cardpayment(amount, cardlast4);
  }

  private static void moveToDocumentContent(io.github.mundanej.mxjb.runtime.XmlEventReader input)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    while (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.START_DOCUMENT
        || isWhitespace(input)) {
      if (!input.next()) {
        return;
      }
    }
  }

  private static void movePastWhitespace(io.github.mundanej.mxjb.runtime.XmlEventReader input)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    while (isWhitespace(input)) {
      if (!input.next()) {
        return;
      }
    }
  }

  private static boolean isWhitespace(io.github.mundanej.mxjb.runtime.XmlEventReader input) {
    return input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.TEXT
        && input.text().isBlank();
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

  private static String readTextElement(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    expectStart(input, name);
    StringBuilder text = new StringBuilder();
    while (input.next()) {
      if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.TEXT) {
        text.append(input.text());
      } else if (input.kind() == io.github.mundanej.mxjb.runtime.XmlEventKind.END_ELEMENT) {
        if (!name.equals(input.name())) {
          throw readException(input, "MXJB-GR-007", "Mismatched text element end.");
        }
        input.next();
        return text.toString();
      } else {
        throw readException(input, "MXJB-GR-007", "Expected XML text content.");
      }
    }
    throw readException(input, "MXJB-GR-007", "Unclosed XML text element.");
  }

  private static String readStringElement(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    return readTextElement(input, name);
  }

  private static Boolean readBooleanElement(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    return parseBoolean(readTextElement(input, name), input.location());
  }

  private static Integer readIntElement(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    return parseInt(readTextElement(input, name), input.location());
  }

  private static java.math.BigInteger readIntegerElement(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    return parseInteger(readTextElement(input, name), input.location());
  }

  private static Long readLongElement(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    return parseLong(readTextElement(input, name), input.location());
  }

  private static java.math.BigDecimal readDecimalElement(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName name)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    return parseDecimal(readTextElement(input, name), input.location());
  }

  private static Boolean parseBoolean(
      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    return switch (value.trim()) {
      case "true", "1" -> Boolean.TRUE;
      case "false", "0" -> Boolean.FALSE;
      default -> throw readException(location, "MXJB-GR-006", "Invalid boolean value.");
    };
  }

  private static Integer parseInt(
      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException exception) {
      throw readException(location, "MXJB-GR-006", "Invalid int value.", exception);
    }
  }

  private static java.math.BigInteger parseInteger(
      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    try {
      return new java.math.BigInteger(value.trim());
    } catch (NumberFormatException exception) {
      throw readException(location, "MXJB-GR-006", "Invalid integer value.", exception);
    }
  }

  private static Long parseLong(String value, io.github.mundanej.mxjb.runtime.XmlLocation location)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    try {
      return Long.valueOf(value.trim());
    } catch (NumberFormatException exception) {
      throw readException(location, "MXJB-GR-006", "Invalid long value.", exception);
    }
  }

  private static java.math.BigDecimal parseDecimal(
      String value, io.github.mundanej.mxjb.runtime.XmlLocation location)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    try {
      return new java.math.BigDecimal(value.trim());
    } catch (NumberFormatException exception) {
      throw readException(location, "MXJB-GR-006", "Invalid decimal value.", exception);
    }
  }

  private static io.github.mundanej.mxjb.runtime.XmlReadException readException(
      io.github.mundanej.mxjb.runtime.XmlEventReader input, String code, String message) {
    return readException(input.location(), code, message);
  }

  private static io.github.mundanej.mxjb.runtime.XmlReadException readException(
      io.github.mundanej.mxjb.runtime.XmlLocation location, String code, String message) {
    return new io.github.mundanej.mxjb.runtime.XmlReadException(
        new io.github.mundanej.mxjb.runtime.XmlDiagnostic(
            io.github.mundanej.mxjb.runtime.XmlDiagnosticSeverity.ERROR, code, message, location));
  }

  private static io.github.mundanej.mxjb.runtime.XmlReadException readException(
      io.github.mundanej.mxjb.runtime.XmlLocation location,
      String code,
      String message,
      Throwable cause) {
    return new io.github.mundanej.mxjb.runtime.XmlReadException(
        new io.github.mundanej.mxjb.runtime.XmlDiagnostic(
            io.github.mundanej.mxjb.runtime.XmlDiagnosticSeverity.ERROR, code, message, location),
        cause);
  }
}
