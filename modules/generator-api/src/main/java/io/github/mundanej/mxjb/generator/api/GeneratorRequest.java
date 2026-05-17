package io.github.mundanej.mxjb.generator.api;

import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable public generator request. */
public record GeneratorRequest(
    List<Path> schemaPaths,
    Path outputDirectory,
    GeneratorProfile profile,
    String defaultPackage,
    Map<String, String> namespacePackages,
    List<Path> localRoots,
    Map<URI, Path> catalogMappings) {
  public static final String DEFAULT_PACKAGE = "io.github.mundanej.mxjb.generated";

  public GeneratorRequest {
    Objects.requireNonNull(schemaPaths, "schemaPaths");
    Objects.requireNonNull(namespacePackages, "namespacePackages");
    Objects.requireNonNull(localRoots, "localRoots");
    Objects.requireNonNull(catalogMappings, "catalogMappings");
    schemaPaths = List.copyOf(schemaPaths);
    profile = profile == null ? GeneratorProfile.XP_DATA_10 : profile;
    defaultPackage =
        defaultPackage == null || defaultPackage.isBlank() ? DEFAULT_PACKAGE : defaultPackage;
    namespacePackages = orderedCopy(namespacePackages);
    localRoots = List.copyOf(localRoots);
    catalogMappings = Map.copyOf(catalogMappings);
  }

  public static GeneratorRequest of(List<Path> schemaPaths, Path outputDirectory) {
    return new GeneratorRequest(
        schemaPaths,
        outputDirectory,
        GeneratorProfile.XP_DATA_10,
        DEFAULT_PACKAGE,
        Map.of(),
        List.of(),
        Map.of());
  }

  @Override
  public List<Path> schemaPaths() {
    return List.copyOf(schemaPaths);
  }

  @Override
  public Map<String, String> namespacePackages() {
    return Map.copyOf(namespacePackages);
  }

  @Override
  public List<Path> localRoots() {
    return List.copyOf(localRoots);
  }

  @Override
  public Map<URI, Path> catalogMappings() {
    return Map.copyOf(catalogMappings);
  }

  private static Map<String, String> orderedCopy(Map<String, String> source) {
    LinkedHashMap<String, String> copy = new LinkedHashMap<>();
    source.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> copy.put(entry.getKey(), entry.getValue()));
    return Map.copyOf(copy);
  }
}
