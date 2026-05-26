package io.github.mundanej.mxjb.runtime;

import java.util.Base64;
import java.util.HexFormat;

final class XmlDatatypeBinary {
  private XmlDatatypeBinary() {}

  static XmlBinary parseHexBinary(String value) {
    String collapsed = XmlDatatypeLexical.collapseWhitespace(value);
    if (collapsed.length() % 2 != 0 || !collapsed.matches("[0-9A-Fa-f]*")) {
      throw new IllegalArgumentException("Invalid hexBinary value.");
    }
    return new XmlBinary(HexFormat.of().parseHex(collapsed));
  }

  static XmlBinary parseBase64Binary(String value) {
    String normalized = XmlDatatypeLexical.replaceWhitespace(value).replace(" ", "");
    return new XmlBinary(Base64.getDecoder().decode(normalized));
  }
}
