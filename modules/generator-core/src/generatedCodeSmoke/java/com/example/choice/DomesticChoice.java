package com.example.choice;

import java.util.Objects;

/** Generated branch for XML choice OrderChoice. */
public record DomesticChoice(String value) implements OrderChoice {
  public DomesticChoice {
    Objects.requireNonNull(value, "value");
  }
}
