package com.mkisten.vacancybackend.service;

import com.mkisten.vacancybackend.dto.SearchRequest;
import com.mkisten.vacancybackend.entity.UserSettings;
import com.mkisten.vacancybackend.entity.Vacancy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VacancySmartService {
    private final UserSettingsService userSettingsService;
    private final HHruApiService hhruApiService;
    private final TelegramNotificationService telegramService;

    public List<Vacancy> searchWithUserSettings(SearchRequest request, String token) {
        UserSettings settings = userSettingsService.getSettings(token);

        // 1. Подмешиваем настройки пользователя, если каких-то полей не хватает в запросе из UI
        if (!StringUtils.hasText(request.getQuery()))
            request.setQuery(settings.getSearchQuery());
        if (request.getDays() == null)
            request.setDays(settings.getDays());
        if (request.getExcludeKeywords() == null || request.getExcludeKeywords().isEmpty())
            request.setExcludeKeywords(settings.getExcludeKeywords());
        if (request.getWorkTypes() == null || request.getWorkTypes().isEmpty())
            request.setWorkTypes(settings.getWorkTypes());
        if (request.getCountries() == null || request.getCountries().isEmpty())
            request.setCountries(settings.getCountries());
        if (request.getTelegramNotify() == null)
            request.setTelegramNotify(settings.getTelegramNotify());

        // ... другие поля по необходимости

        List<Vacancy> vacancies = hhruApiService.searchVacancies(request, token);

        // 2. Если включено уведомление в Telegram — отправить уведомление
        if (Boolean.TRUE.equals(settings.getTelegramNotify())) {
            telegramService.sendNewVacanciesNotification(token, vacancies);
        }

        return vacancies;
    }
}
