package com.mkisten.vacancybackend.controller;

import com.mkisten.vacancybackend.entity.UserSettings;
import com.mkisten.vacancybackend.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "API для управления настройками пользователя")
public class UserSettingsController {

    private final UserSettingsService settingsService;

    @Operation(summary = "Получить настройки пользователя")
    @GetMapping
    public ResponseEntity<UserSettings> getSettings(@AuthenticationPrincipal Long telegramId) {
        try {
            UserSettings settings = settingsService.getSettings(telegramId);
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("Error getting settings for user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Обновить настройки пользователя")
    @PutMapping
    public ResponseEntity<UserSettings> updateSettings(
            @AuthenticationPrincipal Long telegramId,
            @RequestBody UserSettings settings) {

        try {
            UserSettings updated = settingsService.updateSettings(telegramId, settings);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating settings for user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Настройка автообновления")
    @PostMapping("/auto-update")
    public ResponseEntity<Map<String, String>> setupAutoUpdate(
            @AuthenticationPrincipal Long telegramId,
            @RequestBody Map<String, Object> request) {

        try {
            Boolean enabled = (Boolean) request.get("enabled");
            Integer intervalMinutes = (Integer) request.get("intervalMinutes");

            if (enabled == null || intervalMinutes == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Both 'enabled' and 'intervalMinutes' are required"
                ));
            }

            settingsService.setupAutoUpdate(telegramId, enabled, intervalMinutes);

            return ResponseEntity.ok(Map.of(
                    "message", "Auto-update settings updated successfully",
                    "enabled", enabled.toString(),
                    "intervalMinutes", intervalMinutes.toString()
            ));

        } catch (Exception e) {
            log.error("Error setting up auto-update for user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to update auto-update settings",
                    "message", e.getMessage()
            ));
        }
    }
}
