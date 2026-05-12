package com.Jjambbong.PayLens.login.client;

import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class GoogleClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    public String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        try {
            JsonNode response = restTemplate.postForObject(
                    tokenUri, request, JsonNode.class
            );

            if (response == null || !response.has("access_token")) {
                throw new GeneralException(ErrorCode.GOOGLE_AUTH_FAILED);
            }

            return response.get("access_token").asText();

        } catch (Exception e) {
            System.out.println(e.getMessage()); // 후에 삭제
            throw new GeneralException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    public JsonNode getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    userInfoUri,
                    HttpMethod.GET,
                    request,
                    JsonNode.class
            );

            JsonNode body = response.getBody();
            // 구글은 보통 'sub' 또는 'id' 로 고유 식별자를 반환합니다. 일반적으로 OAuth2에서는 'sub'를 사용합니다.
            if (body == null || (!body.has("id") && !body.has("sub"))) {
                throw new GeneralException(ErrorCode.GOOGLE_AUTH_FAILED);
            }

            return body;

        } catch (Exception e) {
            throw new GeneralException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }
}
