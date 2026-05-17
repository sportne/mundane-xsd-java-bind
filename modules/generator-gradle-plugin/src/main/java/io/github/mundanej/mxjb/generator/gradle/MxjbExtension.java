package io.github.mundanej.mxjb.generator.gradle;

import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/** Gradle DSL extension for schema-to-Java generation. */
public class MxjbExtension {
  private final ConfigurableFileCollection schemas;
  private final DirectoryProperty outputDirectory;
  private final Property<String> profile;
  private final Property<String> defaultPackage;
  private final MapProperty<String, String> namespacePackages;
  private final ConfigurableFileCollection localRoots;
  private final MapProperty<String, String> catalogMappings;
  private final ConfigurableFileCollection catalogTargets;
  private final ObjectFactory objects;

  public MxjbExtension(ObjectFactory objects) {
    this.schemas = objects.fileCollection();
    this.outputDirectory = objects.directoryProperty();
    this.profile = objects.property(String.class).convention("XP-DATA-10");
    this.defaultPackage =
        objects.property(String.class).convention(GeneratorRequest.DEFAULT_PACKAGE);
    this.namespacePackages = objects.mapProperty(String.class, String.class);
    this.localRoots = objects.fileCollection();
    this.catalogMappings = objects.mapProperty(String.class, String.class);
    this.catalogTargets = objects.fileCollection();
    this.objects = objects;
  }

  public void schema(Object path) {
    schemas.from(path);
  }

  public void namespacePackage(String namespace, String packageName) {
    namespacePackages.put(namespace, packageName);
  }

  public void localRoot(Object path) {
    localRoots.from(path);
  }

  public void catalog(String uri, Object path) {
    ConfigurableFileCollection files = objects.fileCollection().from(path);
    catalogTargets.from(files);
    catalogMappings.put(
        uri, files.getSingleFile().toPath().toAbsolutePath().normalize().toString());
  }

  public ConfigurableFileCollection getSchemas() {
    return schemas;
  }

  public DirectoryProperty getOutputDirectory() {
    return outputDirectory;
  }

  public Property<String> getProfile() {
    return profile;
  }

  public Property<String> getDefaultPackage() {
    return defaultPackage;
  }

  public MapProperty<String, String> getNamespacePackages() {
    return namespacePackages;
  }

  public ConfigurableFileCollection getLocalRoots() {
    return localRoots;
  }

  public MapProperty<String, String> getCatalogMappings() {
    return catalogMappings;
  }

  public ConfigurableFileCollection getCatalogTargets() {
    return catalogTargets;
  }
}
