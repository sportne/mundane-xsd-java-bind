package com.example.purchase;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Generated immutable model for XML type Order. */
public record Order(Optional<String> version, String id, Optional<String> note, List<Line> line) {
  public Order {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(note, "note");
    line = List.copyOf(Objects.requireNonNull(line, "line"));
  }
}
