package com.mkisten.vacancybackend.service;

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

    @Transactional
    public List<Vacancy> saveVacancies(Long telegramId, List<Vacancy> newVacancies) {
        if (newVacancies.isEmpty()) {
            return List.of();
        }

        // Получаем существующие ID вакансий пользователя
        Set<String> existingIds = vacancyRepository.findVacancyIdsByUser(telegramId);

        // Фильтруем только новые вакансии
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
    public void markAsViewed(Long telegramId, String vacancyId) {
        vacancyRepository.updateStatus(telegramId, vacancyId, VacancyStatus.VIEWED);
        log.debug("Marked vacancy {} as viewed for user {}", vacancyId, telegramId);
    }

    @Transactional
    public void markMultipleAsViewed(Long telegramId, List<String> vacancyIds) {
        if (!vacancyIds.isEmpty()) {
            vacancyRepository.updateMultipleStatus(telegramId, vacancyIds, VacancyStatus.VIEWED);
            log.info("Marked {} vacancies as viewed for user {}", vacancyIds.size(), telegramId);
        }
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getUserVacancies(Long telegramId, VacancyStatus status) {
        if (status == null) {
            return vacancyRepository.findByUserTelegramIdOrderByPublishedAtDesc(telegramId);
        }
        return vacancyRepository.findByUserTelegramIdAndStatusOrderByPublishedAtDesc(telegramId, status);
    }

    @Transactional
    public void deleteVacancy(Long telegramId, String vacancyId) {
        vacancyRepository.deleteByUserAndId(telegramId, vacancyId);
        log.debug("Deleted vacancy {} for user {}", vacancyId, telegramId);
    }

    @Transactional(readOnly = true)
    public Long getNewVacanciesCount(Long telegramId) {
        return vacancyRepository.countNewVacancies(telegramId);
    }
}
