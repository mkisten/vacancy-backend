package com.mkisten.vacancybackend.controller;

import com.mkisten.vacancybackend.client.AuthServiceClient;
import com.mkisten.vacancybackend.dto.AuthRequest;
import com.mkisten.vacancybackend.dto.TokenResponse;
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
@Tag(name = "Authentication", description = "API авторизации (проксируется в сервис авторизации)")
public class AuthController {

    private final AuthServiceClient authServiceClient;

    @Operation(summary = "Получить токен по Telegram ID (через сервис авторизации)")
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> getToken(@RequestBody AuthRequest request) {
        TokenResponse response = authServiceClient.getTokenByTelegramId(request.getTelegramId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Проксировать валидацию токена")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        boolean isValid = authServiceClient.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @Operation(summary = "Обновление токена (refresh - через auth-сервис)")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        TokenResponse refreshed = authServiceClient.refreshToken(token);
        return ResponseEntity.ok(refreshed);
    }

    @Operation(summary = "Получить фиктивную информацию о токене")
    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> getTokenInfo(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        boolean isValid = authServiceClient.validateToken(token);
        Long telegramId = null;
        if (isValid) {
            telegramId = authServiceClient.getCurrentUserProfile(token).getTelegramId();
        }
        return ResponseEntity.ok(Map.of(
                "telegramId", telegramId,
                "valid", isValid,
                "tokenType", "JWT"
        ));
    }
}
