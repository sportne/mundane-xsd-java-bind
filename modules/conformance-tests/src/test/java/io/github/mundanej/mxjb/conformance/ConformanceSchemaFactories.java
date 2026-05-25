package io.github.mundanej.mxjb.conformance;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

final class ConformanceSchemaFactories {
  private ConformanceSchemaFactories() {}

  static Schema newSchema(Path schemaPath) throws SAXException {
    return secureFactory().newSchema(schemaPath.toFile());
  }

  static Schema newSchema(Path schemaPath, Path... additionalSchemaPaths) throws SAXException {
    SchemaFactory factory = secureFactory();
    factory.setResourceResolver(new ExplicitSchemaResolver(List.of(additionalSchemaPaths)));
    return factory.newSchema(schemaPath.toFile());
  }

  static SchemaFactory secureFactory() throws SAXException {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  private static final class ExplicitSchemaResolver implements LSResourceResolver {
    private final Map<String, Path> bySystemId;
    private final Map<String, Path> byFileName;
    private final Set<String> ambiguousFileNames;

    ExplicitSchemaResolver(List<Path> schemaPaths) {
      this.bySystemId = new HashMap<>();
      this.byFileName = new HashMap<>();
      this.ambiguousFileNames = new HashSet<>();
      for (Path schemaPath : schemaPaths) {
        Path normalized = schemaPath.toAbsolutePath().normalize();
        Path fileName = normalized.getFileName();
        if (fileName == null) {
          throw new IllegalArgumentException("Schema path must include a file name: " + normalized);
        }
        bySystemId.put(normalized.toUri().toString(), normalized);
        if (byFileName.putIfAbsent(fileName.toString(), normalized) != null) {
          byFileName.remove(fileName.toString());
          ambiguousFileNames.add(fileName.toString());
        }
      }
    }

    @Override
    public LSInput resolveResource(
        String type, String namespaceUri, String publicId, String systemId, String baseUri) {
      Path resolved = resolve(systemId, baseUri);
      if (resolved == null) {
        return null;
      }
      try {
        return new StringLsInput(
            publicId,
            resolved.toUri().toString(),
            Files.readString(resolved, StandardCharsets.UTF_8));
      } catch (IOException exception) {
        throw new IllegalStateException(
            "Unable to read explicit schema resource " + resolved, exception);
      }
    }

    private Path resolve(String systemId, String baseUri) {
      if (systemId == null) {
        return null;
      }
      Path direct = bySystemId.get(systemId);
      if (direct != null) {
        return direct;
      }
      if (baseUri != null) {
        Path resolved = bySystemId.get(URI.create(baseUri).resolve(systemId).toString());
        if (resolved != null) {
          return resolved;
        }
      }
      if (URI.create(systemId).isAbsolute()) {
        return null;
      }
      Path systemFileName = Path.of(systemId).getFileName();
      if (systemFileName == null || ambiguousFileNames.contains(systemFileName.toString())) {
        return null;
      }
      return byFileName.get(systemFileName.toString());
    }
  }

  private static final class StringLsInput implements LSInput {
    private final String publicId;
    private final String systemId;
    private final String data;

    StringLsInput(String publicId, String systemId, String data) {
      this.publicId = publicId;
      this.systemId = systemId;
      this.data = data;
    }

    @Override
    public Reader getCharacterStream() {
      return null;
    }

    @Override
    public void setCharacterStream(Reader characterStream) {}

    @Override
    public java.io.InputStream getByteStream() {
      return null;
    }

    @Override
    public void setByteStream(java.io.InputStream byteStream) {}

    @Override
    public String getStringData() {
      return data;
    }

    @Override
    public void setStringData(String stringData) {}

    @Override
    public String getSystemId() {
      return systemId;
    }

    @Override
    public void setSystemId(String systemId) {}

    @Override
    public String getPublicId() {
      return publicId;
    }

    @Override
    public void setPublicId(String publicId) {}

    @Override
    public String getBaseURI() {
      return null;
    }

    @Override
    public void setBaseURI(String baseUri) {}

    @Override
    public String getEncoding() {
      return StandardCharsets.UTF_8.name();
    }

    @Override
    public void setEncoding(String encoding) {}

    @Override
    public boolean getCertifiedText() {
      return false;
    }

    @Override
    public void setCertifiedText(boolean certifiedText) {}
  }
}
