package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaCardinality;
import java.util.Objects;

/** Java field shape implied by schema occurrence constraints. */
public record BindingCardinality(String shape, int minOccurs, String maxOccurs) {
  public BindingCardinality {
    Objects.requireNonNull(shape, "shape");
    Objects.requireNonNull(maxOccurs, "maxOccurs");
  }

  static BindingCardinality from(SchemaCardinality cardinality) {
    if ("unbounded".equals(cardinality.maxOccurs())
        || Integer.parseInt(cardinality.maxOccurs()) > 1) {
      return new BindingCardinality("list", cardinality.minOccurs(), cardinality.maxOccurs());
    }
    if (cardinality.minOccurs() == 0) {
      return new BindingCardinality("optional", cardinality.minOccurs(), cardinality.maxOccurs());
    }
    return new BindingCardinality("required", cardinality.minOccurs(), cardinality.maxOccurs());
  }

  public String toText() {
    return shape + " " + minOccurs + ".." + maxOccurs;
  }
}
