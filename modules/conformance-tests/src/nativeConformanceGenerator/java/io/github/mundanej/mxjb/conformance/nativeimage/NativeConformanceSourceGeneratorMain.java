package io.github.mundanej.mxjb.conformance.nativeimage;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import io.github.mundanej.mxjb.generator.core.CoreGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Build-time source generator for the selected Native Image conformance lane. */
public final class NativeConformanceSourceGeneratorMain {
  private static final List<GeneratedFixture> FIXTURES =
      List.of(
          new GeneratedFixture(
              "xp-data-10-choice/order.xsd",
              GeneratorProfile.XP_DATA_10_CHOICE,
              Map.of("urn:choice", "com.example.nativeconf.choice")),
          new GeneratedFixture(
              "xp-validation-10-basic/order.xsd",
              GeneratorProfile.XP_VALIDATION_10_BASIC,
              Map.of("urn:facet", "com.example.nativeconf.facet")),
          new GeneratedFixture(
              "xp-xsd10-composed/order.xsd",
              GeneratorProfile.XP_XSD10_COMPOSED,
              Map.of("urn:composed", "com.example.nativeconf.composed")),
          new GeneratedFixture(
              "xp-xsd10-semantic/order.xsd",
              GeneratorProfile.XP_XSD10_SEMANTIC,
              Map.of("urn:semantic", "com.example.nativeconf.semantic")),
          new GeneratedFixture(
              "xp-xsd10-semantic/substitution-order.xsd",
              GeneratorProfile.XP_XSD10_SEMANTIC,
              Map.of("urn:semantic-substitution", "com.example.nativeconf.substitution")),
          new GeneratedFixture(
              "xp-xsd10-document/order.xsd",
              GeneratorProfile.XP_XSD10_DOCUMENT,
              Map.of("urn:document", "com.example.nativeconf.document")),
          new GeneratedFixture(
              "xp-xsd10-document/mixed-order.xsd",
              GeneratorProfile.XP_XSD10_DOCUMENT,
              Map.of("urn:mixed-document", "com.example.nativeconf.mixed")));

  private NativeConformanceSourceGeneratorMain() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expected generated source output directory argument.");
    }
    Path outputDirectory = Path.of(args[0]);
    Path schemaDirectory = outputDirectory.resolveSibling("schema-copies");
    deleteDirectory(outputDirectory);
    deleteDirectory(schemaDirectory);
    Files.createDirectories(outputDirectory);
    Files.createDirectories(schemaDirectory);

    CoreGenerator generator = new CoreGenerator();
    for (GeneratedFixture fixture : FIXTURES) {
      Path schema = copyResource(fixture.schemaResource(), schemaDirectory);
      GeneratorResult result =
          generator.generate(
              new GeneratorRequest(
                  List.of(schema),
                  outputDirectory,
                  fixture.profile(),
                  "com.example.nativeconf.generated",
                  fixture.namespacePackages(),
                  List.of(),
                  Map.of()));
      if (!result.successful()) {
        throw new IllegalStateException(
            "Native conformance source generation failed for "
                + fixture.schemaResource()
                + ": "
                + result.diagnostics());
      }
    }
  }

  private static Path copyResource(String resourceName, Path schemaDirectory) throws IOException {
    Path target = schemaDirectory.resolve(resourceName);
    Files.createDirectories(Objects.requireNonNull(target.getParent()));
    try (InputStream input =
        NativeConformanceSourceGeneratorMain.class.getResourceAsStream("/" + resourceName)) {
      if (input == null) {
        throw new IllegalArgumentException("Missing conformance schema resource " + resourceName);
      }
      Files.copy(input, target);
    }
    return target;
  }

  private static void deleteDirectory(Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return;
    }
    try (var paths = Files.walk(directory)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(path);
      }
    }
  }

  private record GeneratedFixture(
      String schemaResource, GeneratorProfile profile, Map<String, String> namespacePackages) {}
}
