package com.mkisten.vacancybackend.service;

import com.mkisten.vacancybackend.entity.Vacancy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TelegramNotificationService {

    private final TokenManagerService tokenManagerService;

    @Value("${app.telegram.max-vacancies-per-message:10}")
    private int maxVacanciesPerMessage;

    // Конструктор с dependency injection
    public TelegramNotificationService(TokenManagerService tokenManagerService) {
        this.tokenManagerService = tokenManagerService;
    }

    public void sendNewVacanciesNotification(Long telegramId, List<Vacancy> newVacancies) {
        if (newVacancies.isEmpty()) {
            return;
        }

        try {
            String message = formatNewVacanciesMessage(newVacancies);
            tokenManagerService.sendTelegramNotification(telegramId, message);
            log.info("New vacancies notification sent to user {}: {} vacancies",
                    telegramId, newVacancies.size());

        } catch (Exception e) {
            log.error("Failed to send new vacancies notification to user {}: {}",
                    telegramId, e.getMessage(), e);
        }
    }

    public void sendTextMessage(Long telegramId, String text) {
        try {
            tokenManagerService.sendTelegramNotification(telegramId, text);
            log.debug("Message sent to user {}", telegramId);
        } catch (Exception e) {
            log.error("Failed to send message to user {}: {}", telegramId, e.getMessage());
            throw new RuntimeException("Telegram notification failed", e);
        }
    }

    /**
     * Отправка тестового уведомления
     */
    public void sendTestNotification(Long telegramId) {
        String message = "🧪 <b>Тестовое уведомление</b>\n\n" +
                "Это тестовое сообщение от сервиса вакансий.\n" +
                "Если вы получили это сообщение, значит уведомления работают корректно! ✅";

        sendTextMessage(telegramId, message);
        log.info("Test notification sent to user {}", telegramId);
    }

    /**
     * Отправка уведомления об ошибке
     */
    public void sendErrorNotification(Long telegramId, String errorMessage) {
        String message = "❌ <b>Произошла ошибка</b>\n\n" +
                "При обработке вашего запроса возникла ошибка:\n" +
                "<code>" + escapeHtml(errorMessage) + "</code>\n\n" +
                "Пожалуйста, попробуйте позже или обратитесь в поддержку.";

        sendTextMessage(telegramId, message);
    }

    /**
     * Отправка уведомления об успешной настройке
     */
    public void sendSettingsUpdatedNotification(Long telegramId) {
        String message = "✅ <b>Настройки обновлены</b>\n\n" +
                "Ваши настройки поиска вакансий были успешно сохранены.\n" +
                "Автообновление будет работать в фоновом режиме.";

        sendTextMessage(telegramId, message);
    }

    /**
     * Отправка статистики
     */
    public void sendStatisticsNotification(Long telegramId, long totalVacancies, long newVacancies) {
        String message = "📊 <b>Статистика вакансий</b>\n\n" +
                "Всего вакансий: <b>" + totalVacancies + "</b>\n" +
                "Новых вакансий: <b>" + newVacancies + "</b>\n\n" +
                "Используйте приложение для просмотра деталей.";

        sendTextMessage(telegramId, message);
    }

    /**
     * Форматирование сообщения о новых вакансиях
     */
    private String formatNewVacanciesMessage(List<Vacancy> vacancies) {
        StringBuilder sb = new StringBuilder();

        if (vacancies.size() == 1) {
            sb.append("🎯 Найдена новая вакансия!\n\n");
        } else {
            sb.append("🎯 Найдено новых вакансий: ").append(vacancies.size()).append("\n\n");
        }

        // Показываем первые 5 вакансий
        int maxDisplay = 5;
        for (int i = 0; i < Math.min(vacancies.size(), maxDisplay); i++) {
            Vacancy vacancy = vacancies.get(i);
            sb.append(formatSingleVacancy(vacancy));

            // Разделитель между вакансиями (кроме последней)
            if (i < Math.min(vacancies.size(), maxDisplay) - 1) {
                sb.append("\n").append("─".repeat(30)).append("\n\n");
            }
        }

        // Сообщение о количестве оставшихся вакансий
        if (vacancies.size() > maxDisplay) {
            sb.append("\n\n📊 ... и еще ").append(vacancies.size() - maxDisplay)
                    .append(" вакансий в приложении");
        }

        // Добавляем призыв к действию
        sb.append("\n\n🚀 Открывайте приложение для просмотра всех вакансий!");

        return sb.toString();
    }

    /**
     * Форматирование одной вакансии с Markdown
     */
    private String formatSingleVacancy(Vacancy vacancy) {
        StringBuilder sb = new StringBuilder();

        // Заголовок (жирный через Markdown)
        sb.append("🎯 *").append(escapeMarkdown(vacancy.getTitle())).append("*\n");

        // Компания
        sb.append("🏢 *Компания:* ").append(escapeMarkdown(vacancy.getEmployer() != null ? vacancy.getEmployer() : "Не указана")).append("\n");

        // Город
        sb.append("📍 *Город:* ").append(escapeMarkdown(vacancy.getCity() != null ? vacancy.getCity() : "Не указан")).append("\n");

        // Формат работы
        String schedule = vacancy.getSchedule() != null ? formatSchedule(vacancy.getSchedule()) : "Не указан";
        sb.append("📊 *Формат:* ").append(escapeMarkdown(schedule)).append("\n");

        // Зарплата
        String salary = vacancy.getSalary() != null && !vacancy.getSalary().equals("Не указана") ?
                vacancy.getSalary() : "не указана";
        sb.append("💰 *Зарплата:* ").append(escapeMarkdown(salary)).append("\n");

        // Ссылка
        sb.append("🔗 *Ссылка:* ").append(vacancy.getUrl());

        return sb.toString();
    }

    /**
     * Экранирование символов для Markdown
     */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("*", "\\*")
                .replace("_", "\\_")
                .replace("`", "\\`")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    /**
     * Форматирование графика работы
     */
    private String formatSchedule(String schedule) {
        switch (schedule.toLowerCase()) {
            case "remote": return "🏠 Удаленная работа";
            case "fullDay": return "📅 Полный день";
            case "shift": return "🔄 Сменный график";
            case "flexible": return "⏰ Гибкий график";
            default: return schedule;
        }
    }

    /**
     * Экранирование HTML-символов
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}