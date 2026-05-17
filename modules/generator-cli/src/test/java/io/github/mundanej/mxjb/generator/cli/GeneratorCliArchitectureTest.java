package io.github.mundanej.mxjb.generator.cli;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mxjb.generator.cli",
    importOptions = DoNotIncludeTests.class)
final class GeneratorCliArchitectureTest {
  @ArchTest
  static final ArchRule project_specific_cli_does_not_depend_on_runtime_or_gradle_entrypoints =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mxjb.runtime..",
              "io.github.mundanej.mxjb.generator.gradle..",
              "io.github.mundanej.mxjb.examples..");

  @ArchTest
  static final ArchRule native_image_cli_avoids_dynamic_runtime_mechanisms =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("java.lang.reflect..", "java.lang.invoke..", "org.reflections..")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.util.ServiceLoader")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.ClassLoader");

  @ArchTest
  static final ArchRule native_image_cli_avoids_java_serialization =
      noClasses()
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.io.ObjectInputStream")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.io.ObjectOutputStream")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.io.Externalizable");

  @ArchTest
  static final ArchRule baseline_cli_allows_system_exit_only_in_entrypoint_class =
      noClasses()
          .that()
          .doNotHaveFullyQualifiedName("io.github.mundanej.mxjb.generator.cli.MxjbCli")
          .should()
          .callMethod(System.class, "exit", int.class);

  @ArchTest
  static final ArchRule baseline_cli_does_not_force_gc_or_spawn_processes =
      noClasses()
          .should()
          .callMethod(System.class, "gc")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.Runtime")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.ProcessBuilder");

  @ArchTest
  static final ArchRule baseline_cli_avoids_internal_jdk_apis =
      noClasses().should().dependOnClassesThat().resideInAnyPackage("sun..", "jdk.internal..");

  @ArchTest
  static final ArchRule baseline_cli_has_no_finalizers = noMethods().should().haveName("finalize");

  @ArchTest
  static final ArchRule baseline_cli_has_no_public_static_mutable_fields =
      fields().that().arePublic().and().areStatic().should().beFinal().allowEmptyShould(true);

  private GeneratorCliArchitectureTest() {}
}
