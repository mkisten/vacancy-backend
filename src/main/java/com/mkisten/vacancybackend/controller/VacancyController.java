package com.mkisten.vacancybackend.controller;

import com.mkisten.vacancybackend.dto.SearchRequest;
import com.mkisten.vacancybackend.dto.VacancyResponse;
import com.mkisten.vacancybackend.entity.Vacancy;
import com.mkisten.vacancybackend.entity.VacancyStatus;
import com.mkisten.vacancybackend.service.HHruApiService;
import com.mkisten.vacancybackend.service.TelegramNotificationService;
import com.mkisten.vacancybackend.service.VacancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/vacancies")
@RequiredArgsConstructor
@Tag(name = "Vacancies", description = "API для управления вакансиями")
public class VacancyController {

    private final VacancyService vacancyService;
    private final HHruApiService hhruApiService;
    private final TelegramNotificationService telegramService;

    @Operation(summary = "Поиск вакансий")
    @PostMapping("/search")
    public ResponseEntity<List<VacancyResponse>> searchVacancies(
            @AuthenticationPrincipal Long telegramId,
            @RequestBody SearchRequest request) {

        log.info("Search request from user {}: {}", telegramId, request.getQuery());

        try {
            // Устанавливаем telegramId из аутентификации
            request.setTelegramId(telegramId);

            // Ищем вакансии на HH.ru
            List<Vacancy> foundVacancies = hhruApiService.searchVacancies(request);

            // Сохраняем в базу
            List<Vacancy> savedVacancies = vacancyService.saveVacancies(telegramId, foundVacancies);

            // Отправляем уведомления о новых вакансиях
            List<Vacancy> newVacancies = savedVacancies.stream()
                    .filter(v -> v.getStatus() == VacancyStatus.NEW)
                    .collect(Collectors.toList());

            if (!newVacancies.isEmpty() && Boolean.TRUE.equals(request.getTelegramNotify())) {
                telegramService.sendNewVacanciesNotification(telegramId, newVacancies);
            }

            // Конвертируем в DTO
            List<VacancyResponse> response = savedVacancies.stream()
                    .map(VacancyResponse::new)
                    .collect(Collectors.toList());

            log.info("Search completed for user {}. Found: {}, New: {}",
                    telegramId, foundVacancies.size(), newVacancies.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching vacancies for user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Получить вакансии пользователя")
    @GetMapping
    public ResponseEntity<List<VacancyResponse>> getUserVacancies(
            @AuthenticationPrincipal Long telegramId,
            @RequestParam(required = false) VacancyStatus status) {

        try {
            List<Vacancy> vacancies = vacancyService.getUserVacancies(telegramId, status);
            List<VacancyResponse> response = vacancies.stream()
                    .map(VacancyResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting vacancies for user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Пометить вакансию просмотренной")
    @PostMapping("/{vacancyId}/mark-viewed")
    public ResponseEntity<Void> markAsViewed(
            @AuthenticationPrincipal Long telegramId,
            @PathVariable String vacancyId) {

        try {
            vacancyService.markAsViewed(telegramId, vacancyId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error marking vacancy as viewed for user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Пометить несколько вакансий просмотренными")
    @PostMapping("/mark-multiple-viewed")
    public ResponseEntity<Void> markMultipleAsViewed(
            @AuthenticationPrincipal Long telegramId,
            @RequestBody List<String> vacancyIds) {

        try {
            vacancyService.markMultipleAsViewed(telegramId, vacancyIds);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error marking multiple vacancies for user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Удалить вакансию")
    @DeleteMapping("/{vacancyId}")
    public ResponseEntity<Void> deleteVacancy(
            @AuthenticationPrincipal Long telegramId,
            @PathVariable String vacancyId) {

        try {
            vacancyService.deleteVacancy(telegramId, vacancyId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error deleting vacancy for user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Получить количество новых вакансий")
    @GetMapping("/count/new")
    public ResponseEntity<Long> getNewVacanciesCount(@AuthenticationPrincipal Long telegramId) {
        try {
            Long count = vacancyService.getNewVacanciesCount(telegramId);
            return ResponseEntity.ok(count);

        } catch (Exception e) {
            log.error("Error getting new vacancies count for user {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}