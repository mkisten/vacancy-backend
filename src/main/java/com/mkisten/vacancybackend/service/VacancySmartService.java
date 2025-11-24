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

    /**
     * Выполняет поиск вакансий с подмешиванием user-настроек и отправляет только новые в Telegram.
     *
     * @param request SearchRequest, возможно частично заполненный (UI).
     * @param token access token пользователя.
     * @param telegramId id пользователя для фильтрации/рассылки.
     * @return List<Vacancy> — список всех найденных (и новых, и уже отправленных).
     */
    public List<Vacancy> searchWithUserSettings(SearchRequest request, String token, Long telegramId) {
        UserSettings settings = userSettingsService.getSettings(token);

        // Подмешивание недостающих настроек из UserSettings
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

        // Поиск вакансий через hhruApiService (использует все фильтры)
        List<Vacancy> foundVacancies = hhruApiService.searchVacancies(request, token);

        // Отправить только новые вакансии (централизовано, только если активен notify)
        if (Boolean.TRUE.equals(settings.getTelegramNotify())) {
            telegramService.sendAllUnsentVacanciesToTelegram(token, telegramId);
        }

        // Возвращаем все найденные (можно возвращать только новые по желанию)
        return foundVacancies;
    }
}
