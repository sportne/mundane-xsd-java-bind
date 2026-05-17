package io.github.mundanej.mxjb.generator.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mxjb.generator.core",
    importOptions = DoNotIncludeTests.class)
final class GeneratorCoreArchitectureTest {
  @ArchTest
  static final ArchRule generator_core_does_not_depend_on_runtime_or_entrypoint_modules =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mxjb.runtime..",
              "io.github.mundanej.mxjb.generator.cli..",
              "io.github.mundanej.mxjb.generator.gradle..",
              "io.github.mundanej.mxjb.examples..");

  @ArchTest
  static final ArchRule resolver_code_avoids_dynamic_runtime_mechanisms =
      noClasses()
          .that()
          .resideInAnyPackage(
              "..bind..", "..emit..", "..resolver..", "..schema..", "..diagnostics..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("java.lang.reflect..")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.util.ServiceLoader")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.ClassLoader");

  private GeneratorCoreArchitectureTest() {}
}
