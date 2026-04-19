package top.ilovemyhome.dagtask.scheduler.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.ilovemyhome.dagtask.si.auth.TokenPushRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DefaultTokenPusher implements TokenPusher {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DefaultTokenPusher() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean pushToken(String callbackUrl, String nonce, TokenPushRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(callbackUrl))
                .header("Content-Type", "application/json")
                .header("X-Registration-Nonce", nonce)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }
}
