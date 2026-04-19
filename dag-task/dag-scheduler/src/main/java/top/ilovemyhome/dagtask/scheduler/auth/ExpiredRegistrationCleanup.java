package top.ilovemyhome.dagtask.scheduler.auth;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Cleans up expired pending registration requests.
 * <p>
 * Pending registrations expire after 24 hours to prevent dangling
 * unapproved requests from accumulating in the database.
 * This should be scheduled to run periodically (e.g., daily).
 * </p>
 */
public class ExpiredRegistrationCleanup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredRegistrationCleanup.class);

    private final RegistrationService registrationService;

    @Inject
    public ExpiredRegistrationCleanup(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Run the cleanup - delete all expired pending registrations.
     */
    public void cleanup() {
        LOGGER.info("Starting expired pending registration cleanup...");
        int deleted = registrationService.cleanupExpired(Instant.now());
        LOGGER.info("Completed cleanup: deleted {} expired pending registrations", deleted);
    }
}
