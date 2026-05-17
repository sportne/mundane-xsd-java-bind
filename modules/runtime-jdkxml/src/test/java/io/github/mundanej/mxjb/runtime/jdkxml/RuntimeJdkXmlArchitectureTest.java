package io.github.mundanej.mxjb.runtime.jdkxml;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mxjb.runtime.jdkxml",
    importOptions = DoNotIncludeTests.class)
final class RuntimeJdkXmlArchitectureTest {
  @ArchTest
  static final ArchRule
      project_specific_runtime_jdkxml_has_no_generator_or_entrypoint_dependencies =
          noClasses()
              .should()
              .dependOnClassesThat()
              .resideInAnyPackage(
                  "io.github.mundanej.mxjb.generator..",
                  "io.github.mundanej.mxjb.examples..",
                  "io.github.mundanej.mxjb.conformance..",
                  "io.github.mundanej.mxjb.generator.cli..",
                  "io.github.mundanej.mxjb.generator.gradle..",
                  "io.github.mundanej.mxjb.gradle..",
                  "io.github.mundanej.mxjb.testing..");

  @ArchTest
  static final ArchRule native_image_runtime_jdkxml_avoids_dynamic_runtime_mechanisms =
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
  static final ArchRule native_image_runtime_jdkxml_avoids_java_serialization =
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
  static final ArchRule native_image_runtime_jdkxml_avoids_internal_jdk_and_security_manager_apis =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("sun..", "jdk.internal..")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.security.AccessController")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.SecurityManager");

  @ArchTest
  static final ArchRule baseline_runtime_jdkxml_does_not_terminate_or_spawn_processes =
      noClasses()
          .should()
          .callMethod(System.class, "exit", int.class)
          .orShould()
          .callMethod(System.class, "gc")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.Runtime")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.ProcessBuilder");

  @ArchTest
  static final ArchRule baseline_runtime_jdkxml_has_no_finalizers =
      noMethods().should().haveName("finalize");

  @ArchTest
  static final ArchRule baseline_runtime_jdkxml_has_no_public_static_mutable_fields =
      fields().that().arePublic().and().areStatic().should().beFinal().allowEmptyShould(true);

  private RuntimeJdkXmlArchitectureTest() {}
}
