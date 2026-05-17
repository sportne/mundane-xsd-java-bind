package io.github.mundanej.mxjb.runtime.jdkxml;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mxjb.runtime.jdkxml",
    importOptions = DoNotIncludeTests.class)
final class RuntimeJdkXmlArchitectureTest {
  @ArchTest
  static final ArchRule runtime_jdkxml_main_code_does_not_depend_on_generator_or_entrypoints =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mxjb.generator..",
              "io.github.mundanej.mxjb.examples..",
              "io.github.mundanej.mxjb.gradle..");

  @ArchTest
  static final ArchRule runtime_jdkxml_avoids_dynamic_runtime_mechanisms =
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

  private RuntimeJdkXmlArchitectureTest() {}
}
