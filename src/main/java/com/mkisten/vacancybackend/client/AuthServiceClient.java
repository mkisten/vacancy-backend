package com.mkisten.vacancybackend.client;

import com.mkisten.vacancybackend.dto.AuthResponse;
import com.mkisten.vacancybackend.dto.SubscriptionStatusResponse;
import com.mkisten.vacancybackend.dto.TokenResponse;
import com.mkisten.vacancybackend.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.auth-service.base-url}")
    private String authServiceBaseUrl;

    /**
     * Получение профиля пользователя по токену
     */
    public UserProfileResponse getUserProfile(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<UserProfileResponse> response = restTemplate.exchange(
                    authServiceBaseUrl + "/api/user/profile",
                    HttpMethod.GET,
                    entity,
                    UserProfileResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get user profile from auth service: {}", e.getMessage());
            throw new RuntimeException("Auth service unavailable", e);
        }
    }

    /**
     * Получение токена по Telegram ID
     */
    public AuthResponse getTokenByTelegramId(Long telegramId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    authServiceBaseUrl + "/api/auth/token?telegramId=" + telegramId,
                    HttpMethod.POST,
                    entity,
                    TokenResponse.class
            );

            AuthResponse authResponse = new AuthResponse();
            authResponse.setToken(response.getBody().token());
            authResponse.setTelegramId(telegramId);
            authResponse.setExpiresAt(LocalDateTime.now().plusHours(24)); // Пример

            return authResponse;

        } catch (Exception e) {
            log.error("Failed to get token for telegramId {}: {}", telegramId, e.getMessage());
            throw new RuntimeException("Token creation failed", e);
        }
    }

    /**
     * Получение Telegram ID из токена
     */
    public Long getTelegramIdFromToken(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    authServiceBaseUrl + "/api/auth/me",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().get("telegramId") != null) {
                // В зависимости от формата ответа
                Object telegramIdObj = response.getBody().get("telegramId");
                if (telegramIdObj instanceof Integer) {
                    return ((Integer) telegramIdObj).longValue();
                } else if (telegramIdObj instanceof Long) {
                    return (Long) telegramIdObj;
                } else if (telegramIdObj instanceof String) {
                    return Long.parseLong((String) telegramIdObj);
                }
            }

            throw new RuntimeException("Telegram ID not found in response");

        } catch (Exception e) {
            log.error("Failed to get telegramId from token: {}", e.getMessage());
            throw new RuntimeException("Cannot extract telegramId from token", e);
        }
    }

    /**
     * Проверка статуса подписки
     */
    public SubscriptionStatusResponse getSubscriptionStatus(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<SubscriptionStatusResponse> response = restTemplate.exchange(
                    authServiceBaseUrl + "/api/subscription/status",
                    HttpMethod.GET,
                    entity,
                    SubscriptionStatusResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get subscription status from auth service: {}", e.getMessage());
            throw new RuntimeException("Auth service unavailable", e);
        }
    }

    /**
     * Обновление токена
     */
    public AuthResponse refreshToken(String oldToken) {
        try {
            HttpHeaders headers = createAuthHeaders(oldToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    authServiceBaseUrl + "/api/auth/refresh",
                    HttpMethod.POST,
                    entity,
                    AuthResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * Отправка уведомления через Telegram бот
     */
    public void sendTelegramNotification(String token, String message) {
        try {
            HttpHeaders headers = createAuthHeaders(token);

            Map<String, String> request = new HashMap<>();
            request.put("message", message);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            restTemplate.exchange(
                    authServiceBaseUrl + "/api/bot/notify",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            log.debug("Telegram notification sent via auth service");
        } catch (Exception e) {
            log.error("Failed to send Telegram notification: {}", e.getMessage());
            throw new RuntimeException("Telegram notification failed", e);
        }
    }

    /**
     * Проверка валидности токена
     */
    public boolean validateToken(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    authServiceBaseUrl + "/api/auth/validate",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }
}