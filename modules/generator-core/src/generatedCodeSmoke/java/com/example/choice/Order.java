package com.example.choice;

import java.util.Objects;
import java.util.Optional;

/** Generated immutable model for XML type Order. */
public record Order(String id, Optional<OrderChoice> orderChoice) {
  public Order {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(orderChoice, "orderChoice");
  }
}
