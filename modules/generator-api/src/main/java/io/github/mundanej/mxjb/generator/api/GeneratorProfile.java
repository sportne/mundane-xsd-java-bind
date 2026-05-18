package io.github.mundanej.mxjb.generator.api;

import java.util.Optional;

/** Supported public generator profiles. */
public enum GeneratorProfile {
  XP_DATA_10("XP-DATA-10"),
  XP_DATA_10_CHOICE("XP-DATA-10-CHOICE"),
  XP_VALIDATION_10_BASIC("XP-VALIDATION-10-BASIC"),
  XP_XSD10_COMPOSED("XP-XSD10-COMPOSED"),
  XP_XSD10_SEMANTIC("XP-XSD10-SEMANTIC"),
  XP_XSD10_DOCUMENT("XP-XSD10-DOCUMENT");

  private final String cliToken;

  GeneratorProfile(String cliToken) {
    this.cliToken = cliToken;
  }

  public String cliToken() {
    return cliToken;
  }

  public static Optional<GeneratorProfile> fromCliToken(String token) {
    if (token == null) {
      return Optional.empty();
    }
    for (GeneratorProfile profile : values()) {
      if (profile.cliToken.equals(token)) {
        return Optional.of(profile);
      }
    }
    return Optional.empty();
  }
}
