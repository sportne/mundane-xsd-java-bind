package io.github.mundanej.mxjb.runtime;

import java.util.regex.Pattern;

final class XmlDatatypeLexical {
  static final Pattern NAME = Pattern.compile("[:A-Z_a-z][-.:A-Z_a-z0-9]*");
  static final Pattern NC_NAME = Pattern.compile("[A-Z_a-z][-A-Z_a-z0-9.]*");
  static final Pattern NMTOKEN = Pattern.compile("[-.:A-Z_a-z0-9]+");
  static final Pattern LANGUAGE = Pattern.compile("[A-Za-z]{1,8}(-[A-Za-z0-9]{1,8})*");
  static final Pattern FLOATING_POINT =
      Pattern.compile("[+-]?((([0-9]+)(\\.[0-9]*)?)|(\\.[0-9]+))([eE][+-]?[0-9]+)?");

  private XmlDatatypeLexical() {}

  static String replaceWhitespace(String value) {
    return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
  }

  static String collapseWhitespace(String value) {
    return replaceWhitespace(value).trim().replaceAll(" +", " ");
  }

  static String requirePattern(String type, String value, Pattern pattern) {
    if (!pattern.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return value;
  }
}
