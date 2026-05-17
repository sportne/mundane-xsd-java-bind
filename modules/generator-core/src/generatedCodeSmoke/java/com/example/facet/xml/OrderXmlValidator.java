package com.example.facet.xml;

/** Generated XML validator for {@link com.example.facet.Order}. */
public final class OrderXmlValidator {
  private OrderXmlValidator() {}

  public static io.github.mundanej.mxjb.runtime.ValidationResult validate(
      com.example.facet.Order value) {
    java.util.Objects.requireNonNull(value, "value");
    java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors =
        new java.util.ArrayList<>();
    if (value.code() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value code.");
    } else {
      if (value.code().length() < 3 || value.code().length() > 8) {
        addError(errors, "MXJB-GV-005", "Value length is outside the accepted range.");
      }
      if (!java.util.regex.Pattern.matches("[A-Z0-9]+", value.code())) {
        addError(errors, "MXJB-GV-007", "Value does not match the accepted pattern.");
      }
    }
    if (value.priority() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value priority.");
    } else {
      if (value.priority().compareTo(Integer.valueOf(1)) < 0
          || value.priority().compareTo(Integer.valueOf(9)) > 0) {
        addError(errors, "MXJB-GV-006", "Value is outside the accepted range.");
      }
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
      return validate(com.example.facet.xml.OrderXmlReader.read(input));
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
