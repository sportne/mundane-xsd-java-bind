package io.github.mundanej.mxjb.runtime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mxjb.runtime",
    importOptions = DoNotIncludeTests.class)
final class RuntimeArchitectureTest {
  @ArchTest
  static final ArchRule runtime_core_does_not_depend_on_generator_or_entrypoint_packages =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mxjb.generator..",
              "io.github.mundanej.mxjb.examples..",
              "io.github.mundanej.mxjb.gradle..");

  @ArchTest
  static final ArchRule runtime_core_main_code_has_no_third_party_dependencies =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideOutsideOfPackages("java..", "io.github.mundanej.mxjb.runtime..");

  @ArchTest
  static final ArchRule runtime_core_avoids_dynamic_runtime_mechanisms =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("java.lang.reflect..")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.util.ServiceLoader")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.ClassLoader");

  private RuntimeArchitectureTest() {}
}
