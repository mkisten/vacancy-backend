package com.mkisten.vacancybackend.controller;

import com.mkisten.vacancybackend.client.AuthServiceClient;
import com.mkisten.vacancybackend.dto.AuthRequest;
import com.mkisten.vacancybackend.dto.AuthResponse;
import com.mkisten.vacancybackend.dto.TokenResponse;
import com.mkisten.vacancybackend.dto.ProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class VacancyAuthController {
    private final AuthServiceClient authServiceClient;

    /** Получить токен по Telegram ID (прокси) */
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> getToken(@RequestBody AuthRequest request) {
        // AuthRequest: { telegramId }
        TokenResponse tokenResponse = authServiceClient.getTokenByTelegramId(request.getTelegramId());
        return ResponseEntity.ok(tokenResponse);
    }

    /** Проверка токена (прокси) */
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        boolean valid = authServiceClient.validateToken(token);
        return ResponseEntity.ok(new AuthResponse(valid));
    }

    /** Получить профиль пользователя по токену (прокси) */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        ProfileResponse profile = authServiceClient.getCurrentUserProfile(token);
        return ResponseEntity.ok(profile);
    }

    /** Обновить профиль пользователя (прокси) */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ProfileResponse updateDto) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(authServiceClient.updateProfile(token, updateDto));
    }
}
