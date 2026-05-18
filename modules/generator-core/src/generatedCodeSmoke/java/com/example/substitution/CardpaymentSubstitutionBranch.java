package com.example.substitution;

import java.util.Objects;

/** Generated branch for XML substitution PaymentSubstitution. */
public record CardpaymentSubstitutionBranch(Cardpayment value) implements PaymentSubstitution {
  public CardpaymentSubstitutionBranch {
    Objects.requireNonNull(value, "value");
  }
}
