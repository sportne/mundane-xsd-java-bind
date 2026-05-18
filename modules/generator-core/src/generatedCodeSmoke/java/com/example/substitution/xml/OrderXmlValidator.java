package com.example.substitution.xml;

/** Generated XML validator for {@link com.example.substitution.Order}. */
public final class OrderXmlValidator {
  private OrderXmlValidator() {}

  public static io.github.mundanej.mxjb.runtime.ValidationResult validate(
      com.example.substitution.Order value) {
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
      return validate(com.example.substitution.xml.OrderXmlReader.read(input));
    } catch (io.github.mundanej.mxjb.runtime.XmlReadException exception) {
      io.github.mundanej.mxjb.runtime.XmlDiagnostic diagnostic = exception.diagnostic();
      return io.github.mundanej.mxjb.runtime.ValidationResult.invalid(
          new io.github.mundanej.mxjb.runtime.ValidationError(
              diagnostic.code(), diagnostic.message(), diagnostic.location()));
    }
  }

  private static void validateOrder(
      com.example.substitution.Order value,
      io.github.mundanej.mxjb.runtime.XmlLocation location,
      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {
    if (value.id() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value id.", location);
    }
    if (value.payment() != null && value.payment().isPresent()) {
      if (value.payment().orElse(null)
          instanceof com.example.substitution.PaymentSubstitutionBranch branch) {
        validatePayment(branch.value(), location, errors);
      }
      if (value.payment().orElse(null)
          instanceof com.example.substitution.CardpaymentSubstitutionBranch branch) {
        validateCardpayment(branch.value(), location, errors);
      }
    }
  }

  private static void validatePayment(
      com.example.substitution.Payment value,
      io.github.mundanej.mxjb.runtime.XmlLocation location,
      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {
    if (value.amount() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value amount.", location);
    }
  }

  private static void validateCardpayment(
      com.example.substitution.Cardpayment value,
      io.github.mundanej.mxjb.runtime.XmlLocation location,
      java.util.ArrayList<io.github.mundanej.mxjb.runtime.ValidationError> errors) {
    if (value.amount() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value amount.", location);
    }
    if (value.cardlast4() == null) {
      addError(errors, "MXJB-GV-001", "Missing required value cardlast4.", location);
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
