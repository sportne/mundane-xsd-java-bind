package com.example.substitution;

import java.util.Objects;

/** Generated branch for XML substitution PaymentSubstitution. */
public record PaymentSubstitutionBranch(Payment value) implements PaymentSubstitution {
  public PaymentSubstitutionBranch {
    Objects.requireNonNull(value, "value");
  }
}
