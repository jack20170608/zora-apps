package top.ilovemyhome.dagtask.agent.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local file storage for persisting the agent authentication token.
 * <p>
 * After the agent receives a token from the server via the callback push,
 * it is saved to a local file so it can be reused across agent restarts
 * without needing to re-register every time.
 * </p>
 */
public class LocalTokenStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalTokenStorage.class);

    private final Path tokenPath;

    public LocalTokenStorage(Path tokenPath) {
        this.tokenPath = tokenPath;
    }

    /**
     * Load the token from local storage if it exists.
     *
     * @return the token string if found and valid, empty otherwise
     */
    public String load() {
        if (!Files.exists(tokenPath)) {
            LOGGER.debug("No token file found at {}", tokenPath);
            return null;
        }
        try {
            String token = Files.readString(tokenPath, StandardCharsets.UTF_8).trim();
            if (token.isBlank()) {
                LOGGER.warn("Token file exists but is empty at {}", tokenPath);
                return null;
            }
            LOGGER.info("Loaded token from local storage at {}", tokenPath);
            return token;
        } catch (IOException e) {
            LOGGER.error("Failed to read token from {}", tokenPath, e);
            return null;
        }
    }

    /**
     * Save the token to local storage.
     *
     * @param token the token to save
     * @return true if saved successfully, false otherwise
     */
    public boolean save(String token) {
        if (token == null || token.isBlank()) {
            LOGGER.warn("Attempted to save empty token, skipping");
            return false;
        }
        try {
            // Create parent directories if needed
            Path parent = tokenPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(tokenPath, token, StandardCharsets.UTF_8);
            LOGGER.info("Token saved successfully to {}", tokenPath);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to write token to {}", tokenPath, e);
            return false;
        }
    }

    /**
     * Delete the token from local storage.
     *
     * @return true if deleted successfully, false otherwise
     */
    public boolean delete() {
        if (!Files.exists(tokenPath)) {
            return true;
        }
        try {
            Files.delete(tokenPath);
            LOGGER.info("Token deleted from {}", tokenPath);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to delete token from {}", tokenPath, e);
            return false;
        }
    }

    /**
     * Check if a token exists in local storage.
     *
     * @return true if token file exists and is not empty
     */
    public boolean hasToken() {
        if (!Files.exists(tokenPath)) {
            return false;
        }
        try {
            return !Files.readString(tokenPath, StandardCharsets.UTF_8).isBlank();
        } catch (IOException e) {
            return false;
        }
    }
}
