package io.github.mundanej.mxjb.generator.core;

import io.github.mundanej.mxjb.generator.api.Generator;
import io.github.mundanej.mxjb.generator.api.GeneratorDiagnostic;
import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import io.github.mundanej.mxjb.generator.core.bind.BindingConfiguration;
import io.github.mundanej.mxjb.generator.core.bind.BindingModelBuilder;
import io.github.mundanej.mxjb.generator.core.bind.BindingResult;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.emit.GeneratedJavaSource;
import io.github.mundanej.mxjb.generator.core.emit.GeneratedModelEmissionResult;
import io.github.mundanej.mxjb.generator.core.emit.GeneratedModelEmitter;
import io.github.mundanej.mxjb.generator.core.emit.GeneratedReaderEmissionResult;
import io.github.mundanej.mxjb.generator.core.emit.GeneratedReaderEmitter;
import io.github.mundanej.mxjb.generator.core.emit.GeneratedValidatorEmissionResult;
import io.github.mundanej.mxjb.generator.core.emit.GeneratedValidatorEmitter;
import io.github.mundanej.mxjb.generator.core.emit.GeneratedWriterEmissionResult;
import io.github.mundanej.mxjb.generator.core.emit.GeneratedWriterEmitter;
import io.github.mundanej.mxjb.generator.core.resolver.SchemaResolutionResult;
import io.github.mundanej.mxjb.generator.core.resolver.SchemaResolver;
import io.github.mundanej.mxjb.generator.core.resolver.SchemaResolverPolicy;
import io.github.mundanej.mxjb.generator.core.schema.ResolvedSchema;
import io.github.mundanej.mxjb.generator.core.schema.ResolvedSchemaManifest;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrBuilder;
import io.github.mundanej.mxjb.generator.core.schema.SchemaIrResult;
import io.github.mundanej.mxjb.generator.core.schema.XsdSyntaxParser;
import io.github.mundanej.mxjb.generator.core.schema.XsdSyntaxResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Public API adapter for the accepted generator-core pipeline. */
public final class CoreGenerator implements Generator {
  static final String REQUEST_INVALID = "GENERATOR_REQUEST_INVALID";
  static final String DUPLICATE_OUTPUT = "GENERATOR_DUPLICATE_OUTPUT";
  static final String WRITE_FAILED = "GENERATOR_WRITE_FAILED";

  @Override
  public GeneratorResult generate(GeneratorRequest request) {
    if (request == null) {
      return GeneratorResult.failure(
          List.of(diagnostic(REQUEST_INVALID, "request", "Generator request is required.")));
    }

    List<GeneratorDiagnostic> diagnostics = validateRequest(request);
    if (!diagnostics.isEmpty()) {
      return GeneratorResult.failure(diagnostics);
    }

    SchemaResolutionResult resolutionResult = resolveSchemas(request);
    if (resolutionResult.hasErrors()) {
      return GeneratorResult.failure(publicDiagnostics(resolutionResult.diagnostics()));
    }

    XsdSyntaxResult syntaxResult =
        new XsdSyntaxParser().parse(resolutionResult.manifest(), request.profile());
    if (syntaxResult.hasErrors()) {
      return GeneratorResult.failure(publicDiagnostics(syntaxResult.diagnostics()));
    }

    SchemaIrResult irResult = new SchemaIrBuilder().build(syntaxResult);
    if (irResult.hasErrors()) {
      return GeneratorResult.failure(publicDiagnostics(irResult.diagnostics()));
    }

    BindingConfiguration bindingConfiguration =
        new BindingConfiguration(request.defaultPackage(), request.namespacePackages());
    BindingResult bindingResult = new BindingModelBuilder().build(irResult, bindingConfiguration);
    if (bindingResult.hasErrors()) {
      return GeneratorResult.failure(publicDiagnostics(bindingResult.diagnostics()));
    }

    Emission emission = emitSources(bindingResult);
    if (!emission.diagnostics().isEmpty()) {
      return GeneratorResult.failure(publicDiagnostics(emission.diagnostics()));
    }

    List<GeneratedJavaSource> sources = sortedSources(emission.sources());
    diagnostics = validateUniqueOutputPaths(sources);
    if (!diagnostics.isEmpty()) {
      return GeneratorResult.failure(diagnostics);
    }

    return writeSources(request.outputDirectory(), sources);
  }

  private List<GeneratorDiagnostic> validateRequest(GeneratorRequest request) {
    List<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    if (request.schemaPaths().isEmpty()) {
      diagnostics.add(diagnostic(REQUEST_INVALID, "schema", "At least one schema is required."));
    }
    for (Path schemaPath : request.schemaPaths()) {
      if (schemaPath == null) {
        diagnostics.add(diagnostic(REQUEST_INVALID, "schema", "Schema path must not be null."));
      }
    }
    if (request.outputDirectory() == null) {
      diagnostics.add(diagnostic(REQUEST_INVALID, "output", "Output directory is required."));
    }
    if (request.profile() != GeneratorProfile.XP_DATA_10
        && request.profile() != GeneratorProfile.XP_DATA_10_CHOICE
        && request.profile() != GeneratorProfile.XP_VALIDATION_10_BASIC
        && request.profile() != GeneratorProfile.XP_XSD10_COMPOSED) {
      diagnostics.add(
          diagnostic(
              REQUEST_INVALID,
              "profile",
              "Unsupported generator profile " + request.profile() + "."));
    }
    return diagnostics;
  }

  private SchemaResolutionResult resolveSchemas(GeneratorRequest request) {
    List<Path> localRoots = localRoots(request);
    SchemaResolver resolver =
        new SchemaResolver(SchemaResolverPolicy.withCatalog(localRoots, request.catalogMappings()));
    List<SchemaDiagnostic> diagnostics = new ArrayList<>();
    Map<Path, ResolvedSchema> schemasByPath = new LinkedHashMap<>();
    for (Path schemaPath : request.schemaPaths()) {
      SchemaResolutionResult result = resolver.resolve(schemaPath);
      diagnostics.addAll(result.diagnostics());
      for (ResolvedSchema schema : result.manifest().schemas()) {
        schemasByPath.putIfAbsent(schema.sourcePath().toAbsolutePath().normalize(), schema);
      }
    }
    List<ResolvedSchema> schemas =
        schemasByPath.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .toList();
    return new SchemaResolutionResult(
        new ResolvedSchemaManifest(schemas), sortedDiagnostics(diagnostics));
  }

  private List<Path> localRoots(GeneratorRequest request) {
    Set<Path> roots = new LinkedHashSet<>();
    for (Path schemaPath : request.schemaPaths()) {
      if (schemaPath != null) {
        Path normalized = schemaPath.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        roots.add(parent == null ? normalized : parent);
      }
    }
    for (Path localRoot : request.localRoots()) {
      if (localRoot != null) {
        roots.add(localRoot.toAbsolutePath().normalize());
      }
    }
    return List.copyOf(roots);
  }

  private Emission emitSources(BindingResult bindingResult) {
    List<GeneratedJavaSource> sources = new ArrayList<>();
    List<SchemaDiagnostic> diagnostics = new ArrayList<>();

    GeneratedModelEmissionResult modelResult = new GeneratedModelEmitter().emit(bindingResult);
    sources.addAll(modelResult.sources());
    diagnostics.addAll(modelResult.diagnostics());

    GeneratedWriterEmissionResult writerResult = new GeneratedWriterEmitter().emit(bindingResult);
    sources.addAll(writerResult.sources());
    diagnostics.addAll(writerResult.diagnostics());

    GeneratedReaderEmissionResult readerResult = new GeneratedReaderEmitter().emit(bindingResult);
    sources.addAll(readerResult.sources());
    diagnostics.addAll(readerResult.diagnostics());

    GeneratedValidatorEmissionResult validatorResult =
        new GeneratedValidatorEmitter().emit(bindingResult);
    sources.addAll(validatorResult.sources());
    diagnostics.addAll(validatorResult.diagnostics());

    return new Emission(sources, sortedDiagnostics(diagnostics));
  }

  private List<GeneratedJavaSource> sortedSources(List<GeneratedJavaSource> sources) {
    return sources.stream()
        .sorted(Comparator.comparing(source -> source.relativePath().toString()))
        .toList();
  }

  private List<GeneratorDiagnostic> validateUniqueOutputPaths(List<GeneratedJavaSource> sources) {
    List<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    Set<Path> paths = new LinkedHashSet<>();
    for (GeneratedJavaSource source : sources) {
      Path normalized = source.relativePath().normalize();
      if (normalized.isAbsolute() || normalized.startsWith("..")) {
        diagnostics.add(
            diagnostic(
                REQUEST_INVALID,
                source.relativePath().toString(),
                "Generated source path must be relative and stay under the output directory."));
      } else if (!paths.add(normalized)) {
        diagnostics.add(
            diagnostic(
                DUPLICATE_OUTPUT,
                normalized.toString().replace('\\', '/'),
                "Duplicate generated source path."));
      }
    }
    return diagnostics;
  }

  private GeneratorResult writeSources(Path outputDirectory, List<GeneratedJavaSource> sources) {
    Objects.requireNonNull(outputDirectory, "outputDirectory");
    try {
      preflightTargetDirectories(outputDirectory, sources);
      for (GeneratedJavaSource source : sources) {
        Path relativePath = source.relativePath().normalize();
        Path target = outputDirectory.resolve(relativePath).normalize();
        Files.writeString(target, source.sourceText(), StandardCharsets.UTF_8);
      }
      return GeneratorResult.success(
          sources.stream().map(GeneratedJavaSource::relativePath).toList());
    } catch (IOException exception) {
      return GeneratorResult.failure(
          List.of(diagnostic(WRITE_FAILED, outputDirectory.toString(), exception.getMessage())));
    }
  }

  private void preflightTargetDirectories(Path outputDirectory, List<GeneratedJavaSource> sources)
      throws IOException {
    Files.createDirectories(outputDirectory);
    for (GeneratedJavaSource source : sources) {
      Path relativePath = source.relativePath().normalize();
      Path parent = outputDirectory.resolve(relativePath).normalize().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
    }
  }

  private List<SchemaDiagnostic> sortedDiagnostics(List<SchemaDiagnostic> diagnostics) {
    return diagnostics.stream()
        .sorted(
            Comparator.comparing(SchemaDiagnostic::resource)
                .thenComparing(diagnostic -> diagnostic.code().name())
                .thenComparing(SchemaDiagnostic::message))
        .toList();
  }

  private List<GeneratorDiagnostic> publicDiagnostics(List<SchemaDiagnostic> diagnostics) {
    return diagnostics.stream()
        .map(
            diagnostic ->
                new GeneratorDiagnostic(
                    diagnostic.code().name(), diagnostic.resource(), diagnostic.message()))
        .toList();
  }

  private GeneratorDiagnostic diagnostic(String code, String resource, String message) {
    return new GeneratorDiagnostic(code, resource, message);
  }

  private record Emission(List<GeneratedJavaSource> sources, List<SchemaDiagnostic> diagnostics) {}
}
