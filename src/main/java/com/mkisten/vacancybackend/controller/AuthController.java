package com.mkisten.vacancybackend.controller;

import com.mkisten.vacancybackend.config.JwtTokenProvider;
import com.mkisten.vacancybackend.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API для аутентификации")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserSettingsService settingsService;

    @Operation(summary = "Аутентификация по Telegram ID")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> request) {
        try {
            Object telegramIdObj = request.get("telegramId");
            if (telegramIdObj == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "telegramId is required"
                ));
            }

            Long telegramId;
            if (telegramIdObj instanceof Integer) {
                telegramId = ((Integer) telegramIdObj).longValue();
            } else if (telegramIdObj instanceof Long) {
                telegramId = (Long) telegramIdObj;
            } else if (telegramIdObj instanceof String) {
                telegramId = Long.parseLong((String) telegramIdObj);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "telegramId must be a number"
                ));
            }

            // Создаем или получаем настройки пользователя
            settingsService.getSettings(telegramId);

            // Генерируем JWT токен
            String token = jwtTokenProvider.generateToken(telegramId);

            log.info("User {} authenticated successfully", telegramId);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "telegramId", telegramId,
                    "type", "Bearer",
                    "expiresIn", jwtTokenProvider.getJwtExpiration() / 1000 // в секундах
            ));

        } catch (NumberFormatException e) {
            log.error("Invalid telegramId format: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "telegramId must be a valid number"
            ));
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Authentication failed",
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Проверка токена")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "valid", false,
                        "error", "Authorization header is required. Format: Bearer <token>"
                ));
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtTokenProvider.validateToken(token);

            if (isValid) {
                Long telegramId = jwtTokenProvider.getTelegramIdFromToken(token);
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "telegramId", telegramId
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "valid", false,
                        "error", "Token is invalid or expired"
                ));
            }

        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Обновление токена")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Authorization header is required. Format: Bearer <token>"
                ));
            }

            String oldToken = authHeader.substring(7);

            if (!jwtTokenProvider.validateToken(oldToken)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid or expired token"
                ));
            }

            Long telegramId = jwtTokenProvider.getTelegramIdFromToken(oldToken);
            String newToken = jwtTokenProvider.generateToken(telegramId);

            return ResponseEntity.ok(Map.of(
                    "token", newToken,
                    "telegramId", telegramId,
                    "type", "Bearer",
                    "expiresIn", jwtTokenProvider.getJwtExpiration() / 1000
            ));

        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to refresh token",
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Получить информацию о токене")
    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> getTokenInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Authorization header is required. Format: Bearer <token>"
                ));
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtTokenProvider.validateToken(token);

            if (!isValid) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid token"
                ));
            }

            Long telegramId = jwtTokenProvider.getTelegramIdFromToken(token);

            return ResponseEntity.ok(Map.of(
                    "telegramId", telegramId,
                    "valid", true,
                    "tokenType", "JWT",
                    "algorithm", "HS256"
            ));

        } catch (Exception e) {
            log.error("Token info error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get token info",
                    "message", e.getMessage()
            ));
        }
    }
}