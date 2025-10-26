package com.mkisten.vacancybackend.controller;

import com.mkisten.vacancybackend.client.AuthServiceClient;
import com.mkisten.vacancybackend.dto.AuthResponse;
import com.mkisten.vacancybackend.dto.SubscriptionStatusResponse;
import com.mkisten.vacancybackend.dto.UserProfileResponse;
import com.mkisten.vacancybackend.service.TokenManagerService;
import com.mkisten.vacancybackend.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
@Tag(name = "Session Management", description = "API для управления сессиями пользователя")
public class SessionController {

    private final TokenManagerService tokenManagerService;
    private final UserSettingsService userSettingsService;
    private final AuthServiceClient authServiceClient;

    @Operation(summary = "Инициализация сессии с сервисом авторизации")
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initSession(@RequestBody Map<String, Object> request) {
        try {
            String token = null;
            Long telegramId = null;

            // Извлекаем token и telegramId из запроса
            if (request.get("token") instanceof String) {
                token = (String) request.get("token");
            }

            if (request.get("telegramId") != null) {
                if (request.get("telegramId") instanceof Integer) {
                    telegramId = ((Integer) request.get("telegramId")).longValue();
                } else if (request.get("telegramId") instanceof Long) {
                    telegramId = (Long) request.get("telegramId");
                } else if (request.get("telegramId") instanceof String) {
                    telegramId = Long.parseLong((String) request.get("telegramId"));
                }
            }

            // Если токен не предоставлен, получаем его из сервиса авторизации
            if ((token == null || token.trim().isEmpty()) && telegramId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Either token or telegramId is required"
                ));
            }

            if (token == null || token.trim().isEmpty()) {
                // Получаем токен из сервиса авторизации
                AuthResponse authResponse = authServiceClient.getTokenByTelegramId(telegramId);
                token = authResponse.getToken();
            } else {
                // Если токен предоставлен, получаем telegramId из токена
                if (telegramId == null) {
                    telegramId = authServiceClient.getTelegramIdFromToken(token);
                }
            }

            // Валидируем токен
            if (!authServiceClient.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid token"
                ));
            }

            // Сохраняем токен
            tokenManagerService.saveUserToken(telegramId, token, LocalDateTime.now().plusHours(24));

            return ResponseEntity.ok(Map.of(
                    "message", "Session initialized successfully",
                    "telegramId", telegramId,
                    "tokenPreview", token.substring(0, Math.min(10, token.length())) + "..."
            ));

        } catch (Exception e) {
            log.error("Session initialization failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Session initialization failed",
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Получить информацию о текущей сессии")
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSessionInfo(@AuthenticationPrincipal Long telegramId) {
        try {
            UserProfileResponse profile = tokenManagerService.getUserProfile(telegramId);
            SubscriptionStatusResponse subscription = userSettingsService.getSubscriptionInfo(telegramId);

            return ResponseEntity.ok(Map.of(
                    "telegramId", telegramId,
                    "user", profile,
                    "subscription", subscription
            ));

        } catch (Exception e) {
            log.error("Failed to get session info for user {}: {}", telegramId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get session info",
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Выход из системы")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal Long telegramId) {
        try {
            tokenManagerService.removeUserToken(telegramId);

            return ResponseEntity.ok(Map.of(
                    "message", "Logged out successfully",
                    "telegramId", telegramId.toString()
            ));

        } catch (Exception e) {
            log.error("Logout failed for user {}: {}", telegramId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Logout failed",
                    "message", e.getMessage()
            ));
        }
    }
}