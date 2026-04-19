package top.ilovemyhome.dagtask.scheduler.auth;

import top.ilovemyhome.dagtask.si.auth.TokenPushRequest;

public interface TokenPusher {
    boolean pushToken(String callbackUrl, String nonce, TokenPushRequest request);
}
