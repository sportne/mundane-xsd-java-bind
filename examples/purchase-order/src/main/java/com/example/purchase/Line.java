package com.example.purchase;

import java.util.Objects;

/** Generated immutable model for XML type Line. */
public record Line(String sku, int quantity) {
  public Line {
    Objects.requireNonNull(sku, "sku");
  }
}
