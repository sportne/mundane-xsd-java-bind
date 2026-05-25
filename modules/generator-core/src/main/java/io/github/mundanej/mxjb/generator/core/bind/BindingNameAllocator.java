package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BindingNameAllocator {
  private final BindingConfiguration configuration;
  private final Map<String, Set<String>> usedTypeNamesByPackage = new HashMap<>();

  BindingNameAllocator(BindingConfiguration configuration) {
    this.configuration = configuration;
  }

  List<SchemaDiagnostic> validateConfiguration() {
    List<SchemaDiagnostic> diagnostics = new ArrayList<>();
    if (!JavaNames.isPackageName(configuration.defaultPackage())) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_BINDING_INVALID_CONFIGURATION,
              "binding",
              "Invalid default package "
                  + configuration.defaultPackage()
                  + ". Use Java package syntax, for example com.example.generated."));
    }
    for (String packageName : configuration.namespacePackages().values()) {
      if (!JavaNames.isPackageName(packageName)) {
        diagnostics.add(
            new SchemaDiagnostic(
                DiagnosticCode.SCHEMA_BINDING_INVALID_CONFIGURATION,
                "binding",
                "Invalid namespace package "
                    + packageName
                    + ". Use Java package syntax, for example com.example.generated."));
      }
    }
    return diagnostics;
  }

  BindingJavaName javaName(SchemaQName schemaName) {
    String packageName = packageName(schemaName.namespace());
    Set<String> usedTypeNames =
        usedTypeNamesByPackage.computeIfAbsent(packageName, ignored -> new HashSet<>());
    String simpleName = JavaNames.uniqueTypeName(schemaName, usedTypeNames);
    return new BindingJavaName(packageName, simpleName);
  }

  String uniqueTypeName(String packageName, String baseName) {
    Set<String> usedTypeNames =
        usedTypeNamesByPackage.computeIfAbsent(packageName, ignored -> new HashSet<>());
    return JavaNames.unique(JavaNames.sanitizeIdentifier(baseName, true), usedTypeNames);
  }

  String uniqueFieldName(SchemaQName schemaName, Set<String> usedFieldNames) {
    return JavaNames.uniqueFieldName(schemaName, usedFieldNames);
  }

  String unique(String baseName, Set<String> usedNames) {
    return JavaNames.unique(baseName, usedNames);
  }

  String fieldName(SchemaQName schemaName) {
    return JavaNames.fieldName(schemaName);
  }

  String fieldNameFromTypeName(String typeName) {
    return JavaNames.fieldNameFromTypeName(typeName);
  }

  String typeName(SchemaQName schemaName) {
    return JavaNames.typeName(schemaName);
  }

  private String packageName(String namespace) {
    String override = configuration.namespacePackages().get(namespace);
    if (override != null) {
      return override;
    }
    if (namespace == null || namespace.isBlank()) {
      return configuration.defaultPackage();
    }
    if (namespace.startsWith("urn:")) {
      List<String> tokens = JavaNames.packageTokens(namespace.substring("urn:".length()));
      return tokens.isEmpty()
          ? configuration.defaultPackage()
          : configuration.defaultPackage() + "." + String.join(".", tokens);
    }
    try {
      URI uri = new URI(namespace);
      if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
        List<String> tokens = new ArrayList<>();
        String host = uri.getHost();
        if (host != null) {
          List<String> parts = JavaNames.splitOnDot(host);
          for (int index = parts.size() - 1; index >= 0; index--) {
            tokens.addAll(JavaNames.packageTokens(parts.get(index)));
          }
        }
        tokens.addAll(JavaNames.packageTokens(uri.getPath()));
        return tokens.isEmpty() ? configuration.defaultPackage() : String.join(".", tokens);
      }
    } catch (URISyntaxException ignored) {
      return configuration.defaultPackage();
    }
    return configuration.defaultPackage();
  }
}
