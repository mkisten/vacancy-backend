package com.mkisten.vacancybackend.service;

import com.mkisten.vacancybackend.client.AuthServiceClient;
import com.mkisten.vacancybackend.dto.SearchRequest;
import com.mkisten.vacancybackend.dto.TokenResponse;
import com.mkisten.vacancybackend.entity.UserSettings;
import com.mkisten.vacancybackend.entity.Vacancy;
import com.mkisten.vacancybackend.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VacancyAutoUpdater {

    private final UserSettingsRepository userSettingsRepository;
    private final VacancySmartService vacancySmartService;
    private final VacancyService vacancyService;
    private final AuthServiceClient authServiceClient;

    // Запуск раз в минуту (пример, можно настроить)
    @Scheduled(fixedRate = 60000)
    public void updateAllUsers() {
        log.info("== Автообновление вакансий для пользователей ==");
        List<UserSettings> settingsList = userSettingsRepository.findByAutoUpdateEnabledTrue();

        for (UserSettings settings : settingsList) {
            try {
                if (!shouldUpdateNow(settings)) continue;

                String token = getTokenForUser(settings);
                if (token == null) {
                    log.warn("Токен для пользователя {} не получен, пропускаем", settings.getTelegramId());
                    continue;
                }

                // Составляем SearchRequest из user settings:
                SearchRequest request = new SearchRequest();
                request.setQuery(settings.getSearchQuery());
                request.setDays(settings.getDays());
                request.setWorkTypes(settings.getWorkTypes());
                request.setCountries(settings.getCountries());
                request.setExcludeKeywords(settings.getExcludeKeywords()); // <---- Только строка!
                request.setTelegramNotify(settings.getTelegramNotify());

                // Поиск с учетом пользовательских настроек (по HH.ru + персон фильтрам)
                List<Vacancy> foundVacancies = vacancySmartService.searchWithUserSettings(request, token);

                // Сохраняем их в базу для пользователя
                vacancyService.saveVacancies(token, foundVacancies);

                // (Можно здесь обновить время последнего автообновления, если введёте такое поле!)
                // settings.setLastAutoUpdateAt(LocalDateTime.now());
                // userSettingsRepository.save(settings);

            } catch (Exception e) {
                log.error("Ошибка автообновления для user: {} — {}", settings.getTelegramId(), e.getMessage(), e);
            }
        }
        log.info("== Автообновление вакансий завершено ==");
    }

    /**
     * Получаем токен по telegramId через AuthServiceClient.
     */
    private String getTokenForUser(UserSettings settings) {
        try {
            Long telegramId = settings.getTelegramId();
            if (telegramId == null) return null;
            TokenResponse resp = authServiceClient.getTokenByTelegramId(telegramId);
            if (resp == null || resp.getToken() == null || resp.getToken().isBlank()) {
                log.warn("AuthServiceClient вернул пустой токен для {}", telegramId);
                return null;
            }
            return resp.getToken();
        } catch (Exception e) {
            log.error("getTokenForUser: ошибка получения токена для {}: {}", settings.getTelegramId(), e.getMessage());
            return null;
        }
    }


    /**
     * Реализация логики (например, по lastAutoUpdateAt/интервалу)
     * Пока просто всегда true. Можно легко доработать!
     */
    private boolean shouldUpdateNow(UserSettings settings) {
        // TODO: Можно добавить проверку времени последнего автоапдейта и нужного интервала
        return true;
    }
}
