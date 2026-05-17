package com.example.orders.xml;

/** Generated XML reader for {@link com.example.orders.Order}. */
public final class OrderXmlReader {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "order");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "version");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "id");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_4 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:orders", "note");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_5 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:lines", "line");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_6 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:lines", "sku");

  private OrderXmlReader() {}

  public static com.example.orders.Order read(io.github.mundanej.mxjb.runtime.XmlEventReader input)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    java.util.Objects.requireNonNull(input, "input");
    moveToDocumentContent(input);
    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.START_ELEMENT
        || !NAME_1.equals(input.name())) {
      throw readException(input, "MXJB-GR-001", "Expected root element {urn:orders}order.");
    }
    com.example.orders.Order value = readOrder(input, NAME_1);
    movePastWhitespace(input);
    if (input.kind() != io.github.mundanej.mxjb.runtime.XmlEventKind.END_DOCUMENT) {
      throw readException(input, "MXJB-GR-007", "Unexpected content after root element.");
    }
    return value;
  }

  private static com.example.orders.Order readOrder(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName elementName)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    expectStart(input, elementName);
    for (int index = 0; index < input.attributeCount(); index++) {
      io.github.mundanej.mxjb.runtime.XmlName attributeName = input.attributeName(index);
      if (!NAME_2.equals(attributeName)) {
        throw readException(input, "MXJB-GR-003", "Unexpected XML attribute.");
      }
    }
    String versionText = attribute(input, NAME_2);
    java.util.Optional<String> version =
        versionText == null ? java.util.Optional.empty() : java.util.Optional.of(versionText);
    String id = null;
    java.util.Optional<String> note = java.util.Optional.empty();
    java.util.ArrayList<com.example.lines.Line> lineValues = new java.util.ArrayList<>();
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
      if (NAME_3.equals(input.name())) {
        if (1 < lastElementOrder) {
          throw readException(input, "MXJB-GR-002", "Out-of-order XML element {urn:orders}id.");
        }
        lastElementOrder = Math.max(lastElementOrder, 1);
        if (id != null) {
          throw readException(input, "MXJB-GR-005", "Repeated XML element {urn:orders}id.");
        }
        id = readStringElement(input, NAME_3);
      } else if (NAME_4.equals(input.name())) {
        if (2 < lastElementOrder) {
          throw readException(input, "MXJB-GR-002", "Out-of-order XML element {urn:orders}note.");
        }
        lastElementOrder = Math.max(lastElementOrder, 2);
        if (note.isPresent()) {
          throw readException(input, "MXJB-GR-005", "Repeated XML element {urn:orders}note.");
        }
        note = java.util.Optional.of(readStringElement(input, NAME_4));
      } else if (NAME_5.equals(input.name())) {
        if (3 < lastElementOrder) {
          throw readException(input, "MXJB-GR-002", "Out-of-order XML element {urn:lines}line.");
        }
        lastElementOrder = Math.max(lastElementOrder, 3);
        lineValues.add(readLine(input, NAME_5));
      } else {
        throw readException(input, "MXJB-GR-002", "Unexpected XML element.");
      }
    }
    if (!elementName.equals(input.name())) {
      throw readException(input, "MXJB-GR-007", "Mismatched end element.");
    }
    if (id == null) {
      throw readException(input, "MXJB-GR-004", "Missing required XML element {urn:orders}id.");
    }
    if (lineValues.isEmpty()) {
      throw readException(input, "MXJB-GR-004", "Missing required XML element {urn:lines}line.");
    }
    input.next();
    return new com.example.orders.Order(version, id, note, java.util.List.copyOf(lineValues));
  }

  private static com.example.lines.Line readLine(
      io.github.mundanej.mxjb.runtime.XmlEventReader input,
      io.github.mundanej.mxjb.runtime.XmlName elementName)
      throws io.github.mundanej.mxjb.runtime.XmlReadException {
    expectStart(input, elementName);
    for (int index = 0; index < input.attributeCount(); index++) {
      throw readException(input, "MXJB-GR-003", "Unexpected XML attribute.");
    }
    String sku = null;
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
      if (NAME_6.equals(input.name())) {
        if (1 < lastElementOrder) {
          throw readException(input, "MXJB-GR-002", "Out-of-order XML element {urn:lines}sku.");
        }
        lastElementOrder = Math.max(lastElementOrder, 1);
        if (sku != null) {
          throw readException(input, "MXJB-GR-005", "Repeated XML element {urn:lines}sku.");
        }
        sku = readStringElement(input, NAME_6);
      } else {
        throw readException(input, "MXJB-GR-002", "Unexpected XML element.");
      }
    }
    if (!elementName.equals(input.name())) {
      throw readException(input, "MXJB-GR-007", "Mismatched end element.");
    }
    if (sku == null) {
      throw readException(input, "MXJB-GR-004", "Missing required XML element {urn:lines}sku.");
    }
    input.next();
    return new com.example.lines.Line(sku);
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
}
