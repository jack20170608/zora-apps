package top.ilovemyhome.zorasso.core;

import java.io.Console;
import java.util.Arrays;

public final class PasswordHashCli {

    private PasswordHashCli() {
    }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("An interactive console is required");
        }
        char[] secret = console.readPassword("Secret: ");
        char[] confirmation = console.readPassword("Confirm secret: ");
        try {
            if (!Arrays.equals(secret, confirmation)) {
                throw new IllegalArgumentException("Secrets do not match");
            }
            console.writer().println(new Pbkdf2PasswordHasher().hash(secret));
        } finally {
            Arrays.fill(secret, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}
