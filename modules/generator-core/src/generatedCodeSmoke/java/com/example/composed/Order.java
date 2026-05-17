package com.example.composed;

import java.math.BigDecimal;
import java.util.Objects;

/** Generated immutable model for XML type Order. */
public record Order(String version, String id, BigDecimal total) {
  public Order {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(total, "total");
  }
}
