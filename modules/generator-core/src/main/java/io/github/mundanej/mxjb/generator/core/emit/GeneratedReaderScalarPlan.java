package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.bind.BindingTypeReference;
import io.github.mundanej.mxjb.generator.core.bind.XmlSchemaBuiltIns;
import java.util.Objects;

record GeneratedReaderScalarPlan(BindingTypeReference reference, String javaType) {
  GeneratedReaderScalarPlan {
    Objects.requireNonNull(reference, "reference");
    Objects.requireNonNull(javaType, "javaType");
  }

  static GeneratedReaderScalarPlan of(BindingTypeReference reference) {
    return new GeneratedReaderScalarPlan(reference, qualifiedScalarType(reference.name()));
  }

  String parseExpression(String valueExpression) {
    if ("list".equals(reference.kind())) {
      return "io.github.mundanej.mxjb.runtime.XmlDatatypes.parseList(\""
          + escape(reference.itemType().name())
          + "\", "
          + valueExpression
          + ", input, input.location(), "
          + scalarClassLiteral(reference.itemType())
          + ")";
    }
    if ("union".equals(reference.kind())) {
      return valueExpression;
    }
    if (XmlSchemaBuiltIns.isListValued(reference.name())) {
      return "io.github.mundanej.mxjb.runtime.XmlDatatypes.parseList(\""
          + escape(listBuiltInItemType(reference.name()))
          + "\", "
          + valueExpression
          + ", input, input.location(), String.class)";
    }
    if ("string".equals(reference.name())) {
      return valueExpression;
    }
    return "("
        + javaType
        + ") io.github.mundanej.mxjb.runtime.XmlDatatypes.parse(\""
        + escape(reference.name())
        + "\", "
        + valueExpression
        + ", input, input.location())";
  }

  String datatypeListItemName() {
    if ("list".equals(reference.kind())) {
      return reference.itemType().name();
    }
    return listBuiltInItemType(reference.name());
  }

  String datatypeClassLiteral() {
    if ("list".equals(reference.kind())) {
      return scalarClassLiteral(reference.itemType());
    }
    return scalarClassLiteral(reference);
  }

  private static String qualifiedScalarType(String scalar) {
    String javaType = XmlSchemaBuiltIns.javaType(scalar);
    if (javaType == null) {
      return "String";
    }
    return switch (javaType) {
      case "List<String>" -> "java.util.List<String>";
      case "BigInteger" -> "java.math.BigInteger";
      case "BigDecimal" -> "java.math.BigDecimal";
      case "XmlDuration",
          "XmlDateTime",
          "XmlDate",
          "XmlTime",
          "XmlGYear",
          "XmlGYearMonth",
          "XmlGMonth",
          "XmlGMonthDay",
          "XmlGDay",
          "XmlBinary",
          "XmlAnyUri",
          "XmlQName" ->
          "io.github.mundanej.mxjb.runtime." + javaType;
      default -> javaType;
    };
  }

  private static String listBuiltInItemType(String name) {
    return switch (name) {
      case "NMTOKENS" -> "NMTOKEN";
      case "IDREFS" -> "IDREF";
      case "ENTITIES" -> "ENTITY";
      default -> "string";
    };
  }

  private static String scalarClassLiteral(BindingTypeReference reference) {
    return switch (reference.name()) {
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
          "String.class";
      case "boolean" -> "Boolean.class";
      case "decimal" -> "java.math.BigDecimal.class";
      case "float" -> "Float.class";
      case "double" -> "Double.class";
      case "integer",
          "nonPositiveInteger",
          "negativeInteger",
          "nonNegativeInteger",
          "positiveInteger",
          "unsignedLong" ->
          "java.math.BigInteger.class";
      case "long", "unsignedInt" -> "Long.class";
      case "int", "unsignedShort" -> "Integer.class";
      case "short", "unsignedByte" -> "Short.class";
      case "byte" -> "Byte.class";
      case "duration" -> "io.github.mundanej.mxjb.runtime.XmlDuration.class";
      case "dateTime" -> "io.github.mundanej.mxjb.runtime.XmlDateTime.class";
      case "date" -> "io.github.mundanej.mxjb.runtime.XmlDate.class";
      case "time" -> "io.github.mundanej.mxjb.runtime.XmlTime.class";
      case "gYear" -> "io.github.mundanej.mxjb.runtime.XmlGYear.class";
      case "gYearMonth" -> "io.github.mundanej.mxjb.runtime.XmlGYearMonth.class";
      case "gMonth" -> "io.github.mundanej.mxjb.runtime.XmlGMonth.class";
      case "gMonthDay" -> "io.github.mundanej.mxjb.runtime.XmlGMonthDay.class";
      case "gDay" -> "io.github.mundanej.mxjb.runtime.XmlGDay.class";
      case "hexBinary", "base64Binary" -> "io.github.mundanej.mxjb.runtime.XmlBinary.class";
      case "anyURI" -> "io.github.mundanej.mxjb.runtime.XmlAnyUri.class";
      case "QName", "NOTATION" -> "io.github.mundanej.mxjb.runtime.XmlQName.class";
      default -> "Object.class";
    };
  }

  private static String escape(String value) {
    StringBuilder escaped = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '\\' -> escaped.append("\\\\");
        case '"' -> escaped.append("\\\"");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> {
          if (character < 0x20) {
            escaped.append(String.format("\\u%04x", (int) character));
          } else {
            escaped.append(character);
          }
        }
      }
    }
    return escaped.toString();
  }
}
