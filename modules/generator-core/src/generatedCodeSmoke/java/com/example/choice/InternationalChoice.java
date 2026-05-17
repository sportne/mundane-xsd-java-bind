package com.example.choice;

import java.util.Objects;

/** Generated branch for XML choice OrderChoice. */
public record InternationalChoice(String value) implements OrderChoice {
  public InternationalChoice {
    Objects.requireNonNull(value, "value");
  }
}
