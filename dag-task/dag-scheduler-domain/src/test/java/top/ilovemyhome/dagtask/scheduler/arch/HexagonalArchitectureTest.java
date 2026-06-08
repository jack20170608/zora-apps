package top.ilovemyhome.dagtask.scheduler.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hexagonal architecture guard rails for dag-scheduler-domain.
 */
@AnalyzeClasses(packages = "top.ilovemyhome.dagtask.scheduler")
class HexagonalArchitectureTest {

    @ArchTest
    void domain_application_and_ports_must_not_depend_on_any_framework(JavaClasses classes) {
        noClasses().that().resideInAnyPackage(
                "..scheduler.domain..",
                "..scheduler.application..",
                "..scheduler.port..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "io.muserver..",
                "org.jdbi..",
                "org.springframework..",
                "io.micronaut..",
                "jakarta..",
                "javax.servlet..",
                "javax.sql..",
                "java.sql..",
                "com.fasterxml.jackson..",
                "org.flywaydb..",
                "com.zaxxer.hikari..")
            .because("dag-scheduler-domain must remain zero-infrastructure (spec section 1)")
            .check(classes);
    }

    @ArchTest
    void application_layer_must_not_depend_on_inbound_ports(JavaClasses classes) {
        // application services IMPLEMENT inbound ports — but must not USE other inbound ports.
        // Cross-use case orchestration happens via outbound ports only.
        noClasses().that().resideInAPackage("..scheduler.application..")
            .should().accessClassesThat().resideInAPackage("..scheduler.port.in..")
            .because("application services may implement, but not consume, inbound ports")
            .check(classes);
    }

    @ArchTest
    void domain_layer_must_not_depend_on_ports(JavaClasses classes) {
        noClasses().that().resideInAPackage("..scheduler.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..scheduler.port..",
                "..scheduler.application..")
            .because("pure domain has no knowledge of orchestration or ports (spec section 1)")
            .check(classes);
    }
}
