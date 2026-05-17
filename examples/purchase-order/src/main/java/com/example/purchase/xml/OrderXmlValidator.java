package com.example.purchase.xml;

/** Generated XML validator for {@link com.example.purchase.Order}. */
public final class OrderXmlValidator {
  private OrderXmlValidator() {}

  public static io.github.mundanej.mxjb.runtime.ValidationResult validate(
      com.example.purchase.Order value) {
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
      return validate(com.example.purchase.xml.OrderXmlReader.read(input));
    } catch (io.github.mundanej.mxjb.runtime.XmlReadException exception) {
      io.github.mundanej.mxjb.runtime.XmlDiagnostic diagnostic = exception.diagnostic();
      return io.github.mundanej.mxjb.runtime.ValidationResult.invalid(
          new io.github.mundanej.mxjb.runtime.ValidationError(
              diagnostic.code(), diagnostic.message(), diagnostic.location()));
    }
  }

  private static void validateOrder(
      com.example.purchase.Order value,
      io.github.mundanej.mxjb.runtime.XmlLocation location,
      java.util.List<io.github.mundanej.mxjb.runtime.ValidationError> errors) {
    if (value.id() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value id.", location);
    }
    if (value.line() == null || value.line().isEmpty()) {
      addError(errors, "MXJB-GV-002", "Too few values for line.", location);
    } else {
      for (com.example.purchase.Line item : value.line()) {
        if (item == null) {
          addError(errors, "MXJB-GV-001", "Missing required value line.", location);
        } else {
          validateLine(item, location, errors);
        }
      }
    }
  }

  private static void validateLine(
      com.example.purchase.Line value,
      io.github.mundanej.mxjb.runtime.XmlLocation location,
      java.util.List<io.github.mundanej.mxjb.runtime.ValidationError> errors) {
    if (value.sku() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value sku.", location);
    }
  }

  private static io.github.mundanej.mxjb.runtime.ValidationResult validationResult(
      java.util.List<io.github.mundanej.mxjb.runtime.ValidationError> errors) {
    if (errors.isEmpty()) {
      return io.github.mundanej.mxjb.runtime.ValidationResult.valid();
    }
    return io.github.mundanej.mxjb.runtime.ValidationResult.invalid(errors);
  }

  private static void addError(
      java.util.List<io.github.mundanej.mxjb.runtime.ValidationError> errors,
      String code,
      String message,
      io.github.mundanej.mxjb.runtime.XmlLocation location) {
    errors.add(new io.github.mundanej.mxjb.runtime.ValidationError(code, message, location));
  }
}
