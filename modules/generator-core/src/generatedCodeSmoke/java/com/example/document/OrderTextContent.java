package com.example.document;

import java.util.Objects;

/** Generated branch for XML mixed content text. */
public record OrderTextContent(String value) implements OrderContent {
  public OrderTextContent {
    Objects.requireNonNull(value, "value");
  }
}
