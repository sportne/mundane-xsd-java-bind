package io.github.mundanej.mxjb.generator.core.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Package-private parser for the accepted identity selector/field XPath subset. */
final class SchemaIrIdentityNormalization {
  private SchemaIrIdentityNormalization() {}

  static List<SchemaIrIdentityPath> paths(
      String xpath,
      boolean field,
      Function<String, SchemaQName> nameResolver,
      Consumer<String> diagnosticSink) {
    if (xpath == null || xpath.isBlank()) {
      diagnosticSink.accept("identity constraint XPath is missing.");
      return null;
    }
    List<SchemaIrIdentityPath> paths = new ArrayList<>();
    for (String alternative : splitOn(xpath, '|')) {
      SchemaIrIdentityPath path = path(alternative.trim(), field, nameResolver, diagnosticSink);
      if (path == null) {
        return null;
      }
      paths.add(path);
    }
    return paths;
  }

  private static SchemaIrIdentityPath path(
      String xpath,
      boolean field,
      Function<String, SchemaQName> nameResolver,
      Consumer<String> diagnosticSink) {
    if (xpath.isBlank()
        || xpath.startsWith("/")
        || xpath.contains("[")
        || xpath.contains("]")
        || xpath.contains("(")
        || xpath.contains(")")
        || xpath.contains("::")
        || xpath.contains("$")
        || xpath.contains("..")) {
      diagnosticSink.accept("Unsupported identity constraint XPath " + xpath + ".");
      return null;
    }
    if (".".equals(xpath)) {
      return new SchemaIrIdentityPath(false, true, List.of());
    }
    boolean descendant = xpath.startsWith(".//");
    String body = descendant ? xpath.substring(3) : xpath;
    if (body.isBlank()) {
      diagnosticSink.accept("Unsupported identity constraint XPath " + xpath + ".");
      return null;
    }
    List<String> tokens = splitOn(body, '/');
    List<SchemaIrIdentityStep> steps = new ArrayList<>();
    for (int index = 0; index < tokens.size(); index++) {
      boolean terminal = index == tokens.size() - 1;
      String token = tokens.get(index).trim();
      boolean attribute = token.startsWith("@");
      if (attribute && (!field || !terminal)) {
        diagnosticSink.accept("Attribute steps are supported only as terminal identity fields.");
        return null;
      }
      String nameText = attribute ? token.substring(1) : token;
      if (nameText.isBlank()) {
        diagnosticSink.accept("Unsupported identity constraint XPath " + xpath + ".");
        return null;
      }
      if ("*".equals(nameText)) {
        steps.add(new SchemaIrIdentityStep(null, true, attribute));
      } else {
        SchemaQName name = nameResolver.apply(nameText);
        if (name == null) {
          return null;
        }
        steps.add(new SchemaIrIdentityStep(name, false, attribute));
      }
    }
    return new SchemaIrIdentityPath(descendant, false, steps);
  }

  private static List<String> splitOn(String value, char delimiter) {
    List<String> parts = new ArrayList<>();
    int start = 0;
    for (int index = 0; index < value.length(); index++) {
      if (value.charAt(index) == delimiter) {
        parts.add(value.substring(start, index));
        start = index + 1;
      }
    }
    parts.add(value.substring(start));
    return parts;
  }
}
