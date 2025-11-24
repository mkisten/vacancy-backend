package com.mkisten.vacancybackend.service;

import com.mkisten.vacancybackend.client.AuthServiceClient;
import com.mkisten.vacancybackend.dto.ProfileResponse;
import com.mkisten.vacancybackend.entity.Vacancy;
import com.mkisten.vacancybackend.entity.VacancyStatus;
import com.mkisten.vacancybackend.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VacancyService {

    private final VacancyRepository vacancyRepository;
    private final AuthServiceClient authServiceClient;

    private Long getTelegramId(String token) {
        ProfileResponse profile = authServiceClient.getCurrentUserProfile(token);
        return profile.getTelegramId();
    }

    @Transactional
    public List<Vacancy> saveVacancies(String token, List<Vacancy> newVacancies) {
        Long telegramId = getTelegramId(token);
        if (newVacancies.isEmpty()) {
            return List.of();
        }

        Set<String> existingIds = vacancyRepository.findVacancyIdsByUser(telegramId);
        List<Vacancy> vacanciesToSave = newVacancies.stream()
                .filter(v -> !existingIds.contains(v.getId()))
                .peek(v -> {
                    v.setUserTelegramId(telegramId);
                    v.setStatus(VacancyStatus.NEW);
                    if (v.getLoadedAt() == null) {
                        v.setLoadedAt(java.time.LocalDateTime.now());
                    }
                })
                .collect(Collectors.toList());

        if (!vacanciesToSave.isEmpty()) {
            List<Vacancy> saved = vacancyRepository.saveAll(vacanciesToSave);
            log.info("Saved {} new vacancies for user {}", saved.size(), telegramId);
            return saved;
        }
        log.info("No new vacancies found for user {}", telegramId);
        return List.of();
    }

    @Transactional
    public void markAsSentToTelegram(String token, String vacancyId) {
        Long telegramId = getTelegramId(token);
        vacancyRepository.markAsSentToTelegram(telegramId, List.of(vacancyId));
        log.debug("Marked vacancy {} as sent to telegram for user {}", vacancyId, telegramId);
    }

    @Transactional
    public void markMultipleAsSentToTelegram(String token, List<String> vacancyIds) {
        if (!vacancyIds.isEmpty()) {
            Long telegramId = getTelegramId(token);
            vacancyRepository.markAsSentToTelegram(telegramId, vacancyIds);
            log.info("Marked {} vacancies as sent to telegram for user {}", vacancyIds.size(), telegramId);
        }
    }

    @Transactional
    public void markAsViewed(String token, String vacancyId) {
        Long telegramId = getTelegramId(token);
        vacancyRepository.updateStatus(telegramId, vacancyId, VacancyStatus.VIEWED);
        log.debug("Marked vacancy {} as viewed for user {}", vacancyId, telegramId);
    }

    @Transactional
    public void markMultipleAsViewed(String token, List<String> vacancyIds) {
        if (!vacancyIds.isEmpty()) {
            Long telegramId = getTelegramId(token);
            vacancyRepository.updateMultipleStatus(telegramId, vacancyIds, VacancyStatus.VIEWED);
            log.info("Marked {} vacancies as viewed for user {}", vacancyIds.size(), telegramId);
        }
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getUserVacancies(String token, VacancyStatus status) {
        Long telegramId = getTelegramId(token);
        if (status == null) {
            return vacancyRepository.findByUserTelegramIdOrderByPublishedAtDesc(telegramId);
        }
        return vacancyRepository.findByUserTelegramIdAndStatusOrderByPublishedAtDesc(telegramId, status);
    }

    @Transactional
    public void deleteVacancy(String token, String vacancyId) {
        Long telegramId = getTelegramId(token);
        vacancyRepository.deleteByUserAndId(telegramId, vacancyId);
        log.debug("Deleted vacancy {} for user {}", vacancyId, telegramId);
    }

    @Transactional(readOnly = true)
    public Long getNewVacanciesCount(String token) {
        Long telegramId = getTelegramId(token);
        return vacancyRepository.countNewVacancies(telegramId);
    }
}
