package io.github.mundanej.mxjb.generator.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MxjbGradlePluginFunctionalTest {
  @TempDir private Path projectDirectory;

  @Test
  void pluginRegistersTaskAndWiresGeneratedSourcesIntoJavaCompilation() throws IOException {
    writeSettings();
    writeBuildWithRuntimeDependency(
        """
        plugins {
            id 'java'
            id 'io.github.mundanej.mxjb'
        }

        RUNTIME_DEPENDENCY_BLOCK

        mxjb {
            schema('src/main/resources/schema/purchase-order.xsd')
            namespacePackage('urn:purchase', 'com.example.purchase')
        }
        """);
    copyRepoFile(
        "examples/purchase-order/src/main/resources/schema/purchase-order.xsd",
        "src/main/resources/schema/purchase-order.xsd");

    BuildResult tasks = run("tasks", "--all");
    BuildResult compile = run("compileJava");

    assertTrue(tasks.getOutput().contains("generateMxjbSources"));
    assertEquals(TaskOutcome.SUCCESS, compile.task(":generateMxjbSources").getOutcome());
    assertTrue(
        Files.exists(
            projectDirectory.resolve(
                "build/generated/sources/mxjb/java/com/example/purchase/Order.java")));
  }

  @Test
  void generatesDeterministicPurchaseOrderSourcesAndLeavesSecondRunUpToDate() throws IOException {
    writeSettings();
    writeBuildWithRuntimeDependency(
        """
        plugins {
            id 'java'
            id 'io.github.mundanej.mxjb'
        }

        RUNTIME_DEPENDENCY_BLOCK

        mxjb {
            schema('src/main/resources/schema/purchase-order.xsd')
            localRoot('src/main/resources/schema')
            namespacePackage('urn:purchase', 'com.example.purchase')
        }
        """);
    copyRepoFile(
        "examples/purchase-order/src/main/resources/schema/purchase-order.xsd",
        "src/main/resources/schema/purchase-order.xsd");

    BuildResult first = run("generateMxjbSources");
    String firstSource = generatedSource("com/example/purchase/Order.java");
    BuildResult second = run("generateMxjbSources");
    String secondSource = generatedSource("com/example/purchase/Order.java");

    assertEquals(TaskOutcome.SUCCESS, first.task(":generateMxjbSources").getOutcome());
    assertEquals(firstSource, secondSource);
    assertEquals(TaskOutcome.UP_TO_DATE, second.task(":generateMxjbSources").getOutcome());
  }

  @Test
  void acceptsFullXsd10ProfileToken() throws IOException {
    writeSettings();
    writeBuildWithRuntimeDependency(
        """
        plugins {
            id 'java'
            id 'io.github.mundanej.mxjb'
        }

        RUNTIME_DEPENDENCY_BLOCK

        mxjb {
            profile = 'XP-XSD10-FULL'
            schema('src/main/resources/schema/purchase-order.xsd')
            namespacePackage('urn:purchase', 'com.example.purchase')
        }
        """);
    copyRepoFile(
        "examples/purchase-order/src/main/resources/schema/purchase-order.xsd",
        "src/main/resources/schema/purchase-order.xsd");

    BuildResult result = run("compileJava");

    assertEquals(TaskOutcome.SUCCESS, result.task(":generateMxjbSources").getOutcome());
    assertTrue(
        Files.exists(
            projectDirectory.resolve(
                "build/generated/sources/mxjb/java/com/example/purchase/Order.java")));
  }

  @Test
  void generatesMultiNamespaceSourcesThroughCatalogAndLocalRoot() throws IOException {
    writeSettings();
    writeBuildWithRuntimeDependency(
        """
        plugins {
            id 'java'
            id 'io.github.mundanej.mxjb'
        }

        RUNTIME_DEPENDENCY_BLOCK

        mxjb {
            schema('src/main/resources/schema/order.xsd')
            localRoot('src/main/resources/schema')
            catalog('https://example.invalid/line.xsd', 'src/main/resources/schema/line.xsd')
            namespacePackage('urn:orders', 'com.example.orders')
            namespacePackage('urn:lines', 'com.example.lines')
        }
        """);
    copyRepoFile(
        "examples/multi-namespace/src/main/resources/schema/order.xsd",
        "src/main/resources/schema/order.xsd");
    copyRepoFile(
        "examples/multi-namespace/src/main/resources/schema/line.xsd",
        "src/main/resources/schema/line.xsd");

    BuildResult result = run("compileJava");

    assertEquals(TaskOutcome.SUCCESS, result.task(":generateMxjbSources").getOutcome());
    assertTrue(generatedSource("com/example/orders/Order.java").contains("com.example.lines.Line"));
    assertTrue(Files.exists(generatedPath("com/example/lines/Line.java")));
  }

  @Test
  void deniedNetworkDiagnosticFailsWithoutWritingOutput() throws IOException {
    writeSettings();
    writeBuildWithRuntimeDependency(
        """
        plugins {
            id 'java'
            id 'io.github.mundanej.mxjb'
        }

        RUNTIME_DEPENDENCY_BLOCK

        mxjb {
            schema('src/main/resources/schema/order.xsd')
        }
        """);
    writeSchema(
        "src/main/resources/schema/order.xsd", orderSchema("https://example.invalid/line.xsd"));

    BuildResult result = fail("generateMxjbSources");

    assertTrue(result.getOutput().contains("SCHEMA_RESOURCE_NETWORK_DENIED"));
    assertFalse(Files.exists(generatedPath("com/example/orders/Order.java")));
  }

  @Test
  void invalidProfileFailsWithStableGradleDiagnostic() throws IOException {
    writeSettings();
    writeBuild(
        """
        plugins {
            id 'io.github.mundanej.mxjb'
        }

        mxjb {
            profile = 'XP-DATA-11'
            schema('src/main/resources/schema/purchase-order.xsd')
        }
        """);
    copyRepoFile(
        "examples/purchase-order/src/main/resources/schema/purchase-order.xsd",
        "src/main/resources/schema/purchase-order.xsd");

    BuildResult result = fail("generateMxjbSources");

    assertTrue(result.getOutput().contains("GENERATOR_GRADLE_INVALID_ARGUMENT"));
    assertTrue(result.getOutput().contains("Unsupported generator profile XP-DATA-11"));
  }

  @Test
  void missingSchemaAndInvalidPackageDiagnosticsFailWithoutPartialOutput() throws IOException {
    writeSettings();
    writeBuild(
        """
        plugins {
            id 'io.github.mundanej.mxjb'
        }

        mxjb {
            defaultPackage = 'not-valid!'
        }
        """);

    BuildResult missing = fail("generateMxjbSources");
    assertTrue(missing.getOutput().contains("GENERATOR_REQUEST_INVALID"));

    writeBuild(
        """
        plugins {
            id 'io.github.mundanej.mxjb'
        }

        mxjb {
            defaultPackage = 'not-valid!'
            schema('src/main/resources/schema/purchase-order.xsd')
        }
        """);
    copyRepoFile(
        "examples/purchase-order/src/main/resources/schema/purchase-order.xsd",
        "src/main/resources/schema/purchase-order.xsd");
    BuildResult invalidPackage = fail("generateMxjbSources");

    assertTrue(invalidPackage.getOutput().contains("SCHEMA_BINDING_INVALID_CONFIGURATION"));
    assertFalse(Files.exists(generatedPath("com/example/purchase/Order.java")));
  }

  @Test
  void supportsConfigurationCacheReuse() throws IOException {
    writeSettings();
    writeBuildWithRuntimeDependency(
        """
        plugins {
            id 'java'
            id 'io.github.mundanej.mxjb'
        }

        RUNTIME_DEPENDENCY_BLOCK

        mxjb {
            schema('src/main/resources/schema/purchase-order.xsd')
            namespacePackage('urn:purchase', 'com.example.purchase')
        }
        """);
    writeSchema("src/main/resources/schema/purchase-order.xsd", purchaseOrderSchema());

    BuildResult first = run("--configuration-cache", "compileJava");
    BuildResult second = run("--configuration-cache", "compileJava");

    assertTrue(first.getOutput().contains("Configuration cache entry stored."));
    assertTrue(second.getOutput().contains("Reusing configuration cache."));
  }

  private BuildResult run(String... arguments) {
    return runner(arguments).build();
  }

  private BuildResult fail(String... arguments) {
    return runner(arguments).buildAndFail();
  }

  private GradleRunner runner(String... arguments) {
    return GradleRunner.create()
        .withProjectDir(projectDirectory.toFile())
        .withArguments(arguments)
        .withPluginClasspath();
  }

  private void writeSettings() throws IOException {
    Files.writeString(projectDirectory.resolve("settings.gradle"), "", StandardCharsets.UTF_8);
  }

  private void writeBuild(String buildScript) throws IOException {
    Files.writeString(
        projectDirectory.resolve("build.gradle"), buildScript, StandardCharsets.UTF_8);
  }

  private void writeBuildWithRuntimeDependency(String buildScript) throws IOException {
    writeBuild(buildScript.replace("RUNTIME_DEPENDENCY_BLOCK", runtimeDependencyBlock()));
  }

  private void writeSchema(String relativePath, String content) throws IOException {
    Path target = projectDirectory.resolve(relativePath);
    Path parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(target, content, StandardCharsets.UTF_8);
  }

  private void copyRepoFile(String sourceRelativePath, String targetRelativePath)
      throws IOException {
    Path source = Path.of(System.getProperty("mxjb.repoRoot")).resolve(sourceRelativePath);
    Path target = projectDirectory.resolve(targetRelativePath);
    Path parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.copy(source, target);
  }

  private String generatedSource(String relativePath) throws IOException {
    return Files.readString(generatedPath(relativePath), StandardCharsets.UTF_8);
  }

  private Path generatedPath(String relativePath) {
    return projectDirectory.resolve("build/generated/sources/mxjb/java").resolve(relativePath);
  }

  private String runtimeDependencyBlock() {
    return "dependencies { implementation files(" + runtimeCoreClasspath() + ") }";
  }

  private String runtimeCoreClasspath() {
    String classpath = System.getProperty("mxjb.runtimeCoreClasses");
    java.util.List<String> entries =
        java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(java.io.File.pathSeparator))
            .splitAsStream(classpath)
            .toList();
    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < entries.size(); index++) {
      if (index > 0) {
        builder.append(", ");
      }
      builder.append("'").append(entries.get(index).replace("\\", "\\\\")).append("'");
    }
    return builder.toString();
  }

  private String purchaseOrderSchema() {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:purchase"
            xmlns:p="urn:purchase"
            elementFormDefault="qualified"
            attributeFormDefault="qualified">
          <xs:element name="order" type="p:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:element name="line" type="p:Line" maxOccurs="unbounded"/>
            </xs:sequence>
          </xs:complexType>
          <xs:complexType name="Line">
            <xs:sequence>
              <xs:element name="sku" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """;
  }

  private String orderSchema(String lineLocation) {
    return """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
            targetNamespace="urn:orders"
            xmlns:o="urn:orders"
            xmlns:l="urn:lines"
            elementFormDefault="qualified">
          <xs:import namespace="urn:lines" schemaLocation="LINE_SCHEMA_LOCATION"/>
          <xs:element name="order" type="o:Order"/>
          <xs:complexType name="Order">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
              <xs:element ref="l:line" maxOccurs="unbounded"/>
            </xs:sequence>
          </xs:complexType>
        </xs:schema>
        """
        .replace("LINE_SCHEMA_LOCATION", lineLocation);
  }
}
