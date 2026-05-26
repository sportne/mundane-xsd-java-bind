package io.github.mundanej.mxjb.runtime;

import java.util.ArrayList;
import java.util.List;

final class XmlDatatypeLists {
  private XmlDatatypeLists() {}

  interface ItemParser {
    Object parse(String itemType, String token) throws XmlReadException;
  }

  static List<?> parseValues(String itemType, String value, ItemParser parser)
      throws XmlReadException {
    String collapsed = XmlDatatypeLexical.collapseWhitespace(value);
    if (collapsed.isEmpty()) {
      return List.of();
    }
    ArrayList<Object> values = new ArrayList<>();
    int start = 0;
    while (start <= collapsed.length()) {
      int end = collapsed.indexOf(' ', start);
      String token = end < 0 ? collapsed.substring(start) : collapsed.substring(start, end);
      values.add(parser.parse(itemType, token));
      if (end < 0) {
        break;
      }
      start = end + 1;
    }
    return List.copyOf(values);
  }

  static List<String> tokenList(String value, String itemType, ItemParser parser)
      throws XmlReadException {
    String collapsed = XmlDatatypeLexical.collapseWhitespace(value);
    if (collapsed.isEmpty()) {
      throw new IllegalArgumentException(itemType + " list must contain at least one item.");
    }
    ArrayList<String> values = new ArrayList<>();
    int start = 0;
    while (start <= collapsed.length()) {
      int end = collapsed.indexOf(' ', start);
      String token = end < 0 ? collapsed.substring(start) : collapsed.substring(start, end);
      values.add((String) parser.parse(itemType, token));
      if (end < 0) {
        break;
      }
      start = end + 1;
    }
    return List.copyOf(values);
  }
}
