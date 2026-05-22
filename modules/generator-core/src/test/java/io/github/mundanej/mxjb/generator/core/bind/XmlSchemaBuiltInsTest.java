package io.github.mundanej.mxjb.generator.core.bind;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class XmlSchemaBuiltInsTest {
  @Test
  void reportsSupportedBuiltInsAndJavaMappings() {
    assertTrue(XmlSchemaBuiltIns.isSupported("dateTime"));
    assertTrue(XmlSchemaBuiltIns.isListValued("IDREFS"));
    assertTrue(XmlSchemaBuiltIns.isBigIntegerValued("unsignedLong"));
    assertTrue(XmlSchemaBuiltIns.isBigDecimalValued("decimal"));
    assertTrue(XmlSchemaBuiltIns.isRuntimeValued("QName"));
    assertFalse(XmlSchemaBuiltIns.isSupported("anyType"));
    assertNull(XmlSchemaBuiltIns.javaType("anyType"));
  }

  @Test
  void validatesScalarAndListLexicalFormsForBindingDefaults() {
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("string", "a\tb"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("normalizedString", "a\tb"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("token", " a\t b "));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("language", "en-US"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("Name", "p:name"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("NCName", "name"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("ID", "id1"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("IDREF", "id1"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("ENTITY", "entity1"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("NMTOKEN", "name.1"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("NMTOKENS", "A B"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("IDREFS", "A B"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("ENTITIES", "A B"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("boolean", "1"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("decimal", "12.30"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("float", "INF"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("float", "1.25"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("double", "-INF"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("duration", "P1D"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("dateTime", "2026-05-21T12:30:00Z"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("time", "24:00:00"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("date", "2026-05-21"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("gYearMonth", "2026-05"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("gYear", "2026"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("gMonthDay", "--05-21"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("gDay", "---21"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("gMonth", "--05"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("hexBinary", "0A0b"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("base64Binary", "AQID"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("anyURI", ""));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("QName", "p:name"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("NOTATION", "name"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("integer", "12"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("nonPositiveInteger", "0"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("negativeInteger", "-1"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("long", "12"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("int", "12"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("short", "12"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("byte", "12"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("nonNegativeInteger", "0"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("unsignedLong", "18446744073709551615"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("unsignedInt", "4294967295"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("unsignedShort", "65535"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("unsignedByte", "255"));
    assertTrue(XmlSchemaBuiltIns.isLexicallyValid("positiveInteger", "1"));

    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("unknown", "value"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("language", "toolongtag"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("NCName", "p:name"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("NMTOKENS", " "));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("boolean", "yes"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("float", "Infinity"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("double", "Infinity"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("duration", "P"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("duration", "PT"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("dateTime", "2026-02-30T12:30:00"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("dateTime", "2026-05-21T12:30:00+14:01"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("dateTime", "0000-01-01T00:00:00"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("time", "24:00:00.1"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("date", "2026-13-01"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("gYearMonth", "2026-13"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("gYearMonth", "0000-01"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("gMonthDay", "--02-30"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("gDay", "---32"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("gMonth", "--13"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("gYear", "0000"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("hexBinary", "ABC"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("base64Binary", "A"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("QName", "p:name:again"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("unsignedInt", "-1"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("unsignedShort", "70000"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("unsignedByte", "300"));
    assertFalse(XmlSchemaBuiltIns.isLexicallyValid("positiveInteger", "0"));
  }

  @Test
  void validatesBindingRestrictionFacetsAgainstParsedValues() {
    BindingSimpleRestriction decimalRestriction =
        new BindingSimpleRestriction(
            "decimal",
            List.of("12.30"),
            null,
            null,
            null,
            "10",
            "20",
            null,
            null,
            4,
            2,
            null,
            List.of("[0-9]+\\.[0-9]+"));
    assertTrue(XmlSchemaBuiltIns.matchesRestriction("decimal", "12.30", decimalRestriction));
    assertFalse(XmlSchemaBuiltIns.matchesRestriction("decimal", "12.345", decimalRestriction));

    BindingSimpleRestriction stringRestriction =
        new BindingSimpleRestriction(
            "string", List.of(), 3, 2, 4, null, null, null, null, null, null, null, List.of("a.c"));
    assertTrue(XmlSchemaBuiltIns.matchesRestriction("string", "abc", stringRestriction));
    assertFalse(XmlSchemaBuiltIns.matchesRestriction("string", "abcd", stringRestriction));
    assertFalse(XmlSchemaBuiltIns.matchesRestriction("string", "axd", stringRestriction));

    BindingSimpleRestriction exclusiveRestriction =
        new BindingSimpleRestriction(
            "int", List.of(), null, null, null, null, null, "5", "10", null, null, null, List.of());
    assertTrue(XmlSchemaBuiltIns.matchesRestriction("int", "6", exclusiveRestriction));
    assertFalse(XmlSchemaBuiltIns.matchesRestriction("int", "5", exclusiveRestriction));
    assertFalse(XmlSchemaBuiltIns.matchesRestriction("int", "10", exclusiveRestriction));

    BindingSimpleRestriction binaryRestriction =
        new BindingSimpleRestriction(
            "hexBinary",
            List.of("0a0b"),
            2,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of());
    assertTrue(XmlSchemaBuiltIns.matchesRestriction("hexBinary", "0A0B", binaryRestriction));
    assertFalse(XmlSchemaBuiltIns.matchesRestriction("hexBinary", "0A0B0C", binaryRestriction));

    BindingSimpleRestriction listRestriction =
        new BindingSimpleRestriction(
            "NMTOKENS",
            List.of(),
            2,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of());
    assertTrue(XmlSchemaBuiltIns.matchesRestriction("NMTOKENS", "A B", listRestriction));
    assertFalse(XmlSchemaBuiltIns.matchesRestriction("NMTOKENS", "A B C", listRestriction));

    assertTrue(XmlSchemaBuiltIns.matchesRestriction("string", "abc", null));
    assertTrue(XmlSchemaBuiltIns.hasPrefixedQNameLexical("QName", "p:name"));
    assertFalse(XmlSchemaBuiltIns.hasPrefixedQNameLexical("QName", "name"));
    assertFalse(XmlSchemaBuiltIns.hasPrefixedQNameLexical("string", "p:name"));
  }
}
