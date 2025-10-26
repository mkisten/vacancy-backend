package com.mkisten.vacancybackend.service;

import com.mkisten.vacancybackend.dto.SubscriptionStatusResponse;
import com.mkisten.vacancybackend.entity.UserSettings;
import com.mkisten.vacancybackend.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository settingsRepository;
    private final TokenManagerService tokenManagerService;
    private final TelegramNotificationService telegramService;

    @Transactional(readOnly = true)
    public UserSettings getSettings(Long telegramId) {
        return settingsRepository.findByTelegramId(telegramId)
                .orElseGet(() -> createDefaultSettings(telegramId));
    }

    /**
     * Проверка активной подписки пользователя
     */
    public boolean isSubscriptionActive(Long telegramId) {
        try {
            SubscriptionStatusResponse status = tokenManagerService.getSubscriptionStatus(telegramId);
            return status.isActive();
        } catch (Exception e) {
            log.error("Failed to check subscription status for user {}: {}", telegramId, e.getMessage());
            return false;
        }
    }

    /**
     * Получение информации о подписке
     */
    public SubscriptionStatusResponse getSubscriptionInfo(Long telegramId) {
        return tokenManagerService.getSubscriptionStatus(telegramId);
    }

    @Transactional
    public UserSettings updateSettings(Long telegramId, UserSettings newSettings) {
        UserSettings existingSettings = settingsRepository.findByTelegramId(telegramId)
                .orElseGet(() -> createDefaultSettings(telegramId));

        // Обновляем поля
        existingSettings.setSearchQuery(newSettings.getSearchQuery());
        existingSettings.setDays(newSettings.getDays());
        existingSettings.setExcludeKeywords(newSettings.getExcludeKeywords());
        existingSettings.setWorkTypes(newSettings.getWorkTypes());
        existingSettings.setCountries(newSettings.getCountries());
        existingSettings.setTelegramNotify(newSettings.getTelegramNotify());
        existingSettings.setTheme(newSettings.getTheme());

        UserSettings saved = settingsRepository.save(existingSettings);

        // Отправляем уведомление об обновлении настроек
        if (Boolean.TRUE.equals(saved.getTelegramNotify())) {
            try {
                telegramService.sendSettingsUpdatedNotification(telegramId);
            } catch (Exception e) {
                log.warn("Failed to send settings update notification to user {}", telegramId, e);
            }
        }

        log.info("Settings updated for user {}", telegramId);
        return saved;
    }

    @Transactional
    public void setupAutoUpdate(Long telegramId, Boolean enabled, Integer intervalMinutes) {
        UserSettings settings = getSettings(telegramId);
        settings.setAutoUpdateEnabled(enabled);
        settings.setAutoUpdateInterval(intervalMinutes);

        settingsRepository.save(settings);
        log.info("Auto-update settings updated for user {}: enabled={}, interval={}min",
                telegramId, enabled, intervalMinutes);
    }

    private UserSettings createDefaultSettings(Long telegramId) {
        UserSettings settings = new UserSettings(telegramId);
        return settingsRepository.save(settings);
    }
}
