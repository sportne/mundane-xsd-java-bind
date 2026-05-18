package com.example.substitution;

import java.math.BigDecimal;
import java.util.Objects;

/** Generated immutable model for XML type Cardpayment. */
public record Cardpayment(BigDecimal amount, String cardlast4) {
  public Cardpayment {
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(cardlast4, "cardlast4");
  }
}
