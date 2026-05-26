package io.github.mundanej.mxjb.runtime;

import java.math.BigInteger;

final class XmlDatatypeNumeric {
  private XmlDatatypeNumeric() {}

  static Boolean parseBoolean(String value) {
    return switch (XmlDatatypeLexical.collapseWhitespace(value)) {
      case "true", "1" -> Boolean.TRUE;
      case "false", "0" -> Boolean.FALSE;
      default -> throw new IllegalArgumentException("Invalid boolean value.");
    };
  }

  static Float parseFloat(String value) {
    String collapsed = XmlDatatypeLexical.collapseWhitespace(value);
    return switch (collapsed) {
      case "INF" -> Float.POSITIVE_INFINITY;
      case "-INF" -> Float.NEGATIVE_INFINITY;
      case "NaN" -> Float.NaN;
      default -> {
        if (!XmlDatatypeLexical.FLOATING_POINT.matcher(collapsed).matches()) {
          throw new IllegalArgumentException("Invalid float lexical value.");
        }
        yield Float.valueOf(collapsed);
      }
    };
  }

  static Double parseDouble(String value) {
    String collapsed = XmlDatatypeLexical.collapseWhitespace(value);
    return switch (collapsed) {
      case "INF" -> Double.POSITIVE_INFINITY;
      case "-INF" -> Double.NEGATIVE_INFINITY;
      case "NaN" -> Double.NaN;
      default -> {
        if (!XmlDatatypeLexical.FLOATING_POINT.matcher(collapsed).matches()) {
          throw new IllegalArgumentException("Invalid double lexical value.");
        }
        yield Double.valueOf(collapsed);
      }
    };
  }

  static String formatFloat(Float value) {
    if (value.isNaN()) {
      return "NaN";
    }
    if (value == Float.POSITIVE_INFINITY) {
      return "INF";
    }
    if (value == Float.NEGATIVE_INFINITY) {
      return "-INF";
    }
    return value.toString();
  }

  static String formatDouble(Double value) {
    if (value.isNaN()) {
      return "NaN";
    }
    if (value == Double.POSITIVE_INFINITY) {
      return "INF";
    }
    if (value == Double.NEGATIVE_INFINITY) {
      return "-INF";
    }
    return value.toString();
  }

  static BigInteger requireMin(BigInteger value, BigInteger min, String type) {
    if (value.compareTo(min) < 0) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return value;
  }

  static BigInteger requireMax(BigInteger value, BigInteger max, String type) {
    if (value.compareTo(max) > 0) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return value;
  }

  static BigInteger requireRange(BigInteger value, BigInteger min, BigInteger max, String type) {
    requireMin(value, min, type);
    requireMax(value, max, type);
    return value;
  }

  static Long requireRangeLong(String value, long min, long max, String type) {
    long parsed = Long.parseLong(value);
    if (parsed < min || parsed > max) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return parsed;
  }

  static Integer requireRangeInt(String value, int min, int max, String type) {
    int parsed = Integer.parseInt(value);
    if (parsed < min || parsed > max) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return parsed;
  }

  static Short requireRangeShort(String value, int min, int max, String type) {
    int parsed = Integer.parseInt(value);
    if (parsed < min || parsed > max) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return (short) parsed;
  }
}
