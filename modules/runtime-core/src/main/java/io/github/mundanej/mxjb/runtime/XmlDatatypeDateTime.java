package io.github.mundanej.mxjb.runtime;

import java.util.regex.Pattern;

final class XmlDatatypeDateTime {
  private static final Pattern DURATION =
      Pattern.compile(
          "-?P(?=.+)([0-9]+Y)?([0-9]+M)?([0-9]+D)?(T([0-9]+H)?([0-9]+M)?([0-9]+(\\.[0-9]+)?S)?)?");
  private static final Pattern DATE_TIME =
      Pattern.compile(
          "-?[0-9]{4,}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?(Z|[+-][0-9]{2}:[0-9]{2})?");
  private static final Pattern DATE =
      Pattern.compile("-?[0-9]{4,}-[0-9]{2}-[0-9]{2}(Z|[+-][0-9]{2}:[0-9]{2})?");
  private static final Pattern TIME =
      Pattern.compile("[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?(Z|[+-][0-9]{2}:[0-9]{2})?");
  private static final Pattern G_YEAR = Pattern.compile("-?[0-9]{4,}(Z|[+-][0-9]{2}:[0-9]{2})?");
  private static final Pattern G_YEAR_MONTH =
      Pattern.compile("-?[0-9]{4,}-[0-9]{2}(Z|[+-][0-9]{2}:[0-9]{2})?");
  private static final Pattern G_MONTH =
      Pattern.compile("--[0-9]{2}(--)?(Z|[+-][0-9]{2}:[0-9]{2})?");
  private static final Pattern G_MONTH_DAY =
      Pattern.compile("--[0-9]{2}-[0-9]{2}(Z|[+-][0-9]{2}:[0-9]{2})?");
  private static final Pattern G_DAY = Pattern.compile("---[0-9]{2}(Z|[+-][0-9]{2}:[0-9]{2})?");

  private XmlDatatypeDateTime() {}

  static String requireDurationLexical(String value) {
    String collapsed =
        XmlDatatypeLexical.requirePattern(
            "duration", XmlDatatypeLexical.collapseWhitespace(value), DURATION);
    if (collapsed.endsWith("T")) {
      throw new IllegalArgumentException("Invalid duration value.");
    }
    return collapsed;
  }

  static String requireDateTimeLexical(String value) {
    String collapsed =
        XmlDatatypeLexical.requirePattern(
            "dateTime", XmlDatatypeLexical.collapseWhitespace(value), DATE_TIME);
    int separator = collapsed.indexOf('T');
    validateDatePart(stripTimezone(collapsed.substring(0, separator)), "dateTime");
    validateTimePart(stripTimezone(collapsed.substring(separator + 1)), "dateTime");
    validateTimezone(collapsed, "dateTime");
    return collapsed;
  }

  static String requireDateLexical(String value) {
    String collapsed =
        XmlDatatypeLexical.requirePattern(
            "date", XmlDatatypeLexical.collapseWhitespace(value), DATE);
    validateDatePart(stripTimezone(collapsed), "date");
    validateTimezone(collapsed, "date");
    return collapsed;
  }

  static String requireTimeLexical(String value) {
    String collapsed =
        XmlDatatypeLexical.requirePattern(
            "time", XmlDatatypeLexical.collapseWhitespace(value), TIME);
    validateTimePart(stripTimezone(collapsed), "time");
    validateTimezone(collapsed, "time");
    return collapsed;
  }

  static String requireGYearLexical(String value) {
    String collapsed =
        XmlDatatypeLexical.requirePattern(
            "gYear", XmlDatatypeLexical.collapseWhitespace(value), G_YEAR);
    validateYearPart(stripTimezone(collapsed), "gYear");
    validateTimezone(collapsed, "gYear");
    return collapsed;
  }

  static String requireGYearMonthLexical(String value) {
    String collapsed =
        XmlDatatypeLexical.requirePattern(
            "gYearMonth", XmlDatatypeLexical.collapseWhitespace(value), G_YEAR_MONTH);
    String local = stripTimezone(collapsed);
    int separator = local.lastIndexOf('-');
    validateYearPart(local.substring(0, separator), "gYearMonth");
    int month = parseTwoDigits(local.substring(separator + 1), "gYearMonth month");
    requireRange(month, 1, 12, "gYearMonth month");
    validateTimezone(collapsed, "gYearMonth");
    return collapsed;
  }

  static String requireGMonthLexical(String value) {
    String collapsed =
        XmlDatatypeLexical.requirePattern(
            "gMonth", XmlDatatypeLexical.collapseWhitespace(value), G_MONTH);
    String local = stripTimezone(collapsed);
    int month = parseTwoDigits(local.substring(2, 4), "gMonth month");
    requireRange(month, 1, 12, "gMonth month");
    validateTimezone(collapsed, "gMonth");
    return collapsed;
  }

  static String requireGMonthDayLexical(String value) {
    String collapsed =
        XmlDatatypeLexical.requirePattern(
            "gMonthDay", XmlDatatypeLexical.collapseWhitespace(value), G_MONTH_DAY);
    String local = stripTimezone(collapsed);
    int month = parseTwoDigits(local.substring(2, 4), "gMonthDay month");
    int day = parseTwoDigits(local.substring(5, 7), "gMonthDay day");
    requireRange(month, 1, 12, "gMonthDay month");
    requireRange(day, 1, daysInMonth(2000, month), "gMonthDay day");
    validateTimezone(collapsed, "gMonthDay");
    return collapsed;
  }

  static String requireGDayLexical(String value) {
    String collapsed =
        XmlDatatypeLexical.requirePattern(
            "gDay", XmlDatatypeLexical.collapseWhitespace(value), G_DAY);
    String local = stripTimezone(collapsed);
    int day = parseTwoDigits(local.substring(3, 5), "gDay day");
    requireRange(day, 1, 31, "gDay day");
    validateTimezone(collapsed, "gDay");
    return collapsed;
  }

  private static void validateDatePart(String local, String type) {
    int daySeparator = local.lastIndexOf('-');
    int monthSeparator = local.lastIndexOf('-', daySeparator - 1);
    int year = Integer.parseInt(local.substring(0, monthSeparator));
    if (year == 0) {
      throw new IllegalArgumentException(type + " year zero is not allowed.");
    }
    int month = parseTwoDigits(local.substring(monthSeparator + 1, daySeparator), type + " month");
    int day = parseTwoDigits(local.substring(daySeparator + 1), type + " day");
    requireRange(month, 1, 12, type + " month");
    requireRange(day, 1, daysInMonth(year, month), type + " day");
  }

  private static void validateYearPart(String value, String type) {
    if (Integer.parseInt(value) == 0) {
      throw new IllegalArgumentException(type + " year zero is not allowed.");
    }
  }

  private static void validateTimePart(String local, String type) {
    String[] parts = local.split(":", -1);
    int hour = parseTwoDigits(parts[0], type + " hour");
    int minute = parseTwoDigits(parts[1], type + " minute");
    int second =
        parseTwoDigits(
            parts[2].contains(".") ? parts[2].substring(0, 2) : parts[2], type + " second");
    requireRange(hour, 0, 24, type + " hour");
    requireRange(minute, 0, 59, type + " minute");
    requireRange(second, 0, 59, type + " second");
    if (hour == 24 && (minute != 0 || second != 0 || parts[2].contains("."))) {
      throw new IllegalArgumentException(type + " 24th hour must be exactly 24:00:00.");
    }
  }

  private static String stripTimezone(String value) {
    if (value.endsWith("Z")) {
      return value.substring(0, value.length() - 1);
    }
    if (value.length() > 6) {
      char marker = value.charAt(value.length() - 6);
      if ((marker == '+' || marker == '-') && value.charAt(value.length() - 3) == ':') {
        return value.substring(0, value.length() - 6);
      }
    }
    return value;
  }

  private static void validateTimezone(String value, String type) {
    if (value.endsWith("Z") || value.length() <= 6) {
      return;
    }
    char marker = value.charAt(value.length() - 6);
    if ((marker == '+' || marker == '-') && value.charAt(value.length() - 3) == ':') {
      int hour =
          parseTwoDigits(
              value.substring(value.length() - 5, value.length() - 3), type + " timezone hour");
      int minute = parseTwoDigits(value.substring(value.length() - 2), type + " timezone minute");
      requireRange(hour, 0, 14, type + " timezone hour");
      requireRange(minute, 0, 59, type + " timezone minute");
      if (hour == 14 && minute != 0) {
        throw new IllegalArgumentException(type + " timezone offset exceeds 14:00.");
      }
    }
  }

  private static int parseTwoDigits(String value, String label) {
    if (value.length() != 2) {
      throw new IllegalArgumentException(label + " must contain two digits.");
    }
    return Integer.parseInt(value);
  }

  private static void requireRange(int value, int min, int max, String label) {
    if (value < min || value > max) {
      throw new IllegalArgumentException(label + " is out of range.");
    }
  }

  private static int daysInMonth(int year, int month) {
    return switch (month) {
      case 2 -> isLeapYear(year) ? 29 : 28;
      case 4, 6, 9, 11 -> 30;
      default -> 31;
    };
  }

  private static boolean isLeapYear(int year) {
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
  }
}
