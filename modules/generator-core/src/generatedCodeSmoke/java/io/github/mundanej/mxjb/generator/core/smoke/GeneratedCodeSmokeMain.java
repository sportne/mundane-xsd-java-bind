package io.github.mundanej.mxjb.generator.core.smoke;

import com.example.lines.Line;
import com.example.orders.Order;
import com.example.orders.xml.OrderXmlWriter;
import io.github.mundanej.mxjb.runtime.XmlName;
import io.github.mundanej.mxjb.runtime.XmlOutput;
import io.github.mundanej.mxjb.runtime.XmlWriteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Executable smoke check for approved generated model and writer fixtures. */
public final class GeneratedCodeSmokeMain {
  private GeneratedCodeSmokeMain() {}

  public static void main(String[] args) throws XmlWriteException {
    RecordingXmlOutput output = new RecordingXmlOutput();
    Order order =
        new Order(
            Optional.of("v1"),
            "A-1",
            Optional.of("gift"),
            List.of(new Line("SKU-1"), new Line("SKU-2")));

    OrderXmlWriter.write(output, order);

    List<String> expected =
        List.of(
            "start:{urn:orders}order",
            "attr:{urn:orders}version=v1",
            "start:{urn:orders}id",
            "text:A-1",
            "end:{urn:orders}id",
            "start:{urn:orders}note",
            "text:gift",
            "end:{urn:orders}note",
            "start:{urn:orders}line",
            "start:{urn:orders}sku",
            "text:SKU-1",
            "end:{urn:orders}sku",
            "end:{urn:orders}line",
            "start:{urn:orders}line",
            "start:{urn:orders}sku",
            "text:SKU-2",
            "end:{urn:orders}sku",
            "end:{urn:orders}line",
            "end:{urn:orders}order");
    if (!expected.equals(output.events)) {
      throw new AssertionError("Generated-code smoke output mismatch: " + output.events);
    }
  }

  private static final class RecordingXmlOutput implements XmlOutput {
    private final List<String> events = new ArrayList<>();

    @Override
    public void startDocument() {}

    @Override
    public void endDocument() {}

    @Override
    public void startElement(XmlName name) {
      events.add("start:" + toText(name));
    }

    @Override
    public void attribute(XmlName name, String value) {
      events.add("attr:" + toText(name) + "=" + value);
    }

    @Override
    public void text(String value) {
      events.add("text:" + value);
    }

    @Override
    public void endElement(XmlName name) {
      events.add("end:" + toText(name));
    }

    @Override
    public void flush() {}

    private String toText(XmlName name) {
      return "{" + name.namespaceUri() + "}" + name.localName();
    }
  }
}
