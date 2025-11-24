package com.mkisten.vacancybackend.service;

import com.mkisten.vacancybackend.client.AuthServiceClient;
import com.mkisten.vacancybackend.entity.Vacancy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class TelegramNotificationService {

    private final AuthServiceClient authServiceClient;

    @Value("${app.telegram.max-vacancies-per-message:10}")
    private int maxVacanciesPerMessage;

    // dependency injection через конструктор
    public TelegramNotificationService(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    /** Отправка сообщений о новых вакансиях */
    public void sendNewVacanciesNotification(String userToken, List<Vacancy> newVacancies) {
        if (newVacancies.isEmpty()) return;
        try {
            String message = formatNewVacanciesMessage(newVacancies);
            sendTextMessage(userToken, message);
            log.info("New vacancies notification sent: {} vacancies", newVacancies.size());
        } catch (Exception e) {
            log.error("Failed to send new vacancies notification: {}", e.getMessage(), e);
        }
    }

    /** Универсальная отправка текста в Telegram */
    public void sendTextMessage(String userToken, String text) {
        try {
            authServiceClient.sendTelegramNotification(userToken, text);
            log.debug("Message sent to user via AuthService");
        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage());
            throw new RuntimeException("Telegram notification failed", e);
        }
    }

    public void sendTestNotification(String userToken) {
        String message = "🧪 <b>Тестовое уведомление</b>\n\n" +
                "Это тестовое сообщение от сервиса вакансий.\n" +
                "Если вы получили это сообщение, значит уведомления работают корректно! ✅";
        sendTextMessage(userToken, message);
        log.info("Test notification sent via AuthService");
    }

    public void sendErrorNotification(String userToken, String errorMessage) {
        String message = "❌ <b>Произошла ошибка</b>\n\n" +
                "При обработке вашего запроса возникла ошибка:\n" +
                "<code>" + escapeHtml(errorMessage) + "</code>\n\n" +
                "Пожалуйста, попробуйте позже или обратитесь в поддержку.";
        sendTextMessage(userToken, message);
    }

    public void sendSettingsUpdatedNotification(String userToken) {
        String message = "✅ <b>Настройки обновлены</b>\n\n" +
                "Ваши настройки поиска вакансий были успешно сохранены.\n" +
                "Автообновление будет работать в фоновом режиме.";
        sendTextMessage(userToken, message);
    }

    public void sendStatisticsNotification(String userToken, long totalVacancies, long newVacancies) {
        String message = "📊 <b>Статистика вакансий</b>\n\n" +
                "Всего вакансий: <b>" + totalVacancies + "</b>\n" +
                "Новых вакансий: <b>" + newVacancies + "</b>\n\n" +
                "Используйте приложение для просмотра деталей.";
        sendTextMessage(userToken, message);
    }

    private String formatNewVacanciesMessage(List<Vacancy> vacancies) {
        StringBuilder sb = new StringBuilder();
        if (vacancies.size() == 1) {
            sb.append("🎯 Найдена новая вакансия!\n\n");
        } else {
            sb.append("🎯 Найдено новых вакансий: ").append(vacancies.size()).append("\n\n");
        }
        int maxDisplay = 5;
        for (int i = 0; i < Math.min(vacancies.size(), maxDisplay); i++) {
            Vacancy vacancy = vacancies.get(i);
            sb.append(formatSingleVacancy(vacancy));
            if (i < Math.min(vacancies.size(), maxDisplay) - 1) {
                sb.append("\n").append("─".repeat(30)).append("\n\n");
            }
        }
        if (vacancies.size() > maxDisplay) {
            sb.append("\n\n📊 ... и еще ").append(vacancies.size() - maxDisplay)
                    .append(" вакансий в приложении");
        }
        sb.append("\n\n🚀 Открывайте приложение для просмотра всех вакансий!");
        return sb.toString();
    }

    private String formatSingleVacancy(Vacancy vacancy) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎯 *").append(escapeMarkdown(vacancy.getTitle())).append("*\n");
        sb.append("🏢 *Компания:* ").append(escapeMarkdown(vacancy.getEmployer() != null ? vacancy.getEmployer() : "Не указана")).append("\n");
        sb.append("📍 *Город:* ").append(escapeMarkdown(vacancy.getCity() != null ? vacancy.getCity() : "Не указан")).append("\n");
        String schedule = vacancy.getSchedule() != null ? formatSchedule(vacancy.getSchedule()) : "Не указан";
        sb.append("📊 *Формат:* ").append(escapeMarkdown(schedule)).append("\n");
        String salary = vacancy.getSalary() != null && !vacancy.getSalary().equals("Не указана") ?
                vacancy.getSalary() : "не указана";
        sb.append("💰 *Зарплата:* ").append(escapeMarkdown(salary)).append("\n");
        sb.append("🔗 *Ссылка:* ").append(vacancy.getUrl());
        return sb.toString();
    }

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

    private String formatSchedule(String schedule) {
        switch (schedule.toLowerCase()) {
            case "remote": return "🏠 Удаленная работа";
            case "fullDay": return "📅 Полный день";
            case "shift": return "🔄 Сменный график";
            case "flexible": return "⏰ Гибкий график";
            default: return schedule;
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
