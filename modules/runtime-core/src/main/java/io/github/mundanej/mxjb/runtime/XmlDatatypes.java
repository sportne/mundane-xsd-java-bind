package io.github.mundanej.mxjb.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** XML Schema 1.0 datatype lexical conversion and generated-validation helpers. */
public final class XmlDatatypes {
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
  private static final Set<String> STRING_VALUED =
      Set.of(
          "string",
          "normalizedString",
          "token",
          "language",
          "Name",
          "NCName",
          "NMTOKEN",
          "ID",
          "IDREF",
          "ENTITY");
  private static final Set<String> BIG_INTEGER_VALUED =
      Set.of(
          "integer",
          "nonPositiveInteger",
          "negativeInteger",
          "nonNegativeInteger",
          "positiveInteger",
          "unsignedLong");
  private static final Set<String> LONG_VALUED = Set.of("long", "unsignedInt");
  private static final Set<String> INTEGER_VALUED = Set.of("int", "unsignedShort");
  private static final Set<String> SHORT_VALUED = Set.of("short", "unsignedByte");
  private static final Set<String> LIST_VALUED = Set.of("NMTOKENS", "IDREFS", "ENTITIES");

  private XmlDatatypes() {}

  public static Object parse(String type, String value, XmlEventReader input, XmlLocation location)
      throws XmlReadException {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(value, "value");
    return parseUnchecked(type, value, input, location, true);
  }

  public static List<?> parseList(
      String itemType, String value, XmlEventReader input, XmlLocation location)
      throws XmlReadException {
    return parseListValues(itemType, value, input, location);
  }

  public static <T> List<T> parseList(
      String itemType, String value, XmlEventReader input, XmlLocation location, Class<T> javaType)
      throws XmlReadException {
    Objects.requireNonNull(javaType, "javaType");
    ArrayList<T> values = new ArrayList<>();
    for (Object item : parseListValues(itemType, value, input, location)) {
      values.add(javaType.cast(item));
    }
    return List.copyOf(values);
  }

  private static List<?> parseListValues(
      String itemType, String value, XmlEventReader input, XmlLocation location)
      throws XmlReadException {
    String collapsed = collapseXmlWhitespace(value);
    if (collapsed.isEmpty()) {
      return List.of();
    }
    ArrayList<Object> values = new ArrayList<>();
    int start = 0;
    while (start <= collapsed.length()) {
      int end = collapsed.indexOf(' ', start);
      String token = end < 0 ? collapsed.substring(start) : collapsed.substring(start, end);
      values.add(parse(itemType, token, input, location));
      if (end < 0) {
        break;
      }
      start = end + 1;
    }
    return List.copyOf(values);
  }

  public static boolean isLexicallyValid(String type, String value) {
    try {
      parseUnchecked(type, value, null, XmlLocation.UNKNOWN, false);
      return true;
    } catch (XmlReadException | IllegalArgumentException exception) {
      return false;
    }
  }

  public static String format(String type, Object value, XmlOutput output)
      throws XmlWriteException {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(value, "value");
    return switch (type) {
      case "string",
          "normalizedString",
          "token",
          "language",
          "Name",
          "NCName",
          "NMTOKEN",
          "ID",
          "IDREF",
          "ENTITY" ->
          value.toString();
      case "NMTOKENS", "IDREFS", "ENTITIES" -> formatList("string", (List<?>) value, output);
      case "boolean" -> Boolean.TRUE.equals(value) ? "true" : "false";
      case "decimal" -> ((BigDecimal) value).toPlainString();
      case "float" -> formatFloat((Float) value);
      case "double" -> formatDouble((Double) value);
      case "integer",
          "nonPositiveInteger",
          "negativeInteger",
          "nonNegativeInteger",
          "positiveInteger",
          "unsignedLong" ->
          ((BigInteger) value).toString();
      case "long", "unsignedInt" -> ((Long) value).toString();
      case "int", "unsignedShort" -> ((Integer) value).toString();
      case "short", "unsignedByte" -> ((Short) value).toString();
      case "byte" -> ((Byte) value).toString();
      case "duration" -> ((XmlDuration) value).lexicalValue();
      case "dateTime" -> ((XmlDateTime) value).lexicalValue();
      case "date" -> ((XmlDate) value).lexicalValue();
      case "time" -> ((XmlTime) value).lexicalValue();
      case "gYear" -> ((XmlGYear) value).lexicalValue();
      case "gYearMonth" -> ((XmlGYearMonth) value).lexicalValue();
      case "gMonth" -> ((XmlGMonth) value).lexicalValue();
      case "gMonthDay" -> ((XmlGMonthDay) value).lexicalValue();
      case "gDay" -> ((XmlGDay) value).lexicalValue();
      case "hexBinary" -> ((XmlBinary) value).hexLexicalValue();
      case "base64Binary" -> ((XmlBinary) value).base64LexicalValue();
      case "anyURI" -> ((XmlAnyUri) value).lexicalValue();
      case "QName", "NOTATION" -> output.qNameText((XmlQName) value);
      default -> value.toString();
    };
  }

  public static String formatList(String itemType, List<?> values, XmlOutput output)
      throws XmlWriteException {
    Objects.requireNonNull(values, "values");
    ArrayList<String> lexicalValues = new ArrayList<>();
    for (Object value : values) {
      lexicalValues.add(format(itemType, value, output));
    }
    return String.join(" ", lexicalValues);
  }

  public static boolean matchesFacets(
      String type,
      Object value,
      List<String> enumerations,
      Integer length,
      Integer minLength,
      Integer maxLength,
      String minInclusive,
      String maxInclusive,
      String minExclusive,
      String maxExclusive,
      Integer totalDigits,
      Integer fractionDigits,
      List<String> patterns) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(value, "value");
    List<String> safeEnumerations = List.copyOf(enumerations);
    List<String> safePatterns = List.copyOf(patterns);
    if (!isValidValue(type, value)) {
      return false;
    }
    String lexical = lexicalForValidation(type, value);
    if (!safeEnumerations.isEmpty()
        && safeEnumerations.stream().noneMatch(item -> equalValue(type, value, item))) {
      return false;
    }
    int measuredLength = valueLength(value, lexical);
    if (length != null && measuredLength != length) {
      return false;
    }
    if (minLength != null && measuredLength < minLength) {
      return false;
    }
    if (maxLength != null && measuredLength > maxLength) {
      return false;
    }
    if (!rangeMatches(type, value, minInclusive, maxInclusive, minExclusive, maxExclusive)) {
      return false;
    }
    if (!digitFacetsMatch(value, totalDigits, fractionDigits)) {
      return false;
    }
    for (String pattern : safePatterns) {
      if (!Pattern.matches(pattern, lexical)) {
        return false;
      }
    }
    return true;
  }

  private static Object parseUnchecked(
      String type, String value, XmlEventReader input, XmlLocation location, boolean checked)
      throws XmlReadException {
    try {
      return switch (type) {
        case "string" -> value;
        case "normalizedString" -> replaceXmlWhitespace(value);
        case "token" -> collapseXmlWhitespace(value);
        case "language" -> requirePattern(type, collapseXmlWhitespace(value), LANGUAGE);
        case "Name" -> requirePattern(type, collapseXmlWhitespace(value), NAME);
        case "NCName", "ID", "IDREF", "ENTITY" ->
            requirePattern(type, collapseXmlWhitespace(value), NC_NAME);
        case "NMTOKEN" -> requirePattern(type, collapseXmlWhitespace(value), NMTOKEN);
        case "NMTOKENS" -> tokenList(value, "NMTOKEN");
        case "IDREFS" -> tokenList(value, "IDREF");
        case "ENTITIES" -> tokenList(value, "ENTITY");
        case "boolean" -> parseBoolean(value);
        case "decimal" -> new BigDecimal(collapseXmlWhitespace(value));
        case "float" -> parseFloat(value);
        case "double" -> parseDouble(value);
        case "integer" -> new BigInteger(collapseXmlWhitespace(value));
        case "nonPositiveInteger" ->
            requireMax(new BigInteger(collapseXmlWhitespace(value)), BigInteger.ZERO, type);
        case "negativeInteger" ->
            requireMax(new BigInteger(collapseXmlWhitespace(value)), BigInteger.valueOf(-1), type);
        case "long" -> Long.valueOf(collapseXmlWhitespace(value));
        case "int" -> Integer.valueOf(collapseXmlWhitespace(value));
        case "short" -> Short.valueOf(collapseXmlWhitespace(value));
        case "byte" -> Byte.valueOf(collapseXmlWhitespace(value));
        case "nonNegativeInteger" ->
            requireMin(new BigInteger(collapseXmlWhitespace(value)), BigInteger.ZERO, type);
        case "unsignedLong" ->
            requireRange(
                new BigInteger(collapseXmlWhitespace(value)),
                BigInteger.ZERO,
                new BigInteger("18446744073709551615"),
                type);
        case "unsignedInt" -> requireRangeLong(collapseXmlWhitespace(value), 0L, 4294967295L, type);
        case "unsignedShort" -> requireRangeInt(collapseXmlWhitespace(value), 0, 65535, type);
        case "unsignedByte" -> requireRangeShort(collapseXmlWhitespace(value), 0, 255, type);
        case "positiveInteger" ->
            requireMin(new BigInteger(collapseXmlWhitespace(value)), BigInteger.ONE, type);
        case "duration" -> new XmlDuration(requireDurationLexical(value));
        case "dateTime" -> new XmlDateTime(requireDateTimeLexical(value));
        case "date" -> new XmlDate(requireDateLexical(value));
        case "time" -> new XmlTime(requireTimeLexical(value));
        case "gYear" -> new XmlGYear(requireGYearLexical(value));
        case "gYearMonth" -> new XmlGYearMonth(requireGYearMonthLexical(value));
        case "gMonth" -> new XmlGMonth(requireGMonthLexical(value));
        case "gMonthDay" -> new XmlGMonthDay(requireGMonthDayLexical(value));
        case "gDay" -> new XmlGDay(requireGDayLexical(value));
        case "hexBinary" -> parseHexBinary(value);
        case "base64Binary" ->
            new XmlBinary(Base64.getMimeDecoder().decode(collapseXmlWhitespace(value)));
        case "anyURI" -> new XmlAnyUri(collapseXmlWhitespace(value));
        case "QName", "NOTATION" -> parseQName(value, input);
        default ->
            throw new IllegalArgumentException("Unsupported XML Schema datatype " + type + ".");
      };
    } catch (IllegalArgumentException exception) {
      if (!checked) {
        throw exception;
      }
      throw readException(location, "Invalid " + type + " value.", exception);
    }
  }

  private static XmlQName parseQName(String value, XmlEventReader input) {
    String lexical = collapseXmlWhitespace(value);
    int separator = lexical.indexOf(':');
    if (separator < 0) {
      requirePattern("QName", lexical, NC_NAME);
      return new XmlQName("", lexical, lexical);
    }
    String prefix = lexical.substring(0, separator);
    String localName = lexical.substring(separator + 1);
    requirePattern("QName prefix", prefix, NC_NAME);
    requirePattern("QName local name", localName, NC_NAME);
    String namespace = input == null ? null : input.namespaceUriForPrefix(prefix);
    if (namespace == null || namespace.isEmpty()) {
      throw new IllegalArgumentException("Unresolved QName prefix " + prefix + ".");
    }
    return new XmlQName(namespace, localName, lexical);
  }

  private static List<String> tokenList(String value, String itemType) throws XmlReadException {
    String collapsed = collapseXmlWhitespace(value);
    if (collapsed.isEmpty()) {
      throw new IllegalArgumentException(itemType + " list must contain at least one item.");
    }
    ArrayList<String> values = new ArrayList<>();
    int start = 0;
    while (start <= collapsed.length()) {
      int end = collapsed.indexOf(' ', start);
      String token = end < 0 ? collapsed.substring(start) : collapsed.substring(start, end);
      values.add((String) parseUnchecked(itemType, token, null, XmlLocation.UNKNOWN, false));
      if (end < 0) {
        break;
      }
      start = end + 1;
    }
    return List.copyOf(values);
  }

  private static Boolean parseBoolean(String value) {
    return switch (collapseXmlWhitespace(value)) {
      case "true", "1" -> Boolean.TRUE;
      case "false", "0" -> Boolean.FALSE;
      default -> throw new IllegalArgumentException("Invalid boolean value.");
    };
  }

  private static Float parseFloat(String value) {
    String collapsed = collapseXmlWhitespace(value);
    return switch (collapsed) {
      case "INF" -> Float.POSITIVE_INFINITY;
      case "-INF" -> Float.NEGATIVE_INFINITY;
      case "NaN" -> Float.NaN;
      default -> {
        if (!FLOATING_POINT.matcher(collapsed).matches()) {
          throw new IllegalArgumentException("Invalid float lexical value.");
        }
        yield Float.valueOf(collapsed);
      }
    };
  }

  private static Double parseDouble(String value) {
    String collapsed = collapseXmlWhitespace(value);
    return switch (collapsed) {
      case "INF" -> Double.POSITIVE_INFINITY;
      case "-INF" -> Double.NEGATIVE_INFINITY;
      case "NaN" -> Double.NaN;
      default -> {
        if (!FLOATING_POINT.matcher(collapsed).matches()) {
          throw new IllegalArgumentException("Invalid double lexical value.");
        }
        yield Double.valueOf(collapsed);
      }
    };
  }

  static String requireDurationLexical(String value) {
    String collapsed = requirePattern("duration", collapseXmlWhitespace(value), DURATION);
    if (collapsed.endsWith("T")) {
      throw new IllegalArgumentException("Invalid duration value.");
    }
    return collapsed;
  }

  static String requireDateTimeLexical(String value) {
    String collapsed = requirePattern("dateTime", collapseXmlWhitespace(value), DATE_TIME);
    int separator = collapsed.indexOf('T');
    validateDatePart(stripTimezone(collapsed.substring(0, separator)), "dateTime");
    validateTimePart(stripTimezone(collapsed.substring(separator + 1)), "dateTime");
    validateTimezone(collapsed, "dateTime");
    return collapsed;
  }

  static String requireDateLexical(String value) {
    String collapsed = requirePattern("date", collapseXmlWhitespace(value), DATE);
    validateDatePart(stripTimezone(collapsed), "date");
    validateTimezone(collapsed, "date");
    return collapsed;
  }

  static String requireTimeLexical(String value) {
    String collapsed = requirePattern("time", collapseXmlWhitespace(value), TIME);
    validateTimePart(stripTimezone(collapsed), "time");
    validateTimezone(collapsed, "time");
    return collapsed;
  }

  static String requireGYearLexical(String value) {
    String collapsed = requirePattern("gYear", collapseXmlWhitespace(value), G_YEAR);
    validateYearPart(stripTimezone(collapsed), "gYear");
    validateTimezone(collapsed, "gYear");
    return collapsed;
  }

  static String requireGYearMonthLexical(String value) {
    String collapsed = requirePattern("gYearMonth", collapseXmlWhitespace(value), G_YEAR_MONTH);
    String local = stripTimezone(collapsed);
    int separator = local.lastIndexOf('-');
    validateYearPart(local.substring(0, separator), "gYearMonth");
    int month = parseTwoDigits(local.substring(separator + 1), "gYearMonth month");
    requireRange(month, 1, 12, "gYearMonth month");
    validateTimezone(collapsed, "gYearMonth");
    return collapsed;
  }

  static String requireGMonthLexical(String value) {
    String collapsed = requirePattern("gMonth", collapseXmlWhitespace(value), G_MONTH);
    String local = stripTimezone(collapsed);
    int month = parseTwoDigits(local.substring(2, 4), "gMonth month");
    requireRange(month, 1, 12, "gMonth month");
    validateTimezone(collapsed, "gMonth");
    return collapsed;
  }

  static String requireGMonthDayLexical(String value) {
    String collapsed = requirePattern("gMonthDay", collapseXmlWhitespace(value), G_MONTH_DAY);
    String local = stripTimezone(collapsed);
    int month = parseTwoDigits(local.substring(2, 4), "gMonthDay month");
    int day = parseTwoDigits(local.substring(5, 7), "gMonthDay day");
    requireRange(month, 1, 12, "gMonthDay month");
    requireRange(day, 1, daysInMonth(2000, month), "gMonthDay day");
    validateTimezone(collapsed, "gMonthDay");
    return collapsed;
  }

  static String requireGDayLexical(String value) {
    String collapsed = requirePattern("gDay", collapseXmlWhitespace(value), G_DAY);
    String local = stripTimezone(collapsed);
    int day = parseTwoDigits(local.substring(3, 5), "gDay day");
    requireRange(day, 1, 31, "gDay day");
    validateTimezone(collapsed, "gDay");
    return collapsed;
  }

  static void requireQNameValue(String namespaceUri, String localName, String lexicalName) {
    Objects.requireNonNull(namespaceUri, "namespaceUri");
    Objects.requireNonNull(localName, "localName");
    Objects.requireNonNull(lexicalName, "lexicalName");
    requirePattern("QName local name", localName, NC_NAME);
    int separator = lexicalName.indexOf(':');
    if (separator < 0) {
      requirePattern("QName lexical name", lexicalName, NC_NAME);
      return;
    }
    if (separator != lexicalName.lastIndexOf(':')) {
      throw new IllegalArgumentException("Invalid QName lexical name value.");
    }
    requirePattern("QName prefix", lexicalName.substring(0, separator), NC_NAME);
    requirePattern("QName local name", lexicalName.substring(separator + 1), NC_NAME);
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

  private static String formatFloat(Float value) {
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

  private static String formatDouble(Double value) {
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

  private static XmlBinary parseHexBinary(String value) {
    String collapsed = collapseXmlWhitespace(value);
    if (collapsed.length() % 2 != 0 || !collapsed.matches("[0-9A-Fa-f]*")) {
      throw new IllegalArgumentException("Invalid hexBinary value.");
    }
    return new XmlBinary(HexFormat.of().parseHex(collapsed));
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

  private static String lexicalForValidation(String type, Object value) {
    if (value instanceof XmlBinary binary) {
      return "hexBinary".equals(type) ? binary.hexLexicalValue() : binary.base64LexicalValue();
    }
    if (value instanceof XmlQName qName) {
      return qName.lexicalName();
    }
    if (value instanceof List<?> list) {
      return String.join(" ", list.stream().map(Object::toString).toList());
    }
    return switch (type) {
      case "boolean" -> Boolean.TRUE.equals(value) ? "true" : "false";
      case "decimal" -> ((BigDecimal) value).toPlainString();
      case "float" -> formatFloat((Float) value);
      case "double" -> formatDouble((Double) value);
      default -> value.toString();
    };
  }

  private static boolean isValidValue(String type, Object value) {
    try {
      if ("QName".equals(type) || "NOTATION".equals(type)) {
        return value instanceof XmlQName;
      }
      if ("hexBinary".equals(type) || "base64Binary".equals(type)) {
        return value instanceof XmlBinary;
      }
      if (LIST_VALUED.contains(type)) {
        return switch (type) {
          case "NMTOKENS" -> isValidStringList(value, "NMTOKEN");
          case "IDREFS" -> isValidStringList(value, "IDREF");
          case "ENTITIES" -> isValidStringList(value, "ENTITY");
          default -> false;
        };
      }
      if (!isCompatibleValue(type, value)) {
        return false;
      }
      Object parsed =
          parseUnchecked(type, lexicalForValidation(type, value), null, XmlLocation.UNKNOWN, false);
      return Objects.equals(normalizeComparable(value), normalizeComparable(parsed));
    } catch (XmlReadException | IllegalArgumentException exception) {
      return false;
    }
  }

  private static boolean isCompatibleValue(String type, Object value) {
    if (STRING_VALUED.contains(type)) {
      return value instanceof String;
    }
    if (BIG_INTEGER_VALUED.contains(type)) {
      return value instanceof BigInteger;
    }
    if (LONG_VALUED.contains(type)) {
      return value instanceof Long;
    }
    if (INTEGER_VALUED.contains(type)) {
      return value instanceof Integer;
    }
    if (SHORT_VALUED.contains(type)) {
      return value instanceof Short;
    }
    return switch (type) {
      case "boolean" -> value instanceof Boolean;
      case "decimal" -> value instanceof BigDecimal;
      case "float" -> value instanceof Float;
      case "double" -> value instanceof Double;
      case "byte" -> value instanceof Byte;
      case "duration" -> value instanceof XmlDuration;
      case "dateTime" -> value instanceof XmlDateTime;
      case "date" -> value instanceof XmlDate;
      case "time" -> value instanceof XmlTime;
      case "gYear" -> value instanceof XmlGYear;
      case "gYearMonth" -> value instanceof XmlGYearMonth;
      case "gMonth" -> value instanceof XmlGMonth;
      case "gMonthDay" -> value instanceof XmlGMonthDay;
      case "gDay" -> value instanceof XmlGDay;
      case "anyURI" -> value instanceof XmlAnyUri;
      default -> false;
    };
  }

  private static boolean isValidStringList(Object value, String itemType) {
    if (!(value instanceof List<?> list) || list.isEmpty()) {
      return false;
    }
    return list.stream()
        .allMatch(item -> item instanceof String text && isLexicallyValid(itemType, text));
  }

  private static int valueLength(Object value, String lexical) {
    if (value instanceof XmlBinary binary) {
      return binary.bytes().length;
    }
    if (value instanceof List<?> list) {
      return list.size();
    }
    return lexical.length();
  }

  private static boolean equalValue(String type, Object value, String lexical) {
    try {
      Object parsed = parseUnchecked(type, lexical, null, XmlLocation.UNKNOWN, false);
      return Objects.equals(normalizeComparable(value), normalizeComparable(parsed));
    } catch (XmlReadException | IllegalArgumentException exception) {
      return false;
    }
  }

  private static Object normalizeComparable(Object value) {
    if (value instanceof BigDecimal decimal) {
      return decimal.stripTrailingZeros();
    }
    return value;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static boolean rangeMatches(
      String type,
      Object value,
      String minInclusive,
      String maxInclusive,
      String minExclusive,
      String maxExclusive) {
    if (!isOrderedValue(value)) {
      return true;
    }
    Comparable comparable = (Comparable) normalizeComparable(value);
    return compareBound(type, comparable, minInclusive, true, true)
        && compareBound(type, comparable, maxInclusive, false, true)
        && compareBound(type, comparable, minExclusive, true, false)
        && compareBound(type, comparable, maxExclusive, false, false);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static boolean compareBound(
      String type, Comparable value, String bound, boolean lower, boolean inclusive) {
    if (bound == null) {
      return true;
    }
    try {
      Comparable parsed =
          (Comparable)
              normalizeComparable(parseUnchecked(type, bound, null, XmlLocation.UNKNOWN, false));
      int comparison = value.compareTo(parsed);
      return lower
          ? (inclusive ? comparison >= 0 : comparison > 0)
          : (inclusive ? comparison <= 0 : comparison < 0);
    } catch (XmlReadException | IllegalArgumentException exception) {
      return false;
    }
  }

  private static boolean isOrderedValue(Object value) {
    return value instanceof BigDecimal
        || value instanceof BigInteger
        || value instanceof Long
        || value instanceof Integer
        || value instanceof Short
        || value instanceof Byte
        || value instanceof Float
        || value instanceof Double;
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

  private static XmlReadException readException(
      XmlLocation location, String message, Throwable cause) {
    return new XmlReadException(
        new XmlDiagnostic(XmlDiagnosticSeverity.ERROR, "MXJB-DT-001", message, location), cause);
  }
}
