package io.github.mundanej.mxjb.runtime;

import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable XML Schema binary value for base64Binary and hexBinary. */
public final class XmlBinary {
  private final byte[] bytes;

  public XmlBinary(byte[] bytes) {
    this.bytes = Objects.requireNonNull(bytes, "bytes").clone();
  }

  public byte[] bytes() {
    return bytes.clone();
  }

  public String base64LexicalValue() {
    return Base64.getEncoder().encodeToString(bytes);
  }

  public String hexLexicalValue() {
    return HexFormat.of().formatHex(bytes);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof XmlBinary value && Arrays.equals(bytes, value.bytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  @Override
  public String toString() {
    return base64LexicalValue();
  }
}
