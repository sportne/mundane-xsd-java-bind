package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class JavaNames {
  private static final Set<String> KEYWORDS =
      Set.of(
          "abstract",
          "assert",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "default",
          "do",
          "double",
          "else",
          "enum",
          "extends",
          "final",
          "finally",
          "float",
          "for",
          "goto",
          "if",
          "implements",
          "import",
          "instanceof",
          "int",
          "interface",
          "long",
          "native",
          "new",
          "package",
          "private",
          "protected",
          "public",
          "record",
          "return",
          "sealed",
          "short",
          "static",
          "strictfp",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "try",
          "var",
          "void",
          "volatile",
          "while",
          "yield");

  private JavaNames() {}

  static String uniqueTypeName(SchemaQName schemaName, Set<String> used) {
    return unique(typeName(schemaName), used);
  }

  static String uniqueFieldName(SchemaQName schemaName, Set<String> used) {
    return unique(fieldName(schemaName), used);
  }

  static String typeName(SchemaQName schemaName) {
    return sanitizeIdentifier(upperCamel(tokens(schemaName.localName())), true);
  }

  static String fieldName(SchemaQName schemaName) {
    return sanitizeIdentifier(lowerCamel(tokens(schemaName.localName())), false);
  }

  static String fieldNameFromTypeName(String typeName) {
    if (typeName == null || typeName.isBlank()) {
      return "value";
    }
    return sanitizeIdentifier(
        typeName.substring(0, 1).toLowerCase(Locale.ROOT) + typeName.substring(1), false);
  }

  static String unique(String base, Set<String> used) {
    String candidate = base;
    int suffix = 2;
    while (!used.add(candidate)) {
      candidate = base + suffix;
      suffix++;
    }
    return candidate;
  }

  static boolean isPackageName(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    for (String part : splitOnDot(value)) {
      if (part.isEmpty() || !part.equals(sanitizeIdentifier(part, false))) {
        return false;
      }
    }
    return true;
  }

  static List<String> packageTokens(String value) {
    return tokens(value).stream()
        .map(token -> sanitizeIdentifier(token.toLowerCase(Locale.ROOT), false))
        .filter(token -> !token.isBlank())
        .toList();
  }

  static List<String> splitOnDot(String value) {
    List<String> result = new ArrayList<>();
    int start = 0;
    for (int index = 0; index < value.length(); index++) {
      if (value.charAt(index) == '.') {
        result.add(value.substring(start, index));
        start = index + 1;
      }
    }
    result.add(value.substring(start));
    return result;
  }

  static String capitalize(String value) {
    String lower = value.toLowerCase(Locale.ROOT);
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }

  static String sanitizeIdentifier(String value, boolean typeName) {
    if (value == null || value.isBlank()) {
      value = typeName ? "Value" : "value";
    }
    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      builder.append(Character.isLetterOrDigit(character) || character == '_' ? character : '_');
    }
    String sanitized = builder.toString();
    if (!Character.isJavaIdentifierStart(sanitized.charAt(0))) {
      sanitized = "_" + sanitized;
    }
    if (KEYWORDS.contains(sanitized)) {
      sanitized = "_" + sanitized;
    }
    return sanitized;
  }

  private static List<String> tokens(String value) {
    List<String> result = new ArrayList<>();
    StringBuilder token = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (Character.isLetterOrDigit(character)) {
        token.append(character);
      } else if (!token.isEmpty()) {
        result.add(token.toString());
        token.setLength(0);
      }
    }
    if (!token.isEmpty()) {
      result.add(token.toString());
    }
    return result;
  }

  private static String upperCamel(List<String> tokens) {
    if (tokens.isEmpty()) {
      return "Value";
    }
    return tokens.stream()
        .map(JavaNames::capitalize)
        .collect(java.util.stream.Collectors.joining());
  }

  private static String lowerCamel(List<String> tokens) {
    String upper = upperCamel(tokens);
    return Character.toLowerCase(upper.charAt(0)) + upper.substring(1);
  }
}
