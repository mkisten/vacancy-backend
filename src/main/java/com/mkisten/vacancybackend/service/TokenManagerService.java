package com.mkisten.vacancybackend.service;

import com.mkisten.vacancybackend.client.AuthServiceClient;
import com.mkisten.vacancybackend.dto.AuthResponse;
import com.mkisten.vacancybackend.dto.SubscriptionStatusResponse;
import com.mkisten.vacancybackend.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenManagerService {

    private final AuthServiceClient authServiceClient;

    // In-memory хранилище токенов
    private final Map<Long, String> userTokens = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> tokenExpiry = new ConcurrentHashMap<>();

    /**
     * Получение профиля пользователя по токену (без сохранения в кэш)
     */
    public UserProfileResponse getUserProfileByToken(String token) {
        return authServiceClient.getUserProfile(token);
    }

    /**
     * Получение telegramId из токена
     */
    public Long getTelegramIdFromToken(String token) {
        try {
            UserProfileResponse profile = getUserProfileByToken(token);
            return profile.getTelegramId();
        } catch (Exception e) {
            log.error("Failed to get telegramId from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Сохранение токена пользователя
     */
    public void saveUserToken(Long telegramId, String token, LocalDateTime expiresAt) {
        userTokens.put(telegramId, token);
        tokenExpiry.put(telegramId, expiresAt);
        log.info("Token saved for user {}, expires at: {}", telegramId, expiresAt);
    }

    /**
     * Получение токена пользователя с автоматическим обновлением при необходимости
     */
    public String getValidToken(Long telegramId) {
        String token = userTokens.get(telegramId);

        if (token == null) {
            throw new RuntimeException("No token found for user: " + telegramId);
        }

        // Проверяем, нужно ли обновить токен
        if (isTokenExpiringSoon(telegramId)) {
            log.info("Token for user {} is expiring soon, refreshing...", telegramId);
            token = refreshToken(telegramId, token);
        }

        // Проверяем валидность токена
        if (!authServiceClient.validateToken(token)) {
            log.warn("Token for user {} is invalid, refreshing...", telegramId);
            token = refreshToken(telegramId, token);
        }

        return token;
    }

    /**
     * Обновление токена
     */
    private String refreshToken(Long telegramId, String oldToken) {
        try {
            AuthResponse authResponse = authServiceClient.refreshToken(oldToken);
            saveUserToken(telegramId, authResponse.getToken(), authResponse.getExpiresAt());
            log.info("Token refreshed for user {}", telegramId);
            return authResponse.getToken();
        } catch (Exception e) {
            log.error("Failed to refresh token for user {}: {}", telegramId, e.getMessage());
            // Удаляем невалидный токен
            userTokens.remove(telegramId);
            tokenExpiry.remove(telegramId);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * Проверка, что токен скоро истечет (менее 5 минут)
     */
    private boolean isTokenExpiringSoon(Long telegramId) {
        LocalDateTime expiry = tokenExpiry.get(telegramId);
        if (expiry == null) return true;

        return LocalDateTime.now().plusMinutes(5).isAfter(expiry);
    }

    /**
     * Получение профиля пользователя
     */
    public UserProfileResponse getUserProfile(Long telegramId) {
        String token = getValidToken(telegramId);
        return authServiceClient.getUserProfile(token);
    }

    /**
     * Получение статуса подписки
     */
    public SubscriptionStatusResponse getSubscriptionStatus(Long telegramId) {
        String token = getValidToken(telegramId);
        return authServiceClient.getSubscriptionStatus(token);
    }

    /**
     * Отправка уведомления через Telegram
     */
    public void sendTelegramNotification(Long telegramId, String message) {
        String token = getValidToken(telegramId);
        authServiceClient.sendTelegramNotification(token, message);
    }

    /**
     * Удаление токена пользователя (при logout)
     */
    public void removeUserToken(Long telegramId) {
        userTokens.remove(telegramId);
        tokenExpiry.remove(telegramId);
        log.info("Token removed for user {}", telegramId);
    }

    /**
     * Периодическая очистка просроченных токенов
     */
    @Scheduled(fixedRate = 300000) // Каждые 5 минут
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int removedCount = 0;

        for (Map.Entry<Long, LocalDateTime> entry : tokenExpiry.entrySet()) {
            if (entry.getValue().isBefore(now)) {
                userTokens.remove(entry.getKey());
                tokenExpiry.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} expired tokens", removedCount);
        }
    }
}