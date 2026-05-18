package com.example.semantic.xml;

/** Generated XML validator for {@link com.example.semantic.Order}. */
public final class OrderXmlValidator {
  private OrderXmlValidator() {}

  public static io.github.mundanej.mxjb.runtime.ValidationResult validate(
      com.example.semantic.Order value) {
    java.util.Objects.requireNonNull(value, "value");
    java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors =
        new java.util.ArrayList<>();
    validateOrder(value, io.github.mundanej.mxjb.runtime.XmlLocation.UNKNOWN, errors);
    return validationResult(errors);
  }

  public static io.github.mundanej.mxjb.runtime.ValidationResult validate(
      io.github.mundanej.mxjb.runtime.XmlEventReader input) {
    java.util.Objects.requireNonNull(input, "input");
    try {
      return validate(com.example.semantic.xml.OrderXmlReader.read(input));
    } catch (io.github.mundanej.mxjb.runtime.XmlReadException exception) {
      io.github.mundanej.mxjb.runtime.XmlDiagnostic diagnostic = exception.diagnostic();
      return io.github.mundanej.mxjb.runtime.ValidationResult.invalid(
          new io.github.mundanej.mxjb.runtime.ValidationError(
              diagnostic.code(), diagnostic.message(), diagnostic.location()));
    }
  }

  private static void validateOrder(
      com.example.semantic.Order value,
      io.github.mundanej.mxjb.runtime.XmlLocation location,
      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {
    if (value.status() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value status.", location);
    }
    if (value.version() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value version.", location);
    } else {
      if (!java.util.Objects.equals(value.version(), "1")) {
        addError(errors, "MXJB-GV-009", "Value does not match the fixed value.", location);
      }
    }
    if (value.code() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value code.", location);
    }
  }

  private static io.github.mundanej.mxjb.runtime.ValidationResult validationResult(
      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {
    if (errors.isEmpty()) {
      return io.github.mundanej.mxjb.runtime.ValidationResult.valid();
    }
    return io.github.mundanej.mxjb.runtime.ValidationResult.invalid(errors);
  }

  private static void addError(
      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors,
      String code,
      String message,
      io.github.mundanej.mxjb.runtime.XmlLocation location) {
    errors.add(new io.github.mundanej.mxjb.runtime.ValidationError(code, message, location));
  }
}
