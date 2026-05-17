package io.github.mundanej.mxjb.runtime.jdkxml;

import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.XmlOutput;
import java.util.Objects;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/** Entry points for optional JDK XML adapters. */
public final class JdkXmlAdapters {
  private static final XMLResolver DENYING_RESOLVER =
      (publicId, systemId, baseUri, namespace) -> {
        throw new UnsupportedOperationException("External XML resources are denied by default.");
      };

  private JdkXmlAdapters() {}

  /**
   * Creates a StAX input factory with DTDs and external entities disabled.
   *
   * @return a configured JDK XML input factory
   */
  public static XMLInputFactory secureInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    factory.setXMLResolver(DENYING_RESOLVER);
    return factory;
  }

  /**
   * Adapts a StAX reader to the project-owned XML event interface.
   *
   * @param reader StAX reader to adapt
   * @return event reader backed by {@code reader}
   */
  public static XmlEventReader eventReader(XMLStreamReader reader) {
    return new StaxXmlEventReader(Objects.requireNonNull(reader, "reader"));
  }

  /**
   * Adapts a StAX writer to the project-owned XML output interface.
   *
   * @param writer StAX writer to adapt
   * @return XML output backed by {@code writer}
   */
  public static XmlOutput output(XMLStreamWriter writer) {
    return new StaxXmlOutput(Objects.requireNonNull(writer, "writer"));
  }
}
