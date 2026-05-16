package io.github.mundanej.mxjb.generator.core.resolver;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Explicit schema resolution policy for local roots and catalog mappings. */
public final class SchemaResolverPolicy {
  private final List<Path> localRoots;
  private final Map<URI, Path> catalogMappings;

  public SchemaResolverPolicy(List<Path> localRoots, Map<URI, Path> catalogMappings) {
    Objects.requireNonNull(localRoots, "localRoots");
    Objects.requireNonNull(catalogMappings, "catalogMappings");
    this.localRoots = localRoots.stream().map(Path::toAbsolutePath).map(Path::normalize).toList();
    this.catalogMappings = Map.copyOf(catalogMappings);
  }

  public static SchemaResolverPolicy localRoots(List<Path> localRoots) {
    return new SchemaResolverPolicy(localRoots, Map.of());
  }

  public static SchemaResolverPolicy withCatalog(
      List<Path> localRoots, Map<URI, Path> catalogMappings) {
    return new SchemaResolverPolicy(localRoots, catalogMappings);
  }

  public List<Path> localRoots() {
    return List.copyOf(localRoots);
  }

  public Map<URI, Path> catalogMappings() {
    return Map.copyOf(catalogMappings);
  }
}
