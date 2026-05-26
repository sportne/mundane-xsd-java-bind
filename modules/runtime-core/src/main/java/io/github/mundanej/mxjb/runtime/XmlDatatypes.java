package io.github.mundanej.mxjb.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** XML Schema 1.0 datatype lexical conversion and generated-validation helpers. */
public final class XmlDatatypes {
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
    return XmlDatatypeLists.parseValues(
        itemType, value, (type, token) -> parse(type, token, input, location));
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
      case "float" -> XmlDatatypeNumeric.formatFloat((Float) value);
      case "double" -> XmlDatatypeNumeric.formatDouble((Double) value);
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
        case "normalizedString" -> XmlDatatypeLexical.replaceWhitespace(value);
        case "token" -> XmlDatatypeLexical.collapseWhitespace(value);
        case "language" ->
            XmlDatatypeLexical.requirePattern(
                type, XmlDatatypeLexical.collapseWhitespace(value), XmlDatatypeLexical.LANGUAGE);
        case "Name" ->
            XmlDatatypeLexical.requirePattern(
                type, XmlDatatypeLexical.collapseWhitespace(value), XmlDatatypeLexical.NAME);
        case "NCName", "ID", "IDREF", "ENTITY" ->
            XmlDatatypeLexical.requirePattern(
                type, XmlDatatypeLexical.collapseWhitespace(value), XmlDatatypeLexical.NC_NAME);
        case "NMTOKEN" ->
            XmlDatatypeLexical.requirePattern(
                type, XmlDatatypeLexical.collapseWhitespace(value), XmlDatatypeLexical.NMTOKEN);
        case "NMTOKENS" -> tokenList(value, "NMTOKEN");
        case "IDREFS" -> tokenList(value, "IDREF");
        case "ENTITIES" -> tokenList(value, "ENTITY");
        case "boolean" -> XmlDatatypeNumeric.parseBoolean(value);
        case "decimal" -> new BigDecimal(XmlDatatypeLexical.collapseWhitespace(value));
        case "float" -> XmlDatatypeNumeric.parseFloat(value);
        case "double" -> XmlDatatypeNumeric.parseDouble(value);
        case "integer" -> new BigInteger(XmlDatatypeLexical.collapseWhitespace(value));
        case "nonPositiveInteger" ->
            XmlDatatypeNumeric.requireMax(
                new BigInteger(XmlDatatypeLexical.collapseWhitespace(value)),
                BigInteger.ZERO,
                type);
        case "negativeInteger" ->
            XmlDatatypeNumeric.requireMax(
                new BigInteger(XmlDatatypeLexical.collapseWhitespace(value)),
                BigInteger.valueOf(-1),
                type);
        case "long" -> Long.valueOf(XmlDatatypeLexical.collapseWhitespace(value));
        case "int" -> Integer.valueOf(XmlDatatypeLexical.collapseWhitespace(value));
        case "short" -> Short.valueOf(XmlDatatypeLexical.collapseWhitespace(value));
        case "byte" -> Byte.valueOf(XmlDatatypeLexical.collapseWhitespace(value));
        case "nonNegativeInteger" ->
            XmlDatatypeNumeric.requireMin(
                new BigInteger(XmlDatatypeLexical.collapseWhitespace(value)),
                BigInteger.ZERO,
                type);
        case "unsignedLong" ->
            XmlDatatypeNumeric.requireRange(
                new BigInteger(XmlDatatypeLexical.collapseWhitespace(value)),
                BigInteger.ZERO,
                new BigInteger("18446744073709551615"),
                type);
        case "unsignedInt" ->
            XmlDatatypeNumeric.requireRangeLong(
                XmlDatatypeLexical.collapseWhitespace(value), 0L, 4294967295L, type);
        case "unsignedShort" ->
            XmlDatatypeNumeric.requireRangeInt(
                XmlDatatypeLexical.collapseWhitespace(value), 0, 65535, type);
        case "unsignedByte" ->
            XmlDatatypeNumeric.requireRangeShort(
                XmlDatatypeLexical.collapseWhitespace(value), 0, 255, type);
        case "positiveInteger" ->
            XmlDatatypeNumeric.requireMin(
                new BigInteger(XmlDatatypeLexical.collapseWhitespace(value)), BigInteger.ONE, type);
        case "duration" -> new XmlDuration(XmlDatatypeDateTime.requireDurationLexical(value));
        case "dateTime" -> new XmlDateTime(XmlDatatypeDateTime.requireDateTimeLexical(value));
        case "date" -> new XmlDate(XmlDatatypeDateTime.requireDateLexical(value));
        case "time" -> new XmlTime(XmlDatatypeDateTime.requireTimeLexical(value));
        case "gYear" -> new XmlGYear(XmlDatatypeDateTime.requireGYearLexical(value));
        case "gYearMonth" -> new XmlGYearMonth(XmlDatatypeDateTime.requireGYearMonthLexical(value));
        case "gMonth" -> new XmlGMonth(XmlDatatypeDateTime.requireGMonthLexical(value));
        case "gMonthDay" -> new XmlGMonthDay(XmlDatatypeDateTime.requireGMonthDayLexical(value));
        case "gDay" -> new XmlGDay(XmlDatatypeDateTime.requireGDayLexical(value));
        case "hexBinary" -> XmlDatatypeBinary.parseHexBinary(value);
        case "base64Binary" -> XmlDatatypeBinary.parseBase64Binary(value);
        case "anyURI" -> new XmlAnyUri(XmlDatatypeLexical.collapseWhitespace(value));
        case "QName", "NOTATION" -> XmlDatatypeQNames.parseQName(value, input);
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

  private static List<String> tokenList(String value, String itemType) throws XmlReadException {
    return XmlDatatypeLists.tokenList(
        value,
        itemType,
        (type, token) -> parseUnchecked(type, token, null, XmlLocation.UNKNOWN, false));
  }

  static void requireQNameValue(String namespaceUri, String localName, String lexicalName) {
    XmlDatatypeQNames.requireQNameValue(namespaceUri, localName, lexicalName);
  }

  static String requireDurationLexical(String value) {
    return XmlDatatypeDateTime.requireDurationLexical(value);
  }

  static String requireDateTimeLexical(String value) {
    return XmlDatatypeDateTime.requireDateTimeLexical(value);
  }

  static String requireDateLexical(String value) {
    return XmlDatatypeDateTime.requireDateLexical(value);
  }

  static String requireTimeLexical(String value) {
    return XmlDatatypeDateTime.requireTimeLexical(value);
  }

  static String requireGYearLexical(String value) {
    return XmlDatatypeDateTime.requireGYearLexical(value);
  }

  static String requireGYearMonthLexical(String value) {
    return XmlDatatypeDateTime.requireGYearMonthLexical(value);
  }

  static String requireGMonthLexical(String value) {
    return XmlDatatypeDateTime.requireGMonthLexical(value);
  }

  static String requireGMonthDayLexical(String value) {
    return XmlDatatypeDateTime.requireGMonthDayLexical(value);
  }

  static String requireGDayLexical(String value) {
    return XmlDatatypeDateTime.requireGDayLexical(value);
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
      case "float" -> XmlDatatypeNumeric.formatFloat((Float) value);
      case "double" -> XmlDatatypeNumeric.formatDouble((Double) value);
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
