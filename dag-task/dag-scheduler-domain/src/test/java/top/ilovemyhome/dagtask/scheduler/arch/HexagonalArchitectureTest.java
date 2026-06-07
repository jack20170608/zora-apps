package top.ilovemyhome.dagtask.scheduler.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hexagonal architecture guard rails for dag-scheduler-domain.
 * <p>
 * Currently {@link Disabled @Disabled} because the module is empty (skeleton step 1).
 * Enable in step 2 after real domain / port code is moved in from the legacy
 * dag-scheduler module. The rules are written now so the contract is visible
 * and the enabling change in step 2 is a single annotation removal.
 */
@Disabled("Enable in step 2 after domain code lands; rules pre-written for visibility.")
class HexagonalArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("top.ilovemyhome.dagtask.scheduler");

    @Test
    void domain_application_and_ports_must_not_depend_on_any_framework() {
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
                "com.zaxxer.hikari..",
                // zora wrappers are infrastructure too — adapters may use them, domain may not
                "top.ilovemyhome.zora.jdbi..",
                "top.ilovemyhome.zora.rdb..",
                "top.ilovemyhome.zora.muserver..",
                "top.ilovemyhome.zora.json..",
                "top.ilovemyhome.zora.httpclient..",
                "top.ilovemyhome.zora.config..",
                "top.ilovemyhome.zora.static..")
            .because("dag-scheduler-domain must remain zero-infrastructure (spec section 1)")
            .check(CLASSES);
    }

    @Test
    void application_layer_must_not_depend_on_inbound_ports() {
        // application services IMPLEMENT inbound ports — but must not USE other inbound ports.
        // Cross-use case orchestration happens via outbound ports only.
        noClasses().that().resideInAPackage("..scheduler.application..")
            .should().dependOnClassesThat().resideInAPackage("..scheduler.port.in..")
            .because("application services may implement, but not consume, inbound ports")
            .check(CLASSES);
    }

    @Test
    void domain_layer_must_not_depend_on_ports() {
        noClasses().that().resideInAPackage("..scheduler.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..scheduler.port..",
                "..scheduler.application..")
            .because("pure domain has no knowledge of orchestration or ports (spec section 1)")
            .check(CLASSES);
    }
}
