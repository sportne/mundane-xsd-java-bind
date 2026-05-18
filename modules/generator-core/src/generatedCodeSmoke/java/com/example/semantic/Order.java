package com.example.semantic;

import java.util.Objects;
import java.util.Optional;

/** Generated immutable model for XML type Order. */
public record Order(String status, String version, Optional<String> code) {
  public Order {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(code, "code");
  }
}
