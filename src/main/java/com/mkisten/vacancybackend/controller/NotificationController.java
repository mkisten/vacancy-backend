package com.mkisten.vacancybackend.controller;

import com.mkisten.vacancybackend.service.TelegramNotificationService;
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
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "API для управления уведомлениями")
public class NotificationController {

    private final TelegramNotificationService telegramService;

    @Operation(summary = "Отправить тестовое уведомление")
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestNotification(
            @AuthenticationPrincipal Long telegramId) {

        try {
            telegramService.sendTestNotification(telegramId);

            return ResponseEntity.ok(Map.of(
                    "message", "Test notification sent successfully",
                    "telegramId", telegramId.toString()
            ));

        } catch (Exception e) {
            log.error("Error sending test notification to user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to send test notification",
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Отправить кастомное уведомление")
    @PostMapping("/custom")
    public ResponseEntity<Map<String, String>> sendCustomNotification(
            @AuthenticationPrincipal Long telegramId,
            @RequestBody Map<String, String> request) {

        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Message is required"
                ));
            }

            telegramService.sendTextMessage(telegramId, message);

            return ResponseEntity.ok(Map.of(
                    "message", "Custom notification sent successfully",
                    "telegramId", telegramId.toString(),
                    "messageLength", String.valueOf(message.length())
            ));

        } catch (Exception e) {
            log.error("Error sending custom notification to user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to send custom notification",
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Отправить уведомление об ошибке")
    @PostMapping("/error")
    public ResponseEntity<Map<String, String>> sendErrorNotification(
            @AuthenticationPrincipal Long telegramId,
            @RequestBody Map<String, String> request) {

        try {
            String errorMessage = request.get("error");
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Error message is required"
                ));
            }

            telegramService.sendErrorNotification(telegramId, errorMessage);

            return ResponseEntity.ok(Map.of(
                    "message", "Error notification sent successfully",
                    "telegramId", telegramId.toString()
            ));

        } catch (Exception e) {
            log.error("Error sending error notification to user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to send error notification",
                    "message", e.getMessage()
            ));
        }
    }
}
