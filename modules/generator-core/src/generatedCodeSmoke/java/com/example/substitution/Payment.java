package com.example.substitution;

import java.math.BigDecimal;
import java.util.Objects;

/** Generated immutable model for XML type Payment. */
public record Payment(BigDecimal amount) {
  public Payment {
    Objects.requireNonNull(amount, "amount");
  }
}
