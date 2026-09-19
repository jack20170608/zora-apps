package top.ilovemyhome.zorasso.si;

public interface IdentityAuthenticator {

    AuthenticatedIdentity authenticate(String username, char[] password);
}
