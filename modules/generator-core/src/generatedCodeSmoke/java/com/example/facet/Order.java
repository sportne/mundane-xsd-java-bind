package com.example.facet;

import java.util.Objects;

/** Generated immutable model for XML type Order. */
public record Order(String code, Integer priority) {
  public Order {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(priority, "priority");
  }
}
