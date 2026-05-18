package io.github.mundanej.mxjb.generator.core.bind;

/** Generated binding semantics for nil/default/fixed XML values. */
public record BindingValueSemantics(boolean nillable, String defaultValue, String fixedValue) {
  public static final BindingValueSemantics NONE = new BindingValueSemantics(false, null, null);

  public boolean hasDefault() {
    return defaultValue != null;
  }

  public boolean hasFixed() {
    return fixedValue != null;
  }

  public boolean hasAny() {
    return nillable || hasDefault() || hasFixed();
  }

  public String toText() {
    if (!hasAny()) {
      return "";
    }
    StringBuilder text = new StringBuilder(" semantics[");
    boolean previous = false;
    if (nillable) {
      text.append("nillable=true");
      previous = true;
    }
    if (defaultValue != null) {
      if (previous) {
        text.append(',');
      }
      text.append("default=").append(defaultValue);
      previous = true;
    }
    if (fixedValue != null) {
      if (previous) {
        text.append(',');
      }
      text.append("fixed=").append(fixedValue);
    }
    return text.append(']').toString();
  }
}
