package io.github.mundanej.mxjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RuntimePrimitivesTest {
  @Test
  void xmlNamesRequireLocalNamesAndAllowEmptyNamespaces() {
    XmlName name = new XmlName("", "order");

    assertEquals("", name.namespaceUri());
    assertEquals("order", name.localName());
    assertThrows(NullPointerException.class, () -> new XmlName(null, "order"));
    assertThrows(NullPointerException.class, () -> new XmlName("", null));
    assertThrows(IllegalArgumentException.class, () -> new XmlName("", " "));
  }

  @Test
  void xmlLocationsRepresentUnknownAndValidateKnownCoordinates() {
    assertEquals("", XmlLocation.UNKNOWN.systemId());
    assertEquals(-1, XmlLocation.UNKNOWN.lineNumber());
    assertEquals(-1, XmlLocation.UNKNOWN.columnNumber());

    XmlLocation location = new XmlLocation("order.xml", 12, 4);

    assertEquals("order.xml", location.systemId());
    assertEquals(12, location.lineNumber());
    assertEquals(4, location.columnNumber());
    assertThrows(NullPointerException.class, () -> new XmlLocation(null, -1, -1));
    assertThrows(IllegalArgumentException.class, () -> new XmlLocation("", 0, -1));
    assertThrows(IllegalArgumentException.class, () -> new XmlLocation("", -1, 0));
  }

  @Test
  void diagnosticsAndCheckedExceptionsRetainStableData() {
    XmlDiagnostic diagnostic =
        new XmlDiagnostic(
            XmlDiagnosticSeverity.ERROR, "MXJB-R-001", "Expected order.", XmlLocation.UNKNOWN);
    IllegalStateException cause = new IllegalStateException("cause");

    XmlReadException readException = new XmlReadException(diagnostic, cause);
    XmlWriteException writeException = new XmlWriteException(diagnostic, cause);

    assertSame(diagnostic, readException.diagnostic());
    assertSame(cause, readException.getCause());
    assertEquals("Expected order.", readException.getMessage());
    assertSame(diagnostic, writeException.diagnostic());
    assertSame(cause, writeException.getCause());
    assertEquals("Expected order.", writeException.getMessage());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new XmlDiagnostic(
                XmlDiagnosticSeverity.ERROR, " ", "Expected order.", XmlLocation.UNKNOWN));
  }

  @Test
  void validationResultsAreImmutableAndReportValidity() {
    ValidationError error =
        new ValidationError("MXJB-V-001", "Missing required element.", XmlLocation.UNKNOWN);
    List<ValidationError> errors = new ArrayList<>();
    errors.add(error);

    ValidationResult result = ValidationResult.invalid(errors);
    errors.clear();

    assertFalse(result.isValid());
    assertEquals(List.of(error), result.errors());
    assertEquals(List.of(error), ValidationResult.invalid(error).errors());
    assertThrows(UnsupportedOperationException.class, () -> result.errors().add(error));
    assertTrue(ValidationResult.valid().isValid());
    assertThrows(
        IllegalArgumentException.class,
        () -> new ValidationError("", "message", XmlLocation.UNKNOWN));
    assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid(List.of()));
    assertThrows(NullPointerException.class, () -> new ValidationResult(null));
    assertThrows(
        NullPointerException.class, () -> ValidationResult.invalid((List<ValidationError>) null));
    assertThrows(NullPointerException.class, () -> ValidationResult.invalid(null, error));
    assertThrows(
        NullPointerException.class,
        () -> ValidationResult.invalid(error, (ValidationError[]) null));
  }

  @Test
  void xmlFragmentsRetainExpandedNamesAttributesAndContentImmutably() {
    XmlName extensionName = new XmlName("urn:extension", "note");
    XmlName attributeName = new XmlName("", "code");
    XmlAttribute attribute = new XmlAttribute(attributeName, "A-1");
    XmlFragment child = new XmlFragment(new XmlName("", "child"), List.of(), List.of());
    List<XmlAttribute> attributes = new ArrayList<>();
    attributes.add(attribute);
    List<XmlFragmentContent> content = new ArrayList<>();
    content.add(new XmlFragmentText("before"));
    content.add(new XmlFragmentElement(child));

    XmlFragment fragment = new XmlFragment(extensionName, attributes, content);
    attributes.clear();
    content.clear();

    assertEquals(extensionName, fragment.name());
    assertEquals(List.of(attribute), fragment.attributes());
    assertEquals(2, fragment.content().size());
    assertThrows(UnsupportedOperationException.class, () -> fragment.attributes().add(attribute));
    assertThrows(UnsupportedOperationException.class, () -> fragment.content().clear());
    assertThrows(NullPointerException.class, () -> new XmlAttribute(null, "value"));
    assertThrows(NullPointerException.class, () -> new XmlAttribute(attributeName, null));
    assertThrows(NullPointerException.class, () -> new XmlFragmentText(null));
    assertThrows(NullPointerException.class, () -> new XmlFragmentElement(null));
    assertThrows(NullPointerException.class, () -> new XmlFragment(null, List.of(), List.of()));
    assertThrows(NullPointerException.class, () -> new XmlFragment(extensionName, null, List.of()));
    assertThrows(NullPointerException.class, () -> new XmlFragment(extensionName, List.of(), null));
  }

  @Test
  void noCauseExceptionConstructorsRetainDiagnostics() {
    XmlDiagnostic diagnostic =
        new XmlDiagnostic(
            XmlDiagnosticSeverity.WARNING, "MXJB-W-001", "Optional warning.", XmlLocation.UNKNOWN);

    assertSame(diagnostic, new XmlReadException(diagnostic).diagnostic());
    assertSame(diagnostic, new XmlWriteException(diagnostic).diagnostic());
    assertThrows(NullPointerException.class, () -> readException(null));
    assertThrows(NullPointerException.class, () -> writeException(null));
  }

  @Test
  void pullInterfacesSupportGeneratedReaderAndWriterShapes()
      throws XmlReadException, XmlWriteException {
    XmlName order = new XmlName("urn:orders", "order");
    XmlName id = new XmlName("", "id");
    FakeReader reader = new FakeReader(order, id, "A-1");
    RecordingOutput output = new RecordingOutput();

    assertEquals(XmlEventKind.START_ELEMENT, reader.kind());
    assertEquals(null, reader.namespaceUriForPrefix("missing"));
    assertEquals(order, reader.name());
    assertEquals(1, reader.attributeCount());
    assertEquals(id, reader.attributeName(0));
    assertEquals("A-1", reader.attributeValue(0));
    assertTrue(reader.next());
    assertEquals(XmlEventKind.END_ELEMENT, reader.kind());
    assertFalse(reader.next());

    output.startDocument();
    output.startElement(order);
    output.attribute(id, "A-1");
    output.text("body");
    assertEquals("localName", output.qNameText(new XmlQName("", "localName")));
    output.endElement(order);
    output.endDocument();
    output.flush();

    assertEquals(
        List.of(
            "startDocument",
            "start:urn:orders:order",
            "attr:id=A-1",
            "text:body",
            "end:order",
            "endDocument",
            "flush"),
        output.events);
  }

  @Test
  void xmlSchemaDatatypeValueTypesParseFormatAndRetainExactLexicalSemantics()
      throws XmlReadException, XmlWriteException {
    assertEquals(true, XmlDatatypes.parse("boolean", " 1 ", null, XmlLocation.UNKNOWN));
    assertEquals(
        new java.math.BigInteger("18446744073709551615"),
        XmlDatatypes.parse("unsignedLong", "18446744073709551615", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlDuration("P1Y2M3DT4H5M6.7S"),
        XmlDatatypes.parse("duration", "P1Y2M3DT4H5M6.7S", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlDateTime("2026-05-21T12:30:00-04:00"),
        XmlDatatypes.parse("dateTime", "2026-05-21T12:30:00-04:00", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlGMonthDay("--05-21"),
        XmlDatatypes.parse("gMonthDay", "--05-21", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlAnyUri("urn:test:value"),
        XmlDatatypes.parse("anyURI", "urn:test:value", null, XmlLocation.UNKNOWN));
    assertEquals(
        List.of("A", "B"), XmlDatatypes.parse("NMTOKENS", " A   B ", null, XmlLocation.UNKNOWN));

    XmlBinary hex = (XmlBinary) XmlDatatypes.parse("hexBinary", "0A0b", null, XmlLocation.UNKNOWN);
    assertEquals("0a0b", XmlDatatypes.format("hexBinary", hex, new RecordingOutput()));
    XmlBinary base64 =
        (XmlBinary) XmlDatatypes.parse("base64Binary", "AQID", null, XmlLocation.UNKNOWN);
    assertEquals("AQID", XmlDatatypes.format("base64Binary", base64, new RecordingOutput()));

    assertTrue(
        XmlDatatypes.matchesFacets(
            "decimal",
            new java.math.BigDecimal("12.30"),
            List.of(),
            null,
            null,
            null,
            "10",
            "20",
            null,
            null,
            4,
            2,
            List.of("[0-9]+\\.[0-9]+")));
    assertFalse(XmlDatatypes.isLexicallyValid("unsignedByte", "300"));
    assertThrows(
        XmlReadException.class,
        () -> XmlDatatypes.parse("positiveInteger", "0", null, XmlLocation.UNKNOWN));
  }

  @Test
  void temporalAndQualifiedDatatypeWrappersValidateConstructionAndToString()
      throws XmlReadException {
    List<Object> values =
        List.of(
            new XmlDuration("P1D"),
            new XmlDateTime("2026-05-21T12:30:00Z"),
            new XmlDate("2026-05-21"),
            new XmlTime("12:30:00"),
            new XmlGYear("2026"),
            new XmlGYearMonth("2026-05"),
            new XmlGMonth("--05"),
            new XmlGMonthDay("--05-21"),
            new XmlGDay("---21"),
            new XmlAnyUri(""),
            new XmlQName("urn:test", "name", "p:name"));

    assertEquals(
        List.of(
            "P1D",
            "2026-05-21T12:30:00Z",
            "2026-05-21",
            "12:30:00",
            "2026",
            "2026-05",
            "--05",
            "--05-21",
            "---21",
            "",
            "XmlQName[namespaceUri=urn:test, localName=name, lexicalName=p:name]"),
        values.stream().map(Object::toString).toList());

    assertEquals(
        new XmlDate("2026-05-21"),
        XmlDatatypes.parse("date", "2026-05-21", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlTime("12:30:00Z"),
        XmlDatatypes.parse("time", "12:30:00Z", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlQName("urn:test", "name", "p:name"), new XmlQName("urn:test", "name", "q:name"));
    assertEquals(
        new XmlQName("urn:test", "name", "p:name").hashCode(),
        new XmlQName("urn:test", "name", "q:name").hashCode());
    assertEquals(
        new XmlGYear("2026"), XmlDatatypes.parse("gYear", "2026", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlGYearMonth("2026-05"),
        XmlDatatypes.parse("gYearMonth", "2026-05", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlGMonth("--05"), XmlDatatypes.parse("gMonth", "--05", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlGDay("---21"), XmlDatatypes.parse("gDay", "---21", null, XmlLocation.UNKNOWN));

    assertThrows(NullPointerException.class, () -> new XmlDuration(null));
    assertThrows(IllegalArgumentException.class, () -> new XmlDuration(" "));
    assertThrows(IllegalArgumentException.class, () -> new XmlDuration("P"));
    assertThrows(IllegalArgumentException.class, () -> new XmlDuration("PT"));
    assertThrows(IllegalArgumentException.class, () -> new XmlDateTime(" "));
    assertThrows(IllegalArgumentException.class, () -> new XmlDateTime("2026-02-30T12:30:00"));
    assertThrows(IllegalArgumentException.class, () -> new XmlDateTime("0000-01-01T00:00:00"));
    assertThrows(NullPointerException.class, () -> new XmlDate(null));
    assertThrows(IllegalArgumentException.class, () -> new XmlDate(" "));
    assertThrows(IllegalArgumentException.class, () -> new XmlDate("2026-13-01"));
    assertThrows(IllegalArgumentException.class, () -> new XmlDate("0000-01-01"));
    assertThrows(IllegalArgumentException.class, () -> new XmlTime(" "));
    assertThrows(IllegalArgumentException.class, () -> new XmlTime("25:00:00"));
    assertThrows(NullPointerException.class, () -> new XmlGYear(null));
    assertThrows(IllegalArgumentException.class, () -> new XmlGYear(" "));
    assertThrows(IllegalArgumentException.class, () -> new XmlGYear("99"));
    assertThrows(IllegalArgumentException.class, () -> new XmlGYear("0000"));
    assertThrows(IllegalArgumentException.class, () -> new XmlGYearMonth(" "));
    assertThrows(IllegalArgumentException.class, () -> new XmlGYearMonth("2026-13"));
    assertThrows(IllegalArgumentException.class, () -> new XmlGYearMonth("0000-01"));
    assertThrows(NullPointerException.class, () -> new XmlGMonth(null));
    assertThrows(IllegalArgumentException.class, () -> new XmlGMonth(" "));
    assertThrows(IllegalArgumentException.class, () -> new XmlGMonth("--13"));
    assertThrows(IllegalArgumentException.class, () -> new XmlGMonthDay(" "));
    assertThrows(IllegalArgumentException.class, () -> new XmlGMonthDay("--02-30"));
    assertThrows(NullPointerException.class, () -> new XmlGDay(null));
    assertThrows(IllegalArgumentException.class, () -> new XmlGDay(" "));
    assertThrows(IllegalArgumentException.class, () -> new XmlGDay("---32"));
    assertThrows(NullPointerException.class, () -> new XmlAnyUri(null));
    assertThrows(NullPointerException.class, () -> new XmlQName(null, "name"));
    assertThrows(NullPointerException.class, () -> new XmlQName("", null));
    assertThrows(IllegalArgumentException.class, () -> new XmlQName("", " "));
    assertThrows(IllegalArgumentException.class, () -> new XmlQName("", "bad:name"));
    assertThrows(IllegalArgumentException.class, () -> new XmlQName("", "name", " "));
    assertThrows(IllegalArgumentException.class, () -> new XmlQName("", "name", "bad:name:again"));
  }

  @Test
  void xmlDatatypesCoverXsd10BuiltInLexicalFamilies() throws XmlReadException, XmlWriteException {
    RecordingOutput output = new RecordingOutput();

    assertEquals("a b", XmlDatatypes.parse("normalizedString", "a\tb", null, XmlLocation.UNKNOWN));
    assertEquals("a b", XmlDatatypes.parse("token", " a\t  b ", null, XmlLocation.UNKNOWN));
    assertEquals("en-US", XmlDatatypes.parse("language", "en-US", null, XmlLocation.UNKNOWN));
    assertEquals("root:name", XmlDatatypes.parse("Name", "root:name", null, XmlLocation.UNKNOWN));
    assertEquals("name", XmlDatatypes.parse("NCName", "name", null, XmlLocation.UNKNOWN));
    assertEquals("ID1", XmlDatatypes.parse("ID", "ID1", null, XmlLocation.UNKNOWN));
    assertEquals("ID1", XmlDatatypes.parse("IDREF", "ID1", null, XmlLocation.UNKNOWN));
    assertEquals("entity1", XmlDatatypes.parse("ENTITY", "entity1", null, XmlLocation.UNKNOWN));
    assertEquals("token.1", XmlDatatypes.parse("NMTOKEN", "token.1", null, XmlLocation.UNKNOWN));
    assertEquals(List.of("A", "B"), XmlDatatypes.parse("IDREFS", "A B", null, XmlLocation.UNKNOWN));
    assertEquals(
        List.of("E1", "E2"), XmlDatatypes.parse("ENTITIES", "E1 E2", null, XmlLocation.UNKNOWN));
    assertEquals(
        new XmlQName("urn:test", "value", "p:value"),
        XmlDatatypes.parse("QName", "p:value", prefixReader(), XmlLocation.UNKNOWN));
    assertEquals(
        new XmlQName("urn:test", "notation", "p:notation"),
        XmlDatatypes.parse("NOTATION", "p:notation", prefixReader(), XmlLocation.UNKNOWN));
    assertEquals(false, XmlDatatypes.parse("boolean", "false", null, XmlLocation.UNKNOWN));
    assertEquals(
        Float.POSITIVE_INFINITY, XmlDatatypes.parse("float", "INF", null, XmlLocation.UNKNOWN));
    assertEquals(Float.NaN, XmlDatatypes.parse("float", "NaN", null, XmlLocation.UNKNOWN));
    assertEquals(
        Float.valueOf("1.25"), XmlDatatypes.parse("float", "1.25", null, XmlLocation.UNKNOWN));
    assertEquals(
        Double.NEGATIVE_INFINITY, XmlDatatypes.parse("double", "-INF", null, XmlLocation.UNKNOWN));
    assertEquals(Double.NaN, XmlDatatypes.parse("double", "NaN", null, XmlLocation.UNKNOWN));
    assertEquals(
        Double.valueOf("1.25"), XmlDatatypes.parse("double", "1.25", null, XmlLocation.UNKNOWN));
    assertEquals(
        new java.math.BigInteger("-1"),
        XmlDatatypes.parse("negativeInteger", "-1", null, XmlLocation.UNKNOWN));
    assertEquals(
        new java.math.BigInteger("0"),
        XmlDatatypes.parse("nonNegativeInteger", "0", null, XmlLocation.UNKNOWN));
    assertEquals(
        Long.valueOf(4294967295L),
        XmlDatatypes.parse("unsignedInt", "4294967295", null, XmlLocation.UNKNOWN));
    assertEquals(
        Integer.valueOf(65535),
        XmlDatatypes.parse("unsignedShort", "65535", null, XmlLocation.UNKNOWN));
    assertEquals(
        Short.valueOf((short) 255),
        XmlDatatypes.parse("unsignedByte", "255", null, XmlLocation.UNKNOWN));
    assertEquals(
        Short.valueOf((short) 12), XmlDatatypes.parse("short", "12", null, XmlLocation.UNKNOWN));
    assertEquals(
        Byte.valueOf((byte) 12), XmlDatatypes.parse("byte", "12", null, XmlLocation.UNKNOWN));
    assertEquals("NaN", XmlDatatypes.format("float", Float.NaN, output));
    assertEquals("-INF", XmlDatatypes.format("float", Float.NEGATIVE_INFINITY, output));
    assertEquals("1.25", XmlDatatypes.format("float", 1.25F, output));
    assertEquals("NaN", XmlDatatypes.format("double", Double.NaN, output));
    assertEquals("-INF", XmlDatatypes.format("double", Double.NEGATIVE_INFINITY, output));
    assertEquals("1 2", XmlDatatypes.formatList("int", List.of(1, 2), output));
    assertEquals("INF", XmlDatatypes.format("float", Float.POSITIVE_INFINITY, output));
    assertEquals("INF", XmlDatatypes.format("double", Double.POSITIVE_INFINITY, output));
    assertEquals(
        "12.30", XmlDatatypes.format("decimal", new java.math.BigDecimal("12.30"), output));
    assertEquals("12", XmlDatatypes.format("long", 12L, output));
    assertEquals("12", XmlDatatypes.format("unsignedInt", 12L, output));
    assertEquals("12", XmlDatatypes.format("int", 12, output));
    assertEquals("12", XmlDatatypes.format("unsignedShort", 12, output));
    assertEquals("12", XmlDatatypes.format("short", (short) 12, output));
    assertEquals("12", XmlDatatypes.format("unsignedByte", (short) 12, output));
    assertEquals("12", XmlDatatypes.format("byte", (byte) 12, output));
    assertEquals(
        "12", XmlDatatypes.format("nonPositiveInteger", new java.math.BigInteger("12"), output));
    assertEquals("P1D", XmlDatatypes.format("duration", new XmlDuration("P1D"), output));
    assertEquals(
        "2026-05-21T12:30:00Z",
        XmlDatatypes.format("dateTime", new XmlDateTime("2026-05-21T12:30:00Z"), output));
    assertEquals("2026-05-21", XmlDatatypes.format("date", new XmlDate("2026-05-21"), output));
    assertEquals("12:30:00", XmlDatatypes.format("time", new XmlTime("12:30:00"), output));
    assertEquals("2026", XmlDatatypes.format("gYear", new XmlGYear("2026"), output));
    assertEquals(
        "2026-05", XmlDatatypes.format("gYearMonth", new XmlGYearMonth("2026-05"), output));
    assertEquals("--05", XmlDatatypes.format("gMonth", new XmlGMonth("--05"), output));
    assertEquals("--05-21", XmlDatatypes.format("gMonthDay", new XmlGMonthDay("--05-21"), output));
    assertEquals("---21", XmlDatatypes.format("gDay", new XmlGDay("---21"), output));
    assertEquals("urn:x", XmlDatatypes.format("anyURI", new XmlAnyUri("urn:x"), output));
    assertEquals("name", XmlDatatypes.format("QName", new XmlQName("", "name"), output));
    assertEquals("name", XmlDatatypes.format("NOTATION", new XmlQName("", "name"), output));
    assertEquals("A B", XmlDatatypes.format("NMTOKENS", List.of("A", "B"), output));

    assertFalse(XmlDatatypes.isLexicallyValid("NCName", "p:name"));
    assertFalse(XmlDatatypes.isLexicallyValid("language", "toolongtag"));
    assertFalse(XmlDatatypes.isLexicallyValid("hexBinary", "ABC"));
    assertFalse(XmlDatatypes.isLexicallyValid("hexBinary", "GG"));
    assertFalse(XmlDatatypes.isLexicallyValid("base64Binary", "A"));
    assertFalse(XmlDatatypes.isLexicallyValid("base64Binary", "AQ$ID"));
    assertFalse(XmlDatatypes.isLexicallyValid("unknown", "value"));
    assertFalse(XmlDatatypes.isLexicallyValid("unsignedInt", "-1"));
    assertFalse(XmlDatatypes.isLexicallyValid("unsignedShort", "70000"));
    assertFalse(XmlDatatypes.isLexicallyValid("unsignedByte", "300"));
    assertFalse(XmlDatatypes.isLexicallyValid("nonPositiveInteger", "1"));
    assertFalse(XmlDatatypes.isLexicallyValid("negativeInteger", "0"));
    assertFalse(XmlDatatypes.isLexicallyValid("duration", "P"));
    assertFalse(XmlDatatypes.isLexicallyValid("duration", "P1YT"));
    assertFalse(XmlDatatypes.isLexicallyValid("dateTime", "2026-05-21 12:30:00"));
    assertFalse(XmlDatatypes.isLexicallyValid("dateTime", "2026-02-30T12:30:00"));
    assertFalse(XmlDatatypes.isLexicallyValid("dateTime", "0000-01-01T00:00:00"));
    assertFalse(XmlDatatypes.isLexicallyValid("date", "2026/05/21"));
    assertFalse(XmlDatatypes.isLexicallyValid("date", "2026-13-01"));
    assertFalse(XmlDatatypes.isLexicallyValid("date", "0000-01-01"));
    assertFalse(XmlDatatypes.isLexicallyValid("time", "25:99"));
    assertFalse(XmlDatatypes.isLexicallyValid("time", "25:00:00"));
    assertFalse(XmlDatatypes.isLexicallyValid("gYear", "99"));
    assertFalse(XmlDatatypes.isLexicallyValid("gYear", "0000"));
    assertFalse(XmlDatatypes.isLexicallyValid("gYearMonth", "2026-5"));
    assertFalse(XmlDatatypes.isLexicallyValid("gYearMonth", "2026-13"));
    assertFalse(XmlDatatypes.isLexicallyValid("gMonth", "-05"));
    assertFalse(XmlDatatypes.isLexicallyValid("gMonth", "--13"));
    assertFalse(XmlDatatypes.isLexicallyValid("gMonthDay", "05-21"));
    assertFalse(XmlDatatypes.isLexicallyValid("gMonthDay", "--02-30"));
    assertFalse(XmlDatatypes.isLexicallyValid("gDay", "21"));
    assertFalse(XmlDatatypes.isLexicallyValid("gDay", "---32"));
    assertFalse(XmlDatatypes.isLexicallyValid("NMTOKENS", "   "));
    assertFalse(XmlDatatypes.isLexicallyValid("QName", "p:value"));
    assertFalse(XmlDatatypes.isLexicallyValid("QName", ":value"));
    assertFalse(XmlDatatypes.isLexicallyValid("boolean", "yes"));
    assertFalse(XmlDatatypes.isLexicallyValid("float", "Infinity"));
    assertFalse(XmlDatatypes.isLexicallyValid("double", "Infinity"));
  }

  @Test
  void xmlDatatypeFacetValidationCoversFailureBranches() throws XmlReadException {
    XmlBinary binary = new XmlBinary(new byte[] {1, 2, 3});

    assertFalse(
        XmlDatatypes.matchesFacets(
            "string",
            "abc",
            List.of("def"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "string", "abc", List.of(), 4, null, null, null, null, null, null, null, null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "string", "abc", List.of(), null, 4, null, null, null, null, null, null, null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "string", "abc", List.of(), null, null, 2, null, null, null, null, null, null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "int", 5, List.of(), null, null, null, "6", null, null, null, null, null, List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "int", 5, List.of(), null, null, null, null, "4", null, null, null, null, List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "int", 5, List.of(), null, null, null, null, null, "5", null, null, null, List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "int", 5, List.of(), null, null, null, null, null, null, "5", null, null, List.of()));
    assertTrue(
        XmlDatatypes.matchesFacets(
            "int", 5, List.of("5"), null, null, null, "5", "5", null, null, 1, null, List.of()));
    assertTrue(
        XmlDatatypes.matchesFacets(
            "boolean",
            false,
            List.of("0"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of("false")));
    assertTrue(
        XmlDatatypes.matchesFacets(
            "QName",
            new XmlQName("", "name"),
            List.of("name"),
            4,
            1,
            10,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of("name")));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "decimal",
            new java.math.BigDecimal("123.45"),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            4,
            null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "decimal",
            new java.math.BigDecimal("123.45"),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            1,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "string",
            "abc",
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of("[0-9]+")));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "hexBinary",
            binary,
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
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "NMTOKENS",
            List.of("A", "B"),
            List.of(),
            3,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "string",
            "abc",
            List.of("["),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "NCName", "p:name", List.of(), null, null, null, null, null, null, null, null, null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "unsignedByte",
            (short) 300,
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "NMTOKENS",
            List.of(""),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertTrue(
        XmlDatatypes.matchesFacets(
            "normalizedString",
            "a b",
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "normalizedString",
            "a\tb",
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertTrue(
        XmlDatatypes.matchesFacets(
            "token", "a b", List.of(), null, null, null, null, null, null, null, null, null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "token", " a", List.of(), null, null, null, null, null, null, null, null, null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "boolean", "true", List.of(), null, null, null, null, null, null, null, null, null,
            List.of()));
    assertTrue(
        XmlDatatypes.matchesFacets(
            "positiveInteger",
            java.math.BigInteger.ONE,
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "unsignedInt",
            -1L,
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "unsignedShort",
            70000,
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertTrue(
        XmlDatatypes.matchesFacets(
            "byte", (byte) 1, List.of(), null, null, null, null, null, null, null, null, null,
            List.of()));
    assertTrue(
        XmlDatatypes.matchesFacets(
            "date",
            new XmlDate("2026-05-21"),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()));
    assertFalse(
        XmlDatatypes.matchesFacets(
            "unknown", "value", List.of(), null, null, null, null, null, null, null, null, null,
            List.of()));
    assertTrue(
        XmlDatatypes.matchesFacets(
            "string", "abc", List.of(), null, null, null, "bad", null, null, null, null, null,
            List.of()));
    assertTrue(
        XmlDatatypes.matchesFacets(
            "string", "abc", List.of(), null, null, null, null, null, null, null, 1, 1, List.of()));

    assertEquals(List.of(1, 2), XmlDatatypes.parseList("int", "1 2", null, XmlLocation.UNKNOWN));
    assertEquals(List.of(), XmlDatatypes.parseList("int", "   ", null, XmlLocation.UNKNOWN));
    assertThrows(
        XmlReadException.class,
        () -> XmlDatatypes.parseList("int", "1 x", null, XmlLocation.UNKNOWN));
  }

  @Test
  void xmlBinaryIsImmutable() throws XmlReadException {
    byte[] source = new byte[] {1, 2, 3};
    XmlBinary binary = new XmlBinary(source);
    source[0] = 9;

    assertTrue(Arrays.equals(new byte[] {1, 2, 3}, binary.bytes()));
    byte[] copy = binary.bytes();
    copy[1] = 9;
    assertTrue(Arrays.equals(new byte[] {1, 2, 3}, binary.bytes()));
    assertEquals(binary, XmlDatatypes.parse("base64Binary", "AQID", null, XmlLocation.UNKNOWN));
    assertEquals(binary.hashCode(), new XmlBinary(new byte[] {1, 2, 3}).hashCode());
    assertEquals("AQID", binary.toString());
    assertFalse(binary.equals(new XmlBinary(new byte[] {1, 2})));
    assertFalse(binary.equals("AQID"));
  }

  private static final class FakeReader implements XmlEventReader {
    private final XmlName elementName;
    private final XmlName attributeName;
    private final String attributeValue;
    private boolean ended;

    private FakeReader(XmlName elementName, XmlName attributeName, String attributeValue) {
      this.elementName = elementName;
      this.attributeName = attributeName;
      this.attributeValue = attributeValue;
    }

    @Override
    public XmlEventKind kind() {
      return ended ? XmlEventKind.END_ELEMENT : XmlEventKind.START_ELEMENT;
    }

    @Override
    public XmlName name() {
      return elementName;
    }

    @Override
    public String text() {
      return "";
    }

    @Override
    public int attributeCount() {
      return ended ? 0 : 1;
    }

    @Override
    public XmlName attributeName(int index) {
      if (index != 0 || ended) {
        throw new IndexOutOfBoundsException(index);
      }
      return attributeName;
    }

    @Override
    public String attributeValue(int index) {
      if (index != 0 || ended) {
        throw new IndexOutOfBoundsException(index);
      }
      return attributeValue;
    }

    @Override
    public XmlLocation location() {
      return XmlLocation.UNKNOWN;
    }

    @Override
    public boolean next() {
      if (ended) {
        return false;
      }
      ended = true;
      return true;
    }
  }

  private static XmlReadException readException(XmlDiagnostic diagnostic) {
    return new XmlReadException(diagnostic);
  }

  private static XmlWriteException writeException(XmlDiagnostic diagnostic) {
    return new XmlWriteException(diagnostic);
  }

  private XmlEventReader prefixReader() {
    return new XmlEventReader() {
      @Override
      public XmlEventKind kind() {
        return XmlEventKind.START_DOCUMENT;
      }

      @Override
      public XmlName name() {
        return null;
      }

      @Override
      public String text() {
        return "";
      }

      @Override
      public int attributeCount() {
        return 0;
      }

      @Override
      public XmlName attributeName(int index) {
        throw new IndexOutOfBoundsException(index);
      }

      @Override
      public String attributeValue(int index) {
        throw new IndexOutOfBoundsException(index);
      }

      @Override
      public XmlLocation location() {
        return XmlLocation.UNKNOWN;
      }

      @Override
      public String namespaceUriForPrefix(String prefix) {
        return "p".equals(prefix) ? "urn:test" : null;
      }

      @Override
      public boolean next() {
        return false;
      }
    };
  }

  private static final class RecordingOutput implements XmlOutput {
    private final List<String> events = new ArrayList<>();

    @Override
    public void startDocument() {
      events.add("startDocument");
    }

    @Override
    public void endDocument() {
      events.add("endDocument");
    }

    @Override
    public void startElement(XmlName name) {
      events.add("start:" + name.namespaceUri() + ":" + name.localName());
    }

    @Override
    public void attribute(XmlName name, String value) {
      events.add("attr:" + name.localName() + "=" + value);
    }

    @Override
    public void text(String value) {
      events.add("text:" + value);
    }

    @Override
    public void endElement(XmlName name) {
      events.add("end:" + name.localName());
    }

    @Override
    public void flush() {
      events.add("flush");
    }
  }
}
