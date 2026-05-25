package io.github.mundanej.mxjb.conformance.benchmark;

import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import io.github.mundanej.mxjb.generator.core.CoreGenerator;
import io.github.mundanej.mxjb.runtime.ValidationResult;
import io.github.mundanej.mxjb.runtime.XmlEventReader;
import io.github.mundanej.mxjb.runtime.XmlOutput;
import io.github.mundanej.mxjb.runtime.XmlReadException;
import io.github.mundanej.mxjb.runtime.XmlWriteException;
import io.github.mundanej.mxjb.runtime.jdkxml.JdkXmlAdapters;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/** Advisory generated-binding benchmark smoke checks for TASK-0043 and TASK-0071. */
public final class ConformanceBenchmarkSmokeMain {
  private static final int ITERATIONS = 8;
  private static final int WARMUP_ITERATIONS = 2;
  private static final int LINE_COUNT = 240;
  private static final int DOCUMENT_FRAGMENT_COUNT = 120;
  private static final Path WORK_DIRECTORY = Path.of("build", "benchmarkSmoke", "runtime");

  private ConformanceBenchmarkSmokeMain() {}

  public static void main(String[] args) throws IOException, ReflectiveOperationException {
    Files.createDirectories(WORK_DIRECTORY);
    try (GeneratedDocumentBindings documentBindings =
            generateDocumentBindings(
                "document",
                "/xp-xsd10-document/order.xsd",
                "urn:document",
                "com.example.benchmark.document");
        GeneratedDocumentBindings mixedBindings =
            generateDocumentBindings(
                "mixed",
                "/xp-xsd10-document/mixed-order.xsd",
                "urn:mixed-document",
                "com.example.benchmark.mixed")) {
      runBenchmark(
          "xp-data-10-purchase-read-write-validate",
          () -> runPurchaseWorkload(purchaseOrderXml(LINE_COUNT), LINE_COUNT));
      runBenchmark(
          "xp-data-10-multins-read-write-validate",
          () -> runMultiNamespaceWorkload(multiNamespaceXml(LINE_COUNT), LINE_COUNT));
      runBenchmark(
          "xp-xsd10-document-wildcard-read-write-validate",
          () ->
              runGeneratedDocumentWorkload(
                  documentBindings, documentWildcardXml(DOCUMENT_FRAGMENT_COUNT)));
      runBenchmark(
          "xp-xsd10-document-mixed-read-write-validate",
          () ->
              runGeneratedDocumentWorkload(
                  mixedBindings, mixedDocumentXml(DOCUMENT_FRAGMENT_COUNT)));
    }
  }

  private static void runBenchmark(String workloadName, BenchmarkOperation operation) {
    BenchmarkObservation observation = null;
    for (int index = 0; index < WARMUP_ITERATIONS; index++) {
      observation = operation.run();
    }
    MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    long heapBefore = memory.getHeapMemoryUsage().getUsed();
    long startNanos = System.nanoTime();
    for (int index = 0; index < ITERATIONS; index++) {
      observation = operation.run();
    }
    long elapsedNanos = System.nanoTime() - startNanos;
    long heapAfter = memory.getHeapMemoryUsage().getUsed();
    if (observation == null) {
      throw new IllegalStateException("Benchmark operation did not run.");
    }
    double elapsedMillis = elapsedNanos / 1_000_000.0d;
    double opsPerSecond = ITERATIONS / (elapsedNanos / 1_000_000_000.0d);
    System.out.printf(
        Locale.ROOT,
        "BENCHMARK workload=%s iterations=%d inputChars=%d outputChars=%d "
            + "elapsedMillis=%.3f opsPerSecond=%.3f heapBeforeBytes=%d heapAfterBytes=%d%n",
        workloadName,
        ITERATIONS,
        observation.inputChars(),
        observation.outputChars(),
        elapsedMillis,
        opsPerSecond,
        heapBefore,
        heapAfter);
  }

  private static BenchmarkObservation runPurchaseWorkload(String xml, int expectedLineCount) {
    try {
      com.example.purchase.Order order =
          com.example.purchase.xml.OrderXmlReader.read(readerFor(xml));
      require(order.line().size() == expectedLineCount, "Purchase-order line count mismatch.");
      require(
          com.example.purchase.xml.OrderXmlValidator.validate(order).isValid(),
          "Purchase-order object validation failed.");
      String writtenXml = writePurchaseOrder(order);
      require(!writtenXml.isBlank(), "Purchase-order writer produced empty XML.");
      com.example.purchase.Order reparsed =
          com.example.purchase.xml.OrderXmlReader.read(readerFor(writtenXml));
      require(order.equals(reparsed), "Purchase-order round trip changed the model.");
      require(
          com.example.purchase.xml.OrderXmlValidator.validate(readerFor(writtenXml)).isValid(),
          "Purchase-order XML validation failed.");
      return new BenchmarkObservation(xml.length(), writtenXml.length());
    } catch (XMLStreamException | XmlReadException | XmlWriteException exception) {
      throw new IllegalStateException("Purchase-order benchmark failed.", exception);
    }
  }

  private static BenchmarkObservation runMultiNamespaceWorkload(String xml, int expectedLineCount) {
    try {
      com.example.orders.Order order = com.example.orders.xml.OrderXmlReader.read(readerFor(xml));
      require(order.line().size() == expectedLineCount, "Multi-namespace line count mismatch.");
      require(
          com.example.orders.xml.OrderXmlValidator.validate(order).isValid(),
          "Multi-namespace object validation failed.");
      String writtenXml = writeMultiNamespaceOrder(order);
      require(!writtenXml.isBlank(), "Multi-namespace writer produced empty XML.");
      com.example.orders.Order reparsed =
          com.example.orders.xml.OrderXmlReader.read(readerFor(writtenXml));
      require(order.equals(reparsed), "Multi-namespace round trip changed the model.");
      require(
          com.example.orders.xml.OrderXmlValidator.validate(readerFor(writtenXml)).isValid(),
          "Multi-namespace XML validation failed.");
      return new BenchmarkObservation(xml.length(), writtenXml.length());
    } catch (XMLStreamException | XmlReadException | XmlWriteException exception) {
      throw new IllegalStateException("Multi-namespace benchmark failed.", exception);
    }
  }

  private static BenchmarkObservation runGeneratedDocumentWorkload(
      GeneratedDocumentBindings bindings, String xml) {
    try {
      Object order =
          bindings.reader().getMethod("read", XmlEventReader.class).invoke(null, readerFor(xml));
      ValidationResult validation =
          (ValidationResult)
              bindings.validator().getMethod("validate", bindings.order()).invoke(null, order);
      require(validation.isValid(), "Document object validation failed.");
      String writtenXml = writeGeneratedOrder(bindings, order);
      require(!writtenXml.isBlank(), "Document writer produced empty XML.");
      Object reparsed =
          bindings
              .reader()
              .getMethod("read", XmlEventReader.class)
              .invoke(null, readerFor(writtenXml));
      require(order.equals(reparsed), "Document round trip changed the model.");
      ValidationResult xmlValidation =
          (ValidationResult)
              bindings
                  .validator()
                  .getMethod("validate", XmlEventReader.class)
                  .invoke(null, readerFor(writtenXml));
      require(xmlValidation.isValid(), "Document XML validation failed.");
      return new BenchmarkObservation(xml.length(), writtenXml.length());
    } catch (ReflectiveOperationException | XMLStreamException exception) {
      throw new IllegalStateException("Document benchmark failed.", exception);
    }
  }

  private static String writePurchaseOrder(com.example.purchase.Order order)
      throws XMLStreamException, XmlWriteException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    XMLStreamWriter streamWriter =
        XMLOutputFactory.newFactory().createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
    XmlOutput xmlOutput = JdkXmlAdapters.output(streamWriter);
    xmlOutput.startDocument();
    com.example.purchase.xml.OrderXmlWriter.write(xmlOutput, order);
    xmlOutput.endDocument();
    xmlOutput.flush();
    streamWriter.close();
    return output.toString(StandardCharsets.UTF_8);
  }

  private static String writeMultiNamespaceOrder(com.example.orders.Order order)
      throws XMLStreamException, XmlWriteException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    XMLStreamWriter streamWriter =
        XMLOutputFactory.newFactory().createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
    XmlOutput xmlOutput = JdkXmlAdapters.output(streamWriter);
    xmlOutput.startDocument();
    com.example.orders.xml.OrderXmlWriter.write(xmlOutput, order);
    xmlOutput.endDocument();
    xmlOutput.flush();
    streamWriter.close();
    return output.toString(StandardCharsets.UTF_8);
  }

  private static String writeGeneratedOrder(GeneratedDocumentBindings bindings, Object order)
      throws ReflectiveOperationException, XMLStreamException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    XMLStreamWriter streamWriter =
        XMLOutputFactory.newFactory().createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
    bindings
        .writer()
        .getMethod("write", XmlOutput.class, bindings.order())
        .invoke(null, JdkXmlAdapters.output(streamWriter), order);
    streamWriter.close();
    return output.toString(StandardCharsets.UTF_8);
  }

  private static XmlEventReader readerFor(String xml) throws XMLStreamException {
    XMLInputFactory factory = JdkXmlAdapters.secureInputFactory();
    XMLStreamReader streamReader = factory.createXMLStreamReader(new StringReader(xml));
    return JdkXmlAdapters.eventReader(streamReader);
  }

  private static String purchaseOrderXml(int lineCount) {
    StringBuilder xml = new StringBuilder();
    xml.append("<p:order xmlns:p=\"urn:purchase\" p:version=\"1.0\">");
    xml.append("<p:id>PO-BENCH</p:id><p:note>Benchmark order</p:note>");
    for (int index = 0; index < lineCount; index++) {
      xml.append("<p:line><p:sku>SKU-")
          .append(index)
          .append("</p:sku><p:quantity>")
          .append((index % 9) + 1)
          .append("</p:quantity></p:line>");
    }
    xml.append("</p:order>");
    return xml.toString();
  }

  private static String multiNamespaceXml(int lineCount) {
    StringBuilder xml = new StringBuilder();
    xml.append("<o:order xmlns:o=\"urn:orders\" xmlns:l=\"urn:lines\" o:version=\"1.0\">");
    xml.append("<o:id>ORD-BENCH</o:id><o:note>Benchmark order</o:note>");
    for (int index = 0; index < lineCount; index++) {
      xml.append("<l:line><l:sku>SKU-").append(index).append("</l:sku></l:line>");
    }
    xml.append("</o:order>");
    return xml.toString();
  }

  private static String documentWildcardXml(int fragmentCount) {
    StringBuilder xml = new StringBuilder();
    xml.append("<order xmlns=\"urn:document\" xmlns:ext=\"urn:extension\"><id>A-BENCH</id>");
    for (int index = 0; index < fragmentCount; index++) {
      xml.append("<ext:note code=\"N-")
          .append(index)
          .append("\">retained text ")
          .append(index)
          .append("<ext:child>value-")
          .append(index)
          .append("</ext:child></ext:note>");
    }
    xml.append("</order>");
    return xml.toString();
  }

  private static String mixedDocumentXml(int fragmentCount) {
    StringBuilder xml = new StringBuilder();
    xml.append("<order xmlns=\"urn:mixed-document\" xmlns:ext=\"urn:extension\">");
    xml.append("before benchmark <id>A-MIXED</id>");
    for (int index = 0; index < fragmentCount; index++) {
      xml.append(" text-")
          .append(index)
          .append(' ')
          .append("<ext:note priority=\"")
          .append(index % 3)
          .append("\">retained <ext:marker>ok-")
          .append(index)
          .append("</ext:marker></ext:note>");
    }
    xml.append(" before tail <tail>done</tail> after benchmark</order>");
    return xml.toString();
  }

  private static GeneratedDocumentBindings generateDocumentBindings(
      String name, String schemaResource, String namespace, String packageName)
      throws IOException, ReflectiveOperationException {
    Path output = WORK_DIRECTORY.resolve(name).resolve("generated");
    Path classes = WORK_DIRECTORY.resolve(name).resolve("classes");
    deleteDirectory(output);
    deleteDirectory(classes);
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(resourcePath(schemaResource)),
            output,
            GeneratorProfile.XP_XSD10_DOCUMENT,
            "com.example.generated",
            Map.of(namespace, packageName),
            List.of(),
            Map.of());
    MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    long heapBefore = memory.getHeapMemoryUsage().getUsed();
    long generationStartNanos = System.nanoTime();
    GeneratorResult result = new CoreGenerator().generate(request);
    long generationNanos = System.nanoTime() - generationStartNanos;
    require(result.successful(), "Generator failed: " + result.diagnostics());
    long sourceBytes = generatedSourceBytes(output, result.generatedSources());
    require(sourceBytes > 0, "Generated source benchmark produced empty sources.");
    long compileStartNanos = System.nanoTime();
    compileGeneratedSources(output, result.generatedSources(), classes);
    long compileNanos = System.nanoTime() - compileStartNanos;
    long classCount = classFileCount(classes);
    require(classCount > 0, "Generated source benchmark produced no classes.");
    long heapAfter = memory.getHeapMemoryUsage().getUsed();
    printGenerationBenchmark(
        name,
        request.profile(),
        result.generatedSources().size(),
        sourceBytes,
        classCount,
        generationNanos,
        compileNanos,
        heapBefore,
        heapAfter);
    URLClassLoader loader =
        new URLClassLoader(
            new URL[] {classes.toUri().toURL()},
            ConformanceBenchmarkSmokeMain.class.getClassLoader());
    return new GeneratedDocumentBindings(
        loader,
        loader.loadClass(packageName + ".Order"),
        loader.loadClass(packageName + ".xml.OrderXmlReader"),
        loader.loadClass(packageName + ".xml.OrderXmlWriter"),
        loader.loadClass(packageName + ".xml.OrderXmlValidator"));
  }

  private static void printGenerationBenchmark(
      String workloadName,
      GeneratorProfile profile,
      int generatedSources,
      long sourceBytes,
      long classCount,
      long generationNanos,
      long compileNanos,
      long heapBefore,
      long heapAfter) {
    System.out.printf(
        Locale.ROOT,
        "GENERATION_BENCHMARK workload=%s profile=%s schemas=1 "
            + "pipeline=resolve-parse-ir-bind-emit-write generatedSources=%d sourceBytes=%d "
            + "classFiles=%d generationMillis=%.3f javacMillis=%.3f "
            + "heapBeforeBytes=%d heapAfterBytes=%d%n",
        workloadName,
        profile.name(),
        generatedSources,
        sourceBytes,
        classCount,
        generationNanos / 1_000_000.0d,
        compileNanos / 1_000_000.0d,
        heapBefore,
        heapAfter);
  }

  private static void compileGeneratedSources(Path output, List<Path> relativePaths, Path classes)
      throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    require(compiler != null, "A JDK with JavaCompiler is required for benchmark smoke.");
    Files.createDirectories(classes);
    List<String> compilerArguments =
        Stream.concat(
                Stream.of(
                    "--release",
                    "21",
                    "-Xlint:all",
                    "-Werror",
                    "-classpath",
                    existingClasspath(),
                    "-d",
                    classes.toString()),
                relativePaths.stream().map(path -> output.resolve(path).toString()))
            .toList();
    ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
    int exitCode =
        compiler.run(
            null, compilerOutput, compilerOutput, compilerArguments.toArray(String[]::new));
    require(
        exitCode == 0,
        "Generated document benchmark source compilation failed: "
            + compilerOutput.toString(StandardCharsets.UTF_8));
  }

  private static Path resourcePath(String resourceName) {
    URL resource = ConformanceBenchmarkSmokeMain.class.getResource(resourceName);
    if (resource == null) {
      throw new IllegalArgumentException("Missing resource " + resourceName);
    }
    try {
      return Path.of(resource.toURI());
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException("Missing resource " + resourceName, exception);
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }

  private static String existingClasspath() {
    return Stream.of(System.getProperty("java.class.path").split(File.pathSeparator))
        .filter(path -> !path.isBlank())
        .filter(path -> Files.exists(Path.of(path)))
        .reduce((left, right) -> left + File.pathSeparator + right)
        .orElseThrow(() -> new IllegalStateException("No existing benchmark classpath entries."));
  }

  private static long generatedSourceBytes(Path output, List<Path> relativePaths)
      throws IOException {
    long total = 0;
    for (Path relativePath : relativePaths) {
      total += Files.size(output.resolve(relativePath));
    }
    return total;
  }

  private static long classFileCount(Path classes) throws IOException {
    try (Stream<Path> stream = Files.walk(classes)) {
      return stream.filter(path -> path.toString().endsWith(".class")).count();
    }
  }

  private static void deleteDirectory(Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return;
    }
    try (Stream<Path> stream = Files.walk(directory)) {
      List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
      for (Path path : paths) {
        Files.delete(path);
      }
    }
  }

  private interface BenchmarkOperation {
    BenchmarkObservation run();
  }

  private record BenchmarkObservation(int inputChars, int outputChars) {}

  private record GeneratedDocumentBindings(
      URLClassLoader loader, Class<?> order, Class<?> reader, Class<?> writer, Class<?> validator)
      implements AutoCloseable {
    @Override
    public void close() throws IOException {
      loader.close();
    }
  }
}
