package io.github.mundanej.mxjb.generator.core.resolver;

import io.github.mundanej.mxjb.generator.core.diagnostics.DiagnosticCode;
import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.schema.ResolvedSchema;
import io.github.mundanej.mxjb.generator.core.schema.ResolvedSchemaManifest;
import io.github.mundanej.mxjb.generator.core.schema.SchemaReference;
import io.github.mundanej.mxjb.generator.core.schema.SchemaReferenceKind;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Resolves schema resources through an explicit local policy. */
public final class SchemaResolver {
  private static final String XSD_NAMESPACE = XMLConstants.W3C_XML_SCHEMA_NS_URI;

  private final SchemaResolverPolicy policy;

  public SchemaResolver(SchemaResolverPolicy policy) {
    this.policy = Objects.requireNonNull(policy, "policy");
  }

  public SchemaResolutionResult resolve(URI primarySchema) {
    Objects.requireNonNull(primarySchema, "primarySchema");
    List<SchemaDiagnostic> diagnostics = new ArrayList<>();
    List<ResolvedSchema> schemas = new ArrayList<>();
    Set<Path> visited = new HashSet<>();
    resolveUri(primarySchema, null, new ArrayDeque<>(), visited, schemas, diagnostics);
    return new SchemaResolutionResult(new ResolvedSchemaManifest(schemas), diagnostics);
  }

  public SchemaResolutionResult resolve(Path primarySchema) {
    Objects.requireNonNull(primarySchema, "primarySchema");
    return resolve(primarySchema.toAbsolutePath().normalize().toUri());
  }

  private void resolveUri(
      URI uri,
      Path baseDirectory,
      ArrayDeque<Path> stack,
      Set<Path> visited,
      List<ResolvedSchema> schemas,
      List<SchemaDiagnostic> diagnostics) {
    Path path = resolvePath(uri, baseDirectory, diagnostics);
    if (path == null) {
      return;
    }
    resolvePath(path, stack, visited, schemas, diagnostics);
  }

  private void resolvePath(
      Path path,
      ArrayDeque<Path> stack,
      Set<Path> visited,
      List<ResolvedSchema> schemas,
      List<SchemaDiagnostic> diagnostics) {
    if (stack.contains(path)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_RESOURCE_CYCLE,
              resourceId(path),
              "Schema include/import cycle: " + cycleText(stack, path)));
      return;
    }
    if (!visited.add(path)) {
      return;
    }
    if (!Files.exists(path)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_RESOURCE_NOT_FOUND,
              resourceId(path),
              "Schema resource does not exist. Check the schema path or catalog mapping."));
      return;
    }

    stack.addLast(path);
    ParsedSchema parsed = parse(path, diagnostics);
    if (parsed != null) {
      schemas.add(
          new ResolvedSchema(
              resourceId(path), path, parsed.targetNamespace(), parsed.references()));
      for (SchemaReference reference : parsed.references()) {
        resolveUri(
            toUri(reference.target()), path.getParent(), stack, visited, schemas, diagnostics);
      }
    }
    stack.removeLast();
  }

  private Path resolvePath(URI uri, Path baseDirectory, List<SchemaDiagnostic> diagnostics) {
    Path catalogPath = policy.catalogMappings().get(uri);
    if (catalogPath != null) {
      return acceptedLocalPath(catalogPath, uri.toString(), diagnostics);
    }
    if (isNetworkUri(uri)) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_RESOURCE_NETWORK_DENIED,
              uri.toString(),
              "Network schema resolution is denied by default. Add an explicit catalog mapping "
                  + "or local schema copy."));
      return null;
    }
    if (uri.getScheme() == null) {
      if (baseDirectory == null) {
        diagnostics.add(
            new SchemaDiagnostic(
                DiagnosticCode.SCHEMA_RESOURCE_OUTSIDE_LOCAL_ROOT,
                uri.toString(),
                "Relative schema resource has no base directory."));
        return null;
      }
      return acceptedLocalPath(baseDirectory.resolve(uri.toString()), uri.toString(), diagnostics);
    }
    if ("file".equals(uri.getScheme())) {
      return acceptedLocalPath(Path.of(uri), uri.toString(), diagnostics);
    }

    diagnostics.add(
        new SchemaDiagnostic(
            DiagnosticCode.SCHEMA_RESOURCE_OUTSIDE_LOCAL_ROOT,
            uri.toString(),
            "Schema resource is not mapped by the explicit resolver policy."));
    return null;
  }

  private Path acceptedLocalPath(
      Path candidate, String resource, List<SchemaDiagnostic> diagnostics) {
    Path normalized = candidate.toAbsolutePath().normalize();
    boolean allowed = policy.localRoots().stream().anyMatch(normalized::startsWith);
    if (!allowed) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_RESOURCE_OUTSIDE_LOCAL_ROOT,
              resource,
              "Schema resource is outside configured local roots. Add the directory to local "
                  + "roots or catalog the resource explicitly."));
      return null;
    }
    return normalized;
  }

  private ParsedSchema parse(Path path, List<SchemaDiagnostic> diagnostics) {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    try (InputStream input = Files.newInputStream(path)) {
      XMLStreamReader reader = factory.createXMLStreamReader(input);
      try {
        return readSchema(reader);
      } finally {
        reader.close();
      }
    } catch (IOException exception) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_RESOURCE_IO_ERROR, resourceId(path), exception.getMessage()));
    } catch (XMLStreamException exception) {
      diagnostics.add(
          new SchemaDiagnostic(
              DiagnosticCode.SCHEMA_RESOURCE_XML_ERROR, resourceId(path), exception.getMessage()));
    }
    return null;
  }

  private ParsedSchema readSchema(XMLStreamReader reader) throws XMLStreamException {
    String targetNamespace = "";
    List<SchemaReference> references = new ArrayList<>();
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT
          && XSD_NAMESPACE.equals(reader.getNamespaceURI())) {
        String localName = reader.getLocalName();
        if ("schema".equals(localName)) {
          targetNamespace = valueOrEmpty(reader.getAttributeValue(null, "targetNamespace"));
        } else if ("include".equals(localName)) {
          addReference(
              references,
              SchemaReferenceKind.INCLUDE,
              "",
              reader.getAttributeValue(null, "schemaLocation"));
        } else if ("import".equals(localName)) {
          addReference(
              references,
              SchemaReferenceKind.IMPORT,
              reader.getAttributeValue(null, "namespace"),
              reader.getAttributeValue(null, "schemaLocation"));
        }
      }
    }
    return new ParsedSchema(targetNamespace, references);
  }

  private void addReference(
      List<SchemaReference> references,
      SchemaReferenceKind kind,
      String namespace,
      String schemaLocation) {
    if (schemaLocation != null && !schemaLocation.isBlank()) {
      references.add(new SchemaReference(kind, valueOrEmpty(namespace), schemaLocation));
    }
  }

  private URI toUri(String value) {
    try {
      return new URI(value);
    } catch (URISyntaxException exception) {
      return Path.of(value).toUri();
    }
  }

  private boolean isNetworkUri(URI uri) {
    return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
  }

  private String resourceId(Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    for (Path root : policy.localRoots()) {
      if (normalized.startsWith(root)) {
        return resourceId(root, normalized);
      }
    }
    for (Map.Entry<URI, Path> entry : policy.catalogMappings().entrySet()) {
      if (normalized.equals(entry.getValue().toAbsolutePath().normalize())) {
        return entry.getKey().toString();
      }
    }
    Path fileName = normalized.getFileName();
    return fileName == null ? normalized.toString() : fileName.toString();
  }

  private String resourceId(Path root, Path normalized) {
    String relative = root.relativize(normalized).toString().replace('\\', '/');
    if (policy.localRoots().size() == 1) {
      return relative;
    }
    String prefix = displayPrefix(root);
    return prefix.isBlank() ? relative : "root[" + prefix + "]/" + relative;
  }

  private String displayPrefix(Path root) {
    List<Path> roots = policy.localRoots();
    for (int nameCount = 1; nameCount <= root.getNameCount(); nameCount++) {
      Path suffix = root.subpath(root.getNameCount() - nameCount, root.getNameCount());
      boolean unique =
          roots.stream()
              .filter(other -> !other.equals(root))
              .noneMatch(other -> endsWith(other, suffix));
      if (unique) {
        return suffix.toString().replace('\\', '/');
      }
    }
    return root.toString().replace('\\', '/');
  }

  private boolean endsWith(Path root, Path suffix) {
    if (root.getNameCount() < suffix.getNameCount()) {
      return false;
    }
    Path rootSuffix =
        root.subpath(root.getNameCount() - suffix.getNameCount(), root.getNameCount());
    return rootSuffix.equals(suffix);
  }

  private String cycleText(ArrayDeque<Path> stack, Path repeated) {
    List<String> cycle = new ArrayList<>();
    boolean inCycle = false;
    for (Path item : stack) {
      if (item.equals(repeated)) {
        inCycle = true;
      }
      if (inCycle) {
        cycle.add(resourceId(item));
      }
    }
    cycle.add(resourceId(repeated));
    return String.join(" -> ", cycle);
  }

  private String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  private record ParsedSchema(String targetNamespace, List<SchemaReference> references) {
    private ParsedSchema {
      references = List.copyOf(references);
    }
  }
}
