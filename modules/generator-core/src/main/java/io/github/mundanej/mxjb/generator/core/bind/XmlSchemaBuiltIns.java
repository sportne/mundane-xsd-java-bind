package io.github.mundanej.mxjb.generator.core.bind;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Internal XML Schema 1.0 built-in datatype metadata for binding and source emission. */
public final class XmlSchemaBuiltIns {
  private static final Pattern NAME = Pattern.compile("[:A-Z_a-z][-.:A-Z_a-z0-9]*");
  private static final Pattern NC_NAME = Pattern.compile("[A-Z_a-z][-A-Z_a-z0-9.]*");
  private static final Pattern NMTOKEN = Pattern.compile("[-.:A-Z_a-z0-9]+");
  private static final Pattern LANGUAGE = Pattern.compile("[A-Za-z]{1,8}(-[A-Za-z0-9]{1,8})*");
  private static final Pattern FLOATING_POINT =
      Pattern.compile("[+-]?((([0-9]+)(\\.[0-9]*)?)|(\\.[0-9]+))([eE][+-]?[0-9]+)?");
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

  private static final Set<String> BUILT_INS =
      Set.of(
          "string",
          "normalizedString",
          "token",
          "language",
          "Name",
          "NCName",
          "ID",
          "IDREF",
          "IDREFS",
          "ENTITY",
          "ENTITIES",
          "NMTOKEN",
          "NMTOKENS",
          "boolean",
          "decimal",
          "float",
          "double",
          "duration",
          "dateTime",
          "time",
          "date",
          "gYearMonth",
          "gYear",
          "gMonthDay",
          "gDay",
          "gMonth",
          "hexBinary",
          "base64Binary",
          "anyURI",
          "QName",
          "NOTATION",
          "integer",
          "nonPositiveInteger",
          "negativeInteger",
          "long",
          "int",
          "short",
          "byte",
          "nonNegativeInteger",
          "unsignedLong",
          "unsignedInt",
          "unsignedShort",
          "unsignedByte",
          "positiveInteger");

  private static final Map<String, String> JAVA_TYPES =
      Map.ofEntries(
          Map.entry("string", "String"),
          Map.entry("normalizedString", "String"),
          Map.entry("token", "String"),
          Map.entry("language", "String"),
          Map.entry("Name", "String"),
          Map.entry("NCName", "String"),
          Map.entry("ID", "String"),
          Map.entry("IDREF", "String"),
          Map.entry("ENTITY", "String"),
          Map.entry("NMTOKEN", "String"),
          Map.entry("IDREFS", "List<String>"),
          Map.entry("ENTITIES", "List<String>"),
          Map.entry("NMTOKENS", "List<String>"),
          Map.entry("boolean", "Boolean"),
          Map.entry("decimal", "BigDecimal"),
          Map.entry("float", "Float"),
          Map.entry("double", "Double"),
          Map.entry("duration", "XmlDuration"),
          Map.entry("dateTime", "XmlDateTime"),
          Map.entry("time", "XmlTime"),
          Map.entry("date", "XmlDate"),
          Map.entry("gYearMonth", "XmlGYearMonth"),
          Map.entry("gYear", "XmlGYear"),
          Map.entry("gMonthDay", "XmlGMonthDay"),
          Map.entry("gDay", "XmlGDay"),
          Map.entry("gMonth", "XmlGMonth"),
          Map.entry("hexBinary", "XmlBinary"),
          Map.entry("base64Binary", "XmlBinary"),
          Map.entry("anyURI", "XmlAnyUri"),
          Map.entry("QName", "XmlQName"),
          Map.entry("NOTATION", "XmlQName"),
          Map.entry("integer", "BigInteger"),
          Map.entry("nonPositiveInteger", "BigInteger"),
          Map.entry("negativeInteger", "BigInteger"),
          Map.entry("long", "Long"),
          Map.entry("int", "Integer"),
          Map.entry("short", "Short"),
          Map.entry("byte", "Byte"),
          Map.entry("nonNegativeInteger", "BigInteger"),
          Map.entry("unsignedLong", "BigInteger"),
          Map.entry("unsignedInt", "Long"),
          Map.entry("unsignedShort", "Integer"),
          Map.entry("unsignedByte", "Short"),
          Map.entry("positiveInteger", "BigInteger"));

  private XmlSchemaBuiltIns() {}

  public static boolean isSupported(String name) {
    return BUILT_INS.contains(name);
  }

  public static String javaType(String name) {
    return JAVA_TYPES.get(name);
  }

  public static boolean isListValued(String name) {
    return "NMTOKENS".equals(name) || "IDREFS".equals(name) || "ENTITIES".equals(name);
  }

  public static boolean isBigIntegerValued(String name) {
    String javaType = javaType(name);
    return "BigInteger".equals(javaType);
  }

  public static boolean isBigDecimalValued(String name) {
    return "BigDecimal".equals(javaType(name));
  }

  public static boolean isRuntimeValued(String name) {
    String javaType = javaType(name);
    return javaType != null && javaType.startsWith("Xml");
  }

  public static boolean isLexicallyValid(String type, String value) {
    try {
      parse(type, value);
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  public static boolean matchesRestriction(
      String type, String lexicalValue, BindingSimpleRestriction restriction) {
    if (restriction == null || !restriction.hasRules()) {
      return true;
    }
    try {
      Object value = parse(type, lexicalValue);
      String normalized = lexicalForValidation(type, lexicalValue);
      if (!restriction.enumerations().isEmpty()
          && restriction.enumerations().stream().noneMatch(item -> equalValue(type, value, item))) {
        return false;
      }
      int measuredLength = valueLength(type, value, normalized);
      if (restriction.length() != null && measuredLength != restriction.length()) {
        return false;
      }
      if (restriction.minLength() != null && measuredLength < restriction.minLength()) {
        return false;
      }
      if (restriction.maxLength() != null && measuredLength > restriction.maxLength()) {
        return false;
      }
      if (!rangeMatches(type, value, restriction)) {
        return false;
      }
      if (!digitFacetsMatch(value, restriction.totalDigits(), restriction.fractionDigits())) {
        return false;
      }
      for (String pattern : restriction.patterns()) {
        if (!Pattern.matches(pattern, normalized)) {
          return false;
        }
      }
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  public static boolean hasPrefixedQNameLexical(String type, String lexicalValue) {
    return ("QName".equals(type) || "NOTATION".equals(type))
        && collapseXmlWhitespace(lexicalValue).contains(":");
  }

  private static Object parse(String type, String value) {
    String collapsed = collapseXmlWhitespace(value);
    return switch (type) {
      case "string" -> value;
      case "normalizedString" -> replaceXmlWhitespace(value);
      case "token" -> collapsed;
      case "language" -> requirePattern(type, collapsed, LANGUAGE);
      case "Name" -> requirePattern(type, collapsed, NAME);
      case "NCName", "ID", "IDREF", "ENTITY" -> requirePattern(type, collapsed, NC_NAME);
      case "NMTOKEN" -> requirePattern(type, collapsed, NMTOKEN);
      case "NMTOKENS" -> tokenList(value, "NMTOKEN");
      case "IDREFS" -> tokenList(value, "IDREF");
      case "ENTITIES" -> tokenList(value, "ENTITY");
      case "boolean" -> parseBoolean(collapsed);
      case "decimal" -> new BigDecimal(collapsed);
      case "float" -> parseFloatingPoint(collapsed, "float");
      case "double" -> parseFloatingPoint(collapsed, "double");
      case "integer" -> new BigInteger(collapsed);
      case "nonPositiveInteger" -> requireMax(new BigInteger(collapsed), BigInteger.ZERO, type);
      case "negativeInteger" -> requireMax(new BigInteger(collapsed), BigInteger.valueOf(-1), type);
      case "long" -> Long.valueOf(collapsed);
      case "int" -> Integer.valueOf(collapsed);
      case "short" -> Short.valueOf(collapsed);
      case "byte" -> Byte.valueOf(collapsed);
      case "nonNegativeInteger" -> requireMin(new BigInteger(collapsed), BigInteger.ZERO, type);
      case "unsignedLong" ->
          requireRange(
              new BigInteger(collapsed),
              BigInteger.ZERO,
              new BigInteger("18446744073709551615"),
              type);
      case "unsignedInt" -> requireRangeLong(collapsed, 0L, 4294967295L, type);
      case "unsignedShort" -> requireRangeInt(collapsed, 0, 65535, type);
      case "unsignedByte" -> requireRangeShort(collapsed, 0, 255, type);
      case "positiveInteger" -> requireMin(new BigInteger(collapsed), BigInteger.ONE, type);
      case "duration" -> requireDuration(collapsed);
      case "dateTime" -> requireDateTime(value);
      case "date" -> requireDate(value);
      case "time" -> requireTime(value);
      case "gYear" -> requireGYear(value);
      case "gYearMonth" -> requireGYearMonth(value);
      case "gMonth" -> requireGMonth(value);
      case "gMonthDay" -> requireGMonthDay(value);
      case "gDay" -> requireGDay(value);
      case "hexBinary" -> parseHexBinary(value);
      case "base64Binary" -> Base64.getMimeDecoder().decode(collapsed);
      case "anyURI" -> collapsed;
      case "QName", "NOTATION" -> requireQNameLexical(collapsed);
      default -> throw new IllegalArgumentException("Unsupported XML Schema datatype " + type);
    };
  }

  private static Object parseBoolean(String collapsed) {
    return switch (collapsed) {
      case "true", "1" -> true;
      case "false", "0" -> false;
      default -> throw new IllegalArgumentException("Invalid boolean value.");
    };
  }

  private static Object parseFloatingPoint(String collapsed, String type) {
    return switch (collapsed) {
      case "INF" -> "float".equals(type) ? Float.POSITIVE_INFINITY : Double.POSITIVE_INFINITY;
      case "-INF" -> "float".equals(type) ? Float.NEGATIVE_INFINITY : Double.NEGATIVE_INFINITY;
      case "NaN" -> "float".equals(type) ? Float.NaN : Double.NaN;
      default -> {
        requirePattern(type, collapsed, FLOATING_POINT);
        yield parseFiniteFloatingPoint(collapsed, type);
      }
    };
  }

  private static Object parseFiniteFloatingPoint(String collapsed, String type) {
    if ("float".equals(type)) {
      return Float.valueOf(collapsed);
    }
    return Double.valueOf(collapsed);
  }

  private static String requireDuration(String collapsed) {
    requirePattern("duration", collapsed, DURATION);
    if (collapsed.endsWith("T")) {
      throw new IllegalArgumentException("Invalid duration value.");
    }
    return collapsed;
  }

  private static java.util.List<String> tokenList(String value, String itemType) {
    String collapsed = collapseXmlWhitespace(value);
    if (collapsed.isEmpty()) {
      throw new IllegalArgumentException(itemType + " list must contain at least one item.");
    }
    java.util.ArrayList<String> values = new java.util.ArrayList<>();
    int start = 0;
    while (start <= collapsed.length()) {
      int end = collapsed.indexOf(' ', start);
      String token = end < 0 ? collapsed.substring(start) : collapsed.substring(start, end);
      parse(itemType, token);
      values.add(token);
      if (end < 0) {
        break;
      }
      start = end + 1;
    }
    return java.util.List.copyOf(values);
  }

  private static String requireQNameLexical(String lexical) {
    int separator = lexical.indexOf(':');
    if (separator < 0) {
      return requirePattern("QName", lexical, NC_NAME);
    }
    if (separator != lexical.lastIndexOf(':')) {
      throw new IllegalArgumentException("Invalid QName value.");
    }
    requirePattern("QName prefix", lexical.substring(0, separator), NC_NAME);
    requirePattern("QName local name", lexical.substring(separator + 1), NC_NAME);
    return lexical;
  }

  private static byte[] parseHexBinary(String value) {
    String collapsed = collapseXmlWhitespace(value);
    if (collapsed.length() % 2 != 0 || !collapsed.matches("[0-9A-Fa-f]*")) {
      throw new IllegalArgumentException("Invalid hexBinary value.");
    }
    return HexFormat.of().parseHex(collapsed);
  }

  private static String requireDateTime(String value) {
    String collapsed = requirePattern("dateTime", collapseXmlWhitespace(value), DATE_TIME);
    int separator = collapsed.indexOf('T');
    validateDatePart(stripTimezone(collapsed.substring(0, separator)), "dateTime");
    validateTimePart(stripTimezone(collapsed.substring(separator + 1)), "dateTime");
    validateTimezone(collapsed, "dateTime");
    return collapsed;
  }

  private static String requireDate(String value) {
    String collapsed = requirePattern("date", collapseXmlWhitespace(value), DATE);
    validateDatePart(stripTimezone(collapsed), "date");
    validateTimezone(collapsed, "date");
    return collapsed;
  }

  private static String requireTime(String value) {
    String collapsed = requirePattern("time", collapseXmlWhitespace(value), TIME);
    validateTimePart(stripTimezone(collapsed), "time");
    validateTimezone(collapsed, "time");
    return collapsed;
  }

  private static String requireGYear(String value) {
    String collapsed = requirePattern("gYear", collapseXmlWhitespace(value), G_YEAR);
    validateYearPart(stripTimezone(collapsed), "gYear");
    validateTimezone(collapsed, "gYear");
    return collapsed;
  }

  private static String requireGYearMonth(String value) {
    String collapsed = requirePattern("gYearMonth", collapseXmlWhitespace(value), G_YEAR_MONTH);
    String local = stripTimezone(collapsed);
    int separator = local.lastIndexOf('-');
    validateYearPart(local.substring(0, separator), "gYearMonth");
    int month = parseTwoDigits(local.substring(separator + 1), "gYearMonth month");
    requireRange(month, 1, 12, "gYearMonth month");
    validateTimezone(collapsed, "gYearMonth");
    return collapsed;
  }

  private static String requireGMonth(String value) {
    String collapsed = requirePattern("gMonth", collapseXmlWhitespace(value), G_MONTH);
    String local = stripTimezone(collapsed);
    int month = parseTwoDigits(local.substring(2, 4), "gMonth month");
    requireRange(month, 1, 12, "gMonth month");
    validateTimezone(collapsed, "gMonth");
    return collapsed;
  }

  private static String requireGMonthDay(String value) {
    String collapsed = requirePattern("gMonthDay", collapseXmlWhitespace(value), G_MONTH_DAY);
    String local = stripTimezone(collapsed);
    int month = parseTwoDigits(local.substring(2, 4), "gMonthDay month");
    int day = parseTwoDigits(local.substring(5, 7), "gMonthDay day");
    requireRange(month, 1, 12, "gMonthDay month");
    requireRange(day, 1, daysInMonth(2000, month), "gMonthDay day");
    validateTimezone(collapsed, "gMonthDay");
    return collapsed;
  }

  private static String requireGDay(String value) {
    String collapsed = requirePattern("gDay", collapseXmlWhitespace(value), G_DAY);
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

  private static String requirePattern(String type, String value, Pattern pattern) {
    if (!pattern.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return value;
  }

  private static BigInteger requireMin(BigInteger value, BigInteger min, String type) {
    if (value.compareTo(min) < 0) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return value;
  }

  private static BigInteger requireMax(BigInteger value, BigInteger max, String type) {
    if (value.compareTo(max) > 0) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return value;
  }

  private static BigInteger requireRange(
      BigInteger value, BigInteger min, BigInteger max, String type) {
    requireMin(value, min, type);
    requireMax(value, max, type);
    return value;
  }

  private static Long requireRangeLong(String value, long min, long max, String type) {
    long parsed = Long.parseLong(value);
    if (parsed < min || parsed > max) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return parsed;
  }

  private static Integer requireRangeInt(String value, int min, int max, String type) {
    int parsed = Integer.parseInt(value);
    if (parsed < min || parsed > max) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return parsed;
  }

  private static Short requireRangeShort(String value, int min, int max, String type) {
    int parsed = Integer.parseInt(value);
    if (parsed < min || parsed > max) {
      throw new IllegalArgumentException("Invalid " + type + " value.");
    }
    return (short) parsed;
  }

  private static String replaceXmlWhitespace(String value) {
    return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
  }

  private static String collapseXmlWhitespace(String value) {
    return replaceXmlWhitespace(value).trim().replaceAll(" +", " ");
  }

  private static String lexicalForValidation(String type, String lexicalValue) {
    return switch (type) {
      case "normalizedString" -> replaceXmlWhitespace(lexicalValue);
      case "token",
          "language",
          "Name",
          "NCName",
          "ID",
          "IDREF",
          "ENTITY",
          "NMTOKEN",
          "NMTOKENS",
          "IDREFS",
          "ENTITIES" ->
          collapseXmlWhitespace(lexicalValue);
      default -> lexicalValue;
    };
  }

  private static int valueLength(String type, Object value, String lexical) {
    if (value instanceof byte[] bytes) {
      return bytes.length;
    }
    if (isListValued(type) && value instanceof java.util.List<?> list) {
      return list.size();
    }
    return lexical.length();
  }

  private static boolean equalValue(String type, Object value, String lexical) {
    try {
      return normalizeComparable(value).equals(normalizeComparable(parse(type, lexical)));
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private static Object normalizeComparable(Object value) {
    if (value instanceof BigDecimal decimal) {
      return decimal.stripTrailingZeros();
    }
    if (value instanceof byte[] bytes) {
      return java.util.Arrays.toString(bytes);
    }
    return value;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static boolean rangeMatches(
      String type, Object value, BindingSimpleRestriction restriction) {
    if (!(value instanceof Comparable<?> comparable)) {
      return true;
    }
    Comparable normalized = (Comparable) normalizeComparable(comparable);
    return compareBound(type, normalized, restriction.minInclusive(), true, true)
        && compareBound(type, normalized, restriction.maxInclusive(), false, true)
        && compareBound(type, normalized, restriction.minExclusive(), true, false)
        && compareBound(type, normalized, restriction.maxExclusive(), false, false);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static boolean compareBound(
      String type, Comparable value, String bound, boolean lower, boolean inclusive) {
    if (bound == null) {
      return true;
    }
    Object parsed = parse(type, bound);
    if (!(parsed instanceof Comparable<?> comparable)) {
      return true;
    }
    int comparison = value.compareTo(normalizeComparable(comparable));
    return lower
        ? (inclusive ? comparison >= 0 : comparison > 0)
        : (inclusive ? comparison <= 0 : comparison < 0);
  }

  private static boolean digitFacetsMatch(
      Object value, Integer totalDigits, Integer fractionDigits) {
    if (totalDigits == null && fractionDigits == null) {
      return true;
    }
    if (!(value instanceof BigDecimal)
        && !(value instanceof BigInteger)
        && !(value instanceof Long)
        && !(value instanceof Integer)
        && !(value instanceof Short)
        && !(value instanceof Byte)) {
      return true;
    }
    BigDecimal decimal = new BigDecimal(value.toString()).stripTrailingZeros();
    String plain = decimal.abs().toPlainString();
    int decimalPoint = plain.indexOf('.');
    String integral = decimalPoint < 0 ? plain : plain.substring(0, decimalPoint);
    String fraction = decimalPoint < 0 ? "" : plain.substring(decimalPoint + 1);
    String digits = (integral + fraction).replaceFirst("^0+(?!$)", "");
    return (totalDigits == null || digits.length() <= totalDigits)
        && (fractionDigits == null || fraction.length() <= fractionDigits);
  }
}
