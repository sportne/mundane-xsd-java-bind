package com.example.document.xml;

/** Generated XML writer for {@link com.example.document.Order}. */
public final class OrderXmlWriter {
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_1 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:document-smoke", "order");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_2 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:document-smoke", "version");
  private static final io.github.mundanej.mxjb.runtime.XmlName NAME_3 =
      new io.github.mundanej.mxjb.runtime.XmlName("urn:document-smoke", "id");

  private OrderXmlWriter() {}

  public static void write(
      io.github.mundanej.mxjb.runtime.XmlOutput output, com.example.document.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(output, "output");
    java.util.Objects.requireNonNull(value, "value");
    writeOrder(output, NAME_1, value);
  }

  private static void writeOrder(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlName elementName,
      com.example.document.Order value)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    output.startElement(elementName);
    output.attribute(NAME_2, value.version());
    for (com.example.document.OrderContent contentValue : value.content()) {
      if (contentValue instanceof com.example.document.OrderTextContent branch) {
        output.text(branch.value());
      } else if (contentValue instanceof com.example.document.IdContent branch) {
        output.startElement(NAME_3);
        output.text(branch.value());
        output.endElement(NAME_3);
      } else if (contentValue instanceof com.example.document.OrderWildcardContent branch) {
        writeFragment(output, branch.value());
      }
    }
    output.endElement(elementName);
  }

  private static void writeFragment(
      io.github.mundanej.mxjb.runtime.XmlOutput output,
      io.github.mundanej.mxjb.runtime.XmlFragment fragment)
      throws io.github.mundanej.mxjb.runtime.XmlWriteException {
    java.util.Objects.requireNonNull(fragment, "fragment");
    output.startElement(fragment.name());
    for (io.github.mundanej.mxjb.runtime.XmlAttribute attribute : fragment.attributes()) {
      output.attribute(attribute.name(), attribute.value());
    }
    for (io.github.mundanej.mxjb.runtime.XmlFragmentContent content : fragment.content()) {
      if (content instanceof io.github.mundanej.mxjb.runtime.XmlFragmentText text) {
        output.text(text.text());
      } else if (content instanceof io.github.mundanej.mxjb.runtime.XmlFragmentElement element) {
        writeFragment(output, element.fragment());
      }
    }
    output.endElement(fragment.name());
  }
}
