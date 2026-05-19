package com.example.document;

import java.util.List;
import java.util.Objects;

/** Generated immutable model for XML type Order. */
public record Order(List<OrderContent> content, String version) {
  public Order {
    content = List.copyOf(Objects.requireNonNull(content, "content"));
    Objects.requireNonNull(version, "version");
  }
}
