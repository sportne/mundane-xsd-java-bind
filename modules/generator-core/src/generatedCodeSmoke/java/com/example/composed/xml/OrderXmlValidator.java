package com.example.composed.xml;

/** Generated XML validator for {@link com.example.composed.Order}. */
public final class OrderXmlValidator {
  private OrderXmlValidator() {}

  public static io.github.mundanej.mxjb.runtime.ValidationResult validate(
      com.example.composed.Order value) {
    java.util.Objects.requireNonNull(value, "value");
    java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors =
        new java.util.ArrayList<>();
    if (value.version() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value version.");
    }
    if (value.id() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value id.");
    }
    if (value.total() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value total.");
    }
    if (errors.isEmpty()) {
      return io.github.mundanej.mxjb.runtime.ValidationResult.valid();
    }
    return io.github.mundanej.mxjb.runtime.ValidationResult.invalid(errors);
  }

  public static io.github.mundanej.mxjb.runtime.ValidationResult validate(
      io.github.mundanej.mxjb.runtime.XmlEventReader input) {
    java.util.Objects.requireNonNull(input, "input");
    try {
      return validate(com.example.composed.xml.OrderXmlReader.read(input));
    } catch (io.github.mundanej.mxjb.runtime.XmlReadException exception) {
      io.github.mundanej.mxjb.runtime.XmlDiagnostic diagnostic = exception.diagnostic();
      return io.github.mundanej.mxjb.runtime.ValidationResult.invalid(
          new io.github.mundanej.mxjb.runtime.ValidationError(
              diagnostic.code(), diagnostic.message(), diagnostic.location()));
    }
  }

  private static void addError(
      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors,
      String code,
      String message) {
    errors.add(
        new io.github.mundanej.mxjb.runtime.ValidationError(
            code, message, io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN));
  }
}
