package io.github.mundanej.mxjb.generator.cli;

import io.github.mundanej.mxjb.generator.api.GeneratorDiagnostic;
import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import io.github.mundanej.mxjb.generator.core.CoreGenerator;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Command-line entry point for mundane XSD Java Binding. */
public final class MxjbCli {
  private static final String INVALID_ARGUMENT = "GENERATOR_CLI_INVALID_ARGUMENT";
  private static final String HELP =
      """
      Usage:
        mxjb generate --schema <path> --output <dir> [options]

      Options:
        --schema <path>                    XSD schema input. May be repeated.
        --output <dir>                     Generated Java source output directory.
        --profile <XP-DATA-10|XP-DATA-10-CHOICE>
                                           Generator compatibility profile.
        --default-package <package>        Package for schemas without a namespace.
        --namespace-package <ns=package>   Namespace to Java package mapping. May be repeated.
        --local-root <path>                Additional local schema resolver root.
        --catalog <uri=path>               Explicit schemaLocation URI to local path mapping.
        --help                             Show this help.
      """;

  private MxjbCli() {}

  public static void main(String[] args) {
    System.exit(run(args, System.out, System.err));
  }

  public static int run(String[] args, PrintStream out, PrintStream err) {
    if (args.length == 0 || isHelp(args[0])) {
      out.print(HELP);
      return 0;
    }
    if (!"generate".equals(args[0])) {
      printDiagnostic(err, diagnostic("cli", "Expected subcommand generate."));
      return 2;
    }
    ParseResult parseResult = parseGenerate(java.util.Arrays.copyOfRange(args, 1, args.length));
    if (parseResult.help()) {
      out.print(HELP);
      return 0;
    }
    if (!parseResult.diagnostics().isEmpty()) {
      printDiagnostics(err, parseResult.diagnostics());
      return 2;
    }

    GeneratorResult result = new CoreGenerator().generate(parseResult.request());
    if (!result.successful()) {
      printDiagnostics(err, result.diagnostics());
      return 1;
    }
    for (Path generatedSource : result.generatedSources()) {
      out.println(generatedSource.toString().replace('\\', '/'));
    }
    return 0;
  }

  private static ParseResult parseGenerate(String[] args) {
    List<Path> schemas = new ArrayList<>();
    Path outputDirectory = null;
    GeneratorProfile profile = GeneratorProfile.XP_DATA_10;
    String defaultPackage = GeneratorRequest.DEFAULT_PACKAGE;
    Map<String, String> namespacePackages = new LinkedHashMap<>();
    List<Path> localRoots = new ArrayList<>();
    Map<URI, Path> catalogMappings = new LinkedHashMap<>();
    List<GeneratorDiagnostic> diagnostics = new ArrayList<>();

    for (int index = 0; index < args.length; index++) {
      String option = args[index];
      if (isHelp(option)) {
        return ParseResult.helpResult();
      }
      if (isCodeToSchemaOption(option)) {
        diagnostics.add(diagnostic(option, "Code-to-schema generation is not supported."));
        continue;
      }
      String inlineValue = inlineValue(option);
      String optionName = inlineValue == null ? option : option.substring(0, option.indexOf('='));
      switch (optionName) {
        case "--schema" -> {
          Value value = value(args, index, optionName, inlineValue, diagnostics);
          if (value.present()) {
            schemas.add(Path.of(value.text()));
          }
          index = value.index();
        }
        case "--output" -> {
          Value value = value(args, index, optionName, inlineValue, diagnostics);
          if (value.present()) {
            outputDirectory = Path.of(value.text());
          }
          index = value.index();
        }
        case "--profile" -> {
          Value value = value(args, index, optionName, inlineValue, diagnostics);
          if (value.present()) {
            java.util.Optional<GeneratorProfile> parsedProfile =
                GeneratorProfile.fromCliToken(value.text());
            if (parsedProfile.isPresent()) {
              profile = parsedProfile.orElseThrow();
            } else {
              diagnostics.add(
                  diagnostic(optionName, "Unsupported generator profile " + value.text() + "."));
            }
          }
          index = value.index();
        }
        case "--default-package" -> {
          Value value = value(args, index, optionName, inlineValue, diagnostics);
          if (value.present()) {
            defaultPackage = value.text();
          }
          index = value.index();
        }
        case "--namespace-package" -> {
          Value value = value(args, index, optionName, inlineValue, diagnostics);
          if (value.present()) {
            addNamespacePackage(value.text(), namespacePackages, diagnostics);
          }
          index = value.index();
        }
        case "--local-root" -> {
          Value value = value(args, index, optionName, inlineValue, diagnostics);
          if (value.present()) {
            localRoots.add(Path.of(value.text()));
          }
          index = value.index();
        }
        case "--catalog" -> {
          Value value = value(args, index, optionName, inlineValue, diagnostics);
          if (value.present()) {
            addCatalogMapping(value.text(), catalogMappings, diagnostics);
          }
          index = value.index();
        }
        default -> diagnostics.add(diagnostic(optionName, "Unknown option " + optionName + "."));
      }
    }

    GeneratorRequest request =
        new GeneratorRequest(
            schemas,
            outputDirectory,
            profile,
            defaultPackage,
            namespacePackages,
            localRoots,
            catalogMappings);
    return new ParseResult(false, request, diagnostics);
  }

  private static boolean isHelp(String value) {
    return "--help".equals(value) || "-h".equals(value);
  }

  private static boolean isCodeToSchemaOption(String value) {
    return "--java".equals(value) || "--class".equals(value) || "--code-to-schema".equals(value);
  }

  private static String inlineValue(String option) {
    int separator = option.indexOf('=');
    if (separator < 0) {
      return null;
    }
    return option.substring(separator + 1);
  }

  private static Value value(
      String[] args,
      int index,
      String option,
      String inlineValue,
      List<GeneratorDiagnostic> diagnostics) {
    if (inlineValue != null) {
      if (inlineValue.isBlank()) {
        diagnostics.add(diagnostic(option, "Missing value for option " + option + "."));
        return new Value(false, "", index);
      }
      return new Value(true, inlineValue, index);
    }
    int valueIndex = index + 1;
    if (valueIndex >= args.length
        || args[valueIndex].startsWith("--")
        || args[valueIndex].isBlank()) {
      diagnostics.add(diagnostic(option, "Missing value for option " + option + "."));
      return new Value(false, "", index);
    }
    return new Value(true, args[valueIndex], valueIndex);
  }

  private static void addNamespacePackage(
      String value, Map<String, String> namespacePackages, List<GeneratorDiagnostic> diagnostics) {
    int separator = value.indexOf('=');
    if (separator < 0 || separator == value.length() - 1) {
      diagnostics.add(
          diagnostic("--namespace-package", "Expected namespace package mapping ns=package."));
      return;
    }
    namespacePackages.put(value.substring(0, separator), value.substring(separator + 1));
  }

  private static void addCatalogMapping(
      String value, Map<URI, Path> catalogMappings, List<GeneratorDiagnostic> diagnostics) {
    int separator = value.indexOf('=');
    if (separator <= 0 || separator == value.length() - 1) {
      diagnostics.add(diagnostic("--catalog", "Expected catalog mapping uri=path."));
      return;
    }
    try {
      catalogMappings.put(
          URI.create(value.substring(0, separator)), Path.of(value.substring(separator + 1)));
    } catch (IllegalArgumentException exception) {
      diagnostics.add(
          diagnostic("--catalog", "Invalid catalog URI " + value.substring(0, separator) + "."));
    }
  }

  private static GeneratorDiagnostic diagnostic(String resource, String message) {
    return new GeneratorDiagnostic(INVALID_ARGUMENT, resource, message);
  }

  private static void printDiagnostics(PrintStream err, List<GeneratorDiagnostic> diagnostics) {
    for (GeneratorDiagnostic diagnostic : diagnostics) {
      printDiagnostic(err, diagnostic);
    }
  }

  private static void printDiagnostic(PrintStream err, GeneratorDiagnostic diagnostic) {
    err.println(diagnostic.toManifestLine());
  }

  private record ParseResult(
      boolean help, GeneratorRequest request, List<GeneratorDiagnostic> diagnostics) {
    private ParseResult {
      diagnostics = List.copyOf(diagnostics);
    }

    private static ParseResult helpResult() {
      return new ParseResult(true, null, List.of());
    }
  }

  private record Value(boolean present, String text, int index) {}
}
