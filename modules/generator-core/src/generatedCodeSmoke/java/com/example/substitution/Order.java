package com.example.substitution;

import java.util.Objects;
import java.util.Optional;

/** Generated immutable model for XML type Order. */
public record Order(String id, Optional<PaymentSubstitution> payment) {
  public Order {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(payment, "payment");
  }
}
