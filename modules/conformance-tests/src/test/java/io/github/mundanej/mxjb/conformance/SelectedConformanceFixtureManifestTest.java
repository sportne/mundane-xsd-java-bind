package io.github.mundanej.mxjb.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import io.github.mundanej.mxjb.generator.core.CoreGenerator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SelectedConformanceFixtureManifestTest {
  private static final Set<String> KNOWN_CATEGORIES =
      Set.of("supported-profile", "unsupported-diagnostic", "future-study", "blocked");
  private static final Set<String> KNOWN_PROFILES =
      Set.of(
          "XP-DATA-10",
          "XP-DATA-10-CHOICE",
          "XP-VALIDATION-10-BASIC",
          "XP-XSD10-COMPOSED",
          "XP-XSD10-SEMANTIC",
          "XP-XSD10-DOCUMENT",
          "XP-XSD10-FULL");

  @TempDir private Path tempDirectory;

  @Test
  void selectedFixtureManifestUsesKnownStableClassifications() throws IOException {
    List<SelectedFixture> fixtures = selectedFixtures();
    Set<String> ids = new HashSet<>();
    Set<String> supportedProfiles = new HashSet<>();

    assertFalse(fixtures.isEmpty());
    for (SelectedFixture fixture : fixtures) {
      assertTrue(fixture.id().matches("T-CONF-[A-Z0-9-]+"), fixture.id());
      assertTrue(ids.add(fixture.id()), "Duplicate selected fixture id " + fixture.id());
      assertTrue(KNOWN_PROFILES.contains(fixture.profile()), fixture.id());
      assertTrue(KNOWN_CATEGORIES.contains(fixture.category()), fixture.id());
      assertFalse(claimsBroadW3cCoverage(fixture), fixture.id());

      if ("supported-profile".equals(fixture.category())) {
        supportedProfiles.add(fixture.profile());
        assertResourceExistsIfPresent(fixture.schema(), fixture.id());
        assertResourceExistsIfPresent(fixture.xml(), fixture.id());
        assertEquals("-", fixture.expectedDiagnostics(), fixture.id());
        assertFalse("generator-diagnostic".equals(fixture.comparison()), fixture.id());
      } else if ("unsupported-diagnostic".equals(fixture.category())) {
        assertResourceExists(fixture.schema(), fixture.id());
        assertEquals("-", fixture.xml(), fixture.id());
        assertEquals("generator-diagnostic", fixture.comparison(), fixture.id());
        assertFalse("-".equals(fixture.expectedDiagnostics()), fixture.id());
      } else {
        assertFalse(fixture.reason().isBlank(), fixture.id());
        assertResourceExistsIfPresent(fixture.schema(), fixture.id());
        assertResourceExistsIfPresent(fixture.xml(), fixture.id());
      }
    }

    assertTrue(supportedProfiles.contains("XP-DATA-10"));
    assertTrue(supportedProfiles.contains("XP-DATA-10-CHOICE"));
    assertTrue(supportedProfiles.contains("XP-VALIDATION-10-BASIC"));
    assertTrue(supportedProfiles.contains("XP-XSD10-COMPOSED"));
    assertTrue(supportedProfiles.contains("XP-XSD10-SEMANTIC"));
    assertTrue(supportedProfiles.contains("XP-XSD10-DOCUMENT"));
    assertTrue(supportedProfiles.contains("XP-XSD10-FULL"));
  }

  @Test
  void unsupportedDiagnosticFixturesFailGenerationWithStableCodes() throws IOException {
    List<SelectedFixture> unsupportedFixtures =
        selectedFixtures().stream()
            .filter(fixture -> "unsupported-diagnostic".equals(fixture.category()))
            .toList();

    assertFalse(unsupportedFixtures.isEmpty());
    for (SelectedFixture fixture : unsupportedFixtures) {
      GeneratorResult result =
          new CoreGenerator()
              .generate(
                  new GeneratorRequest(
                      List.of(resourcePath("/" + fixture.schema())),
                      tempDirectory.resolve(fixture.id().toLowerCase(java.util.Locale.ROOT)),
                      generatorProfile(fixture.profile()),
                      "com.example.generated",
                      Map.of("urn:unsupported", "com.example.unsupported"),
                      List.of(),
                      Map.of()));

      assertFalse(result.successful(), fixture.id());
      assertTrue(result.generatedSources().isEmpty(), fixture.id());
      assertEquals(
          expectedDiagnostics(fixture),
          result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList(),
          fixture.id() + " diagnostics: " + result.diagnostics());
    }
  }

  private static List<String> expectedDiagnostics(SelectedFixture fixture) {
    return List.of(fixture.expectedDiagnostics().split(",", -1));
  }

  private static GeneratorProfile generatorProfile(String cliToken) {
    return GeneratorProfile.fromCliToken(cliToken).orElseThrow();
  }

  private static boolean claimsBroadW3cCoverage(SelectedFixture fixture) {
    String rowText =
        String.join(
                " ",
                fixture.id(),
                fixture.profile(),
                fixture.category(),
                fixture.comparison(),
                fixture.reason())
            .toLowerCase(java.util.Locale.ROOT);
    return rowText.contains("full w3c")
        || rowText.contains("full-suite pass")
        || rowText.contains("full xml schema")
        || rowText.contains("complete w3c");
  }

  private static void assertResourceExistsIfPresent(String resourceName, String fixtureId) {
    if (!"-".equals(resourceName)) {
      assertResourceExists(resourceName, fixtureId);
    }
  }

  private static void assertResourceExists(String resourceName, String fixtureId) {
    assertNotNull(
        SelectedConformanceFixtureManifestTest.class.getResource("/" + resourceName),
        fixtureId + " missing resource " + resourceName);
  }

  private static Path resourcePath(String resourceName) {
    URL resource = SelectedConformanceFixtureManifestTest.class.getResource(resourceName);
    if (resource == null) {
      throw new IllegalArgumentException("Missing resource " + resourceName);
    }
    try {
      return Path.of(resource.toURI());
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException("Missing resource " + resourceName, exception);
    }
  }

  private static List<SelectedFixture> selectedFixtures() throws IOException {
    try (InputStream input =
        SelectedConformanceFixtureManifestTest.class.getResourceAsStream(
            "/selected-fixtures.tsv")) {
      assertNotNull(input, "Missing selected fixture manifest.");
      return parseManifest(input);
    }
  }

  private static List<SelectedFixture> parseManifest(InputStream input) throws IOException {
    List<SelectedFixture> fixtures = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      String line;
      boolean headerSeen = false;
      int lineNumber = 0;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.isBlank() || line.startsWith("#")) {
          continue;
        }
        if (!headerSeen) {
          assertEquals(
              "id\tprofile\tcategory\tschema\txml\tcomparison\texpectedDiagnostics\treason",
              line,
              "Unexpected selected fixture manifest header.");
          headerSeen = true;
          continue;
        }
        String[] columns = line.split("\t", -1);
        assertEquals(8, columns.length, "Invalid TSV column count on line " + lineNumber);
        fixtures.add(
            new SelectedFixture(
                columns[0],
                columns[1],
                columns[2],
                columns[3],
                columns[4],
                columns[5],
                columns[6],
                columns[7]));
      }
      assertTrue(headerSeen, "Selected fixture manifest header was not found.");
    }
    return fixtures;
  }

  private record SelectedFixture(
      String id,
      String profile,
      String category,
      String schema,
      String xml,
      String comparison,
      String expectedDiagnostics,
      String reason) {}
}
