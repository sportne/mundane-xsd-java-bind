package com.example.choice.xml;

/** Generated XML validator for {@link com.example.choice.Order}. */
public final class OrderXmlValidator {
  private OrderXmlValidator() {}

  public static io.github.mundanej.mxjb.runtime.ValidationResult validate(
      com.example.choice.Order value) {
    java.util.Objects.requireNonNull(value, "value");
    java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors =
        new java.util.ArrayList<>();
    if (value.id() == null) {
      errors.add(
          new io.github.mundanej.mxjb.runtime.ValidationError(
              "MXJB-GV-001",
              "Missing required value id.",
              io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN));
    }
    if (value.orderChoice() == null) {
      errors.add(
          new io.github.mundanej.mxjb.runtime.ValidationError(
              "MXJB-GV-001",
              "Missing required value orderChoice.",
              io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN));
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
      return validate(com.example.choice.xml.OrderXmlReader.read(input));
    } catch (io.github.mundanej.mxjb.runtime.XmlReadException exception) {
      io.github.mundanej.mxjb.runtime.XmlDiagnostic diagnostic = exception.diagnostic();
      return io.github.mundanej.mxjb.runtime.ValidationResult.invalid(
          new io.github.mundanej.mxjb.runtime.ValidationError(
              diagnostic.code(), diagnostic.message(), diagnostic.location()));
    }
  }
}
