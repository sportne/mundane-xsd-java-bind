package com.example.document;

import java.util.Objects;

/** Generated branch for XML mixed content element id. */
public record IdContent(String value) implements OrderContent {
  public IdContent {
    Objects.requireNonNull(value, "value");
  }
}
