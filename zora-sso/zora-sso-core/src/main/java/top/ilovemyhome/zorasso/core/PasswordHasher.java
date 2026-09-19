package top.ilovemyhome.zorasso.core;

public interface PasswordHasher {

    String hash(char[] secret);

    boolean verify(char[] secret, String encodedHash);
}
